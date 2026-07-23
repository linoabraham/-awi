package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.EnviarPushRequest;
import pe.nawin.enumeracion.CategoriaNotificacion;
import pe.nawin.servicio.NotificacionPushServicio;

/** Envío manual de notificaciones push desde el panel (solo ADMIN). */
@RestController
@RequestMapping("/api/admin/notificaciones")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "19 Admin Notificaciones", description = "Envío manual de push desde el panel.")
public class AdminNotificacionesControlador {

	private final NotificacionPushServicio servicio;

	public AdminNotificacionesControlador(NotificacionPushServicio servicio) {
		this.servicio = servicio;
	}

	/** Envía una push a un usuario ({@code idUsuario}) o a todos si viene null. */
	@PostMapping("/enviar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> enviar(@Valid @RequestBody EnviarPushRequest request) {
		if (request.idUsuario() == null) {
			servicio.enviarATodos(request.titulo(), request.cuerpo(), Map.of());
		} else {
			servicio.enviarAUsuario(request.idUsuario(), CategoriaNotificacion.GENERAL,
					request.titulo(), request.cuerpo(), Map.of());
		}
		return ResponseEntity.ok(RespuestaApi.ok(Map.of("mensaje", "Envío en proceso.")));
	}
}
