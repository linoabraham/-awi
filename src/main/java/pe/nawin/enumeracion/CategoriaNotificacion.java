package pe.nawin.enumeracion;

/**
 * Categoría de una notificación push. Determina qué preferencia del usuario la
 * gobierna (además del interruptor maestro {@code push}).
 */
public enum CategoriaNotificacion {
	GENERAL,
	PROMOS,
	PAGOS,
	CONSULTAS,
	REFERIDOS,
	SEGURIDAD
}
