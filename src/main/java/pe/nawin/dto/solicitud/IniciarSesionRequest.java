package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import pe.nawin.enumeracion.TipoCliente;

public record IniciarSesionRequest(
		@NotBlank String nombreUsuario,
		@NotBlank String clave,
		TipoCliente tipoCliente,
		// Identificador del dispositivo (UUID guardado en flutter_secure_storage).
		// Opcional para no romper clientes web/antiguos que no lo envían.
		String installationId,
		String nombreDispositivo,
		String modelo,
		String plataforma
) {
	/** Los clientes antiguos no envían tipoCliente: se asume WEB (sesión corta). */
	public TipoCliente tipoClienteEfectivo() {
		return tipoCliente == null ? TipoCliente.WEB : tipoCliente;
	}
}
