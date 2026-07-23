package pe.nawin.excepcion;

public class NawinException extends RuntimeException {

	private final CodigoError codigoError;

	public NawinException(CodigoError codigoError) {
		super(codigoError.mensaje());
		this.codigoError = codigoError;
	}

	public NawinException(CodigoError codigoError, String mensaje) {
		super(mensaje);
		this.codigoError = codigoError;
	}

	public CodigoError codigoError() {
		return codigoError;
	}
}
