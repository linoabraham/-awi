package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VentaCreditoRequest(
		@NotNull Long idCliente,
		@NotNull Long idPaqueteCredito,
		@Min(1) int cantidadPaquetes
) {
}
