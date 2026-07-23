package pe.nawin.utilidad;

import java.util.Set;

/**
 * Normaliza correos para detectar cuentas duplicadas por alias:
 * minúsculas, sin sufijo {@code +algo} en la parte local y, en Gmail,
 * sin puntos (Gmail ignora los puntos y los alias con +).
 */
public final class CorreoNormalizador {

	private static final Set<String> DOMINIOS_SIN_PUNTOS = Set.of("gmail.com", "googlemail.com");

	private CorreoNormalizador() {
	}

	public static String normalizar(String correo) {
		if (correo == null) {
			return null;
		}
		String texto = correo.trim().toLowerCase();
		int arroba = texto.lastIndexOf('@');
		if (arroba <= 0) {
			return texto;
		}
		String local = texto.substring(0, arroba);
		String dominio = texto.substring(arroba + 1);
		int mas = local.indexOf('+');
		if (mas >= 0) {
			local = local.substring(0, mas);
		}
		if (DOMINIOS_SIN_PUNTOS.contains(dominio)) {
			local = local.replace(".", "");
		}
		return local + "@" + dominio;
	}
}
