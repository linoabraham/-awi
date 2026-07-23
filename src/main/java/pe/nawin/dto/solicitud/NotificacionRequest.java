package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import pe.nawin.enumeracion.CanalNotificacion;
import pe.nawin.enumeracion.TipoNotificacion;

public record NotificacionRequest(
		@NotNull Long idCliente,
		Long idMembresia,
		@NotNull CanalNotificacion canal,
		@NotNull TipoNotificacion tipo,
		@NotBlank String titulo,
		@NotBlank String mensaje,
		LocalDateTime fechaProgramada
) {
}
