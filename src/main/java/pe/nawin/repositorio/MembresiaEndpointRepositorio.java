package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.MembresiaEndpoint;
import pe.nawin.enumeracion.CodigoEndpoint;

public interface MembresiaEndpointRepositorio extends JpaRepository<MembresiaEndpoint, Long> {
	@EntityGraph(attributePaths = {"endpoint", "membresia"})
	List<MembresiaEndpoint> findByMembresia_IdMembresia(Long idMembresia);

	Optional<MembresiaEndpoint> findByMembresia_IdMembresiaAndIdMembresiaEndpoint(Long idMembresia, Long idMembresiaEndpoint);

	Optional<MembresiaEndpoint> findByMembresia_IdMembresiaAndEndpoint_Codigo(Long idMembresia, CodigoEndpoint codigo);
}
