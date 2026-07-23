package pe.nawin.notificacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pe.nawin.configuracion.NawinPropiedades;

/**
 * Transporte de push vía FCM HTTP v1. Autofirmado con la cuenta de servicio de
 * Firebase (OAuth2 JWT-bearer firmado con JJWT RS256, sin dependencias extra).
 *
 * <p>Si no está habilitado o falta el archivo de credenciales, {@link #habilitado()}
 * devuelve false y el orquestador cae a modo registro (solo log). Todos los envíos
 * están protegidos: un fallo se registra pero nunca se propaga.
 */
@Component
public class NotificadorPushFcm {

	private static final Logger log = LoggerFactory.getLogger(NotificadorPushFcm.class);
	private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
	private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/{proyecto}/messages:send";

	private final NawinPropiedades propiedades;
	private final ObjectMapper mapper;
	private final RestClient rest;

	// Credenciales cargadas perezosamente desde el JSON de la cuenta de servicio.
	private volatile boolean inicializado;
	private volatile boolean disponible;
	private String clientEmail;
	private String tokenUri;
	private String proyectoId;
	private PrivateKey clavePrivada;

	// Cache del access token de Google (válido ~1h).
	private volatile String accessToken;
	private volatile Instant accessTokenExpira = Instant.EPOCH;

	public NotificadorPushFcm(NawinPropiedades propiedades, ObjectMapper mapper, RestClient.Builder builder) {
		this.propiedades = propiedades;
		this.mapper = mapper;
		this.rest = builder.build();
	}

	public boolean habilitado() {
		NawinPropiedades.Push push = propiedades.push();
		if (push == null || !push.habilitado()) {
			return false;
		}
		inicializar(push);
		return disponible;
	}

	/** Resultado de un envío: entregado, fallo transitorio, o token muerto (a desactivar). */
	public enum Resultado { ENVIADO, FALLO, TOKEN_INVALIDO }

	/** Envía una push a un token FCM. */
	public Resultado enviar(String fcmToken, String titulo, String cuerpo, Map<String, String> datos) {
		if (!habilitado()) {
			return Resultado.FALLO;
		}
		try {
			ObjectNode raiz = mapper.createObjectNode();
			ObjectNode message = raiz.putObject("message");
			message.put("token", fcmToken);
			ObjectNode notif = message.putObject("notification");
			notif.put("title", titulo);
			notif.put("body", cuerpo);
			if (datos != null && !datos.isEmpty()) {
				ObjectNode data = message.putObject("data");
				datos.forEach(data::put);
			}
			rest.post()
					.uri(FCM_URL, proyectoId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + obtenerAccessToken())
					.contentType(MediaType.APPLICATION_JSON)
					.body(raiz)
					.retrieve()
					.toBodilessEntity();
			return Resultado.ENVIADO;
		} catch (HttpClientErrorException ex) {
			// Token muerto (app desinstalada / token rotado): 404 UNREGISTERED o similar.
			String cuerpoError = ex.getResponseBodyAsString();
			if (ex.getStatusCode().value() == 404 || cuerpoError.contains("UNREGISTERED")
					|| cuerpoError.contains("NotRegistered")) {
				return Resultado.TOKEN_INVALIDO;
			}
			log.warn("Fallo al enviar push FCM: {}", ex.getMessage());
			return Resultado.FALLO;
		} catch (RuntimeException ex) {
			log.warn("Fallo al enviar push FCM: {}", ex.getMessage());
			return Resultado.FALLO;
		}
	}

	private synchronized void inicializar(NawinPropiedades.Push push) {
		if (inicializado) {
			return;
		}
		inicializado = true;
		try {
			String contenido = cargarContenidoCredenciales(push);
			if (contenido == null) {
				return; // El motivo ya quedó registrado en el log.
			}
			JsonNode cuenta = mapper.readTree(contenido);
			this.clientEmail = cuenta.path("client_email").asText();
			this.tokenUri = cuenta.path("token_uri").asText("https://oauth2.googleapis.com/token");
			this.proyectoId = StringUtils.hasText(push.proyectoId())
					? push.proyectoId()
					: cuenta.path("project_id").asText();
			this.clavePrivada = leerClavePrivada(cuenta.path("private_key").asText());
			this.disponible = StringUtils.hasText(clientEmail) && clavePrivada != null
					&& StringUtils.hasText(proyectoId);
			if (disponible) {
				log.info("Push FCM inicializado para el proyecto {}.", proyectoId);
			}
		} catch (Exception ex) {
			log.warn("No se pudo inicializar Push FCM ({}): se usa modo registro.", ex.getMessage());
			this.disponible = false;
		}
	}

	/**
	 * Resuelve el contenido del JSON de credenciales según la prioridad: variable
	 * con el contenido &rarr; recurso classpath &rarr; archivo en disco. Devuelve
	 * null (y registra el motivo) si no hay credenciales disponibles.
	 */
	private String cargarContenidoCredenciales(NawinPropiedades.Push push) throws IOException {
		if (StringUtils.hasText(push.credencialesJson())) {
			return push.credencialesJson();
		}
		String ruta = push.credencialesRuta();
		if (!StringUtils.hasText(ruta)) {
			log.info("Push FCM habilitado pero sin credenciales (PUSH_CREDENCIALES_JSON / PUSH_CREDENCIALES): "
					+ "se usa modo registro.");
			return null;
		}
		if (ruta.startsWith("classpath:")) {
			ClassPathResource recurso = new ClassPathResource(ruta.substring("classpath:".length()));
			if (!recurso.exists()) {
				log.warn("Push FCM: no se encontró el recurso '{}' en el classpath: se usa modo registro.", ruta);
				return null;
			}
			try (InputStream in = recurso.getInputStream()) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
		Path archivo = Path.of(ruta);
		if (!Files.exists(archivo)) {
			log.warn("Push FCM: no se encontró el archivo de credenciales en '{}' (working dir: '{}'). "
					+ "Usa PUSH_CREDENCIALES_JSON, un recurso classpath: o una ruta absoluta. Se usa modo registro.",
					archivo.toAbsolutePath(), System.getProperty("user.dir"));
			return null;
		}
		return Files.readString(archivo, StandardCharsets.UTF_8);
	}

	private PrivateKey leerClavePrivada(String pem) throws Exception {
		String limpio = pem
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
		byte[] der = Base64.getDecoder().decode(limpio);
		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
	}

	private synchronized String obtenerAccessToken() {
		if (accessToken != null && Instant.now().isBefore(accessTokenExpira.minusSeconds(60))) {
			return accessToken;
		}
		Instant ahora = Instant.now();
		// aud DEBE ser un string simple (el token_uri). Con .audience().add() JJWT lo
		// serializa como array y Google lo rechaza ("Failed audience check").
		String assertion = Jwts.builder()
				.issuer(clientEmail)
				.claim("aud", tokenUri)
				.claim("scope", SCOPE)
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(ahora.plusSeconds(3600)))
				.signWith(clavePrivada, Jwts.SIG.RS256)
				.compact();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		form.add("assertion", assertion);
		JsonNode respuesta = rest.post()
				.uri(tokenUri)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(JsonNode.class);
		String token = respuesta == null ? null : respuesta.path("access_token").asText(null);
		long expiraEn = respuesta == null ? 3600 : respuesta.path("expires_in").asLong(3600);
		this.accessToken = token;
		this.accessTokenExpira = ahora.plusSeconds(expiraEn);
		return token;
	}
}
