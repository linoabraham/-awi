package pe.nawin.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.ResultadoConsulta;

public interface ResultadoConsultaRepositorio extends JpaRepository<ResultadoConsulta, Long> {
	Optional<ResultadoConsulta> findByConsulta_CodigoConsulta(String codigoConsulta);

	List<ResultadoConsulta> findByEliminadoPorRetencionFalseAndFechaExpiracionBefore(LocalDateTime fecha);
}
