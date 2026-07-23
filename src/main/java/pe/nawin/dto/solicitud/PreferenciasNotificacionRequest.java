package pe.nawin.dto.solicitud;

/** Preferencias de notificación del usuario (interruptor maestro + categorías). */
public record PreferenciasNotificacionRequest(
		boolean push,
		boolean promos,
		boolean pagos,
		boolean consultas,
		boolean referidos,
		boolean seguridad
) {
}
