package pe.nawin.servicio;

import jakarta.mail.internet.MimeMessage;
import java.time.Year;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;

/**
 * Correos transaccionales de Ñawi (verificación, reset y cambio de clave,
 * bienvenida). Todas comparten una plantilla HTML con la identidad de la marca
 * (tema oscuro + verde). Si el envío está deshabilitado el código se registra en
 * el log para poder probar en desarrollo sin SMTP.
 */
@Service
public class CorreoServicio {

	private static final Logger log = LoggerFactory.getLogger(CorreoServicio.class);

	// Paleta Ñawi (email-safe, colores absolutos).
	private static final String VERDE = "#61FF1A";
	private static final String NEGRO = "#0B0B0B";
	private static final String CARD = "#15181B";
	private static final String BORDE = "#2A2E31";
	private static final String TEXTO = "#F4F1F1";
	private static final String GRIS = "#9AA0A6";

	// Marca comercial y dirección para los correos (independiente de la razón
	// social legal de la empresa, que se usa en los comprobantes).
	private static final String MARCA = "Ñawi";
	private static final String DIRECCION = "Av. Las Camelias 423, San Isidro - Lima";

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final NawinPropiedades propiedades;

	public CorreoServicio(ObjectProvider<JavaMailSender> mailSenderProvider, NawinPropiedades propiedades) {
		this.mailSenderProvider = mailSenderProvider;
		this.propiedades = propiedades;
	}

	// @Async: el envío corre en segundo plano para que el tiempo de respuesta de
	// los endpoints (reenviar código, recuperar clave) sea el mismo exista o no el
	// correo, y no se pueda deducir por latencia si una cuenta está registrada.
	@Async
	public void enviarCodigoVerificacion(String destinatario, String nombre, String codigo, long expiracionMinutos) {
		String contenido = bloqueCodigo(
				"Verifica tu correo 📩",
				saludo(nombre),
				"¡Bienvenido a " + marca() + "! Usa este código en la app para confirmar tu correo y activar tu cuenta.",
				codigo, expiracionMinutos);
		enviar(destinatario, "Verifica tu correo · " + marca(),
				documento("Tu código de verificación de " + marca(), contenido), codigo);
	}

	@Async
	public void enviarCodigoResetClave(String destinatario, String nombre, String codigo, long expiracionMinutos) {
		String contenido = bloqueCodigo(
				"Recupera tu contraseña 🔐",
				saludo(nombre),
				"Recibimos una solicitud para restablecer tu contraseña. Ingresa este código en la app para continuar.",
				codigo, expiracionMinutos);
		enviar(destinatario, "Recupera tu contraseña · " + marca(),
				documento("Código para restablecer tu contraseña", contenido), codigo);
	}

	@Async
	public void enviarCodigoCambioClave(String destinatario, String nombre, String codigo, long expiracionMinutos) {
		String contenido = bloqueCodigo(
				"Confirma el cambio 🔐",
				saludo(nombre),
				"Estás cambiando tu contraseña desde la app. Ingresa este código para confirmar que eres tú.",
				codigo, expiracionMinutos);
		enviar(destinatario, "Confirma tu nueva contraseña · " + marca(),
				documento("Código para cambiar tu contraseña", contenido), codigo);
	}

	@Async
	public void enviarBienvenida(String destinatario, String nombre) {
		String contenido = """
					<tr><td style="padding:0 32px 8px;">
						<h1 style="margin:0;color:%s;font-size:24px;font-weight:800;">¡Tu cuenta está lista! 🎉</h1>
					</td></tr>
					<tr><td style="padding:6px 32px 0;color:%s;font-size:15px;line-height:1.6;">
						%s Gracias por unirte a %s. Ya puedes hacer tus consultas y aprovechar tus créditos.
					</td></tr>
					<tr><td style="padding:20px 32px 4px;">
						%s
						%s
						%s
					</td></tr>
					<tr><td style="padding:10px 32px 0;color:%s;font-size:13px;line-height:1.6;">
						Invita a tus amigos con tu código de referido y gana créditos cuando hagan su primera consulta.
					</td></tr>
				""".formatted(VERDE, TEXTO, saludo(nombre), marca(),
				beneficio("Consulta con créditos", "Sin plan obligatorio: pagas solo por lo que usas."),
				beneficio("Recarga fácil", "Compra créditos con Yape y súbelos en segundos."),
				beneficio("Tu cuenta protegida", "Máximo 2 dispositivos y alertas de seguridad."),
				GRIS);
		enviar(destinatario, "¡Bienvenido a " + marca() + "! 🎉",
				documento("Tu cuenta de " + marca() + " ya está activa", contenido), null);
	}

	// ------------------------------------------------------------------
	// Plantilla base
	// ------------------------------------------------------------------

	private String marca() {
		return MARCA;
	}

	private String saludo(String nombre) {
		String limpio = nombre == null ? "" : nombre.trim().split("\\s+")[0];
		return limpio.isBlank() ? "Hola," : "Hola " + limpio + ",";
	}

	/** Bloque con el código destacado (verificación, reset, cambio de clave). */
	private String bloqueCodigo(String titulo, String saludo, String intro, String codigo, long expiracionMinutos) {
		return """
					<tr><td style="padding:0 32px 6px;">
						<h1 style="margin:0;color:%s;font-size:23px;font-weight:800;">%s</h1>
					</td></tr>
					<tr><td style="padding:6px 32px 0;color:%s;font-size:15px;line-height:1.6;">%s</td></tr>
					<tr><td style="padding:4px 32px 0;color:%s;font-size:15px;line-height:1.6;">%s</td></tr>
					<tr><td align="center" style="padding:24px 32px 8px;">
						<table role="presentation" cellpadding="0" cellspacing="0"><tr>
							<td style="background:%s;border:2px dashed %s;border-radius:12px;
								padding:13px 20px;font-size:27px;font-weight:800;letter-spacing:9px;
								color:%s;font-family:'Courier New',monospace;">%s</td>
						</tr></table>
					</td></tr>
					<tr><td style="padding:6px 32px 0;color:%s;font-size:13px;line-height:1.6;">
						El código vence en %d minutos y solo puede usarse una vez.
						Si no fuiste tú, ignora este mensaje: tu cuenta sigue segura.
					</td></tr>
				""".formatted(VERDE, titulo, TEXTO, saludo, TEXTO, intro,
				NEGRO, VERDE, TEXTO, espaciar(codigo), GRIS, expiracionMinutos);
	}

	private String beneficio(String titulo, String detalle) {
		return """
				<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:10px;">
					<tr>
						<td width="34" valign="top" style="padding-top:2px;">
							<div style="width:22px;height:22px;border-radius:50%%;background:%s;color:%s;
								text-align:center;line-height:22px;font-weight:800;font-size:13px;">✓</div>
						</td>
						<td style="color:%s;font-size:14px;line-height:1.5;">
							<strong style="color:%s;">%s.</strong> %s
						</td>
					</tr>
				</table>
				""".formatted(VERDE, NEGRO, GRIS, TEXTO, titulo, detalle);
	}

	/** Separa el código con espacios finos para que se lea de un vistazo. */
	private String espaciar(String codigo) {
		return codigo == null ? "" : String.join(" ", codigo.split(""));
	}

	/** Envuelve el contenido en la maqueta completa (header con marca + card + footer). */
	private String documento(String preheader, String contenidoCard) {
		return """
				<!DOCTYPE html>
				<html lang="es"><head><meta charset="UTF-8">
				<meta name="viewport" content="width=device-width,initial-scale=1"></head>
				<body style="margin:0;padding:0;background:%s;">
				<span style="display:none;max-height:0;overflow:hidden;opacity:0;">%s</span>
				<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:%s;padding:28px 12px;">
					<tr><td align="center">
						<table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;">
							<tr><td align="center" style="padding:4px 0 22px;">
								<table role="presentation" cellpadding="0" cellspacing="0"><tr>
									<td style="width:44px;height:44px;background:%s;border-radius:12px;text-align:center;
										vertical-align:middle;font-size:26px;font-weight:800;color:%s;">Ñ</td>
									<td style="padding-left:12px;font-size:24px;font-weight:800;color:%s;
										font-family:Arial,Helvetica,sans-serif;letter-spacing:0.5px;">%s</td>
								</tr></table>
							</td></tr>
							<tr><td style="background:%s;border:1px solid %s;border-radius:20px;
								font-family:Arial,Helvetica,sans-serif;">
								<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:28px 0;">
									%s
								</table>
							</td></tr>
							<tr><td align="center" style="padding:20px 24px 0;color:%s;font-size:12px;line-height:1.6;
								font-family:Arial,Helvetica,sans-serif;">
								%s%s<br>
								Este es un correo automático, por favor no respondas.<br>
								© %d %s. Todos los derechos reservados.
							</td></tr>
						</table>
					</td></tr>
				</table>
				</body></html>
				""".formatted(
				NEGRO, preheader, NEGRO,
				VERDE, NEGRO, TEXTO, marca(),
				CARD, BORDE,
				contenidoCard,
				GRIS,
				marca(),
				direccionFooter(),
				Year.now().getValue(), marca());
	}

	private String direccionFooter() {
		return " · " + DIRECCION;
	}

	private void enviar(String destinatario, String asunto, String cuerpoHtml, String codigo) {
		if (!propiedades.correo().habilitado()) {
			log.warn("[CORREO DESHABILITADO] Para: {} | Asunto: {} | Código: {}", destinatario, asunto, codigo);
			return;
		}
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (mailSender == null) {
			log.error("Correo habilitado pero spring.mail no está configurado (MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD).");
			throw new NawinException(CodigoError.GEN_001, "El servicio de correo no está disponible. Intenta más tarde.");
		}
		try {
			MimeMessage mensaje = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
			helper.setFrom(propiedades.correo().remitente());
			helper.setTo(destinatario);
			helper.setSubject(asunto);
			helper.setText(cuerpoHtml, true);
			mailSender.send(mensaje);
		} catch (Exception ex) {
			log.error("No se pudo enviar el correo a {}", destinatario, ex);
			throw new NawinException(CodigoError.GEN_001, "No se pudo enviar el correo. Verifica la dirección e intenta de nuevo.");
		}
	}
}
