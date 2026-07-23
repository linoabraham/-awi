package pe.nawin.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.entidad.Usuario;
import pe.nawin.repositorio.UsuarioRepositorio;

@Service
public class DetalleUsuarioServicio implements UserDetailsService {

	private final UsuarioRepositorio usuarioRepositorio;

	public DetalleUsuarioServicio(UsuarioRepositorio usuarioRepositorio) {
		this.usuarioRepositorio = usuarioRepositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// Permite iniciar sesión con nombre de usuario o con correo.
		Usuario usuario = usuarioRepositorio.findByNombreUsuario(username)
				.or(() -> usuarioRepositorio.findByCorreoIgnoreCase(username))
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

		// 👇 Forzar carga del Rol dentro de la transacción
		usuario.getRol().getCodigo(); // Inicializa el proxy

		return new UsuarioPrincipal(usuario);
	}
}