package pe.nawin.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.ConfiguracionProveedor;

public interface ConfiguracionProveedorRepositorio extends JpaRepository<ConfiguracionProveedor, Long> {

	Optional<ConfiguracionProveedor> findFirstByOrderByIdConfiguracionProveedorAsc();
}
