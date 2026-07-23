package pe.nawin.servicio;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.entidad.CodigoVerificacion;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.TipoCodigoVerificacion;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.repositorio.CodigoVerificacionRepositorio;
import pe.nawin.repositorio.TokenRefrescoRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.utilidad.HashServicio;

/**
 * Códigos de un solo uso enviados por correo: verificación de cuenta nueva y
 * recuperación de clave. El código tiene 4 caracteres (letras y números, sin
 * caracteres confusos como 0/O o 1/I), se guarda hasheado, vence a los pocos
 * minutos y se bloquea tras varios intentos fallidos.
 */
@Service
public class VerificacionCorreoServicio {

	private static final Logger log = LoggerFactory.getLogger(VerificacionCorreoServicio.class);

	// Sin 0, O, 1, I, L para que el código sea fácil de leer y escribir.
	private static final String LETRAS = "ABCDEFGHJKMNPQRSTUVWXYZ";
	private static final String NUMEROS = "23456789";
	private static final String ALFABETO = LETRAS + NUMEROS;

	private final CodigoVerificacionRepositorio codigoRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final TokenRefrescoRepositorio tokenRefrescoRepositorio;
	private final CorreoServicio correoServicio;
	private final HashServicio hashServicio;
	private final PasswordEncoder passwordEncoder;
	private final AutenticacionServicio autenticacionServicio;
	private final NawinPropiedades propiedades;
	private final SecureRandom secureRandom = new SecureRandom();

	public VerificacionCorreoServicio(CodigoVerificacionRepositorio codigoRepositorio,
			UsuarioRepositorio usuarioRepositorio, TokenRefrescoRepositorio tokenRefrescoRepositorio,
			CorreoServicio correoServicio, HashServicio hashServicio, PasswordEncoder passwordEncoder,
			AutenticacionServicio autenticacionServicio, NawinPropiedades propiedades) {
		this.codigoRepositorio = codigoRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.tokenRefrescoRepositorio = tokenRefrescoRepositorio;
		this.correoServicio = correoServicio;
		this.hashServicio = hashServicio;
		this.passwordEncoder = passwordEncoder;
		this.autenticacionServicio = autenticacionServicio;
		this.propiedades = propiedades;
	}

	/**
	 * Genera y envía el código de verificación tras el registro público. Si el
	 * envío falla la cuenta ya quedó creada, así que no se propaga el error: el
	 * usuario puede pedir un reenvío desde la pantalla de verificación.
	 */
	@Transactional
	public void enviarVerificacionRegistro(String correo) {
		Usuario usuario = usuarioRepositorio.findByCorreoIgnoreCase(correo.trim())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		String codigo = generarCodigo(usuario, TipoCodigoVerificacion.VERIFICAR_CORREO, false);
		try {
			correoServicio.enviarCodigoVerificacion(usuario.getCorreo(), usuario.getNombres(), codigo,
					propiedades.correo().expiracionMinutos());
		} catch (Exception ex) {
			log.warn("Registro creado pero el correo de verificación no se pudo enviar a {}", usuario.getCorreo(), ex);
		}
	}

	/**
	 * Reenvía el código de verificación. Respuesta siempre genérica para no
	 * revelar si el correo existe o no en el sistema.
	 */
	@Transactional
	public void reenviarVerificacion(String correo) {
		Optional<Usuario> usuarioOpt = usuarioRepositorio.findByCorreoIgnoreCase(correo.trim());
		if (usuarioOpt.isEmpty() || usuarioOpt.get().isCorreoVerificado()) {
			return;
		}
		Usuario usuario = usuarioOpt.get();
		String codigo = generarCodigo(usuario, TipoCodigoVerificacion.VERIFICAR_CORREO, true);
		correoServicio.enviarCodigoVerificacion(usuario.getCorreo(), usuario.getNombres(), codigo,
				propiedades.correo().expiracionMinutos());
	}

	// noRollbackFor: el aumento de intentos fallidos y la invalidación del código
	// deben guardarse aunque la verificación sea rechazada.
	@Transactional(noRollbackFor = NawinException.class)
	public void verificarCorreo(String correo, String codigo) {
		Usuario usuario = usuarioRepositorio.findByCorreoIgnoreCase(correo.trim())
				.orElseThrow(() -> new NawinException(CodigoError.VER_001));
		if (usuario.isCorreoVerificado()) {
			return;
		}
		consumirCodigo(usuario, TipoCodigoVerificacion.VERIFICAR_CORREO, codigo);
		usuario.setCorreoVerificado(true);
		// Correo de bienvenida (best-effort: no debe romper la verificación).
		try {
			correoServicio.enviarBienvenida(usuario.getCorreo(), usuario.getNombres());
		} catch (Exception ex) {
			log.warn("Cuenta verificada pero no se pudo enviar la bienvenida a {}", usuario.getCorreo(), ex);
		}
	}

	/**
	 * Cambio de contraseña por código al correo, para el usuario autenticado
	 * (alternativa a "contraseña actual + nueva" desde Seguridad). Envía el código
	 * al correo del propio usuario en sesión.
	 */
	@Transactional
	public void solicitarCodigoCambioClave() {
		Usuario usuario = usuarioRepositorio.findById(
				pe.nawin.utilidad.ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
		String codigo = generarCodigo(usuario, TipoCodigoVerificacion.RESET_CLAVE, true);
		correoServicio.enviarCodigoCambioClave(usuario.getCorreo(), usuario.getNombres(), codigo,
				propiedades.correo().expiracionMinutos());
	}

	@Transactional(noRollbackFor = NawinException.class)
	public void confirmarCambioClaveConCodigo(String codigo, String nuevaClave) {
		Usuario usuario = usuarioRepositorio.findById(
				pe.nawin.utilidad.ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
		autenticacionServicio.validarClave(nuevaClave);
		consumirCodigo(usuario, TipoCodigoVerificacion.RESET_CLAVE, codigo);
		usuario.setClaveHash(passwordEncoder.encode(nuevaClave));
		usuario.setIntentosFallidos(0);
		usuario.setBloqueadoHasta(null);
		// Cambiar la clave cierra todas las sesiones activas por seguridad.
		tokenRefrescoRepositorio.findByUsuario_IdUsuarioAndRevocadoFalse(usuario.getIdUsuario())
				.forEach(token -> token.setRevocado(true));
	}

	/**
	 * Envía el código de recuperación de clave. Respuesta siempre genérica para
	 * no revelar si el correo existe o no.
	 */
	@Transactional
	public void solicitarResetClave(String correo) {
		Optional<Usuario> usuarioOpt = usuarioRepositorio.findByCorreoIgnoreCase(correo.trim());
		if (usuarioOpt.isEmpty()) {
			return;
		}
		Usuario usuario = usuarioOpt.get();
		String codigo = generarCodigo(usuario, TipoCodigoVerificacion.RESET_CLAVE, true);
		correoServicio.enviarCodigoResetClave(usuario.getCorreo(), usuario.getNombres(), codigo,
				propiedades.correo().expiracionMinutos());
	}

	@Transactional(noRollbackFor = NawinException.class)
	public void confirmarResetClave(String correo, String codigo, String nuevaClave) {
		Usuario usuario = usuarioRepositorio.findByCorreoIgnoreCase(correo.trim())
				.orElseThrow(() -> new NawinException(CodigoError.VER_001));
		autenticacionServicio.validarClave(nuevaClave);
		consumirCodigo(usuario, TipoCodigoVerificacion.RESET_CLAVE, codigo);
		usuario.setClaveHash(passwordEncoder.encode(nuevaClave));
		usuario.setIntentosFallidos(0);
		usuario.setBloqueadoHasta(null);
		// Recuperar la clave por correo también demuestra la propiedad del correo.
		usuario.setCorreoVerificado(true);
		tokenRefrescoRepositorio.findByUsuario_IdUsuarioAndRevocadoFalse(usuario.getIdUsuario())
				.forEach(token -> token.setRevocado(true));
	}

	/**
	 * Genera un código de 4 caracteres con al menos una letra y un número,
	 * invalida los códigos anteriores del mismo tipo y aplica el tiempo de
	 * espera entre reenvíos.
	 */
	private String generarCodigo(Usuario usuario, TipoCodigoVerificacion tipo, boolean validarReenvio) {
		if (validarReenvio) {
			codigoRepositorio.findTopByUsuario_IdUsuarioAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
					usuario.getIdUsuario(), tipo).ifPresent(previo -> {
						long segundos = Duration.between(previo.getFechaCreacion(), LocalDateTime.now()).getSeconds();
						if (segundos < propiedades.correo().reenvioSegundos()) {
							throw new NawinException(CodigoError.VER_003,
									"Espera " + (propiedades.correo().reenvioSegundos() - segundos)
											+ " segundos antes de solicitar otro código.");
						}
					});
		}
		List<CodigoVerificacion> activos = codigoRepositorio
				.findByUsuario_IdUsuarioAndTipoAndUsadoFalse(usuario.getIdUsuario(), tipo);
		activos.forEach(activo -> activo.setUsado(true));

		char[] codigo = new char[4];
		codigo[0] = LETRAS.charAt(secureRandom.nextInt(LETRAS.length()));
		codigo[1] = NUMEROS.charAt(secureRandom.nextInt(NUMEROS.length()));
		codigo[2] = ALFABETO.charAt(secureRandom.nextInt(ALFABETO.length()));
		codigo[3] = ALFABETO.charAt(secureRandom.nextInt(ALFABETO.length()));
		for (int i = codigo.length - 1; i > 0; i--) {
			int j = secureRandom.nextInt(i + 1);
			char temporal = codigo[i];
			codigo[i] = codigo[j];
			codigo[j] = temporal;
		}
		String codigoPlano = new String(codigo);

		CodigoVerificacion registro = new CodigoVerificacion();
		registro.setUsuario(usuario);
		registro.setTipo(tipo);
		registro.setCodigoHash(hashServicio.sha256(codigoPlano));
		registro.setFechaExpiracion(LocalDateTime.now().plusMinutes(propiedades.correo().expiracionMinutos()));
		codigoRepositorio.save(registro);
		return codigoPlano;
	}

	/** Valida el código ingresado: existencia, expiración, intentos y hash. */
	private void consumirCodigo(Usuario usuario, TipoCodigoVerificacion tipo, String codigoIngresado) {
		CodigoVerificacion registro = codigoRepositorio
				.findTopByUsuario_IdUsuarioAndTipoAndUsadoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario(), tipo)
				.orElseThrow(() -> new NawinException(CodigoError.VER_001));
		if (registro.getFechaExpiracion().isBefore(LocalDateTime.now())) {
			registro.setUsado(true);
			throw new NawinException(CodigoError.VER_001);
		}
		if (registro.getIntentos() >= propiedades.correo().maxIntentos()) {
			registro.setUsado(true);
			throw new NawinException(CodigoError.VER_003);
		}
		String hashIngresado = hashServicio.sha256(codigoIngresado.trim().toUpperCase());
		if (!registro.getCodigoHash().equals(hashIngresado)) {
			registro.setIntentos(registro.getIntentos() + 1);
			throw new NawinException(CodigoError.VER_001);
		}
		registro.setUsado(true);
	}
}
