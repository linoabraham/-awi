package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.MovimientoCredito;

public interface MovimientoCreditoRepositorio extends JpaRepository<MovimientoCredito, Long> {
	@EntityGraph(attributePaths = {"cliente", "ventaCredito", "consulta"})
	List<MovimientoCredito> findByCliente_IdClienteOrderByFechaCreacionDesc(Long idCliente);
}
