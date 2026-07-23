package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.nawin.enumeracion.TipoMovimientoCredito;

public record AjusteCreditoRequest(
		@NotNull TipoMovimientoCredito tipo,
		@Min(1) int cantidad,
		@NotBlank String motivo
) {
}
