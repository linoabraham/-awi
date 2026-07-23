package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MembresiaActualizarRequest(
		LocalDate fechaInicio,
		@DecimalMin("0.00") BigDecimal precioPagado,
		String observacion
) {
}
