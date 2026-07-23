package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Pattern;

public record ConsultaRucRequest(
		@Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos") String ruc
) implements SolicitudConsulta {
}
