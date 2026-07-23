package pe.nawin.servicio;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.entidad.PreferenciaNotificacion;
import pe.nawin.entidad.TokenPush;
import pe.nawin.enumeracion.CategoriaNotificacion;
import pe.nawin.notificacion.NotificadorPushFcm;
import pe.nawin.repositorio.PreferenciaNotificacionRepositorio;
import pe.nawin.repositorio.TokenPushRepositorio;

/**
 * Orquesta el envío de notificaciones push: administra los tokens FCM por
 * dispositivo, respeta las preferencias del usuario y delega el transporte en
 * {@link NotificadorPushFcm}. El envío es best-effort y asíncrono: nunca rompe
 * ni bloquea el flujo que lo dispara (login, aprobación de pago, etc.).
 */
@Service
public class NotificacionPushServicio {

	private static final Logger log = LoggerFactory.getLogger(NotificacionPushServicio.class);

	private final TokenPushRepositorio tokenPushRepositorio;
	private final PreferenciaNotificacionRepositorio preferenciaRepositorio;
	private final NotificadorPushFcm notificador;

	public NotificacionPushServicio(TokenPushRepositorio tokenPushRepositorio,
			PreferenciaNotificacionRepositorio preferenciaRepositorio, NotificadorPushFcm notificador) {
		this.tokenPushRepositorio = tokenPushRepositorio;
		this.preferenciaRepositorio = preferenciaRepositorio;
		this.notificador = notificador;
	}

	/** Registra o actualiza el token FCM de un dispositivo (uno por usuario+instalación). */
	@Transactional
	public void registrarToken(Long idUsuario, String installationId, String fcmToken, String plataforma) {
		if (idUsuario == null || installationId == null || fcmToken == null || fcmToken.isBlank()) {
			return;
		}
		// El mismo token no puede quedar ligado a otro usuario/dispositivo (reinstalación, cambio de cuenta).
		tokenPushRepositorio.findByFcmToken(fcmToken).ifPresent(otro -> {
			if (!otro.getIdUsuario().equals(idUsuario) || !otro.getInstallationId().equals(installationId)) {
				tokenPushRepositorio.delete(otro);
			}
		});
		TokenPush token = tokenPushRepositorio.findByIdUsuarioAndInstallationId(idUsuario, installationId)
				.orElseGet(TokenPush::new);
		token.setIdUsuario(idUsuario);
		token.setInstallationId(installationId);
		token.setFcmToken(fcmToken);
		token.setPlataforma(plataforma);
		token.setActivo(true);
		tokenPushRepositorio.save(token);
	}

	/** Preferencias del usuario; crea las de por defecto (todo activo) si no existen. */
	@Transactional
	public PreferenciaNotificacion preferencias(Long idUsuario) {
		return preferenciaRepositorio.findByIdUsuario(idUsuario).orElseGet(() -> {
			PreferenciaNotificacion p = new PreferenciaNotificacion();
			p.setIdUsuario(idUsuario);
			return preferenciaRepositorio.save(p);
		});
	}

	@Transactional
	public PreferenciaNotificacion actualizarPreferencias(Long idUsuario, boolean push, boolean promos, boolean pagos,
			boolean consultas, boolean referidos, boolean seguridad) {
		PreferenciaNotificacion p = preferencias(idUsuario);
		p.setPush(push);
		p.setPromos(promos);
		p.setPagos(pagos);
		p.setConsultas(consultas);
		p.setReferidos(referidos);
		p.setSeguridad(seguridad);
		return preferenciaRepositorio.save(p);
	}

	/** Envía una push a todos los dispositivos del usuario, respetando sus preferencias. */
	@Async
	@Transactional
	public void enviarAUsuario(Long idUsuario, CategoriaNotificacion categoria, String titulo, String cuerpo,
			Map<String, String> datos) {
		try {
			if (idUsuario == null || !permitido(idUsuario, categoria)) {
				return;
			}
			List<TokenPush> tokens = tokenPushRepositorio.findByIdUsuarioAndActivoTrue(idUsuario);
			if (tokens.isEmpty()) {
				return;
			}
			for (TokenPush t : tokens) {
				entregar(t, titulo, cuerpo, datos,
						() -> log.info("[push-registro] usuario={} categoria={} titulo='{}'", idUsuario, categoria, titulo));
			}
		} catch (RuntimeException ex) {
			log.warn("Fallo al enviar push al usuario {}: {}", idUsuario, ex.getMessage());
		}
	}

	/** Envío masivo (panel admin). Ignora la categoría; respeta el interruptor maestro push. */
	@Async
	@Transactional
	public void enviarATodos(String titulo, String cuerpo, Map<String, String> datos) {
		try {
			for (TokenPush t : tokenPushRepositorio.findAll()) {
				if (!t.isActivo()) {
					continue;
				}
				PreferenciaNotificacion pref = preferenciaRepositorio.findByIdUsuario(t.getIdUsuario()).orElse(null);
				if (pref != null && !pref.isPush()) {
					continue;
				}
				entregar(t, titulo, cuerpo, datos,
						() -> log.info("[push-registro-masivo] usuario={} titulo='{}'", t.getIdUsuario(), titulo));
			}
		} catch (RuntimeException ex) {
			log.warn("Fallo en el envío masivo de push: {}", ex.getMessage());
		}
	}

	/** Envía a un token y desactiva el token si FCM lo reporta como muerto (UNREGISTERED). */
	private void entregar(TokenPush token, String titulo, String cuerpo, Map<String, String> datos,
			Runnable enModoRegistro) {
		NotificadorPushFcm.Resultado resultado = notificador.enviar(token.getFcmToken(), titulo, cuerpo, datos);
		if (resultado == NotificadorPushFcm.Resultado.TOKEN_INVALIDO) {
			token.setActivo(false);
			tokenPushRepositorio.save(token);
			log.info("Token FCM desactivado (no registrado) del usuario {}.", token.getIdUsuario());
		} else if (resultado != NotificadorPushFcm.Resultado.ENVIADO && !notificador.habilitado()) {
			enModoRegistro.run();
		}
	}

	private boolean permitido(Long idUsuario, CategoriaNotificacion categoria) {
		PreferenciaNotificacion p = preferenciaRepositorio.findByIdUsuario(idUsuario)
				.orElse(null);
		if (p == null) {
			return true; // Sin preferencias guardadas: todo permitido por defecto.
		}
		if (!p.isPush()) {
			return false; // Interruptor maestro apagado.
		}
		return switch (categoria) {
			case PROMOS -> p.isPromos();
			case PAGOS -> p.isPagos();
			case CONSULTAS -> p.isConsultas();
			case REFERIDOS -> p.isReferidos();
			case SEGURIDAD -> p.isSeguridad();
			case GENERAL -> true;
		};
	}
}
