package pe.nawin.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.entidad.CanjeReferido;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.SaldoCredito;
import pe.nawin.enumeracion.EstadoBonoReferente;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.repositorio.CanjeReferidoRepositorio;
import pe.nawin.repositorio.ClienteRepositorio;

/**
 * Programa de referidos con controles anti-abuso:
 * <ul>
 *   <li>El invitado recibe su bono al canjear (una sola vez por cuenta y por
 *       dispositivo — installationId).</li>
 *   <li>El bono del referente queda PENDIENTE y se acredita recién cuando el
 *       invitado hace su primera consulta exitosa (mata las granjas de cuentas).</li>
 *   <li>Tope diario de bonos acreditables por referente.</li>
 * </ul>
 */
@Service
public class ReferidoServicio {

	private static final Logger log = LoggerFactory.getLogger(ReferidoServicio.class);

	private static final int BONO_CREDITOS = 20;
	private static final int META_AMIGOS = 20;
	/** Máximo de bonos de referente acreditables por día (anti-granjas). */
	private static final int TOPE_BONOS_DIARIOS = 5;

	private final ClienteServicio clienteServicio;
	private final ClienteRepositorio clienteRepositorio;
	private final CanjeReferidoRepositorio canjeRepositorio;
	private final CreditoServicio creditoServicio;
	private final NotificacionServicio notificacionServicio;
	private final NotificacionPushServicio notificacionPush;

	public ReferidoServicio(ClienteServicio clienteServicio, ClienteRepositorio clienteRepositorio,
			CanjeReferidoRepositorio canjeRepositorio, CreditoServicio creditoServicio,
			NotificacionServicio notificacionServicio, NotificacionPushServicio notificacionPush) {
		this.clienteServicio = clienteServicio;
		this.clienteRepositorio = clienteRepositorio;
		this.canjeRepositorio = canjeRepositorio;
		this.creditoServicio = creditoServicio;
		this.notificacionServicio = notificacionServicio;
		this.notificacionPush = notificacionPush;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> resumen() {
		Cliente cliente = clienteServicio.clienteActual();
		long amigos = canjeRepositorio.countByReferente_IdCliente(cliente.getIdCliente());
		int ganados = canjeRepositorio.sumCreditosReferentePorEstado(
				cliente.getIdCliente(), EstadoBonoReferente.ACREDITADO);
		int pendientes = canjeRepositorio.sumCreditosReferentePorEstado(
				cliente.getIdCliente(), EstadoBonoReferente.PENDIENTE);
		boolean yaCanjeo = canjeRepositorio.existsByInvitado_IdCliente(cliente.getIdCliente());
		return Map.of(
				"codigoReferido", cliente.getCodigoReferido(),
				"amigosInvitados", amigos,
				"creditosGanados", ganados,
				"creditosPendientes", pendientes,
				"meta", META_AMIGOS,
				"progreso", Math.min(amigos, META_AMIGOS),
				"bonoPorReferido", BONO_CREDITOS,
				"yaCanjeoCodigo", yaCanjeo);
	}

	@Transactional
	public Map<String, Object> canjear(String codigo, String installationId, String ip) {
		Cliente invitado = clienteServicio.clienteActual();
		String cod = codigo == null ? "" : codigo.trim().toUpperCase();
		if (cod.isBlank()) {
			throw new NawinException(CodigoError.GEN_001, "Ingresa un código de referido válido.");
		}
		Cliente referente = clienteRepositorio.findByCodigoReferido(cod)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_001, "El código de referido no existe."));
		if (referente.getIdCliente().equals(invitado.getIdCliente())) {
			throw new NawinException(CodigoError.GEN_001, "No puedes canjear tu propio código.");
		}
		if (canjeRepositorio.existsByInvitado_IdCliente(invitado.getIdCliente())) {
			throw new NawinException(CodigoError.GEN_001, "Ya canjeaste un código de referido.");
		}
		// Anti-granjas: un solo canje por dispositivo, sin importar la cuenta.
		if (installationId != null && !installationId.isBlank()
				&& canjeRepositorio.existsByInstallationId(installationId)) {
			throw new NawinException(CodigoError.GEN_001,
					"Este dispositivo ya canjeó un código de referido.");
		}

		SaldoCredito saldoInvitado = creditoServicio.otorgarBono(invitado, BONO_CREDITOS,
				"Bono por canjear código de referido " + cod);

		// El bono del referente queda pendiente hasta la 1.ª consulta del invitado.
		CanjeReferido canje = new CanjeReferido();
		canje.setReferente(referente);
		canje.setInvitado(invitado);
		canje.setCodigoReferido(cod);
		canje.setCreditosInvitado(BONO_CREDITOS);
		canje.setCreditosReferente(BONO_CREDITOS);
		canje.setEstadoBonoReferente(EstadoBonoReferente.PENDIENTE);
		canje.setInstallationId(installationId);
		canje.setDireccionIp(ip);
		canjeRepositorio.save(canje);

		// Transparencia con el referente: le avisamos del canje y de CUÁNDO cobrará.
		String nombreInvitado = nombreVisible(invitado);
		notificar(referente, "¡" + nombreInvitado + " canjeó tu código!",
				"Tus " + BONO_CREDITOS + " créditos se acreditarán automáticamente cuando "
						+ nombreInvitado + " haga su primera consulta. Te avisaremos.",
				invitado);

		return Map.of(
				"creditosOtorgados", BONO_CREDITOS,
				"saldoDisponible", saldoInvitado.getCreditosDisponibles());
	}

	/**
	 * Acredita el bono del referente cuando el invitado completa su primera
	 * consulta exitosa. Respetando el tope diario: si el referente ya cobró el
	 * máximo de hoy, el bono sigue pendiente y se reintenta en la siguiente
	 * consulta del invitado. Best-effort: nunca rompe la consulta que lo dispara.
	 */
	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	public void acreditarBonoPorConsulta(Cliente invitado) {
		try {
			if (invitado == null || !canjeRepositorio.existsByInvitado_IdClienteAndEstadoBonoReferente(
					invitado.getIdCliente(), EstadoBonoReferente.PENDIENTE)) {
				return;
			}
			CanjeReferido canje = canjeRepositorio.findFirstByInvitado_IdClienteAndEstadoBonoReferente(
					invitado.getIdCliente(), EstadoBonoReferente.PENDIENTE).orElse(null);
			if (canje == null) {
				return;
			}
			Cliente referente = canje.getReferente();
			LocalDateTime desde = LocalDate.now().atStartOfDay();
			LocalDateTime hasta = LocalDate.now().atTime(LocalTime.MAX);
			long acreditadosHoy = canjeRepositorio
					.countByReferente_IdClienteAndEstadoBonoReferenteAndFechaAcreditacionBetween(
							referente.getIdCliente(), EstadoBonoReferente.ACREDITADO, desde, hasta);
			if (acreditadosHoy >= TOPE_BONOS_DIARIOS) {
				log.info("Bono de referido en espera: referente {} alcanzó el tope diario de {} bonos.",
						referente.getIdCliente(), TOPE_BONOS_DIARIOS);
				return; // Queda pendiente; se reintenta en la próxima consulta del invitado.
			}
			creditoServicio.otorgarBono(referente, canje.getCreditosReferente(),
					"Bono por referido de " + nombreVisible(canje.getInvitado()));
			canje.setEstadoBonoReferente(EstadoBonoReferente.ACREDITADO);
			canje.setFechaAcreditacion(LocalDateTime.now());
			// Aviso de acreditación: cierre transparente del ciclo del referido.
			notificar(referente, "¡Bono de referido acreditado!",
					"Recibiste " + canje.getCreditosReferente() + " créditos porque "
							+ nombreVisible(canje.getInvitado()) + " hizo su primera consulta. ¡Sigue invitando!",
					canje.getInvitado());
		} catch (RuntimeException ex) {
			log.warn("No se pudo acreditar el bono de referido del invitado {}: {}",
					invitado == null ? null : invitado.getIdCliente(), ex.getMessage());
		}
	}

	/** Notificación in-app + push al referente (best-effort: nunca rompe el flujo). */
	private void notificar(Cliente referente, String titulo, String mensaje, Cliente origen) {
		try {
			notificacionServicio.crearAutomatica(referente, null,
					pe.nawin.enumeracion.TipoNotificacion.CREDITOS_CARGADOS, titulo, mensaje,
					origen.getUsuario());
			notificacionPush.enviarAUsuario(referente.getUsuario().getIdUsuario(),
					pe.nawin.enumeracion.CategoriaNotificacion.REFERIDOS, titulo, mensaje, Map.of());
		} catch (RuntimeException ex) {
			log.warn("No se pudo notificar al referente {}: {}", referente.getIdCliente(), ex.getMessage());
		}
	}

	private String nombreVisible(Cliente cliente) {
		if (cliente.getNombres() != null && !cliente.getNombres().isBlank()) {
			return cliente.getNombres();
		}
		return cliente.getCodigoReferido();
	}
}
