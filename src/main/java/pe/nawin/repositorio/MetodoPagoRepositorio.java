package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.MetodoPago;

public interface MetodoPagoRepositorio extends JpaRepository<MetodoPago, Long> {

	Optional<MetodoPago> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);

	List<MetodoPago> findByActivoTrueOrderByOrdenAscIdMetodoPagoAsc();

	List<MetodoPago> findAllByOrderByOrdenAscIdMetodoPagoAsc();
}
