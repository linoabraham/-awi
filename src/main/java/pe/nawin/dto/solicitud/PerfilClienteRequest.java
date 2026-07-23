package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import pe.nawin.enumeracion.TipoDocumento;

public record PerfilClienteRequest(
		@NotBlank @Email String correo,
		@NotBlank @Pattern(regexp = "\\d{9,15}") String celular,
		String direccion,
		// Completar el documento (freemium se registra sin él). Solo se acepta
		// cuando el cliente aún no lo tiene registrado.
		TipoDocumento tipoDocumento,
		String numeroDocumento
) {
}
