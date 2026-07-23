package pe.nawin.repositorio;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pe.nawin.entidad.BilleteraMiPlata;

public interface BilleteraMiPlataRepositorio extends JpaRepository<BilleteraMiPlata, Long> {
	Optional<BilleteraMiPlata> findByCliente_IdCliente(Long idCliente);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<BilleteraMiPlata> findWithLockByCliente_IdCliente(Long idCliente);
}
