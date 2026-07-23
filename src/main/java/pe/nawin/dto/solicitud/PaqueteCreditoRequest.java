package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaqueteCreditoRequest(
		@NotBlank String codigo,
		@NotBlank String nombre,
		@Min(1) int cantidadCreditos,
		@NotNull @DecimalMin("0.00") BigDecimal precioSoles,
		@Min(1) int diasVigencia,
		boolean activo
) {
}
