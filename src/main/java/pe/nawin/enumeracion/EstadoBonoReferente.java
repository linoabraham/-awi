package pe.nawin.enumeracion;

/**
 * Estado del bono del referente en un canje de código: queda PENDIENTE al
 * canjear y se ACREDITA cuando el invitado hace su primera consulta exitosa
 * (anti-granjas de cuentas).
 */
public enum EstadoBonoReferente {
	PENDIENTE,
	ACREDITADO
}
