package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Plan;
import pe.nawin.enumeracion.EstadoPlan;

public interface PlanRepositorio extends JpaRepository<Plan, Long> {
	boolean existsByCodigo(String codigo);

	List<Plan> findByEstado(EstadoPlan estado);
}
