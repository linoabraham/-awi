package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

public record RechazarRecargaMiPlataRequest(@NotBlank String motivo) {
}
