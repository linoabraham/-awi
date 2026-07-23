package pe.nawin.integracion.codart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.dto.solicitud.*;
import pe.nawin.entidad.EndpointBusqueda;
import pe.nawin.enumeracion.CodigoEndpoint;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;

@Component
public class ClienteCodart {

	private final NawinPropiedades propiedades;
	private final pe.nawin.servicio.ProveedorConfigServicio proveedorConfig;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public ClienteCodart(NawinPropiedades propiedades, pe.nawin.servicio.ProveedorConfigServicio proveedorConfig,
			ObjectMapper objectMapper, RestClient.Builder builder) {
		this.propiedades = propiedades;
		this.proveedorConfig = proveedorConfig;
		this.objectMapper = objectMapper;
		this.restClient = builder.build();
	}

	public RespuestaProveedor consultar(EndpointBusqueda endpoint, SolicitudConsulta solicitud) {
		validarToken();
		long inicio = System.currentTimeMillis();
		try {
			URI uri = construirUri(endpoint, solicitud);
			return restClient.get()
					.uri(uri)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + proveedorConfig.tokenActual())
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.accept(MediaType.APPLICATION_JSON)
					.exchange((request, response) -> procesarRespuesta(response, inicio, true));
		} catch (NawinException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new NawinException(CodigoError.PRO_001);
		}
	}

	public RespuestaProveedor consultarFacial(EndpointBusqueda endpoint, MultipartFile imagen) {
		validarToken();
		long inicio = System.currentTimeMillis();
		try {
			return restClient.post()
					.uri(proveedorConfig.baseUrlActual() + endpoint.getRutaProveedor())
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + proveedorConfig.tokenActual())
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(new MultipartBodyBuilderSeguro().archivo("image_facial", imagen).build())
					.exchange((request, response) -> procesarRespuesta(response, inicio, true));
		} catch (NawinException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new NawinException(CodigoError.PRO_001);
		}
	}

	private URI construirUri(EndpointBusqueda endpoint, SolicitudConsulta solicitud) {
		String ruta = endpoint.getRutaProveedor();
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(proveedorConfig.baseUrlActual());
		if (endpoint.getCodigo() == CodigoEndpoint.NM && solicitud instanceof ConsultaNombresRequest nombres) {
			builder.path(ruta);
			// CODART exige una palabra por parámetro (n1/n2 nombres, ap1/ap2
			// apellidos); un valor con espacios provoca un redirect 302 del
			// proveedor en lugar de resultados. El materno (ap2) es opcional.
			String[] segmentosNombre = nombres.nombres().trim().split("\\s+");
			builder.queryParam("n1", segmentosNombre[0]);
			if (segmentosNombre.length > 1) {
				builder.queryParam("n2", segmentosNombre[1]);
			}
			builder.queryParam("ap1", primeraPalabra(nombres.apellidoPaterno()));
			String ap2 = nombres.apellidoMaterno();
			if (ap2 != null && !ap2.isBlank()) {
				builder.queryParam("ap2", primeraPalabra(ap2));
			}
			return builder.build().toUri();
		}
		String valor = parametroPrincipal(endpoint.getCodigo(), solicitud);
		ruta = ruta.replace("{dni}", valor)
				.replace("{ruc}", valor)
				.replace("{numero}", valor)
				.replace("{placa}", valor);
		return builder.path(ruta).build().toUri();
	}

	private RespuestaProveedor procesarRespuesta(ClientHttpResponse response, long inicio, boolean permiteSinResultados)
			throws IOException {
		int codigoHttp = response.getStatusCode().value();
		JsonNode json = leerJson(response);
		if (codigoHttp >= 200 && codigoHttp < 300) {
			// Toda respuesta válida de CODART trae "success"; si no viene (p. ej.
			// el HTML de un redirect seguido), no debe guardarse como exitosa.
			if (!json.has("success")) {
				throw new NawinException(CodigoError.PRO_001,
						"CODART devolvió una respuesta inesperada (sin campo success).");
			}
			return new RespuestaProveedor(json, codigoHttp, System.currentTimeMillis() - inicio);
		}
		if (permiteSinResultados && codigoHttp == 404 && esRespuestaSinResultados(json)) {
			return new RespuestaProveedor(json, codigoHttp, System.currentTimeMillis() - inicio);
		}
		throw new NawinException(CodigoError.PRO_001, mensajeProveedor(codigoHttp, json));
	}

	private JsonNode leerJson(ClientHttpResponse response) throws IOException {
		String cuerpo = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
		if (!StringUtils.hasText(cuerpo)) {
			return objectMapper.createObjectNode();
		}
		try {
			return objectMapper.readTree(cuerpo);
		} catch (Exception ex) {
			return objectMapper.createObjectNode().put("message", cuerpo);
		}
	}

	private boolean esRespuestaSinResultados(JsonNode json) {
		String mensaje = json.path("message").asText("").toLowerCase();
		return json.has("success") && !json.path("success").asBoolean()
				&& (json.path("data").isNull() || mensaje.contains("no se encontr"));
	}

	private String mensajeProveedor(int codigoHttp, JsonNode json) {
		String mensaje = json.path("message").asText("");
		if (StringUtils.hasText(mensaje)) {
			return "CODART HTTP " + codigoHttp + ": " + mensaje;
		}
		return "CODART HTTP " + codigoHttp + ": proveedor temporalmente no disponible.";
	}

	private String parametroPrincipal(CodigoEndpoint codigo, SolicitudConsulta solicitud) {
		if (solicitud instanceof ConsultaRucRequest ruc) {
			return ruc.ruc();
		}
		if (solicitud instanceof ConsultaDniRequest dni) {
			return dni.dni();
		}
		if (solicitud instanceof ConsultaCriticaDniRequest dni) {
			return dni.dni();
		}
		if (solicitud instanceof ConsultaNumeroRequest numero) {
			return numero.numero();
		}
		if (solicitud instanceof ConsultaCriticaNumeroRequest numero) {
			return numero.numero();
		}
		if (solicitud instanceof ConsultaPlacaRequest placa) {
			return placa.placa();
		}
		if (solicitud instanceof ConsultaCriticaPlacaRequest placa) {
			return placa.placa();
		}
		throw new NawinException(CodigoError.CON_001, "Solicitud no compatible con " + codigo);
	}

	private String primeraPalabra(String texto) {
		return texto.trim().split("\\s+")[0];
	}

	private void validarToken() {
		if (!StringUtils.hasText(proveedorConfig.tokenActual())) {
			throw new NawinException(CodigoError.PRO_001, "Token de proveedor no configurado.");
		}
	}
}
