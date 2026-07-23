package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByNombreUsuario(String nombreUsuario);

	Optional<Usuario> findByCorreoIgnoreCase(String correo);

	boolean existsByNombreUsuario(String nombreUsuario);

	boolean existsByCorreo(String correo);

	/** Anti alias de correo (Gmail +sufijo/puntos): detecta el mismo buzón real. */
	boolean existsByCorreoNormalizado(String correoNormalizado);

	@Override
	@EntityGraph(attributePaths = {"rol"})
	List<Usuario> findAll();
}
