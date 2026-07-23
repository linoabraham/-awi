package pe.nawin.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.TokenRefresco;

public interface TokenRefrescoRepositorio extends JpaRepository<TokenRefresco, Long> {
	Optional<TokenRefresco> findByTokenHash(String tokenHash);

	Optional<TokenRefresco> findByTokenHashAndRevocadoFalse(String tokenHash);

	List<TokenRefresco> findByUsuario_IdUsuarioAndRevocadoFalse(Long idUsuario);

	/** Sesiones vigentes de un usuario: ni revocadas ni vencidas. */
	List<TokenRefresco> findByUsuario_IdUsuarioAndRevocadoFalseAndFechaExpiracionAfter(
			Long idUsuario, LocalDateTime ahora);

	void deleteByFechaExpiracionBefore(LocalDateTime fecha);
}
