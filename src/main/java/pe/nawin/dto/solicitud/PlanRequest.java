package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import pe.nawin.enumeracion.EstadoPlan;

public record PlanRequest(
		@NotBlank String codigo,
		@NotBlank String nombre,
		String descripcion,
		@NotNull @DecimalMin("0.00") BigDecimal precioSoles,
		@Min(1) int diasVigencia,
		@NotNull EstadoPlan estado
) {
}
