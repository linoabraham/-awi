package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Pago;
import pe.nawin.enumeracion.EstadoPago;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {
	@EntityGraph(attributePaths = {"cliente", "membresia", "ventaCredito"})
	List<Pago> findByCliente_IdCliente(Long idCliente);

	@EntityGraph(attributePaths = {"cliente", "membresia", "ventaCredito"})
	List<Pago> findByEstado(EstadoPago estado);

	@Override
	@EntityGraph(attributePaths = {"cliente", "membresia", "ventaCredito"})
	List<Pago> findAll();

	Optional<Pago> findByClaveIdempotencia(String claveIdempotencia);
}
