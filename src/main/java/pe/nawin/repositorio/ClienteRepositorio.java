package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Cliente;
import pe.nawin.enumeracion.EstadoCliente;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
	Optional<Cliente> findByUsuario_IdUsuario(Long idUsuario);

	Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

	Optional<Cliente> findByCodigoReferido(String codigoReferido);

	boolean existsByNumeroDocumento(String numeroDocumento);

	@EntityGraph(attributePaths = {"usuario"})
	List<Cliente> findByEstado(EstadoCliente estado);

	@Override
	@EntityGraph(attributePaths = {"usuario"})
	List<Cliente> findAll();
}
