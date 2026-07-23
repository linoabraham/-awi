package pe.nawin.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nawin")
public record NawinPropiedades(
		String zonaHoraria,
		String frontendOrigin,
		String almacenamientoPrivadoRuta,
		String cifradoDatosKey,
		Empresa empresa,
		Admin admin,
		Jwt jwt,
		Codart codart,
		Correo correo,
		Seguridad seguridad,
		Push push,
		Consultas consultas
) {
	public record Empresa(String nombre, String ruc, String direccion, String telefono) {
	}

	public record Admin(String usuario, String clave, String correo, String celular) {
	}

	public record Jwt(String secret, long accessMinutos, long refreshHorasWeb, long refreshDiasMovil) {
	}

	public record Codart(String baseUrl, String apiToken) {
	}

	public record Correo(boolean habilitado, String remitente, long expiracionMinutos, long reenvioSegundos, int maxIntentos) {
	}

	public record Seguridad(
			int maxIntentosLogin,
			long bloqueoMinutos,
			int rateLimitSolicitudes,
			long rateLimitVentanaSegundos,
			long horasPurgaNoVerificados,
			boolean exigirSecretosProduccion
	) {
	}

	/**
	 * Push (FCM). Cuando {@code habilitado=false} o faltan las credenciales, el
	 * envío corre en modo registro (solo log), sin romper nada. Las credenciales
	 * (JSON de la cuenta de servicio) se resuelven en este orden:
	 * <ol>
	 *   <li>{@code credencialesJson}: el CONTENIDO del JSON (ideal en producción,
	 *       vía variable de entorno secreta; sin archivos en disco).</li>
	 *   <li>{@code credencialesRuta} con prefijo {@code classpath:}: recurso del
	 *       backend (dev; funciona desde el IDE y el jar).</li>
	 *   <li>{@code credencialesRuta} como ruta de archivo (absoluta en el servidor).</li>
	 * </ol>
	 */
	public record Push(boolean habilitado, String proyectoId, String credencialesRuta, String credencialesJson) {
	}

	/**
	 * Consultas: control anti-abuso para membresías ilimitadas y retención de las
	 * consultas por créditos (que no tienen membresía de dónde tomar la retención).
	 * {@code topeDiarioIlimitado}: corte de seguridad por cliente/día en accesos
	 * ilimitados. {@code alertaConsumoDiario}: umbral para registrar aviso de uso
	 * anómalo (no bloquea). {@code diasRetencionCreditos}: retención de resultados
	 * en consultas solo-créditos. {@code permiteExportarCreditos}: si el free puede
	 * exportar archivos.
	 */
	public record Consultas(
			int topeDiarioIlimitado,
			int alertaConsumoDiario,
			int diasRetencionCreditos,
			boolean permiteExportarCreditos
	) {
	}
}
