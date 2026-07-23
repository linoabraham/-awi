package pe.nawin.dto.respuesta;

import java.time.OffsetDateTime;

public record RespuestaApi<T>(
		boolean exito,
		String mensaje,
		T datos,
		OffsetDateTime fechaHora
) {
	public static <T> RespuestaApi<T> ok(T datos) {
		return new RespuestaApi<>(true, "Operación realizada correctamente", datos, OffsetDateTime.now());
	}

	public static <T> RespuestaApi<T> ok(String mensaje, T datos) {
		return new RespuestaApi<>(true, mensaje, datos, OffsetDateTime.now());
	}

	public static RespuestaApi<ErrorRespuesta> error(String mensaje, ErrorRespuesta error) {
		return new RespuestaApi<>(false, mensaje, error, OffsetDateTime.now());
	}
}
