package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.PlanEndpoint;

public interface PlanEndpointRepositorio extends JpaRepository<PlanEndpoint, Long> {
	@EntityGraph(attributePaths = {"endpoint", "plan"})
	List<PlanEndpoint> findByPlan_IdPlan(Long idPlan);

	Optional<PlanEndpoint> findByPlan_IdPlanAndIdPlanEndpoint(Long idPlan, Long idPlanEndpoint);

	Optional<PlanEndpoint> findByPlan_IdPlanAndEndpoint_IdEndpoint(Long idPlan, Long idEndpoint);
}
