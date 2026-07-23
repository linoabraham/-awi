package pe.nawin.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.ArchivoConsulta;

public interface ArchivoConsultaRepositorio extends JpaRepository<ArchivoConsulta, Long> {
	List<ArchivoConsulta> findByConsulta_IdConsulta(Long idConsulta);

	Optional<ArchivoConsulta> findByIdArchivoConsultaAndConsulta_Cliente_IdCliente(Long idArchivoConsulta, Long idCliente);

	List<ArchivoConsulta> findByEliminadoPorRetencionFalseAndFechaExpiracionBefore(LocalDateTime fecha);
}
