package pe.nawin.integracion.codart;

import com.fasterxml.jackson.databind.JsonNode;

public record RespuestaProveedor(
		JsonNode cuerpo,
		int codigoHttp,
		long duracionMilisegundos
) {
}
