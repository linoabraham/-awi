package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Pattern;

public record ConsultaNumeroRequest(
		@Pattern(regexp = "\\d{9}", message = "El celular debe tener 9 dígitos") String numero
) implements SolicitudConsulta {
}
