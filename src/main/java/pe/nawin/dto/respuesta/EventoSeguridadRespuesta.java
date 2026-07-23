package pe.nawin.dto.respuesta;

import java.time.LocalDateTime;

/** Un evento de auditoría de seguridad para mostrar al usuario. */
public record EventoSeguridadRespuesta(
		String tipo,
		String descripcion,
		String installationId,
		String direccionIp,
		String detalle,
		LocalDateTime fechaCreacion
) {
}
