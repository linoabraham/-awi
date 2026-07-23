package pe.nawin.repositorio;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pe.nawin.entidad.SaldoCredito;

public interface SaldoCreditoRepositorio extends JpaRepository<SaldoCredito, Long> {
	Optional<SaldoCredito> findByCliente_IdCliente(Long idCliente);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<SaldoCredito> findWithLockByCliente_IdCliente(Long idCliente);
}
