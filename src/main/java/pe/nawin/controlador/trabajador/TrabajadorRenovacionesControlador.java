package pe.nawin.controlador.trabajador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.NotificacionRequest;
import pe.nawin.enumeracion.EstadoNotificacion;
import pe.nawin.servicio.NotificacionServicio;
import pe.nawin.servicio.RenovacionServicio;

@RestController
@RequestMapping("/api/trabajador")
@PreAuthorize("hasRole('TRABAJADOR')")
@Tag(name = "24 TRABAJADOR - Renovaciones", description = "Clientes por vencer, vencidos y notificaciones de renovacion.")
public class TrabajadorRenovacionesControlador {

	private final RenovacionServicio renovacionServicio;
	private final NotificacionServicio notificacionServicio;

	public TrabajadorRenovacionesControlador(RenovacionServicio renovacionServicio, NotificacionServicio notificacionServicio) {
		this.renovacionServicio = renovacionServicio;
		this.notificacionServicio = notificacionServicio;
	}

	@GetMapping("/renovaciones/por-vencer")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> porVencer(@RequestParam(defaultValue = "10") int dias) {
		return ResponseEntity.ok(RespuestaApi.ok(renovacionServicio.porVencer(dias)));
	}

	@GetMapping("/renovaciones/vencidas")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> vencidas() {
		return ResponseEntity.ok(RespuestaApi.ok(renovacionServicio.vencidas()));
	}

	@PostMapping("/notificaciones-renovacion")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> registrar(@Valid @RequestBody NotificacionRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(notificacionServicio.crear(request)));
	}

	@GetMapping("/notificaciones-renovacion")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listar(@RequestParam(required = false) Long cliente,
			@RequestParam(required = false) EstadoNotificacion estado) {
		return ResponseEntity.ok(RespuestaApi.ok(notificacionServicio.listar(cliente, estado)));
	}
}
