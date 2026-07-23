package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Notificacion;
import pe.nawin.enumeracion.EstadoNotificacion;

public interface NotificacionRepositorio extends JpaRepository<Notificacion, Long> {
	List<Notificacion> findByCliente_IdClienteOrderByFechaCreacionDesc(Long idCliente);

	List<Notificacion> findByEstado(EstadoNotificacion estado);
}
