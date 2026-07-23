package pe.nawin.repositorio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.CodigoVerificacion;
import pe.nawin.enumeracion.TipoCodigoVerificacion;

public interface CodigoVerificacionRepositorio extends JpaRepository<CodigoVerificacion, Long> {

	Optional<CodigoVerificacion> findTopByUsuario_IdUsuarioAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
			Long idUsuario, TipoCodigoVerificacion tipo);

	List<CodigoVerificacion> findByUsuario_IdUsuarioAndTipoAndUsadoFalse(Long idUsuario, TipoCodigoVerificacion tipo);
}
