package pe.nawin.servicio;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.ConsultaRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.PagoRepositorio;
import pe.nawin.repositorio.VentaCreditoRepositorio;

@Service
public class ReporteServicio {

	private final ClienteRepositorio clienteRepositorio;
	private final MembresiaRepositorio membresiaRepositorio;
	private final VentaCreditoRepositorio ventaCreditoRepositorio;
	private final PagoRepositorio pagoRepositorio;
	private final ConsultaRepositorio consultaRepositorio;

	public ReporteServicio(ClienteRepositorio clienteRepositorio, MembresiaRepositorio membresiaRepositorio,
			VentaCreditoRepositorio ventaCreditoRepositorio, PagoRepositorio pagoRepositorio,
			ConsultaRepositorio consultaRepositorio) {
		this.clienteRepositorio = clienteRepositorio;
		this.membresiaRepositorio = membresiaRepositorio;
		this.ventaCreditoRepositorio = ventaCreditoRepositorio;
		this.pagoRepositorio = pagoRepositorio;
		this.consultaRepositorio = consultaRepositorio;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> resumen() {
		return Map.of(
				"clientes", clienteRepositorio.count(),
				"membresias", membresiaRepositorio.count(),
				"ventasCreditos", ventaCreditoRepositorio.count(),
				"pagos", pagoRepositorio.count(),
				"consultas", consultaRepositorio.count());
	}
}
