package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.EventoSeguridad;

public interface EventoSeguridadRepositorio extends JpaRepository<EventoSeguridad, Long> {

	List<EventoSeguridad> findByIdUsuarioOrderByFechaCreacionDesc(Long idUsuario, Pageable pageable);
}
