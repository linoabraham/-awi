package pe.nawin.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.servicio.SoporteServicio;

/** Canales oficiales de soporte y redes, visibles para el usuario autenticado. */
@RestController
@RequestMapping("/api/soporte")
@Tag(name = "10 Soporte", description = "Canales oficiales de soporte, redes y enlaces informativos.")
public class SoporteControlador {

	private final SoporteServicio servicio;

	public SoporteControlador(SoporteServicio servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> obtener() {
		return ResponseEntity.ok(RespuestaApi.ok(servicio.obtener()));
	}
}
