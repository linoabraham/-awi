package pe.nawin.repositorio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.PreferenciaNotificacion;

public interface PreferenciaNotificacionRepositorio extends JpaRepository<PreferenciaNotificacion, Long> {

	Optional<PreferenciaNotificacion> findByIdUsuario(Long idUsuario);
}
