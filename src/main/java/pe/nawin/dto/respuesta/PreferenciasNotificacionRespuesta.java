package pe.nawin.dto.respuesta;

import pe.nawin.entidad.PreferenciaNotificacion;

/** Preferencias de notificación del usuario. */
public record PreferenciasNotificacionRespuesta(
		boolean push,
		boolean promos,
		boolean pagos,
		boolean consultas,
		boolean referidos,
		boolean seguridad
) {
	public static PreferenciasNotificacionRespuesta de(PreferenciaNotificacion p) {
		return new PreferenciasNotificacionRespuesta(
				p.isPush(), p.isPromos(), p.isPagos(), p.isConsultas(), p.isReferidos(), p.isSeguridad());
	}
}
