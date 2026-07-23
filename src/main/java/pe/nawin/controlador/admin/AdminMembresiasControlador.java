package pe.nawin.controlador.admin;

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
@RequestMapping("/api/admin/membresias")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "13 ADMIN - Membresias", description = "Gestion de membresias, activacion, renovacion y endpoints efectivos.")
public class AdminMembresiasControlador {

	private final MembresiaServicio membresiaServicio;

	public AdminMembresiasControlador(MembresiaServicio membresiaServicio) {
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

	@PatchMapping("/{id}/suspender")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> suspender(@PathVariable Long id, @RequestBody(required = false) MotivoRequest request) {
		membresiaServicio.suspender(id, request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@PatchMapping("/{id}/reactivar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> reactivar(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.reactivar(id)));
	}

	@PostMapping("/{id}/endpoints")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> agregarAcceso(@PathVariable Long id,
			@Valid @RequestBody MembresiaEndpointRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.agregarAcceso(id, request)));
	}

	@GetMapping("/{id}/endpoints")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarAccesos(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.listarAccesos(id)));
	}

	@PutMapping("/{id}/endpoints/{idMe}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarAcceso(@PathVariable Long id, @PathVariable Long idMe,
			@Valid @RequestBody MembresiaEndpointRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(membresiaServicio.actualizarAcceso(id, idMe, request)));
	}

	@DeleteMapping("/{id}/endpoints/{idMe}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> deshabilitarAcceso(@PathVariable Long id, @PathVariable Long idMe) {
		membresiaServicio.deshabilitarAcceso(id, idMe);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}
}
