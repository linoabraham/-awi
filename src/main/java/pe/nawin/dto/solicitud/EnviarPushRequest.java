package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

/**
 * Envío manual de push desde el panel admin. Si {@code idUsuario} es null, la
 * notificación se envía a todos los usuarios con push habilitado.
 */
public record EnviarPushRequest(
		Long idUsuario,
		@NotBlank String titulo,
		@NotBlank String cuerpo
) {
}
