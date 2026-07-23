package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Pattern;

public record ConsultaCriticaNumeroRequest(
		@Pattern(regexp = "\\d{9}", message = "El celular debe tener 9 dígitos") String numero,
		String finalidad,
		String justificacion,
		String codigoMfa
) implements SolicitudConsultaCritica {
}
