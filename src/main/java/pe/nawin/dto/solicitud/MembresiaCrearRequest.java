package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MembresiaCrearRequest(
		@NotNull Long idCliente,
		@NotNull Long idPlan,
		@NotNull LocalDate fechaInicio,
		@NotNull @DecimalMin("0.00") BigDecimal precioPagado,
		String observacion
) {
}
