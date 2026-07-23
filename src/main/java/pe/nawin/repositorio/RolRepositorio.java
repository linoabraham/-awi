package pe.nawin.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Rol;
import pe.nawin.enumeracion.RolCodigo;

public interface RolRepositorio extends JpaRepository<Rol, Long> {
	Optional<Rol> findByCodigo(RolCodigo codigo);
}
