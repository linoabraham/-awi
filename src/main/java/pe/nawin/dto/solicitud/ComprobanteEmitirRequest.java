package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotNull;
import pe.nawin.enumeracion.TipoComprobante;

public record ComprobanteEmitirRequest(
		@NotNull Long idPago,
		@NotNull Long idSerieComprobante,
		@NotNull TipoComprobante tipoComprobante
) {
}
