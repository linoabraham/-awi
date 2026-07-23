package pe.nawin.tarea;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.entidad.Notificacion;
import pe.nawin.entidad.ResultadoConsulta;
import pe.nawin.enumeracion.CanalNotificacion;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.enumeracion.EstadoNotificacion;
import pe.nawin.enumeracion.TipoNotificacion;
import pe.nawin.repositorio.ArchivoConsultaRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.NotificacionRepositorio;
import pe.nawin.repositorio.ResultadoConsultaRepositorio;
import pe.nawin.repositorio.TokenRefrescoRepositorio;
import pe.nawin.servicio.PurgaCuentasServicio;

@Component
public class TareasProgramadas {

	private final MembresiaRepositorio membresiaRepositorio;
	private final NotificacionRepositorio notificacionRepositorio;
	private final ResultadoConsultaRepositorio resultadoRepositorio;
	private final ArchivoConsultaRepositorio archivoRepositorio;
	private final TokenRefrescoRepositorio tokenRefrescoRepositorio;
	private final PurgaCuentasServicio purgaCuentasServicio;

	public TareasProgramadas(MembresiaRepositorio membresiaRepositorio, NotificacionRepositorio notificacionRepositorio,
			ResultadoConsultaRepositorio resultadoRepositorio, ArchivoConsultaRepositorio archivoRepositorio,
			TokenRefrescoRepositorio tokenRefrescoRepositorio, PurgaCuentasServicio purgaCuentasServicio) {
		this.membresiaRepositorio = membresiaRepositorio;
		this.notificacionRepositorio = notificacionRepositorio;
		this.resultadoRepositorio = resultadoRepositorio;
		this.archivoRepositorio = archivoRepositorio;
		this.tokenRefrescoRepositorio = tokenRefrescoRepositorio;
		this.purgaCuentasServicio = purgaCuentasServicio;
	}

	@Transactional
	@Scheduled(cron = "0 5 0 * * *", zone = "America/Lima")
	public void actualizarMembresiasVencidas() {
		membresiaRepositorio.findByEstadoAndFechaFinBefore(EstadoMembresia.ACTIVA, LocalDate.now())
				.forEach(m -> m.setEstado(EstadoMembresia.VENCIDA));
	}

	@Transactional
	@Scheduled(cron = "0 0 8 * * *", zone = "America/Lima")
	public void generarAlertasVencimiento() {
		for (int dias : new int[] {10, 5, 3, 1}) {
			LocalDate fecha = LocalDate.now().plusDays(dias);
			membresiaRepositorio.findByEstadoAndFechaFinBetween(EstadoMembresia.ACTIVA, fecha, fecha).forEach(m -> {
				Notificacion n = new Notificacion();
				n.setCliente(m.getCliente());
				n.setMembresia(m);
				n.setCanal(CanalNotificacion.SISTEMA);
				n.setTipo(TipoNotificacion.POR_VENCER);
				n.setTitulo("Renovación de membresía");
				n.setMensaje("Tu membresía vence en " + dias + " día(s).");
				n.setEstado(EstadoNotificacion.PENDIENTE);
				notificacionRepositorio.save(n);
			});
		}
	}

	@Transactional
	@Scheduled(cron = "0 0 2 * * *", zone = "America/Lima")
	public void purgarResultadosVencidos() {
		LocalDateTime ahora = LocalDateTime.now();
		for (ResultadoConsulta resultado : resultadoRepositorio.findByEliminadoPorRetencionFalseAndFechaExpiracionBefore(ahora)) {
			resultado.setResultadoJsonCifrado("{}");
			resultado.setEliminadoPorRetencion(true);
		}
		archivoRepositorio.findByEliminadoPorRetencionFalseAndFechaExpiracionBefore(ahora)
				.forEach(a -> a.setEliminadoPorRetencion(true));
	}

	@Transactional
	@Scheduled(cron = "0 30 1 * * *", zone = "America/Lima")
	public void purgarTokensRefresco() {
		tokenRefrescoRepositorio.deleteByFechaExpiracionBefore(LocalDateTime.now());
	}

	// La purga maneja su propia transacción dentro del servicio.
	@Scheduled(cron = "0 45 1 * * *", zone = "America/Lima")
	public void purgarCuentasNoVerificadas() {
		purgaCuentasServicio.purgarNoVerificadas();
	}
}
