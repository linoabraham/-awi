package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

public record RenovarTokenRequest(@NotBlank String tokenRefresco) {
}
