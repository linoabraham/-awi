package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

/** Cambio de contraseña confirmado con el código enviado al correo. */
public record CambiarClaveCodigoRequest(
		@NotBlank String codigo,
		@NotBlank String claveNueva
) {
}
