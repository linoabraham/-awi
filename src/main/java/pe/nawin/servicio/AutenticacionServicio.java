package pe.nawin.servicio;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.dto.respuesta.DispositivoRespuesta;
import pe.nawin.dto.respuesta.EventoSeguridadRespuesta;
import pe.nawin.dto.respuesta.TokenRespuesta;
import pe.nawin.dto.respuesta.UsuarioActualRespuesta;
import pe.nawin.dto.solicitud.CambiarClaveRequest;
import pe.nawin.dto.solicitud.IniciarSesionRequest;
import pe.nawin.dto.solicitud.MfaConfigurarRequest;
import pe.nawin.dto.solicitud.MfaVerificarRequest;
import pe.nawin.dto.solicitud.RenovarTokenRequest;
import pe.nawin.entidad.TokenRefresco;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.CategoriaNotificacion;
import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.enumeracion.TipoCliente;
import pe.nawin.enumeracion.TipoEventoSeguridad;
import pe.nawin.enumeracion.TipoMfa;
import pe.nawin.utilidad.CifradoServicio;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.repositorio.TokenRefrescoRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.seguridad.JwtServicio;
import pe.nawin.seguridad.TotpServicio;
import pe.nawin.utilidad.ContextoSeguridad;
import pe.nawin.utilidad.HashServicio;

@Service
public class AutenticacionServicio {

	private final AuthenticationManager authenticationManager;
	private final UsuarioRepositorio usuarioRepositorio;
	private final TokenRefrescoRepositorio tokenRefrescoRepositorio;
	private final PasswordEncoder passwordEncoder;
	private final JwtServicio jwtServicio;
	private final HashServicio hashServicio;
	private final TotpServicio totpServicio;
	private final CifradoServicio cifradoServicio;
	private final NawinPropiedades propiedades;
	private final AuditoriaSeguridadServicio auditoria;
	private final NotificacionPushServicio notificacionPush;
	private final SecureRandom secureRandom = new SecureRandom();

	/** Máximo de dispositivos activos simultáneos por cuenta. */
	private static final int MAX_DISPOSITIVOS = 2;

	/** Estado de un dispositivo al iniciar sesión, para decidir auditoría y límite. */
	private enum EstadoDispositivo { NUEVO, CONOCIDO, LIMITE }

	public AutenticacionServicio(AuthenticationManager authenticationManager, UsuarioRepositorio usuarioRepositorio,
			TokenRefrescoRepositorio tokenRefrescoRepositorio, PasswordEncoder passwordEncoder, JwtServicio jwtServicio,
			HashServicio hashServicio, TotpServicio totpServicio, CifradoServicio cifradoServicio,
			NawinPropiedades propiedades, AuditoriaSeguridadServicio auditoria,
			NotificacionPushServicio notificacionPush) {
		this.authenticationManager = authenticationManager;
		this.usuarioRepositorio = usuarioRepositorio;
		this.tokenRefrescoRepositorio = tokenRefrescoRepositorio;
		this.passwordEncoder = passwordEncoder;
		this.jwtServicio = jwtServicio;
		this.hashServicio = hashServicio;
		this.totpServicio = totpServicio;
		this.cifradoServicio = cifradoServicio;
		this.propiedades = propiedades;
		this.auditoria = auditoria;
		this.notificacionPush = notificacionPush;
	}

	// noRollbackFor: el conteo de intentos fallidos y el bloqueo temporal deben
	// persistir aunque el login termine lanzando una excepción.
	@Transactional(noRollbackFor = NawinException.class)
	public TokenRespuesta iniciarSesion(IniciarSesionRequest request, String ip, String agenteUsuario) {
		// El identificador puede ser nombre de usuario o correo.
		Usuario usuario = usuarioRepositorio.findByNombreUsuario(request.nombreUsuario())
				.or(() -> usuarioRepositorio.findByCorreoIgnoreCase(request.nombreUsuario()))
				.orElse(null);
		if (usuario != null && usuario.getBloqueadoHasta() != null
				&& usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
			throw new NawinException(CodigoError.AUTH_004);
		}
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.nombreUsuario(), request.clave()));
		} catch (AuthenticationException ex) {
			// Respuesta genérica AUTH_001 para no revelar si el usuario existe ni su estado.
			registrarIntentoFallido(usuario);
			throw new NawinException(CodigoError.AUTH_001);
		}
		if (usuario == null || usuario.getEstado() != EstadoUsuario.ACTIVO) {
			throw new NawinException(CodigoError.AUTH_001);
		}
		if (!usuario.isCorreoVerificado()) {
			throw new NawinException(CodigoError.VER_002);
		}
		usuario.setUltimoAcceso(LocalDateTime.now());
		usuario.setIntentosFallidos(0);
		usuario.setBloqueadoHasta(null);
		// Control de dispositivos: máximo MAX_DISPOSITIVOS activos por cuenta.
		EstadoDispositivo estado = evaluarDispositivo(usuario, request.installationId());
		if (estado == EstadoDispositivo.LIMITE) {
			auditoria.registrar(usuario.getIdUsuario(), TipoEventoSeguridad.DEVICE_LIMIT_REACHED,
					request.installationId(), ip);
			throw new NawinException(CodigoError.DEV_001);
		}
		// La sesión (token de refresco) se crea primero para usar su id como sid del access token.
		String tokenRefresco = nuevoTokenPlano();
		TokenRefresco sesion = crearTokenRefresco(usuario, tokenRefresco, ip, agenteUsuario,
				request.tipoClienteEfectivo(), request.installationId(), request.nombreDispositivo(),
				request.modelo(), request.plataforma());
		String tokenAcceso = jwtServicio.generarTokenAcceso(usuario, sesion.getIdTokenRefresco(),
				request.installationId());
		auditoria.registrar(usuario.getIdUsuario(), TipoEventoSeguridad.LOGIN_SUCCESS,
				request.installationId(), ip);
		if (estado == EstadoDispositivo.NUEVO) {
			auditoria.registrar(usuario.getIdUsuario(), TipoEventoSeguridad.NEW_DEVICE_LOGIN,
					request.installationId(), ip, request.nombreDispositivo());
			// Avisa a los demás dispositivos del usuario del nuevo inicio de sesión.
			String equipo = request.nombreDispositivo() != null ? request.nombreDispositivo() : "un dispositivo nuevo";
			notificacionPush.enviarAUsuario(usuario.getIdUsuario(), CategoriaNotificacion.SEGURIDAD,
					"Nuevo inicio de sesión", "Se inició sesión en " + equipo + ".", Map.of());
		}
		return new TokenRespuesta(tokenAcceso, tokenRefresco, jwtServicio.accessMinutos() * 60, usuarioActual(usuario));
	}

	/** Suma un intento fallido y bloquea la cuenta temporalmente al superar el máximo. */
	private void registrarIntentoFallido(Usuario usuario) {
		if (usuario == null) {
			return;
		}
		int intentos = usuario.getIntentosFallidos() + 1;
		if (intentos >= propiedades.seguridad().maxIntentosLogin()) {
			usuario.setIntentosFallidos(0);
			usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(propiedades.seguridad().bloqueoMinutos()));
		} else {
			usuario.setIntentosFallidos(intentos);
		}
	}

	@Transactional
	public TokenRespuesta renovar(RenovarTokenRequest request, String ip, String agenteUsuario) {
		String hash = hashServicio.sha256(request.tokenRefresco());
		TokenRefresco anterior = tokenRefrescoRepositorio.findByTokenHash(hash)
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
		if (anterior.isRevocado()) {
			// Reuso de un token ya rotado: posible robo. Se cierran todas las sesiones del usuario.
			revocarTodas(anterior.getUsuario().getIdUsuario());
			throw new NawinException(CodigoError.AUTH_002);
		}
		if (anterior.getFechaExpiracion().isBefore(LocalDateTime.now())) {
			anterior.setRevocado(true);
			throw new NawinException(CodigoError.AUTH_002);
		}
		anterior.setRevocado(true);
		Usuario usuario = anterior.getUsuario();
		if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
			throw new NawinException(CodigoError.USR_001);
		}
		// Rotación con expiración deslizante: el nuevo token vuelve a durar el plazo completo de su tipo.
		// El dispositivo se hereda del token anterior para no perder la identidad de la sesión.
		String tokenRefresco = nuevoTokenPlano();
		TokenRefresco sesion = crearTokenRefresco(usuario, tokenRefresco, ip, agenteUsuario,
				anterior.getTipoCliente(), anterior.getInstallationId(), anterior.getNombreDispositivo(),
				anterior.getModelo(), anterior.getPlataforma());
		String tokenAcceso = jwtServicio.generarTokenAcceso(usuario, sesion.getIdTokenRefresco(),
				anterior.getInstallationId());
		return new TokenRespuesta(tokenAcceso, tokenRefresco, jwtServicio.accessMinutos() * 60, usuarioActual(usuario));
	}

	@Transactional
	public void cerrarSesion(RenovarTokenRequest request) {
		String hash = hashServicio.sha256(request.tokenRefresco());
		tokenRefrescoRepositorio.findByTokenHashAndRevocadoFalse(hash).ifPresent(token -> token.setRevocado(true));
	}

	@Transactional(readOnly = true)
	public UsuarioActualRespuesta miCuenta() {
		Usuario usuario = usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
		return usuarioActual(usuario);
	}

	@Transactional
	public void cambiarClave(CambiarClaveRequest request) {
		Usuario usuario = usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
		if (!passwordEncoder.matches(request.claveActual(), usuario.getClaveHash())) {
			throw new NawinException(CodigoError.AUTH_001);
		}
		validarClave(request.claveNueva());
		usuario.setClaveHash(passwordEncoder.encode(request.claveNueva()));
		revocarTodas(usuario.getIdUsuario());
		auditoria.registrar(usuario.getIdUsuario(), TipoEventoSeguridad.PASSWORD_CHANGED, null, null);
	}

	private void revocarTodas(Long idUsuario) {
		tokenRefrescoRepositorio.findByUsuario_IdUsuarioAndRevocadoFalse(idUsuario)
				.forEach(token -> token.setRevocado(true));
	}

	/**
	 * Inicia la configuración de MFA por TOTP: genera un secreto aleatorio, lo
	 * guarda cifrado (aún sin habilitar) y devuelve el secreto en Base32 y la URI
	 * otpauth:// para mostrar el QR en una app tipo Google Authenticator. El MFA
	 * recién queda activo cuando el usuario confirma un código con
	 * {@link #verificarMfa}.
	 */
	@Transactional
	public Map<String, Object> configurarMfa(MfaConfigurarRequest request) {
		if (request.tipoMfa() != TipoMfa.TOTP) {
			throw new NawinException(CodigoError.GEN_001, "Por ahora solo se admite MFA por aplicación (TOTP).");
		}
		Usuario usuario = usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
		String secreto = totpServicio.generarSecreto();
		usuario.setTipoMfa(TipoMfa.TOTP);
		usuario.setMfaHabilitado(false);
		usuario.setMfaSecretoCifrado(cifradoServicio.cifrar(secreto));
		String uri = totpServicio.construirUriOtpauth(propiedades.empresa().nombre(), usuario.getCorreo(), secreto);
		return Map.of("tipoMfa", TipoMfa.TOTP, "secreto", secreto, "uriOtpauth", uri);
	}

	@Transactional
	public Map<String, Object> verificarMfa(MfaVerificarRequest request) {
		Usuario usuario = usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
		if (usuario.getMfaSecretoCifrado() == null || usuario.getTipoMfa() != TipoMfa.TOTP) {
			throw new NawinException(CodigoError.MFA_001, "Primero debes configurar el MFA.");
		}
		String secreto = cifradoServicio.descifrar(usuario.getMfaSecretoCifrado());
		if (!totpServicio.verificar(secreto, request.codigo())) {
			throw new NawinException(CodigoError.MFA_001);
		}
		usuario.setMfaHabilitado(true);
		return Map.of("mfaHabilitado", true);
	}

	/** Genera un valor aleatorio para el token de refresco (se entrega en claro y se guarda solo su hash). */
	private String nuevoTokenPlano() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Persiste la sesión (token de refresco) y la devuelve para conocer su id (sid del access token). */
	private TokenRefresco crearTokenRefresco(Usuario usuario, String tokenPlano, String ip, String agenteUsuario,
			TipoCliente tipoCliente, String installationId, String nombreDispositivo, String modelo,
			String plataforma) {
		TokenRefresco token = new TokenRefresco();
		token.setUsuario(usuario);
		token.setTokenHash(hashServicio.sha256(tokenPlano));
		token.setFechaExpiracion(LocalDateTime.now().plus(jwtServicio.duracionRefresh(tipoCliente)));
		token.setDireccionIp(ip == null ? "0.0.0.0" : ip);
		token.setAgenteUsuario(agenteUsuario);
		token.setTipoCliente(tipoCliente);
		token.setInstallationId(installationId);
		token.setNombreDispositivo(nombreDispositivo);
		token.setModelo(modelo);
		token.setPlataforma(plataforma);
		token.setUltimaActividad(LocalDateTime.now());
		return tokenRefrescoRepositorio.save(token);
	}

	// ------------------------------------------------------------------
	// Gestión de dispositivos (máximo 2 activos por cuenta)
	// ------------------------------------------------------------------

	/** Sesiones vigentes (ni revocadas ni vencidas) del usuario. */
	private List<TokenRefresco> sesionesVigentes(Long idUsuario) {
		return tokenRefrescoRepositorio
				.findByUsuario_IdUsuarioAndRevocadoFalseAndFechaExpiracionAfter(idUsuario, LocalDateTime.now());
	}

	/**
	 * Evalúa la regla de máximo 2 dispositivos SIN lanzar excepción (para poder
	 * auditar antes de rechazar). Si el dispositivo ya es conocido, revoca sus
	 * sesiones previas (re-login en el mismo equipo) y devuelve CONOCIDO; si es
	 * nuevo y aún hay cupo devuelve NUEVO; si es nuevo y ya hay 2 devuelve LIMITE.
	 */
	private EstadoDispositivo evaluarDispositivo(Usuario usuario, String installationId) {
		if (installationId == null || installationId.isBlank()) {
			return EstadoDispositivo.CONOCIDO; // Clientes web/antiguos: sin control por dispositivo.
		}
		List<TokenRefresco> vigentes = sesionesVigentes(usuario.getIdUsuario());
		Set<String> dispositivos = vigentes.stream()
				.map(TokenRefresco::getInstallationId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (dispositivos.contains(installationId)) {
			// Mismo dispositivo: revoca sus sesiones anteriores (una sesión por equipo).
			vigentes.stream()
					.filter(t -> installationId.equals(t.getInstallationId()))
					.forEach(t -> t.setRevocado(true));
			return EstadoDispositivo.CONOCIDO;
		}
		if (dispositivos.size() >= MAX_DISPOSITIVOS) {
			return EstadoDispositivo.LIMITE;
		}
		return EstadoDispositivo.NUEVO;
	}

	/** Arma la lista de dispositivos activos (uno por installationId, el más reciente). */
	private List<DispositivoRespuesta> listarDispositivos(Long idUsuario, String installationIdActual) {
		Map<String, TokenRefresco> porDispositivo = new LinkedHashMap<>();
		sesionesVigentes(idUsuario).stream()
				.filter(t -> t.getInstallationId() != null)
				.sorted(Comparator.comparing(TokenRefresco::getFechaCreacion).reversed())
				.forEach(t -> porDispositivo.putIfAbsent(t.getInstallationId(), t));
		return porDispositivo.values().stream()
				.map(t -> new DispositivoRespuesta(
						t.getInstallationId(),
						t.getNombreDispositivo(),
						t.getModelo(),
						t.getPlataforma(),
						t.getDireccionIp(),
						t.getUltimaActividad(),
						t.getFechaCreacion(),
						t.getInstallationId().equals(installationIdActual)))
				.toList();
	}

	/** Lista los dispositivos activos del usuario autenticado (uno por installationId). */
	@Transactional(readOnly = true)
	public List<DispositivoRespuesta> misDispositivos(String installationIdActual) {
		Long idUsuario = ContextoSeguridad.usuarioActual().idUsuario();
		return listarDispositivos(idUsuario, installationIdActual);
	}

	/** Últimos eventos de auditoría de seguridad del usuario autenticado. */
	@Transactional(readOnly = true)
	public List<EventoSeguridadRespuesta> misEventosSeguridad() {
		Long idUsuario = ContextoSeguridad.usuarioActual().idUsuario();
		return auditoria.ultimosEventos(idUsuario, 50).stream()
				.map(e -> new EventoSeguridadRespuesta(
						e.getTipo().name(),
						e.getTipo().descripcion(),
						e.getInstallationId(),
						e.getDireccionIp(),
						e.getDetalle(),
						e.getFechaCreacion()))
				.toList();
	}

	/**
	 * Valida usuario+clave sin crear sesión. Se usa en el flujo de "elegir qué
	 * dispositivo cerrar" cuando el login rebota por DEV_001 (el usuario aún no
	 * tiene sesión, pero sus credenciales sí son válidas).
	 */
	private Usuario autenticar(String nombreUsuario, String clave) {
		Usuario usuario = usuarioRepositorio.findByNombreUsuario(nombreUsuario)
				.or(() -> usuarioRepositorio.findByCorreoIgnoreCase(nombreUsuario))
				.orElse(null);
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(nombreUsuario, clave));
		} catch (AuthenticationException ex) {
			throw new NawinException(CodigoError.AUTH_001);
		}
		if (usuario == null || usuario.getEstado() != EstadoUsuario.ACTIVO) {
			throw new NawinException(CodigoError.AUTH_001);
		}
		return usuario;
	}

	/** Dispositivos activos revalidando credenciales (para el selector del login-límite). */
	@Transactional(readOnly = true)
	public List<DispositivoRespuesta> dispositivosParaGestionar(String nombreUsuario, String clave) {
		Usuario usuario = autenticar(nombreUsuario, clave);
		return listarDispositivos(usuario.getIdUsuario(), null);
	}

	/** Cierra un dispositivo revalidando credenciales (para el selector del login-límite). */
	@Transactional
	public void cerrarDispositivoConCredenciales(String nombreUsuario, String clave, String installationId) {
		Usuario usuario = autenticar(nombreUsuario, clave);
		sesionesVigentes(usuario.getIdUsuario()).stream()
				.filter(t -> installationId.equals(t.getInstallationId()))
				.forEach(t -> t.setRevocado(true));
		auditoria.registrar(usuario.getIdUsuario(), TipoEventoSeguridad.SESSION_REVOKED, installationId, null,
				"Cierre desde el selector de login");
	}

	/** Cierra (revoca) todas las sesiones de un dispositivo del usuario autenticado. */
	@Transactional
	public void cerrarDispositivo(String installationId) {
		Long idUsuario = ContextoSeguridad.usuarioActual().idUsuario();
		sesionesVigentes(idUsuario).stream()
				.filter(t -> installationId.equals(t.getInstallationId()))
				.forEach(t -> t.setRevocado(true));
		auditoria.registrar(idUsuario, TipoEventoSeguridad.SESSION_REVOKED, installationId, null);
	}

	/** Cierra todas las sesiones del usuario salvo la del dispositivo actual. */
	@Transactional
	public void cerrarOtrosDispositivos(String installationIdActual) {
		Long idUsuario = ContextoSeguridad.usuarioActual().idUsuario();
		sesionesVigentes(idUsuario).stream()
				.filter(t -> t.getInstallationId() == null
						|| !t.getInstallationId().equals(installationIdActual))
				.forEach(t -> t.setRevocado(true));
		auditoria.registrar(idUsuario, TipoEventoSeguridad.LOGOUT_ALL_DEVICES, installationIdActual, null);
	}

	private UsuarioActualRespuesta usuarioActual(Usuario usuario) {
		return new UsuarioActualRespuesta(
				usuario.getIdUsuario(),
				usuario.getNombreUsuario(),
				usuario.getNombres(),
				usuario.getApellidos(),
				usuario.getCorreo(),
				usuario.getCelular(),
				usuario.getRol().getCodigo(),
				usuario.getEstado(),
				usuario.isMfaHabilitado(),
				usuario.isCorreoVerificado());
	}

	public void validarClave(String clave) {
		if (clave == null || clave.length() < 10 || !clave.matches(".*[A-Z].*") || !clave.matches(".*[a-z].*")
				|| !clave.matches(".*\\d.*")) {
			throw new NawinException(CodigoError.GEN_001, "La clave debe tener al menos 10 caracteres, mayúsculas, minúsculas y números.");
		}
	}
}
