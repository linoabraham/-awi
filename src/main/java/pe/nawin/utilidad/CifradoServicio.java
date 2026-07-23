package pe.nawin.utilidad;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import pe.nawin.configuracion.NawinPropiedades;

@Service
public class CifradoServicio {

	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private final SecureRandom random = new SecureRandom();
	private final SecretKeySpec clave;

	public CifradoServicio(NawinPropiedades propiedades) {
		this.clave = new SecretKeySpec(sha256(propiedades.cifradoDatosKey()), "AES");
	}

	public String cifrar(String plano) {
		try {
			byte[] iv = new byte[IV_BYTES];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
			byte[] cifrado = cipher.doFinal(plano.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
		} catch (Exception ex) {
			throw new IllegalStateException("No fue posible cifrar datos sensibles", ex);
		}
	}

	public String descifrar(String textoCifrado) {
		try {
			byte[] combinado = Base64.getDecoder().decode(textoCifrado);
			ByteBuffer buffer = ByteBuffer.wrap(combinado);
			byte[] iv = new byte[IV_BYTES];
			buffer.get(iv);
			byte[] cifrado = new byte[buffer.remaining()];
			buffer.get(cifrado);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new IllegalStateException("No fue posible descifrar datos sensibles", ex);
		}
	}

	private byte[] sha256(String valor) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ex) {
			throw new IllegalStateException("SHA-256 no disponible", ex);
		}
	}
}
