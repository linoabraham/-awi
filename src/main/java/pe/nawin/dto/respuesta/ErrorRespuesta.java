package pe.nawin.dto.respuesta;

import java.util.Map;

public record ErrorRespuesta(
		String codigo,
		Map<String, String> errores
) {
	public static ErrorRespuesta simple(String codigo) {
		return new ErrorRespuesta(codigo, Map.of());
	}
}
