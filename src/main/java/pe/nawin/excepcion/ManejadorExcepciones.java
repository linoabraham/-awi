package pe.nawin.excepcion;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.nawin.dto.respuesta.ErrorRespuesta;
import pe.nawin.dto.respuesta.RespuestaApi;

@RestControllerAdvice
public class ManejadorExcepciones {

	private static final Logger log = LoggerFactory.getLogger(ManejadorExcepciones.class);

	@ExceptionHandler(NawinException.class)
	ResponseEntity<RespuestaApi<ErrorRespuesta>> manejarNawin(NawinException ex) {
		CodigoError codigo = ex.codigoError();
		return ResponseEntity.status(codigo.estadoHttp())
				.body(RespuestaApi.error(ex.getMessage(), ErrorRespuesta.simple(codigo.name())));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<RespuestaApi<ErrorRespuesta>> manejarValidacion(MethodArgumentNotValidException ex) {
		Map<String, String> errores = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errores.put(error.getField(), error.getDefaultMessage());
		}
		return ResponseEntity.badRequest()
				.body(RespuestaApi.error(CodigoError.GEN_001.mensaje(), new ErrorRespuesta(CodigoError.GEN_001.name(), errores)));
	}

	// Todas las fallas de autenticación (credenciales, cuenta deshabilitada o
	// bloqueada) responden igual para no revelar qué usuarios existen ni su estado.
	@ExceptionHandler({BadCredentialsException.class, DisabledException.class, LockedException.class,
			AuthenticationException.class})
	ResponseEntity<RespuestaApi<ErrorRespuesta>> manejarCredenciales() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(RespuestaApi.error(CodigoError.AUTH_001.mensaje(), ErrorRespuesta.simple(CodigoError.AUTH_001.name())));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<RespuestaApi<ErrorRespuesta>> manejarAccesoDenegado() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(RespuestaApi.error(CodigoError.AUTH_003.mensaje(), ErrorRespuesta.simple(CodigoError.AUTH_003.name())));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<RespuestaApi<ErrorRespuesta>> manejarGeneral(Exception ex) {
		// Se registra la causa real para no perder el diagnóstico (antes se
		// devolvía un 400 genérico sin dejar rastro del error).
		log.error("Excepción no controlada", ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(RespuestaApi.error(CodigoError.GEN_001.mensaje(), ErrorRespuesta.simple(CodigoError.GEN_001.name())));
	}
}
