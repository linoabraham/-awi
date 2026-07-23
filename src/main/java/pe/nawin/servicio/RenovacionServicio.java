package pe.nawin.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.MembresiaRepositorio;

@Service
public class RenovacionServicio {

	private final MembresiaRepositorio membresiaRepositorio;

	public RenovacionServicio(MembresiaRepositorio membresiaRepositorio) {
		this.membresiaRepositorio = membresiaRepositorio;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> porVencer(int dias) {
		LocalDate hoy = LocalDate.now();
		return membresiaRepositorio.findByEstadoAndFechaFinBetween(EstadoMembresia.ACTIVA, hoy, hoy.plusDays(dias)).stream()
				.map(MapeadorRespuesta::membresia)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> vencidas() {
		return membresiaRepositorio.findByEstadoAndFechaFinBefore(EstadoMembresia.ACTIVA, LocalDate.now()).stream()
				.map(MapeadorRespuesta::membresia)
				.toList();
	}
}
