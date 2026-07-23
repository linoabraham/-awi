package pe.nawin.repositorio;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.nawin.entidad.CanjeReferido;
import pe.nawin.enumeracion.EstadoBonoReferente;

public interface CanjeReferidoRepositorio extends JpaRepository<CanjeReferido, Long> {

	boolean existsByInvitado_IdCliente(Long idCliente);

	/** Anti-granjas: un dispositivo solo puede canjear un código una vez. */
	boolean existsByInstallationId(String installationId);

	long countByReferente_IdCliente(Long idCliente);

	Optional<CanjeReferido> findFirstByInvitado_IdClienteAndEstadoBonoReferente(
			Long idCliente, EstadoBonoReferente estado);

	boolean existsByInvitado_IdClienteAndEstadoBonoReferente(Long idCliente, EstadoBonoReferente estado);

	/** Bonos acreditados hoy al referente (tope diario anti-granjas). */
	long countByReferente_IdClienteAndEstadoBonoReferenteAndFechaAcreditacionBetween(
			Long idCliente, EstadoBonoReferente estado, LocalDateTime desde, LocalDateTime hasta);

	@Query("SELECT COALESCE(SUM(c.creditosReferente), 0) FROM CanjeReferido c "
			+ "WHERE c.referente.idCliente = :idCliente AND c.estadoBonoReferente = :estado")
	int sumCreditosReferentePorEstado(@Param("idCliente") Long idCliente,
			@Param("estado") EstadoBonoReferente estado);
}
