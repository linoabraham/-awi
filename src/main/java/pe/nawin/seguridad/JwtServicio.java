package pe.nawin.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.TipoCliente;

@Service
public class JwtServicio {

	private final NawinPropiedades propiedades;
	private final SecretKey clave;

	private static final Logger log = LoggerFactory.getLogger(JwtServicio.class);
	private static final String SECRET_DEV = "dev-dev-dev-dev-dev-dev-dev-dev-dev-dev-dev-dev-dev-dev-dev-dev";

	public JwtServicio(NawinPropiedades propiedades) {
		this.propiedades = propiedades;
		validarSecreto(propiedades);
		this.clave = Keys.hmacShaKeyFor(sha256(propiedades.jwt().secret()));
	}

	/**
	 * Evita arrancar en producción con el secreto de desarrollo (que es público en
	 * el repo y permitiría falsificar tokens). En desarrollo solo advierte; en
	 * producción se activa con {@code SEG_EXIGIR_SECRETOS=true} y aborta el arranque.
	 */
	private void validarSecreto(NawinPropiedades propiedades) {
		String secreto = propiedades.jwt().secret();
		boolean inseguro = secreto == null || secreto.isBlank() || SECRET_DEV.equals(secreto) || secreto.length() < 32;
		if (!inseguro) {
			return;
		}
		if (propiedades.seguridad().exigirSecretosProduccion()) {
			throw new IllegalStateException(
					"JWT_SECRET inseguro o por defecto. Configura un secreto aleatorio de al menos 32 caracteres en producción.");
		}
		log.warn("⚠️  JWT_SECRET usa el valor por defecto de desarrollo. NO lo uses en producción: "
				+ "define JWT_SECRET y SEG_EXIGIR_SECRETOS=true.");
	}

	/**
	 * Genera el access token. Incluye {@code sid} (id de la sesión / token de
	 * refresco) y {@code deviceId} (installationId) para poder validar en cada
	 * request que la sesión siga activa y cerrarla en remoto ("sesión cerrada
	 * desde otro dispositivo"). {@code jti} identifica de forma única al token.
	 */
	public String generarTokenAcceso(Usuario usuario, Long sid, String deviceId) {
		Instant ahora = Instant.now();
		Instant expiracion = ahora.plusSeconds(propiedades.jwt().accessMinutos() * 60);
		var builder = Jwts.builder()
				.subject(String.valueOf(usuario.getIdUsuario()))
				.claim("usuario", usuario.getNombreUsuario())
				.claim("rol", usuario.getRol().getCodigo().name())
				.id(UUID.randomUUID().toString())
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(expiracion))
				.signWith(clave);
		if (sid != null) {
			builder.claim("sid", sid);
		}
		if (deviceId != null && !deviceId.isBlank()) {
			builder.claim("deviceId", deviceId);
		}
		return builder.compact();
	}

	public Claims validar(String token) {
		return Jwts.parser()
				.verifyWith(clave)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public long accessMinutos() {
		return propiedades.jwt().accessMinutos();
	}

	/** Duración del refresh token según el cliente: sesión corta en web, larga y deslizante en móvil. */
	public Duration duracionRefresh(TipoCliente tipoCliente) {
		if (tipoCliente == TipoCliente.MOVIL) {
			return Duration.ofDays(propiedades.jwt().refreshDiasMovil());
		}
		return Duration.ofHours(propiedades.jwt().refreshHorasWeb());
	}

	private byte[] sha256(String valor) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 no disponible", ex);
		}
	}
}
