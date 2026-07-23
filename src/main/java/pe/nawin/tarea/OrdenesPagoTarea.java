package pe.nawin.tarea;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.nawin.servicio.MiPlataServicio;

/** Cancela automáticamente las órdenes de pago no pagadas tras 24 horas. */
@Component
public class OrdenesPagoTarea {

	private final MiPlataServicio miPlataServicio;

	public OrdenesPagoTarea(MiPlataServicio miPlataServicio) {
		this.miPlataServicio = miPlataServicio;
	}

	// Cada 10 minutos revisa órdenes pendientes vencidas y las cancela.
	@Scheduled(cron = "0 */10 * * * *")
	public void cancelarOrdenesVencidas() {
		miPlataServicio.cancelarOrdenesVencidas();
	}
}
