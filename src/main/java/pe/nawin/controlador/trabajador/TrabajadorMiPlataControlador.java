package pe.nawin.controlador.trabajador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.RechazarRecargaMiPlataRequest;
import pe.nawin.enumeracion.EstadoSolicitudRecargaMiPlata;
import pe.nawin.servicio.MiPlataServicio;

@RestController
@RequestMapping("/api/trabajador/miplata")
@PreAuthorize("hasRole('TRABAJADOR')")
@Tag(name = "25 TRABAJADOR - MiPlata", description = "Revision operativa de recargas y consulta de billeteras MiPlata.")
public class TrabajadorMiPlataControlador {

	private final MiPlataServicio miPlataServicio;

	public TrabajadorMiPlataControlador(MiPlataServicio miPlataServicio) {
		this.miPlataServicio = miPlataServicio;
	}

	@GetMapping("/solicitudes-recarga")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> solicitudes(
			@RequestParam(required = false) Long cliente,
			@RequestParam(required = false) EstadoSolicitudRecargaMiPlata estado) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.listarSolicitudes(cliente, estado)));
	}

	@GetMapping("/solicitudes-recarga/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verSolicitud(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.verSolicitud(id)));
	}

	@PostMapping("/solicitudes-recarga/{id}/aprobar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> aprobar(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.aprobarRecarga(id)));
	}

	@PostMapping("/solicitudes-recarga/{id}/rechazar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> rechazar(@PathVariable Long id,
			@Valid @RequestBody RechazarRecargaMiPlataRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.rechazarRecarga(id, request)));
	}

	@GetMapping("/clientes/{id}/billetera")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> billeteraCliente(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.billeteraCliente(id)));
	}

	@GetMapping("/clientes/{id}/movimientos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> movimientosCliente(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.movimientosCliente(id)));
	}
}
