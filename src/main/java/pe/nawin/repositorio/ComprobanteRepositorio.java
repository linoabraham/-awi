package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Comprobante;

public interface ComprobanteRepositorio extends JpaRepository<Comprobante, Long> {
	Optional<Comprobante> findByPago_IdPago(Long idPago);

	@EntityGraph(attributePaths = {"pago", "pago.cliente"})
	List<Comprobante> findByPago_Cliente_IdCliente(Long idCliente);

	@Override
	@EntityGraph(attributePaths = {"pago", "pago.cliente"})
	List<Comprobante> findAll();
}
