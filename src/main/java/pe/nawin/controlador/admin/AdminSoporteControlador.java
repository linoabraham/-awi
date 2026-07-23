package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.ConfiguracionSoporteRequest;
import pe.nawin.servicio.SoporteServicio;

/** Edición de los canales oficiales de soporte y redes (solo ADMIN). */
@RestController
@RequestMapping("/api/admin/soporte")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "20 Admin Soporte", description = "Canales oficiales de soporte editables desde el panel.")
public class AdminSoporteControlador {

	private final SoporteServicio servicio;

	public AdminSoporteControlador(SoporteServicio servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> obtener() {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.obtener()));
	}

	@PutMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizar(
			@Valid @RequestBody ConfiguracionSoporteRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.actualizar(request)));
	}
}
