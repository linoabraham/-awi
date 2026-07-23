package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

public record CerrarDispositivoRequest(@NotBlank String installationId) {
}
