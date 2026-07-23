package pe.nawin.dto.respuesta;

import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.enumeracion.RolCodigo;

public record UsuarioActualRespuesta(
		Long idUsuario,
		String nombreUsuario,
		String nombres,
		String apellidos,
		String correo,
		String celular,
		RolCodigo rol,
		EstadoUsuario estado,
		boolean mfaHabilitado,
		boolean correoVerificado
) {
}
