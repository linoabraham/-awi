package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SolicitudRecargaMiPlataRequest(
		@NotNull @DecimalMin("0.01") BigDecimal montoSoles,
		@NotBlank String comprobanteBase64,
		String codigoReferido
) {
}
