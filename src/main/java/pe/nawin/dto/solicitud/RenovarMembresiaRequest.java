package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RenovarMembresiaRequest(
		@NotNull Long idPlan,
		@NotNull LocalDate fechaInicio
) {
}
