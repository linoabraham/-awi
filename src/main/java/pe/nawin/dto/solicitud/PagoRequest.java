package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.nawin.enumeracion.MedioPago;

public record PagoRequest(
		@NotNull Long idCliente,
		Long idMembresia,
		Long idVentaCredito,
		@NotNull @DecimalMin("0.00") BigDecimal montoSoles,
		@NotNull MedioPago medioPago,
		String numeroOperacion,
		@NotNull LocalDateTime fechaPago,
		String observacion
) {
}
