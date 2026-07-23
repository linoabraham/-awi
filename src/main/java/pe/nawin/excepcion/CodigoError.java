package pe.nawin.excepcion;

import org.springframework.http.HttpStatus;

public enum CodigoError {
	AUTH_001(HttpStatus.UNAUTHORIZED, "Credenciales inválidas."),
	AUTH_002(HttpStatus.UNAUTHORIZED, "Token vencido o inválido."),
	AUTH_003(HttpStatus.FORBIDDEN, "Rol sin permiso."),
	AUTH_004(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Tu cuenta quedó bloqueada temporalmente, intenta en unos minutos."),
	AUTH_005(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes. Espera unos segundos e intenta de nuevo."),
	USR_001(HttpStatus.LOCKED, "Usuario bloqueado o inactivo."),
	CLI_001(HttpStatus.NOT_FOUND, "Cliente no encontrado."),
	MEM_001(HttpStatus.FORBIDDEN, "Membresía no activa."),
	MEM_002(HttpStatus.FORBIDDEN, "Membresía vencida."),
	END_001(HttpStatus.FORBIDDEN, "Endpoint no habilitado en la membresía."),
	CUO_001(HttpStatus.TOO_MANY_REQUESTS, "Cuota diaria agotada."),
	CUO_002(HttpStatus.FORBIDDEN, "Cuota del ciclo agotada."),
	CRE_001(HttpStatus.PAYMENT_REQUIRED, "Créditos insuficientes."),
	MFA_001(HttpStatus.FORBIDDEN, "MFA requerido o inválido."),
	DEV_001(HttpStatus.CONFLICT, "Alcanzaste el máximo de 2 dispositivos. Cierra sesión en uno para continuar."),
	DEV_002(HttpStatus.UNAUTHORIZED, "Esta sesión fue cerrada desde otro dispositivo."),
	VER_001(HttpStatus.BAD_REQUEST, "Código inválido o vencido."),
	VER_002(HttpStatus.FORBIDDEN, "Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada."),
	VER_003(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera un momento y solicita un nuevo código."),
	CON_001(HttpStatus.BAD_REQUEST, "Parámetro de consulta inválido."),
	CON_002(HttpStatus.CONFLICT, "Consulta duplicada por idempotencia."),
	PRO_001(HttpStatus.SERVICE_UNAVAILABLE, "Proveedor temporalmente no disponible."),
	PAG_001(HttpStatus.CONFLICT, "Pago no puede ser confirmado."),
	COM_001(HttpStatus.CONFLICT, "Comprobante ya emitido."),
	GEN_001(HttpStatus.BAD_REQUEST, "Solicitud inválida."),
	GEN_002(HttpStatus.NOT_FOUND, "Recurso no encontrado.");

	private final HttpStatus estadoHttp;
	private final String mensaje;

	CodigoError(HttpStatus estadoHttp, String mensaje) {
		this.estadoHttp = estadoHttp;
		this.mensaje = mensaje;
	}

	public HttpStatus estadoHttp() {
		return estadoHttp;
	}

	public String mensaje() {
		return mensaje;
	}
}
