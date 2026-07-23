package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ComprarPlanMiPlataRequest(
		@NotNull Long idPlan,
		LocalDate fechaInicio
) {
}
