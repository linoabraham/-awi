package pe.nawin.enumeracion;

/** Tipos de eventos de auditoría de seguridad (tabla eventos_seguridad). */
public enum TipoEventoSeguridad {
	LOGIN_SUCCESS("Inicio de sesión"),
	NEW_DEVICE_LOGIN("Inicio en un dispositivo nuevo"),
	DEVICE_LIMIT_REACHED("Límite de dispositivos alcanzado"),
	SESSION_REVOKED("Sesión de un dispositivo cerrada"),
	DEVICE_TRUSTED("Dispositivo marcado como de confianza"),
	DEVICE_BLOCKED("Dispositivo bloqueado"),
	LOGOUT_ALL_DEVICES("Cierre de las demás sesiones"),
	PASSWORD_CHANGED("Cambio de contraseña");

	private final String descripcion;

	TipoEventoSeguridad(String descripcion) {
		this.descripcion = descripcion;
	}

	public String descripcion() {
		return descripcion;
	}
}
