package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

public record MfaVerificarRequest(@NotBlank String codigo) {
}
