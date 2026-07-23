package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.nawin.enumeracion.CodigoEndpoint;
import pe.nawin.enumeracion.TipoConsumoProveedor;

public record EndpointBusquedaRequest(
		@NotNull CodigoEndpoint codigo,
		@NotBlank String nombre,
		String descripcion,
		@NotBlank String metodoProveedor,
		@NotBlank String rutaProveedor,
		@NotBlank String parametroPrincipal,
		@NotNull TipoConsumoProveedor tipoConsumoProveedor,
		@Min(1) int costoProveedor,
		boolean esCritico,
		boolean activo
) {
}
