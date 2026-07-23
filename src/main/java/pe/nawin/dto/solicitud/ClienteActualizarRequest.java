package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import pe.nawin.enumeracion.EstadoCliente;

public record ClienteActualizarRequest(
		String nombres,
		String apellidos,
		String razonSocial,
		@Email @NotBlank String correo,
		@NotBlank @Pattern(regexp = "\\d{9,15}") String celular,
		String direccion,
		EstadoCliente estado
) {
}
