package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

/** Credenciales + dispositivo a cerrar durante el flujo de login-límite (DEV_001). */
public record CerrarDispositivoCredencialesRequest(
		@NotBlank String nombreUsuario,
		@NotBlank String clave,
		@NotBlank String installationId
) {
}
