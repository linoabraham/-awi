package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.MetodoPagoRequest;
import pe.nawin.servicio.MetodoPagoServicio;

@RestController
@RequestMapping("/api/admin/metodos-pago")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "18 ADMIN - Métodos de pago", description = "Configura Yape, Izipay y otros métodos de pago.")
public class AdminMetodosPagoControlador {

	private final MetodoPagoServicio servicio;

	public AdminMetodosPagoControlador(MetodoPagoServicio servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listar() {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.listar()));
	}

	@PostMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crear(
			@Valid @RequestBody MetodoPagoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.crear(request)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizar(
			@PathVariable Long id, @Valid @RequestBody MetodoPagoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.actualizar(id, request)));
	}

	@PatchMapping("/{id}/estado")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cambiarEstado(
			@PathVariable Long id, @RequestParam boolean activo) {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.cambiarEstado(id, activo)));
	}
}
