package pe.nawin.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.ConfiguracionSoporte;

public interface ConfiguracionSoporteRepositorio extends JpaRepository<ConfiguracionSoporte, Long> {

	Optional<ConfiguracionSoporte> findFirstByOrderByIdConfiguracionSoporteAsc();
}
