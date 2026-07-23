package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import pe.nawin.enumeracion.TipoDocumento;

public record ClienteCrearRequest(
		@NotNull TipoDocumento tipoDocumento,
		@NotBlank String numeroDocumento,
		String nombres,
		String apellidos,
		String razonSocial,
		@Email @NotBlank String correo,
		@NotBlank @Pattern(regexp = "\\d{9,15}") String celular,
		String direccion,
		@NotBlank String nombreUsuario,
		@NotBlank String claveTemporal
) {
}
