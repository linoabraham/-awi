package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.enumeracion.RolCodigo;

public record UsuarioActualizarRequest(
		@NotNull RolCodigo rol,
		@NotBlank String nombres,
		@NotBlank String apellidos,
		@NotBlank @Email String correo,
		@NotBlank @Pattern(regexp = "\\d{9,15}") String celular,
		@NotNull EstadoUsuario estado
) {
}
