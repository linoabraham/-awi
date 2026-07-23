package pe.nawin.controlador.cliente;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.PerfilClienteRequest;
import pe.nawin.servicio.ClientePortalServicio;
import pe.nawin.servicio.ComprobanteServicio;
import pe.nawin.servicio.NotificacionServicio;

@RestController
@RequestMapping("/api/cliente")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "30 CLIENTE - Inicio y perfil", description = "Inicio, perfil, membresias, creditos, comprobantes y notificaciones propias.")
public class ClientePortalControlador {

	private final ClientePortalServicio clientePortalServicio;
	private final ComprobanteServicio comprobanteServicio;
	private final NotificacionServicio notificacionServicio;

	public ClientePortalControlador(ClientePortalServicio clientePortalServicio, ComprobanteServicio comprobanteServicio,
			NotificacionServicio notificacionServicio) {
		this.clientePortalServicio = clientePortalServicio;
		this.comprobanteServicio = comprobanteServicio;
		this.notificacionServicio = notificacionServicio;
	}

	@GetMapping("/inicio")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> inicio() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.inicio()));
	}

	@GetMapping("/perfil")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> perfil() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.perfil()));
	}

	@PutMapping("/perfil")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarPerfil(@Valid @RequestBody PerfilClienteRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.actualizarPerfil(request)));
	}

	@GetMapping("/membresia")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> membresiaActual() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.membresiaActual()));
	}

	@GetMapping("/membresias")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> membresias() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.membresias()));
	}

	@GetMapping("/endpoints-disponibles")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> endpoints() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.endpointsDisponibles()));
	}

	@GetMapping("/creditos/saldo")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> saldo() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.saldo()));
	}

	@GetMapping("/creditos/movimientos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> movimientos() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.movimientos()));
	}

	@GetMapping("/comprobantes")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> comprobantes() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.comprobantes()));
	}

	@GetMapping("/comprobantes/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> comprobante(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(comprobanteServicio.verPropio(id)));
	}

	@GetMapping("/comprobantes/{id}/pdf")
	public ResponseEntity<Resource> pdf(@PathVariable Long id) {
		Resource resource = comprobanteServicio.pdfPropio(id);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comprobante-" + id + ".pdf\"")
				.body(resource);
	}

	@GetMapping("/notificaciones")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> notificaciones() {
		return ResponseEntity.ok(RespuestaApi.ok(clientePortalServicio.notificaciones()));
	}

	@PatchMapping("/notificaciones/{id}/leer")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> leer(@PathVariable Long id) {
		// La validación de propiedad se aplica en el servicio.
		notificacionServicio.marcarLeida(id, null);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}
}
