package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.EndpointBusquedaRequest;
import pe.nawin.dto.solicitud.PlanEndpointRequest;
import pe.nawin.dto.solicitud.PlanRequest;
import pe.nawin.enumeracion.EstadoPlan;
import pe.nawin.servicio.PlanServicio;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "12 ADMIN - Catalogo y planes", description = "Catalogo de busquedas, planes y reglas comerciales de endpoints.")
public class AdminPlanesControlador {

	private final PlanServicio planServicio;

	public AdminPlanesControlador(PlanServicio planServicio) {
		this.planServicio = planServicio;
	}

	@PostMapping("/planes")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crearPlan(@Valid @RequestBody PlanRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.crearPlan(request)));
	}

	@GetMapping("/planes")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarPlanes(@RequestParam(required = false) EstadoPlan estado) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.listarPlanes(estado)));
	}

	@GetMapping("/planes/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verPlan(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.verPlan(id)));
	}

	@PutMapping("/planes/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarPlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.actualizarPlan(id, request)));
	}

	@DeleteMapping("/planes/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> inactivarPlan(@PathVariable Long id) {
		planServicio.inactivarPlan(id);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@PostMapping("/endpoints-busqueda")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crearEndpoint(@Valid @RequestBody EndpointBusquedaRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.crearEndpoint(request)));
	}

	@GetMapping("/endpoints-busqueda")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarEndpoints(
			@RequestParam(required = false) Boolean activo, @RequestParam(required = false) Boolean critico) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.listarEndpoints(activo, critico)));
	}

	@GetMapping("/endpoints-busqueda/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verEndpoint(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.verEndpoint(id)));
	}

	@PutMapping("/endpoints-busqueda/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarEndpoint(@PathVariable Long id,
			@Valid @RequestBody EndpointBusquedaRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.actualizarEndpoint(id, request)));
	}

	@DeleteMapping("/endpoints-busqueda/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> inactivarEndpoint(@PathVariable Long id) {
		planServicio.inactivarEndpoint(id);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@PostMapping("/planes/{idPlan}/endpoints")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> agregarRegla(@PathVariable Long idPlan,
			@Valid @RequestBody PlanEndpointRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.agregarRegla(idPlan, request)));
	}

	@GetMapping("/planes/{idPlan}/endpoints")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarReglas(@PathVariable Long idPlan) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.listarReglas(idPlan)));
	}

	@GetMapping("/planes/{idPlan}/endpoints/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verRegla(@PathVariable Long idPlan, @PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.verRegla(idPlan, id)));
	}

	@PutMapping("/planes/{idPlan}/endpoints/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarRegla(@PathVariable Long idPlan, @PathVariable Long id,
			@Valid @RequestBody PlanEndpointRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(planServicio.actualizarRegla(idPlan, id, request)));
	}

	@DeleteMapping("/planes/{idPlan}/endpoints/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> quitarRegla(@PathVariable Long idPlan, @PathVariable Long id) {
		planServicio.quitarRegla(idPlan, id);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}
}
