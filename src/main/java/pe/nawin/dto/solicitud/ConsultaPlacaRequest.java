package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.Pattern;

public record ConsultaPlacaRequest(
		@Pattern(regexp = "[A-Za-z0-9]{6,7}", message = "La placa debe tener 6 o 7 caracteres alfanuméricos") String placa
) implements SolicitudConsulta {
}
