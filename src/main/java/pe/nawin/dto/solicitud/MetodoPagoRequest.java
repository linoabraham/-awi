package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Crea o actualiza un método de pago desde el panel (solo admin). */
public record MetodoPagoRequest(
		@NotBlank @Size(max = 30) String codigo,
		@NotBlank @Size(max = 80) String nombre,
		@NotBlank @Size(max = 30) String tipo,
		Boolean activo,
		Integer orden,
		@Size(max = 20) String numero,
		@Size(max = 120) String titular,
		String qrBase64,
		String credencialesJson,
		@Size(max = 500) String instrucciones,
		@Size(max = 60) String validacionTexto
) {
}
