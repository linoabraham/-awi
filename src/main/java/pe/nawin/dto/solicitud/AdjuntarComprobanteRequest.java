package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Adjunta la captura del pago y el código de operación a una orden. */
public record AdjuntarComprobanteRequest(
		@NotBlank String comprobanteBase64,
		@NotBlank @Size(min = 4, max = 60) String codigoOperacion
) {
}
