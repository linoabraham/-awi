package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CanjearCodigoRequest(
		@NotBlank @Size(min = 3, max = 20) String codigo
) {
}
