package pe.nawin.repositorio;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pe.nawin.entidad.SolicitudRecargaMiPlata;
import pe.nawin.enumeracion.EstadoSolicitudRecargaMiPlata;

public interface SolicitudRecargaMiPlataRepositorio extends JpaRepository<SolicitudRecargaMiPlata, Long> {
	@EntityGraph(attributePaths = {"cliente", "clienteReferido"})
	List<SolicitudRecargaMiPlata> findByCliente_IdClienteOrderByFechaCreacionDesc(Long idCliente);

	@EntityGraph(attributePaths = {"cliente", "clienteReferido"})
	List<SolicitudRecargaMiPlata> findByEstadoOrderByFechaCreacionDesc(EstadoSolicitudRecargaMiPlata estado);

	@EntityGraph(attributePaths = {"cliente", "clienteReferido"})
	List<SolicitudRecargaMiPlata> findAllByOrderByFechaCreacionDesc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<SolicitudRecargaMiPlata> findWithLockByIdSolicitudRecargaMiPlata(Long id);

	Optional<SolicitudRecargaMiPlata> findByClaveIdempotencia(String claveIdempotencia);

	Optional<SolicitudRecargaMiPlata> findByCodigoOrden(String codigoOrden);

	long countByCliente_IdClienteAndEstado(Long idCliente, EstadoSolicitudRecargaMiPlata estado);

	// Órdenes de pago vencidas: pendientes, sin comprobante y con expiración pasada.
	List<SolicitudRecargaMiPlata> findByEstadoAndComprobanteBase64IsNullAndFechaExpiracionBefore(
			EstadoSolicitudRecargaMiPlata estado, java.time.LocalDateTime limite);
}
