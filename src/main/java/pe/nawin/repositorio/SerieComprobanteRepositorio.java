package pe.nawin.repositorio;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pe.nawin.entidad.SerieComprobante;
import pe.nawin.enumeracion.TipoComprobante;

public interface SerieComprobanteRepositorio extends JpaRepository<SerieComprobante, Long> {
	List<SerieComprobante> findByTipoComprobante(TipoComprobante tipoComprobante);

	List<SerieComprobante> findByActivo(boolean activo);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<SerieComprobante> findWithLockByIdSerieComprobante(Long id);
}
