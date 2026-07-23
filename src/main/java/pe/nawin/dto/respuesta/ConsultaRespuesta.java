package pe.nawin.dto.respuesta;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import pe.nawin.enumeracion.EstadoConsulta;
import pe.nawin.enumeracion.OrigenConsumoConsulta;

public record ConsultaRespuesta(
		String codigoConsulta,
		EstadoConsulta estado,
		OrigenConsumoConsulta origenConsumo,
		int cantidadConsumida,
		boolean permiteExportar,
		JsonNode resultado,
		List<Map<String, Object>> archivos,
		LocalDateTime fecha
) {
}
