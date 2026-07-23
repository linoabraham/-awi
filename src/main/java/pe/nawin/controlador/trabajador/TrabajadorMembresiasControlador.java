package pe.nawin.controlador.trabajador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.*;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.servicio.MembresiaServicio;

@RestController
@RequestMapping("/api/trabajador/membresias")
@PreAuthorize("hasRole('TRABAJADOR')")
@Tag(name = "21 TRABAJADOR - Membresias", description = "Creacion, activacion, renovacion y consulta de membresias.")
public class TrabajadorMembresiasControlador {

	private final MembresiaServicio membresiaServicio;

	public TrabajadorMembresiasControlador(MembresiaServicio membresiaServicio) {
		this.membresiaServicio = membresiaServicio;
	}

	@PostMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crear(@Valid @RequestBody MembresiaCrearRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.crear(request)));
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listar(@RequestParam(required = false) Long cliente,
			@RequestParam(required = false) EstadoMembresia estado) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.listar(cliente, estado)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> ver(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.ver(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizar(@PathVariable Long id,
			@Valid @RequestBody MembresiaActualizarRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.actualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cancelar(@PathVariable Long id, @RequestBody(required = false) MotivoRequest request) {
		membresiaServicio.cancelar(id, request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@PostMapping("/{id}/activar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> activar(@PathVariable Long id, @Valid @RequestBody ActivarMembresiaRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.activar(id, request)));
	}

	@PostMapping("/{id}/renovar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> renovar(@PathVariable Long id, @Valid @RequestBody RenovarMembresiaRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.renovar(id, request)));
	}
}
