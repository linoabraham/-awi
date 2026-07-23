package pe.nawin.dto.respuesta;

import java.time.LocalDateTime;

/** Un dispositivo activo de la cuenta (una sesión vigente por dispositivo). */
public record DispositivoRespuesta(
		String installationId,
		String nombreDispositivo,
		String modelo,
		String plataforma,
		String direccionIp,
		LocalDateTime ultimaActividad,
		LocalDateTime primerInicio,
		boolean esteDispositivo
) {
}
