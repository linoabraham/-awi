package pe.nawin.repositorio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.PaqueteCredito;

public interface PaqueteCreditoRepositorio extends JpaRepository<PaqueteCredito, Long> {
	boolean existsByCodigo(String codigo);

	List<PaqueteCredito> findByActivo(boolean activo);
}
