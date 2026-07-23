package pe.nawin.dto.respuesta;

public record TokenRespuesta(
		String tokenAcceso,
		String tokenRefresco,
		long expiraEnSegundos,
		UsuarioActualRespuesta usuario
) {
}
