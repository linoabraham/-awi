package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

/** Credenciales para listar los dispositivos activos cuando el login rebota por DEV_001. */
public record DispositivosEnEsperaRequest(
		@NotBlank String nombreUsuario,
		@NotBlank String clave
) {
}
