package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Pattern;

public record ConsultaCriticaDniRequest(
		@Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos") String dni,
		String finalidad,
		String justificacion,
		String codigoMfa
) implements SolicitudConsultaCritica {
}
