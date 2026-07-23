package pe.nawin.configuracion;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.entidad.Rol;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.enumeracion.RolCodigo;
import pe.nawin.repositorio.RolRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;

@Component
public class ArranqueSistema implements CommandLineRunner {

	private final NawinPropiedades propiedades;
	private final RolRepositorio rolRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final PasswordEncoder passwordEncoder;

	public ArranqueSistema(NawinPropiedades propiedades, RolRepositorio rolRepositorio,
			UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
		this.propiedades = propiedades;
		this.rolRepositorio = rolRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (usuarioRepositorio.existsByNombreUsuario(propiedades.admin().usuario())) {
			return;
		}
		Rol rolAdmin = rolRepositorio.findByCodigo(RolCodigo.ADMIN)
				.orElseThrow(() -> new IllegalStateException("Rol ADMIN no existe"));
		Usuario admin = new Usuario();
		admin.setRol(rolAdmin);
		admin.setNombres("Administrador");
		admin.setApellidos("NAWIN");
		admin.setCorreo(propiedades.admin().correo());
		admin.setCelular(propiedades.admin().celular());
		admin.setNombreUsuario(propiedades.admin().usuario());
		admin.setClaveHash(passwordEncoder.encode(propiedades.admin().clave()));
		admin.setEstado(EstadoUsuario.ACTIVO);
		usuarioRepositorio.save(admin);
	}
}
