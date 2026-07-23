package pe.nawin.repositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Membresia;
import pe.nawin.enumeracion.EstadoMembresia;

public interface MembresiaRepositorio extends JpaRepository<Membresia, Long> {
	@EntityGraph(attributePaths = {"cliente", "plan"})
	List<Membresia> findByCliente_IdCliente(Long idCliente);

	@EntityGraph(attributePaths = {"cliente", "plan"})
	List<Membresia> findByEstado(EstadoMembresia estado);

	@Override
	@EntityGraph(attributePaths = {"cliente", "plan"})
	List<Membresia> findAll();

	Optional<Membresia> findFirstByCliente_IdClienteAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaFinDesc(
			Long idCliente, EstadoMembresia estado, LocalDate fechaInicio, LocalDate fechaFin);

	Optional<Membresia> findFirstByCliente_IdClienteOrderByFechaFinDesc(Long idCliente);

	boolean existsByCliente_IdClienteAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
			Long idCliente, EstadoMembresia estado, LocalDate fechaInicio, LocalDate fechaFin);

	@EntityGraph(attributePaths = {"cliente", "plan"})
	List<Membresia> findByEstadoAndFechaFinBefore(EstadoMembresia estado, LocalDate fecha);

	@EntityGraph(attributePaths = {"cliente", "plan"})
	List<Membresia> findByEstadoAndFechaFinBetween(EstadoMembresia estado, LocalDate desde, LocalDate hasta);
}
