package pe.nawin.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.MotivoRequest;
import pe.nawin.dto.solicitud.PagoRequest;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Membresia;
import pe.nawin.entidad.Pago;
import pe.nawin.entidad.Usuario;
import pe.nawin.entidad.VentaCredito;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.enumeracion.EstadoPago;
import pe.nawin.enumeracion.EstadoVentaCredito;
import pe.nawin.enumeracion.MedioPago;
import pe.nawin.enumeracion.TipoNotificacion;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.PagoRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.repositorio.VentaCreditoRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;

@Service
public class PagoServicio {

	private final PagoRepositorio pagoRepositorio;
	private final ClienteRepositorio clienteRepositorio;
	private final MembresiaRepositorio membresiaRepositorio;
	private final VentaCreditoRepositorio ventaCreditoRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final CreditoServicio creditoServicio;
	private final ComprobanteServicio comprobanteServicio;
	private final NotificacionServicio notificacionServicio;
	private final MiPlataServicio miPlataServicio;

	public PagoServicio(PagoRepositorio pagoRepositorio, ClienteRepositorio clienteRepositorio,
			MembresiaRepositorio membresiaRepositorio, VentaCreditoRepositorio ventaCreditoRepositorio,
			UsuarioRepositorio usuarioRepositorio, CreditoServicio creditoServicio,
			ComprobanteServicio comprobanteServicio, NotificacionServicio notificacionServicio,
			MiPlataServicio miPlataServicio) {
		this.pagoRepositorio = pagoRepositorio;
		this.clienteRepositorio = clienteRepositorio;
		this.membresiaRepositorio = membresiaRepositorio;
		this.ventaCreditoRepositorio = ventaCreditoRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.creditoServicio = creditoServicio;
		this.comprobanteServicio = comprobanteServicio;
		this.notificacionServicio = notificacionServicio;
		this.miPlataServicio = miPlataServicio;
	}

	@Transactional
	public Map<String, Object> crear(PagoRequest request) {
		validarDestino(request.idMembresia(), request.idVentaCredito());
		Cliente cliente = clienteRepositorio.findById(request.idCliente()).orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		Membresia membresia = request.idMembresia() == null ? null
				: membresiaRepositorio.findById(request.idMembresia()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		VentaCredito venta = request.idVentaCredito() == null ? null
				: ventaCreditoRepositorio.findById(request.idVentaCredito()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		Usuario usuario = usuarioActual();

		Pago pago = new Pago();
		pago.setCliente(cliente);
		pago.setMembresia(membresia);
		pago.setVentaCredito(venta);
		pago.setMontoSoles(request.montoSoles());
		pago.setMedioPago(request.medioPago());
		pago.setNumeroOperacion(request.numeroOperacion());
		pago.setFechaPago(request.fechaPago());
		pago.setEstado(EstadoPago.CONFIRMADO);
		pago.setObservacion(request.observacion());
		pago.setRegistradoPor(usuario);
		pagoRepositorio.save(pago);
		if (venta != null) {
			creditoServicio.cargarPorVentaPagada(venta, usuario);
		}
		Map<String, Object> comprobante = comprobanteServicio.emitirAutomatico(pago, usuario);
		notificarPago(pago, usuario);

		Map<String, Object> respuesta = respuesta(pago);
		respuesta.put("comprobante", comprobante);
		return respuesta;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listar(Long idCliente, EstadoPago estado) {
		List<Pago> pagos = idCliente != null
				? pagoRepositorio.findByCliente_IdCliente(idCliente)
				: estado != null ? pagoRepositorio.findByEstado(estado) : pagoRepositorio.findAll();
		return pagos.stream().map(this::respuesta).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> ver(Long id) {
		return respuesta(obtener(id));
	}

	@Transactional
	public Map<String, Object> actualizar(Long id, PagoRequest request) {
		Pago pago = obtener(id);
		if (pago.getEstado() != EstadoPago.PENDIENTE) {
			throw new NawinException(CodigoError.PAG_001, "Solo se actualizan pagos pendientes.");
		}
		pago.setMontoSoles(request.montoSoles());
		pago.setMedioPago(request.medioPago());
		pago.setNumeroOperacion(request.numeroOperacion());
		pago.setFechaPago(request.fechaPago());
		pago.setObservacion(request.observacion());
		return respuesta(pago);
	}

	/**
	 * Anula un pago revirtiendo todos sus efectos: los créditos cargados por la
	 * venta se descuentan, el saldo pagado con la billetera MiPlata se devuelve,
	 * la membresía comprada con MiPlata se cancela y el comprobante se anula.
	 */
	@Transactional
	public void anular(Long id, MotivoRequest request) {
		Pago pago = obtener(id);
		if (pago.getEstado() == EstadoPago.ANULADO) {
			return;
		}
		String motivo = request == null || request.motivo() == null || request.motivo().isBlank()
				? "Anulado"
				: request.motivo();
		Usuario usuario = usuarioActual();
		if (pago.getVentaCredito() != null && pago.getVentaCredito().getEstado() == EstadoVentaCredito.PAGADA) {
			creditoServicio.revertirPorVentaAnulada(pago.getVentaCredito(), usuario, motivo);
		}
		if (pago.getMedioPago() == MedioPago.MIPLATA) {
			miPlataServicio.devolverPorPagoAnulado(pago, usuario, motivo);
			if (pago.getMembresia() != null && pago.getMembresia().getEstado() == EstadoMembresia.ACTIVA) {
				pago.getMembresia().setEstado(EstadoMembresia.CANCELADA);
			}
		}
		comprobanteServicio.anularPorPago(pago.getIdPago(), motivo);
		pago.setEstado(EstadoPago.ANULADO);
		pago.setObservacion(motivo);
	}

	public Pago obtener(Long id) {
		return pagoRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private void validarDestino(Long idMembresia, Long idVentaCredito) {
		if ((idMembresia == null && idVentaCredito == null) || (idMembresia != null && idVentaCredito != null)) {
			throw new NawinException(CodigoError.GEN_001, "El pago debe referir a una membresía o venta de créditos, no ambas.");
		}
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}

	private void notificarPago(Pago pago, Usuario usuario) {
		if (pago.getVentaCredito() != null) {
			notificacionServicio.crearAutomatica(pago.getCliente(), null, TipoNotificacion.CREDITOS_CARGADOS,
					"Creditos cargados",
					"Tu compra de creditos fue confirmada por S/ " + pago.getMontoSoles()
							+ " y el comprobante fue emitido.",
					usuario);
			return;
		}
		notificacionServicio.crearAutomatica(pago.getCliente(), pago.getMembresia(), TipoNotificacion.PAGO_CONFIRMADO,
				"Pago confirmado",
				"Tu pago por S/ " + pago.getMontoSoles() + " fue confirmado y el comprobante fue emitido.",
				usuario);
	}

	private Map<String, Object> respuesta(Pago pago) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idPago", pago.getIdPago());
		m.put("idCliente", pago.getCliente().getIdCliente());
		m.put("idMembresia", pago.getMembresia() == null ? null : pago.getMembresia().getIdMembresia());
		m.put("idVentaCredito", pago.getVentaCredito() == null ? null : pago.getVentaCredito().getIdVentaCredito());
		m.put("montoSoles", pago.getMontoSoles());
		m.put("medioPago", pago.getMedioPago());
		m.put("numeroOperacion", pago.getNumeroOperacion());
		m.put("fechaPago", pago.getFechaPago());
		m.put("estado", pago.getEstado());
		m.put("observacion", pago.getObservacion());
		return m;
	}
}
