package pe.nawin.seguridad;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * TOTP (RFC 6238) compatible con Google Authenticator / Authy: HMAC-SHA1,
 * códigos de 6 dígitos, paso de 30 segundos y tolerancia de ±1 ventana para
 * absorber desfase de reloj. El secreto se maneja en Base32 (RFC 4648) para
 * poder mostrarlo en el QR y en la app de autenticación.
 */
@Service
public class TotpServicio {

	private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
	private static final int DIGITOS = 6;
	private static final long PASO_SEGUNDOS = 30;
	private static final int VENTANA_TOLERANCIA = 1;
	private static final int SECRETO_BYTES = 20;

	private final SecureRandom secureRandom = new SecureRandom();

	/** Genera un secreto aleatorio de 160 bits codificado en Base32. */
	public String generarSecreto() {
		byte[] bytes = new byte[SECRETO_BYTES];
		secureRandom.nextBytes(bytes);
		return base32Encode(bytes);
	}

	/** URI otpauth:// para generar el QR de alta en la app de autenticación. */
	public String construirUriOtpauth(String emisor, String cuenta, String secretoBase32) {
		String emisorCodificado = URLEncoder.encode(emisor, StandardCharsets.UTF_8);
		String cuentaCodificada = URLEncoder.encode(cuenta, StandardCharsets.UTF_8);
		return "otpauth://totp/" + emisorCodificado + ":" + cuentaCodificada
				+ "?secret=" + secretoBase32
				+ "&issuer=" + emisorCodificado
				+ "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PASO_SEGUNDOS;
	}

	/** Valida el código ingresado contra la ventana actual y las adyacentes. */
	public boolean verificar(String secretoBase32, String codigoIngresado) {
		if (secretoBase32 == null || codigoIngresado == null) {
			return false;
		}
		String limpio = codigoIngresado.trim().replaceAll("\\s", "");
		if (!limpio.matches("\\d{" + DIGITOS + "}")) {
			return false;
		}
		byte[] clave = base32Decode(secretoBase32);
		long ventana = System.currentTimeMillis() / 1000L / PASO_SEGUNDOS;
		for (int desfase = -VENTANA_TOLERANCIA; desfase <= VENTANA_TOLERANCIA; desfase++) {
			if (constanteIguales(limpio, generarCodigo(clave, ventana + desfase))) {
				return true;
			}
		}
		return false;
	}

	private String generarCodigo(byte[] clave, long contador) {
		byte[] datos = new byte[8];
		long valor = contador;
		for (int i = 7; i >= 0; i--) {
			datos[i] = (byte) (valor & 0xff);
			valor >>= 8;
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(clave, "HmacSHA1"));
			byte[] hash = mac.doFinal(datos);
			int offset = hash[hash.length - 1] & 0xf;
			int binario = ((hash[offset] & 0x7f) << 24)
					| ((hash[offset + 1] & 0xff) << 16)
					| ((hash[offset + 2] & 0xff) << 8)
					| (hash[offset + 3] & 0xff);
			int codigo = binario % (int) Math.pow(10, DIGITOS);
			return String.format("%0" + DIGITOS + "d", codigo);
		} catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException("No fue posible calcular el código TOTP", ex);
		}
	}

	/** Comparación en tiempo constante para no filtrar información por timing. */
	private boolean constanteIguales(String a, String b) {
		if (a.length() != b.length()) {
			return false;
		}
		int resultado = 0;
		for (int i = 0; i < a.length(); i++) {
			resultado |= a.charAt(i) ^ b.charAt(i);
		}
		return resultado == 0;
	}

	private String base32Encode(byte[] datos) {
		StringBuilder sb = new StringBuilder();
		int buffer = 0;
		int bits = 0;
		for (byte b : datos) {
			buffer = (buffer << 8) | (b & 0xff);
			bits += 8;
			while (bits >= 5) {
				bits -= 5;
				sb.append(BASE32.charAt((buffer >> bits) & 0x1f));
			}
		}
		if (bits > 0) {
			sb.append(BASE32.charAt((buffer << (5 - bits)) & 0x1f));
		}
		return sb.toString();
	}

	private byte[] base32Decode(String texto) {
		String limpio = texto.trim().replaceAll("[=\\s]", "").toUpperCase();
		int buffer = 0;
		int bits = 0;
		byte[] salida = new byte[limpio.length() * 5 / 8];
		int indice = 0;
		for (char c : limpio.toCharArray()) {
			int valor = BASE32.indexOf(c);
			if (valor < 0) {
				continue;
			}
			buffer = (buffer << 5) | valor;
			bits += 5;
			if (bits >= 8) {
				bits -= 8;
				salida[indice++] = (byte) ((buffer >> bits) & 0xff);
			}
		}
		return salida;
	}
}
