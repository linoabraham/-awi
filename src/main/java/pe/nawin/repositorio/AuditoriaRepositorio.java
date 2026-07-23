package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Auditoria;

public interface AuditoriaRepositorio extends JpaRepository<Auditoria, Long> {
	List<Auditoria> findByEntidad(String entidad);
}
