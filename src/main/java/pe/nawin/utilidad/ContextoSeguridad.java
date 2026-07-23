package pe.nawin.utilidad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.seguridad.UsuarioPrincipal;

public final class ContextoSeguridad {

	private ContextoSeguridad() {
	}

	public static UsuarioPrincipal usuarioActual() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
			throw new NawinException(CodigoError.AUTH_002);
		}
		return principal;
	}
}
