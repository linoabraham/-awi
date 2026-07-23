package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.TokenPush;

public interface TokenPushRepositorio extends JpaRepository<TokenPush, Long> {

	Optional<TokenPush> findByIdUsuarioAndInstallationId(Long idUsuario, String installationId);

	List<TokenPush> findByIdUsuarioAndActivoTrue(Long idUsuario);

	Optional<TokenPush> findByFcmToken(String fcmToken);
}
