package pe.nawin.controlador.cliente;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.CanjearCodigoRequest;
import pe.nawin.seguridad.JwtServicio;
import pe.nawin.servicio.ReferidoServicio;

@RestController
@RequestMapping("/api/cliente/referidos")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "35 CLIENTE - Referidos", description = "Centro de referidos: resumen y canje de código.")
public class ClienteReferidosControlador {

	private final ReferidoServicio referidoServicio;
	private final JwtServicio jwtServicio;

	public ClienteReferidosControlador(ReferidoServicio referidoServicio, JwtServicio jwtServicio) {
		this.referidoServicio = referidoServicio;
		this.jwtServicio = jwtServicio;
	}

	@GetMapping("/resumen")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> resumen() {
		return ResponseEntity.ok(RespuestaApi.ok(referidoServicio.resumen()));
	}

	@PostMapping("/canjear")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> canjear(
			@Valid @RequestBody CanjearCodigoRequest request, HttpServletRequest servletRequest) {
		return ResponseEntity.ok(RespuestaApi.ok(referidoServicio.canjear(
				request.codigo(), deviceId(servletRequest), ip(servletRequest))));
	}

	/**
	 * installationId del dispositivo, tomado del claim {@code deviceId} del JWT
	 * (firmado por el servidor: el cliente no puede falsificarlo).
	 */
	private String deviceId(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			return null;
		}
		try {
			return jwtServicio.validar(header.substring(7)).get("deviceId", String.class);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private String ip(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
