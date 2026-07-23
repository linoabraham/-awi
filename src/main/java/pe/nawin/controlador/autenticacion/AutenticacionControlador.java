package pe.nawin.controlador.autenticacion;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.DispositivoRespuesta;
import pe.nawin.dto.respuesta.EventoSeguridadRespuesta;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.respuesta.TokenRespuesta;
import pe.nawin.dto.respuesta.UsuarioActualRespuesta;
import pe.nawin.dto.solicitud.CambiarClaveRequest;
import pe.nawin.dto.solicitud.CerrarDispositivoCredencialesRequest;
import pe.nawin.dto.solicitud.CerrarDispositivoRequest;
import pe.nawin.dto.solicitud.DispositivosEnEsperaRequest;
import pe.nawin.dto.solicitud.IniciarSesionRequest;
import pe.nawin.dto.solicitud.MfaConfigurarRequest;
import pe.nawin.dto.solicitud.MfaVerificarRequest;
import pe.nawin.dto.solicitud.RecuperarClaveConfirmarRequest;
import pe.nawin.dto.solicitud.ReenviarCodigoRequest;
import pe.nawin.dto.solicitud.RegistroClienteRequest;
import pe.nawin.dto.solicitud.RenovarTokenRequest;
import pe.nawin.dto.solicitud.VerificarCorreoRequest;
import pe.nawin.servicio.AutenticacionServicio;
import pe.nawin.servicio.ClienteServicio;
import pe.nawin.servicio.VerificacionCorreoServicio;

@RestController
@RequestMapping("/api/autenticacion")
@Tag(name = "00 Autenticacion", description = "Autenticacion comun para ADMIN, TRABAJADOR y CLIENTE.")
public class AutenticacionControlador {

	private final AutenticacionServicio autenticacionServicio;
	private final ClienteServicio clienteServicio;
	private final VerificacionCorreoServicio verificacionCorreoServicio;

	public AutenticacionControlador(AutenticacionServicio autenticacionServicio, ClienteServicio clienteServicio,
			VerificacionCorreoServicio verificacionCorreoServicio) {
		this.autenticacionServicio = autenticacionServicio;
		this.clienteServicio = clienteServicio;
		this.verificacionCorreoServicio = verificacionCorreoServicio;
	}

	/**
	 * Registro público de clientes: crea la cuenta con rol CLIENTE (usuario,
	 * cliente, saldo de créditos y billetera MiPlata) y envía un código de
	 * verificación al correo. La sesión se inicia recién cuando el correo queda
	 * verificado con {@code /verificar-correo}.
	 */
	@PostMapping("/registro-cliente")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
		clienteServicio.registrarPublico(request);
		String correo = request.correo().trim().toLowerCase();
		verificacionCorreoServicio.enviarVerificacionRegistro(correo);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of(
				"correo", correo,
				"mensaje", "Cuenta creada. Te enviamos un código de verificación al correo.")));
	}

	/** Verifica el correo con el código de 4 caracteres enviado al registrarse. */
	@PostMapping("/verificar-correo")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verificarCorreo(@Valid @RequestBody VerificarCorreoRequest request) {
		verificacionCorreoServicio.verificarCorreo(request.correo(), request.codigo());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of(
				"mensaje", "Correo verificado. Ya puedes iniciar sesión.")));
	}

	/** Reenvía el código de verificación de correo. Respuesta siempre genérica. */
	@PostMapping("/reenviar-codigo")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> reenviarCodigo(@Valid @RequestBody ReenviarCodigoRequest request) {
		verificacionCorreoServicio.reenviarVerificacion(request.correo());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of(
				"mensaje", "Si el correo está registrado y pendiente de verificar, enviamos un nuevo código.")));
	}

	/** Envía un código al correo para restablecer la clave. Respuesta siempre genérica. */
	@PostMapping("/recuperar-clave/solicitar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> solicitarRecuperacion(@Valid @RequestBody ReenviarCodigoRequest request) {
		verificacionCorreoServicio.solicitarResetClave(request.correo());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of(
				"mensaje", "Si el correo está registrado, enviamos un código para restablecer la clave.")));
	}

	/** Restablece la clave con el código recibido por correo y revoca las sesiones activas. */
	@PostMapping("/recuperar-clave/confirmar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> confirmarRecuperacion(@Valid @RequestBody RecuperarClaveConfirmarRequest request) {
		verificacionCorreoServicio.confirmarResetClave(request.correo(), request.codigo(), request.nuevaClave());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of(
				"mensaje", "Clave actualizada. Inicia sesión con tu nueva clave.")));
	}

	@PostMapping("/iniciar-sesion")
	public ResponseEntity<RespuestaApi<TokenRespuesta>> iniciarSesion(@Valid @RequestBody IniciarSesionRequest request,
			HttpServletRequest servletRequest) {
		return ResponseEntity.ok(RespuestaApi.ok(autenticacionServicio.iniciarSesion(request, ip(servletRequest), servletRequest.getHeader("User-Agent"))));
	}

	@PostMapping("/renovar-token")
	public ResponseEntity<RespuestaApi<TokenRespuesta>> renovar(@Valid @RequestBody RenovarTokenRequest request,
			HttpServletRequest servletRequest) {
		return ResponseEntity.ok(RespuestaApi.ok(autenticacionServicio.renovar(request, ip(servletRequest), servletRequest.getHeader("User-Agent"))));
	}

	@PostMapping("/cerrar-sesion")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cerrarSesion(@Valid @RequestBody RenovarTokenRequest request) {
		autenticacionServicio.cerrarSesion(request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@GetMapping("/mi-cuenta")
	public ResponseEntity<RespuestaApi<UsuarioActualRespuesta>> miCuenta() {
		return ResponseEntity.ok(RespuestaApi.ok(autenticacionServicio.miCuenta()));
	}

	@PatchMapping("/cambiar-clave")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cambiarClave(@Valid @RequestBody CambiarClaveRequest request) {
		autenticacionServicio.cambiarClave(request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	/** Alternativa: envía un código al correo del usuario para cambiar su contraseña. */
	@PostMapping("/cambiar-clave/solicitar-codigo")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> solicitarCodigoCambioClave() {
		verificacionCorreoServicio.solicitarCodigoCambioClave();
		return ResponseEntity.ok(RespuestaApi.ok(Map.of(
				"mensaje", "Te enviamos un código a tu correo para confirmar el cambio.")));
	}

	@PostMapping("/cambiar-clave/confirmar-codigo")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> confirmarCambioClaveConCodigo(
			@Valid @RequestBody pe.nawin.dto.solicitud.CambiarClaveCodigoRequest request) {
		verificacionCorreoServicio.confirmarCambioClaveConCodigo(request.codigo(), request.claveNueva());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of(
				"mensaje", "Contraseña actualizada. Inicia sesión nuevamente.")));
	}

	@PostMapping("/mfa/configurar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> configurarMfa(@Valid @RequestBody MfaConfigurarRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(autenticacionServicio.configurarMfa(request)));
	}

	@PostMapping("/mfa/verificar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verificarMfa(@Valid @RequestBody MfaVerificarRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(autenticacionServicio.verificarMfa(request)));
	}

	/**
	 * Dispositivos activos revalidando credenciales. Público: se usa cuando el
	 * login rebota por DEV_001 (el usuario aún no tiene sesión) para mostrar el
	 * selector "elige qué dispositivo cerrar".
	 */
	@PostMapping("/dispositivos-en-espera")
	public ResponseEntity<RespuestaApi<List<DispositivoRespuesta>>> dispositivosEnEspera(
			@Valid @RequestBody DispositivosEnEsperaRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(
				autenticacionServicio.dispositivosParaGestionar(request.nombreUsuario(), request.clave())));
	}

	/** Cierra un dispositivo revalidando credenciales (selector del login-límite). */
	@PostMapping("/dispositivos-en-espera/cerrar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cerrarDispositivoEnEspera(
			@Valid @RequestBody CerrarDispositivoCredencialesRequest request) {
		autenticacionServicio.cerrarDispositivoConCredenciales(
				request.nombreUsuario(), request.clave(), request.installationId());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	/** Dispositivos activos de la cuenta (máximo 2). El actual se marca con {@code esteDispositivo}. */
	@GetMapping("/dispositivos")
	public ResponseEntity<RespuestaApi<List<DispositivoRespuesta>>> misDispositivos(
			@RequestParam(required = false) String installationId) {
		return ResponseEntity.ok(RespuestaApi.ok(autenticacionServicio.misDispositivos(installationId)));
	}

	/** Cierra (revoca) la sesión de un dispositivo por su installationId. */
	@PostMapping("/dispositivos/cerrar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cerrarDispositivo(
			@Valid @RequestBody CerrarDispositivoRequest request) {
		autenticacionServicio.cerrarDispositivo(request.installationId());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	/** Cierra todas las sesiones salvo la del dispositivo actual. */
	@PostMapping("/dispositivos/cerrar-otros")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cerrarOtrosDispositivos(
			@RequestParam(required = false) String installationId) {
		autenticacionServicio.cerrarOtrosDispositivos(installationId);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	/** Actividad de seguridad reciente del usuario autenticado (auditoría). */
	@GetMapping("/eventos-seguridad")
	public ResponseEntity<RespuestaApi<List<EventoSeguridadRespuesta>>> misEventosSeguridad() {
		return ResponseEntity.ok(RespuestaApi.ok(autenticacionServicio.misEventosSeguridad()));
	}

	private String ip(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
