package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.EndpointBusqueda;
import pe.nawin.enumeracion.CodigoEndpoint;

public interface EndpointBusquedaRepositorio extends JpaRepository<EndpointBusqueda, Long> {
	Optional<EndpointBusqueda> findByCodigo(CodigoEndpoint codigo);

	List<EndpointBusqueda> findByActivo(boolean activo);

	List<EndpointBusqueda> findByEsCritico(boolean esCritico);
}
