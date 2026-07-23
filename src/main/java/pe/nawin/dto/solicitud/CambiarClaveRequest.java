package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarClaveRequest(
		@NotBlank String claveActual,
		@NotBlank @Size(min = 10, max = 100) String claveNueva
) {
}
