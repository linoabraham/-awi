package pe.nawin.controlador.trabajador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.ClienteActualizarRequest;
import pe.nawin.dto.solicitud.ClienteCrearRequest;
import pe.nawin.enumeracion.EstadoCliente;
import pe.nawin.servicio.ClienteServicio;

@RestController
@RequestMapping("/api/trabajador/clientes")
@PreAuthorize("hasRole('TRABAJADOR')")
@Tag(name = "20 TRABAJADOR - Clientes", description = "Creacion, listado, detalle, actualizacion y desactivacion de clientes.")
public class TrabajadorClientesControlador {

	private final ClienteServicio clienteServicio;

	public TrabajadorClientesControlador(ClienteServicio clienteServicio) {
		this.clienteServicio = clienteServicio;
	}

	@PostMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crear(@Valid @RequestBody ClienteCrearRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(clienteServicio.crear(request)));
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listar(@RequestParam(required = false) EstadoCliente estado) {
		return ResponseEntity.ok(RespuestaApi.ok(clienteServicio.listar(estado)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> ver(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(clienteServicio.ver(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizar(@PathVariable Long id,
			@Valid @RequestBody ClienteActualizarRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(clienteServicio.actualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> desactivar(@PathVariable Long id) {
		clienteServicio.desactivar(id);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}
}
