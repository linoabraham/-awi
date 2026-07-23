package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

/** Registro del token FCM de un dispositivo. */
public record RegistrarTokenPushRequest(
		@NotBlank String installationId,
		@NotBlank String fcmToken,
		String plataforma
) {
}
