package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.MovimientoMiPlata;

public interface MovimientoMiPlataRepositorio extends JpaRepository<MovimientoMiPlata, Long> {
	@EntityGraph(attributePaths = {"cliente", "solicitudRecarga", "ventaCredito", "membresia"})
	List<MovimientoMiPlata> findByCliente_IdClienteOrderByFechaCreacionDesc(Long idCliente);
}
