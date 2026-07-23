package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.servicio.ProveedorConfigServicio;

/** Credenciales del proveedor CODART, editables sin reiniciar el backend (solo ADMIN). */
@RestController
@RequestMapping("/api/admin/proveedor")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "21 Admin Proveedor", description = "Token y URL del proveedor CODART editables desde el panel.")
public class AdminProveedorControlador {

	/** Token en blanco = conservar el actual; baseUrl en blanco = volver al valor de entorno. */
	public record ActualizarProveedorRequest(
			@Size(max = 500) String apiToken,
			@Size(max = 255) String baseUrl
	) {
	}

	private final ProveedorConfigServicio servicio;

	public AdminProveedorControlador(ProveedorConfigServicio servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> obtener() {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.obtenerParaPanel()));
	}

	@PutMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizar(
			@Valid @RequestBody ActualizarProveedorRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.actualizar(request.apiToken(), request.baseUrl())));
	}
}
