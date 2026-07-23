package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Edición (panel) de los canales oficiales de soporte y redes. */
public record ConfiguracionSoporteRequest(
		@Pattern(regexp = "(\\+?\\d{9,15})?", message = "WhatsApp debe tener entre 9 y 15 dígitos")
		String whatsapp,
		@Email String correo,
		@Size(max = 255) String facebook,
		@Size(max = 255) String instagram,
		@Size(max = 255) String tiktok,
		@Size(max = 255) String terminosUrl,
		@Size(max = 255) String privacidadUrl
) {
}
