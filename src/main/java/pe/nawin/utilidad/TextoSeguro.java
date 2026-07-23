package pe.nawin.utilidad;

public final class TextoSeguro {

	private TextoSeguro() {
	}

	public static String mascara(String valor) {
		if (valor == null || valor.isBlank()) {
			return "";
		}
		String limpio = valor.trim();
		if (limpio.length() <= 4) {
			return "*".repeat(limpio.length());
		}
		return limpio.substring(0, 2) + "*".repeat(Math.max(1, limpio.length() - 4)) + limpio.substring(limpio.length() - 2);
	}
}
