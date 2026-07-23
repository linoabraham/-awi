package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotNull;
import pe.nawin.enumeracion.TipoMfa;

public record MfaConfigurarRequest(@NotNull TipoMfa tipoMfa) {
}
