package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.VentaCredito;
import pe.nawin.enumeracion.EstadoVentaCredito;

public interface VentaCreditoRepositorio extends JpaRepository<VentaCredito, Long> {
	@EntityGraph(attributePaths = {"cliente", "paqueteCredito"})
	List<VentaCredito> findByCliente_IdCliente(Long idCliente);

	@EntityGraph(attributePaths = {"cliente", "paqueteCredito"})
	List<VentaCredito> findByEstado(EstadoVentaCredito estado);

	@Override
	@EntityGraph(attributePaths = {"cliente", "paqueteCredito"})
	List<VentaCredito> findAll();
}
