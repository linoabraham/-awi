package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Apelación del cliente a una recarga rechazada. */
public record ApelarRecargaMiPlataRequest(
		@NotBlank @Size(max = 500) String mensaje
) {
}
