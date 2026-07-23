package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotNull;

public record ActivarMembresiaRequest(@NotNull Long idPago) {
}
