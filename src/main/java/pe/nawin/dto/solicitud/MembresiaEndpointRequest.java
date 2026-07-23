package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import pe.nawin.enumeracion.ModalidadAccesoEndpoint;

public record MembresiaEndpointRequest(
		@NotNull Long idEndpoint,
		boolean habilitado,
		@NotNull ModalidadAccesoEndpoint modalidadAcceso,
		Integer limiteDiario,
		Integer limiteTotal,
		Integer costoCreditosCliente,
		boolean requiereMfa,
		boolean requiereFinalidad,
		boolean requiereJustificacion,
		boolean permiteExportar,
		@Min(1) int diasRetencion
) {
}
