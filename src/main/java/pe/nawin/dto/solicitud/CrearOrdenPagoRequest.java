package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Crea una orden de pago (código + expiración 24h) para un plan o un paquete de
 * créditos. Se envía uno de los dos ids según el tipo de compra.
 */
public record CrearOrdenPagoRequest(
		Long idPlan,
		Long idPaqueteCredito,
		@NotNull @DecimalMin("1.00") BigDecimal montoSoles,
		String concepto
) {
}
