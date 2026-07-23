package pe.nawin.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.PreferenciasNotificacionRespuesta;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.PreferenciasNotificacionRequest;
import pe.nawin.dto.solicitud.RegistrarTokenPushRequest;
import pe.nawin.servicio.NotificacionPushServicio;
import pe.nawin.utilidad.ContextoSeguridad;

/** Notificaciones del usuario autenticado: token FCM y preferencias. */
@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "09 Notificaciones", description = "Token FCM y preferencias de notificación del usuario.")
public class NotificacionControlador {

	private final NotificacionPushServicio servicio;

	public NotificacionControlador(NotificacionPushServicio servicio) {
		this.servicio = servicio;
	}

	/** Registra o actualiza el token FCM de este dispositivo. */
	@PostMapping("/token-push")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> registrarToken(
			@Valid @RequestBody RegistrarTokenPushRequest request) {
		Long idUsuario = ContextoSeguridad.usuarioActual().idUsuario();
		servicio.registrarToken(idUsuario, request.installationId(), request.fcmToken(), request.plataforma());
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	/** Preferencias de notificación del usuario (crea las de por defecto si no existen). */
	@GetMapping("/preferencias")
	public ResponseEntity<RespuestaApi<PreferenciasNotificacionRespuesta>> preferencias() {
		Long idUsuario = ContextoSeguridad.usuarioActual().idUsuario();
		return ResponseEntity.ok(RespuestaApi.ok(
				PreferenciasNotificacionRespuesta.de(servicio.preferencias(idUsuario))));
	}

	@PutMapping("/preferencias")
	public ResponseEntity<RespuestaApi<PreferenciasNotificacionRespuesta>> actualizarPreferencias(
			@Valid @RequestBody PreferenciasNotificacionRequest request) {
		Long idUsuario = ContextoSeguridad.usuarioActual().idUsuario();
		return ResponseEntity.ok(RespuestaApi.ok(PreferenciasNotificacionRespuesta.de(
				servicio.actualizarPreferencias(idUsuario, request.push(), request.promos(), request.pagos(),
						request.consultas(), request.referidos(), request.seguridad()))));
	}
}
