package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.nawin.enumeracion.TipoDocumento;

/**
 * Registro público (autoservicio) de un cliente. Solo se exige nombre completo,
 * correo y clave; el resto de datos (documento, celular, dirección, usuario) es
 * opcional y se completa después desde "editar perfil".
 */
public record RegistroClienteRequest(
		@NotBlank @Size(max = 160) String nombres,
		@Email @NotBlank @Size(max = 150) String correo,
		@NotBlank String clave,
		TipoDocumento tipoDocumento,
		@Pattern(regexp = "\\d{8}|\\d{11}") String numeroDocumento,
		@Size(max = 120) String apellidos,
		@Size(max = 200) String razonSocial,
		@Pattern(regexp = "\\d{9,15}") String celular,
		@Size(max = 250) String direccion,
		@Size(max = 60) String nombreUsuario
) {
}
