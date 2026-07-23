package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.nawin.enumeracion.TipoComprobante;

public record SerieComprobanteRequest(
		@NotNull TipoComprobante tipoComprobante,
		@NotBlank String serie,
		boolean activo
) {
}
