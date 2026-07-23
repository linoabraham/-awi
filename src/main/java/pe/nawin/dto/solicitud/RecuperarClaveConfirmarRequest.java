package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RecuperarClaveConfirmarRequest(
		@Email @NotBlank String correo,
		@NotBlank @Pattern(regexp = "[A-Za-z0-9]{4}", message = "El código debe tener 4 letras o números.") String codigo,
		@NotBlank String nuevaClave
) {
}
