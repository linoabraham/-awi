package pe.nawin.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pe.nawin.dto.solicitud.ComprarCreditosMiPlataRequest;
import pe.nawin.dto.solicitud.ComprarPlanMiPlataRequest;
import pe.nawin.dto.solicitud.RechazarRecargaMiPlataRequest;
import pe.nawin.dto.solicitud.SolicitudRecargaMiPlataRequest;
import pe.nawin.entidad.BilleteraMiPlata;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Membresia;
import pe.nawin.entidad.MembresiaEndpoint;
import pe.nawin.entidad.MovimientoMiPlata;
import pe.nawin.entidad.Pago;
import pe.nawin.entidad.PaqueteCredito;
import pe.nawin.entidad.Plan;
import pe.nawin.entidad.PlanEndpoint;
import pe.nawin.entidad.SolicitudRecargaMiPlata;
import pe.nawin.entidad.Usuario;
import pe.nawin.entidad.VentaCredito;
import org.springframework.cache.annotation.Cacheable;
import pe.nawin.configuracion.CacheConfiguracion;
import pe.nawin.enumeracion.EstadoCliente;
import pe.nawin.enumeracion.EstadoPago;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.enumeracion.EstadoPlan;
import pe.nawin.enumeracion.EstadoSolicitudRecargaMiPlata;
import pe.nawin.enumeracion.EstadoVentaCredito;
import pe.nawin.enumeracion.MedioPago;
import pe.nawin.enumeracion.TipoMovimientoMiPlata;
import pe.nawin.enumeracion.TipoNotificacion;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.BilleteraMiPlataRepositorio;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.MembresiaEndpointRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.MovimientoMiPlataRepositorio;
import pe.nawin.repositorio.PagoRepositorio;
import pe.nawin.repositorio.PaqueteCreditoRepositorio;
import pe.nawin.repositorio.PlanEndpointRepositorio;
import pe.nawin.repositorio.PlanRepositorio;
import pe.nawin.repositorio.SolicitudRecargaMiPlataRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.repositorio.VentaCreditoRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;

@Service
public class MiPlataServicio {

	private static final BigDecimal BONO_REFERIDO = new BigDecimal("3.00");
	private static final BigDecimal MONTO_MINIMO_RECARGA = new BigDecimal("1.00");
	private static final BigDecimal MONTO_MAXIMO_RECARGA = new BigDecimal("10000.00");
	private static final long MAX_SOLICITUDES_PENDIENTES = 3;
	private static final int MAX_COMPROBANTE_BASE64_CARACTERES = 7_000_000; // ~5 MB binarios
	private static final int MAX_PAQUETES_POR_COMPRA = 50;
	private static final int MAX_DIAS_INICIO_FUTURO = 90;

	private final BilleteraMiPlataRepositorio billeteraRepositorio;
	private final SolicitudRecargaMiPlataRepositorio solicitudRepositorio;
	private final MovimientoMiPlataRepositorio movimientoRepositorio;
	private final ClienteRepositorio clienteRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final PaqueteCreditoRepositorio paqueteRepositorio;
	private final VentaCreditoRepositorio ventaRepositorio;
	private final PlanRepositorio planRepositorio;
	private final MembresiaRepositorio membresiaRepositorio;
	private final PlanEndpointRepositorio planEndpointRepositorio;
	private final MembresiaEndpointRepositorio membresiaEndpointRepositorio;
	private final CreditoServicio creditoServicio;
	private final PagoRepositorio pagoRepositorio;
	private final ComprobanteServicio comprobanteServicio;
	private final NotificacionServicio notificacionServicio;
	private final NotificacionPushServicio notificacionPush;

	public MiPlataServicio(BilleteraMiPlataRepositorio billeteraRepositorio,
			SolicitudRecargaMiPlataRepositorio solicitudRepositorio, MovimientoMiPlataRepositorio movimientoRepositorio,
			ClienteRepositorio clienteRepositorio, UsuarioRepositorio usuarioRepositorio,
			PaqueteCreditoRepositorio paqueteRepositorio, VentaCreditoRepositorio ventaRepositorio,
			PlanRepositorio planRepositorio, MembresiaRepositorio membresiaRepositorio,
			PlanEndpointRepositorio planEndpointRepositorio, MembresiaEndpointRepositorio membresiaEndpointRepositorio,
			CreditoServicio creditoServicio, PagoRepositorio pagoRepositorio, ComprobanteServicio comprobanteServicio,
			NotificacionServicio notificacionServicio, NotificacionPushServicio notificacionPush) {
		this.billeteraRepositorio = billeteraRepositorio;
		this.solicitudRepositorio = solicitudRepositorio;
		this.movimientoRepositorio = movimientoRepositorio;
		this.clienteRepositorio = clienteRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.paqueteRepositorio = paqueteRepositorio;
		this.ventaRepositorio = ventaRepositorio;
		this.planRepositorio = planRepositorio;
		this.membresiaRepositorio = membresiaRepositorio;
		this.planEndpointRepositorio = planEndpointRepositorio;
		this.membresiaEndpointRepositorio = membresiaEndpointRepositorio;
		this.creditoServicio = creditoServicio;
		this.pagoRepositorio = pagoRepositorio;
		this.comprobanteServicio = comprobanteServicio;
		this.notificacionServicio = notificacionServicio;
		this.notificacionPush = notificacionPush;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> miBilletera() {
		Cliente cliente = clienteActual();
		return billeteraRespuesta(obtenerBilletera(cliente.getIdCliente()));
	}

	@Transactional
	public Map<String, Object> solicitarRecarga(SolicitudRecargaMiPlataRequest request, String claveIdempotencia) {
		Cliente cliente = clienteActual();
		if (StringUtils.hasText(claveIdempotencia)) {
			SolicitudRecargaMiPlata existente = solicitudRepositorio.findByClaveIdempotencia(claveIdempotencia.trim())
					.orElse(null);
			if (existente != null) {
				if (!existente.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
					throw new NawinException(CodigoError.CON_002, "Clave de idempotencia ya usada por otro cliente.");
				}
				return solicitudRespuesta(existente, false);
			}
		}
		validarClienteActivo(cliente);
		validarMontoRecarga(request.montoSoles());
		validarBase64(request.comprobanteBase64());
		if (solicitudRepositorio.countByCliente_IdClienteAndEstado(cliente.getIdCliente(),
				EstadoSolicitudRecargaMiPlata.PENDIENTE) >= MAX_SOLICITUDES_PENDIENTES) {
			throw new NawinException(CodigoError.GEN_001,
					"Tienes " + MAX_SOLICITUDES_PENDIENTES + " recargas pendientes de revisión. Espera su aprobación antes de registrar otra.");
		}
		Cliente referido = resolverReferido(cliente, request.codigoReferido());
		SolicitudRecargaMiPlata solicitud = new SolicitudRecargaMiPlata();
		solicitud.setCliente(cliente);
		solicitud.setMontoSoles(request.montoSoles());
		solicitud.setComprobanteBase64(request.comprobanteBase64());
		solicitud.setCodigoReferidoIngresado(normalizarCodigo(request.codigoReferido()));
		solicitud.setClienteReferido(referido);
		solicitud.setEstado(EstadoSolicitudRecargaMiPlata.PENDIENTE);
		solicitud.setClaveIdempotencia(StringUtils.hasText(claveIdempotencia) ? claveIdempotencia.trim() : null);
		solicitudRepositorio.save(solicitud);
		notificar(cliente, null, TipoNotificacion.RECARGA_MIPLATA, "Recarga MiPlata registrada",
				"Tu solicitud de recarga por S/ " + solicitud.getMontoSoles() + " esta pendiente de revision.",
				cliente.getUsuario());
		return solicitudRespuesta(solicitud, true);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> misSolicitudes() {
		Cliente cliente = clienteActual();
		return solicitudRepositorio.findByCliente_IdClienteOrderByFechaCreacionDesc(cliente.getIdCliente()).stream()
				.map(s -> solicitudRespuesta(s, false))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> misMovimientos() {
		Cliente cliente = clienteActual();
		return movimientoRepositorio.findByCliente_IdClienteOrderByFechaCreacionDesc(cliente.getIdCliente()).stream()
				.map(this::movimientoRespuesta)
				.toList();
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheConfiguracion.CACHE_PAQUETES, key = "'MIPLATA_ACTIVOS'")
	public List<Map<String, Object>> paquetesCreditosDisponibles() {
		return paqueteRepositorio.findByActivo(true).stream()
				.map(MapeadorRespuesta::paquete)
				.toList();
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheConfiguracion.CACHE_PLANES, key = "'MIPLATA_ACTIVOS'")
	public List<Map<String, Object>> planesDisponibles() {
		return planRepositorio.findByEstado(EstadoPlan.ACTIVO).stream()
				.map(MapeadorRespuesta::plan)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listarSolicitudes(Long idCliente, EstadoSolicitudRecargaMiPlata estado) {
		List<SolicitudRecargaMiPlata> solicitudes = estado != null
				? solicitudRepositorio.findByEstadoOrderByFechaCreacionDesc(estado)
				: solicitudRepositorio.findAllByOrderByFechaCreacionDesc();
		return solicitudes.stream()
				.filter(s -> idCliente == null || s.getCliente().getIdCliente().equals(idCliente))
				.map(s -> solicitudRespuesta(s, false))
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verSolicitud(Long id) {
		return solicitudRespuesta(obtenerSolicitud(id), true);
	}

	@Transactional
	public Map<String, Object> aprobarRecarga(Long id) {
		SolicitudRecargaMiPlata solicitud = obtenerSolicitudBloqueada(id);
		if (solicitud.getEstado() != EstadoSolicitudRecargaMiPlata.PENDIENTE) {
			throw new NawinException(CodigoError.GEN_001, "La solicitud ya fue revisada.");
		}
		Cliente cliente = solicitud.getCliente();
		validarClienteActivo(cliente);
		Usuario revisor = usuarioActual();

		Map<String, Object> extras = new LinkedHashMap<>();
		String descripcionPago;
		String mensajeNotificacion;

		if (solicitud.getIdPaqueteCredito() != null) {
			// Compra de paquete de créditos: se otorgan los créditos a la cuenta.
			PaqueteCredito paquete = paqueteRepositorio.findById(solicitud.getIdPaqueteCredito())
					.orElseThrow(() -> new NawinException(CodigoError.GEN_002, "Paquete de créditos no encontrado."));
			creditoServicio.otorgarBono(cliente, paquete.getCantidadCreditos(),
					"Compra de créditos aprobada (orden " + solicitud.getCodigoOrden() + ")");
			descripcionPago = "Compra de créditos " + paquete.getNombre();
			mensajeNotificacion = "Se acreditaron " + paquete.getCantidadCreditos() + " créditos a tu cuenta.";
			extras.put("creditosOtorgados", paquete.getCantidadCreditos());
		} else if (solicitud.getIdPlan() != null) {
			// Compra de plan ilimitado: se activa la membresía correspondiente.
			Plan plan = planRepositorio.findById(solicitud.getIdPlan())
					.orElseThrow(() -> new NawinException(CodigoError.GEN_002, "Plan no encontrado."));
			Membresia membresia = activarMembresiaPorAprobacion(cliente, plan);
			descripcionPago = "Compra de plan " + plan.getNombre();
			mensajeNotificacion = "Tu plan " + plan.getNombre() + " fue activado.";
			extras.put("membresia", MapeadorRespuesta.membresia(membresia));
		} else {
			// Recarga simple: se acredita saldo en soles a la billetera MiPlata.
			acreditar(cliente, solicitud.getMontoSoles(), TipoMovimientoMiPlata.RECARGA_APROBADA,
					"Recarga MiPlata aprobada", solicitud, null, null, revisor);
			boolean bonoReferido = solicitud.getClienteReferido() != null
					&& solicitud.getClienteReferido().getEstado() == EstadoCliente.ACTIVO;
			if (bonoReferido) {
				acreditar(solicitud.getClienteReferido(), BONO_REFERIDO, TipoMovimientoMiPlata.BONO_REFERIDO,
						"Bono por referido MiPlata", solicitud, null, null, revisor);
				notificar(solicitud.getClienteReferido(), null, TipoNotificacion.RECARGA_MIPLATA,
						"Bono por referido acreditado",
						"Recibiste S/ " + BONO_REFERIDO + " por una recarga aprobada con tu codigo de referido.",
						revisor);
			}
			descripcionPago = "Recarga MiPlata aprobada";
			mensajeNotificacion = "Se acreditó S/ " + solicitud.getMontoSoles() + " en tu billetera MiPlata.";
		}

		solicitud.setEstado(EstadoSolicitudRecargaMiPlata.APROBADA);
		solicitud.setRevisadoPor(revisor);
		solicitud.setFechaRevision(LocalDateTime.now());

		Pago pago = crearPagoInterno(cliente, null, null, solicitud.getMontoSoles(),
				MedioPago.TRANSFERENCIA, "MIPLATA-ORDEN-" + solicitud.getIdSolicitudRecargaMiPlata(),
				descripcionPago, revisor, null);
		// El comprobante requiere el documento del cliente (tipo y número). Los
		// clientes freemium pueden no tenerlo aún: en ese caso se otorga igual y
		// el comprobante queda pendiente para cuando completen sus datos.
		boolean puedeEmitirComprobante = cliente.getTipoDocumento() != null
				&& cliente.getNumeroDocumento() != null;
		Map<String, Object> comprobante = puedeEmitirComprobante
				? comprobanteServicio.emitirAutomatico(pago, revisor)
				: null;
		notificar(cliente, null, TipoNotificacion.RECARGA_MIPLATA, "Pago aprobado",
				mensajeNotificacion + (puedeEmitirComprobante ? " Comprobante emitido." : ""),
				revisor);
		// Push al cliente: su pago fue procesado.
		notificacionPush.enviarAUsuario(cliente.getUsuario().getIdUsuario(),
				pe.nawin.enumeracion.CategoriaNotificacion.PAGOS, "Pago aprobado", mensajeNotificacion, java.util.Map.of());

		Map<String, Object> respuesta = solicitudRespuesta(solicitud, false);
		respuesta.putAll(extras);
		respuesta.put("pago", pagoRespuesta(pago));
		respuesta.put("comprobante", comprobante);
		return respuesta;
	}

	private Membresia activarMembresiaPorAprobacion(Cliente cliente, Plan plan) {
		if (plan.getEstado() != EstadoPlan.ACTIVO) {
			throw new NawinException(CodigoError.GEN_001, "Plan no disponible.");
		}
		LocalDate inicio = LocalDate.now();
		LocalDate fin = inicio.plusDays(plan.getDiasVigencia() - 1L);
		if (membresiaRepositorio.existsByCliente_IdClienteAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
				cliente.getIdCliente(), EstadoMembresia.ACTIVA, fin, inicio)) {
			throw new NawinException(CodigoError.MEM_001, "Ya existe una membresía activa superpuesta.");
		}
		Membresia membresia = new Membresia();
		membresia.setCliente(cliente);
		membresia.setPlan(plan);
		membresia.setFechaInicio(inicio);
		membresia.setFechaFin(fin);
		membresia.setDiasVigencia(plan.getDiasVigencia());
		membresia.setPrecioPagado(plan.getPrecioSoles());
		membresia.setEstado(EstadoMembresia.ACTIVA);
		membresia.setObservacion("Activado por aprobación de pago Yape");
		membresia.setCreadoPor(cliente.getUsuario());
		membresia.setFechaActivacion(LocalDateTime.now());
		membresiaRepositorio.save(membresia);
		copiarAccesosDelPlan(membresia);
		return membresia;
	}

	@Transactional
	public Map<String, Object> rechazarRecarga(Long id, RechazarRecargaMiPlataRequest request) {
		if (request == null || !StringUtils.hasText(request.motivo())) {
			throw new NawinException(CodigoError.GEN_001, "El motivo de rechazo es obligatorio.");
		}
		SolicitudRecargaMiPlata solicitud = obtenerSolicitudBloqueada(id);
		if (solicitud.getEstado() != EstadoSolicitudRecargaMiPlata.PENDIENTE) {
			throw new NawinException(CodigoError.GEN_001, "La solicitud ya fue revisada.");
		}
		Usuario revisor = usuarioActual();
		solicitud.setEstado(EstadoSolicitudRecargaMiPlata.RECHAZADA);
		solicitud.setMotivoRechazo(request.motivo());
		solicitud.setRevisadoPor(revisor);
		solicitud.setFechaRevision(LocalDateTime.now());
		registrarRechazo(solicitud, revisor);
		notificar(solicitud.getCliente(), null, TipoNotificacion.RECARGA_MIPLATA, "Recarga MiPlata rechazada",
				"Tu solicitud de recarga por S/ " + solicitud.getMontoSoles() + " fue rechazada. Motivo: "
						+ request.motivo(),
				revisor);
		return solicitudRespuesta(solicitud, false);
	}

	/** Apelación del cliente a una recarga rechazada (solo una vez, solo si está RECHAZADA). */
	@Transactional
	public Map<String, Object> apelarRecarga(Long id, pe.nawin.dto.solicitud.ApelarRecargaMiPlataRequest request) {
		if (request == null || !StringUtils.hasText(request.mensaje())) {
			throw new NawinException(CodigoError.GEN_001, "Escribe el motivo de tu apelación.");
		}
		Cliente cliente = clienteActual();
		SolicitudRecargaMiPlata solicitud = obtenerSolicitud(id);
		if (!solicitud.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
			throw new NawinException(CodigoError.GEN_002, "Recarga no encontrada.");
		}
		if (solicitud.getEstado() != EstadoSolicitudRecargaMiPlata.RECHAZADA) {
			throw new NawinException(CodigoError.GEN_001, "Solo puedes apelar una recarga rechazada.");
		}
		if (StringUtils.hasText(solicitud.getApelacion())) {
			throw new NawinException(CodigoError.GEN_001, "Ya enviaste una apelación para esta recarga.");
		}
		solicitud.setApelacion(request.mensaje().trim());
		solicitud.setFechaApelacion(LocalDateTime.now());
		solicitud.setApelacionRespondida(false);
		return solicitudRespuesta(solicitud, false);
	}

	/** Admin: acepta la apelación y devuelve la recarga a la cola de revisión (PENDIENTE). */
	@Transactional
	public Map<String, Object> reabrirRecarga(Long id) {
		SolicitudRecargaMiPlata solicitud = obtenerSolicitudBloqueada(id);
		if (solicitud.getEstado() != EstadoSolicitudRecargaMiPlata.RECHAZADA) {
			throw new NawinException(CodigoError.GEN_001, "Solo se puede reabrir una recarga rechazada.");
		}
		Usuario revisor = usuarioActual();
		solicitud.setEstado(EstadoSolicitudRecargaMiPlata.PENDIENTE);
		solicitud.setApelacionRespondida(true);
		notificar(solicitud.getCliente(), null, TipoNotificacion.RECARGA_MIPLATA, "Apelación aceptada",
				"Revisaremos de nuevo tu recarga por S/ " + solicitud.getMontoSoles() + ". Gracias por avisarnos.",
				revisor);
		notificacionPush.enviarAUsuario(solicitud.getCliente().getUsuario().getIdUsuario(),
				pe.nawin.enumeracion.CategoriaNotificacion.PAGOS, "Apelación aceptada",
				"Tu recarga volvió a revisión.", java.util.Map.of());
		return solicitudRespuesta(solicitud, false);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> billeteraCliente(Long idCliente) {
		return billeteraRespuesta(obtenerBilletera(idCliente));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> movimientosCliente(Long idCliente) {
		return movimientoRepositorio.findByCliente_IdClienteOrderByFechaCreacionDesc(idCliente).stream()
				.map(this::movimientoRespuesta)
				.toList();
	}

	@Transactional
	public Map<String, Object> comprarCreditos(ComprarCreditosMiPlataRequest request, String claveIdempotencia) {
		Cliente cliente = clienteActual();
		Map<String, Object> replay = respuestaCompraExistente(cliente, claveIdempotencia);
		if (replay != null) {
			return replay;
		}
		validarClienteActivo(cliente);
		if (request.cantidadPaquetes() < 1 || request.cantidadPaquetes() > MAX_PAQUETES_POR_COMPRA) {
			throw new NawinException(CodigoError.GEN_001,
					"La cantidad de paquetes debe estar entre 1 y " + MAX_PAQUETES_POR_COMPRA + ".");
		}
		PaqueteCredito paquete = paqueteRepositorio.findById(request.idPaqueteCredito())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		if (!paquete.isActivo()) {
			throw new NawinException(CodigoError.GEN_001, "Paquete de creditos inactivo.");
		}
		BigDecimal total = paquete.getPrecioSoles().multiply(BigDecimal.valueOf(request.cantidadPaquetes()));

		VentaCredito venta = new VentaCredito();
		venta.setCliente(cliente);
		venta.setPaqueteCredito(paquete);
		venta.setCantidadPaquetes(request.cantidadPaquetes());
		venta.setCreditosOtorgados(paquete.getCantidadCreditos() * request.cantidadPaquetes());
		venta.setPrecioUnitario(paquete.getPrecioSoles());
		venta.setTotalSoles(total);
		venta.setFechaVencimientoCreditos(LocalDate.now().plusDays(paquete.getDiasVigencia()));
		venta.setEstado(EstadoVentaCredito.PENDIENTE);
		venta.setCreadoPor(cliente.getUsuario());
		ventaRepositorio.save(venta);

		BilleteraMiPlata billetera = debitar(cliente, total, TipoMovimientoMiPlata.COMPRA_CREDITOS,
				"Compra de creditos con MiPlata", null, venta, null, cliente.getUsuario());
		creditoServicio.cargarPorVentaPagada(venta, cliente.getUsuario());
		Pago pago = crearPagoInterno(cliente, null, venta, total, MedioPago.MIPLATA,
				"MIPLATA-CREDITOS-" + venta.getIdVentaCredito(), "Compra de creditos con MiPlata",
				cliente.getUsuario(), claveIdempotencia);
		Map<String, Object> comprobante = comprobanteServicio.emitirAutomatico(pago, cliente.getUsuario());
		notificar(cliente, null, TipoNotificacion.CREDITOS_CARGADOS, "Creditos cargados",
				"Se cargaron " + venta.getCreditosOtorgados()
						+ " creditos por S/ " + total + " y el comprobante fue emitido.",
				cliente.getUsuario());

		Map<String, Object> respuesta = new LinkedHashMap<>();
		respuesta.put("billetera", billeteraRespuesta(billetera));
		respuesta.put("ventaCredito", ventaRespuesta(venta));
		respuesta.put("pago", pagoRespuesta(pago));
		respuesta.put("comprobante", comprobante);
		return respuesta;
	}

	@Transactional
	public Map<String, Object> comprarPlan(ComprarPlanMiPlataRequest request, String claveIdempotencia) {
		Cliente cliente = clienteActual();
		Map<String, Object> replay = respuestaCompraExistente(cliente, claveIdempotencia);
		if (replay != null) {
			return replay;
		}
		validarClienteActivo(cliente);
		Plan plan = planRepositorio.findById(request.idPlan()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		if (plan.getEstado() != EstadoPlan.ACTIVO) {
			throw new NawinException(CodigoError.GEN_001, "Plan no disponible.");
		}
		LocalDate inicio = request.fechaInicio() == null ? LocalDate.now() : request.fechaInicio();
		if (inicio.isBefore(LocalDate.now())) {
			throw new NawinException(CodigoError.GEN_001, "La fecha de inicio no puede estar en el pasado.");
		}
		if (inicio.isAfter(LocalDate.now().plusDays(MAX_DIAS_INICIO_FUTURO))) {
			throw new NawinException(CodigoError.GEN_001,
					"La fecha de inicio no puede superar los " + MAX_DIAS_INICIO_FUTURO + " días desde hoy.");
		}
		LocalDate fin = inicio.plusDays(plan.getDiasVigencia() - 1L);
		if (membresiaRepositorio.existsByCliente_IdClienteAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
				cliente.getIdCliente(), EstadoMembresia.ACTIVA, fin, inicio)) {
			throw new NawinException(CodigoError.MEM_001, "Ya existe una membresia activa superpuesta.");
		}

		Membresia membresia = new Membresia();
		membresia.setCliente(cliente);
		membresia.setPlan(plan);
		membresia.setFechaInicio(inicio);
		membresia.setFechaFin(fin);
		membresia.setDiasVigencia(plan.getDiasVigencia());
		membresia.setPrecioPagado(plan.getPrecioSoles());
		membresia.setEstado(EstadoMembresia.ACTIVA);
		membresia.setObservacion("Compra con MiPlata");
		membresia.setCreadoPor(cliente.getUsuario());
		membresia.setFechaActivacion(LocalDateTime.now());
		membresiaRepositorio.save(membresia);
		copiarAccesosDelPlan(membresia);

		BilleteraMiPlata billetera = debitar(cliente, plan.getPrecioSoles(), TipoMovimientoMiPlata.COMPRA_PLAN,
				"Compra de plan con MiPlata", null, null, membresia, cliente.getUsuario());
		Pago pago = crearPagoInterno(cliente, membresia, null, plan.getPrecioSoles(), MedioPago.MIPLATA,
				"MIPLATA-PLAN-" + membresia.getIdMembresia(), "Compra de plan con MiPlata",
				cliente.getUsuario(), claveIdempotencia);
		Map<String, Object> comprobante = comprobanteServicio.emitirAutomatico(pago, cliente.getUsuario());
		notificar(cliente, membresia, TipoNotificacion.COMPRA_MIPLATA, "Plan activado",
				"Tu plan " + plan.getNombre() + " fue activado por S/ " + plan.getPrecioSoles()
						+ " y el comprobante fue emitido.",
				cliente.getUsuario());

		Map<String, Object> respuesta = new LinkedHashMap<>();
		respuesta.put("billetera", billeteraRespuesta(billetera));
		respuesta.put("membresia", MapeadorRespuesta.membresia(membresia));
		respuesta.put("pago", pagoRespuesta(pago));
		respuesta.put("comprobante", comprobante);
		return respuesta;
	}

	private void copiarAccesosDelPlan(Membresia membresia) {
		for (PlanEndpoint regla : planEndpointRepositorio.findByPlan_IdPlan(membresia.getPlan().getIdPlan())) {
			if (!regla.isHabilitado()) {
				continue;
			}
			MembresiaEndpoint acceso = new MembresiaEndpoint();
			acceso.setMembresia(membresia);
			acceso.setEndpoint(regla.getEndpoint());
			acceso.setHabilitado(true);
			acceso.setModalidadAcceso(regla.getModalidadAcceso());
			acceso.setLimiteDiario(regla.getLimiteDiario());
			acceso.setLimiteTotal(regla.getLimiteCiclo());
			acceso.setCostoCreditosCliente(regla.getCostoCreditosCliente());
			acceso.setRequiereMfa(regla.isRequiereMfa());
			acceso.setRequiereFinalidad(regla.isRequiereFinalidad());
			acceso.setRequiereJustificacion(regla.isRequiereJustificacion());
			acceso.setPermiteExportar(regla.isPermiteExportar());
			acceso.setDiasRetencion(regla.getDiasRetencion());
			membresiaEndpointRepositorio.save(acceso);
		}
	}

	private Pago crearPagoInterno(Cliente cliente, Membresia membresia, VentaCredito venta, BigDecimal monto,
			MedioPago medioPago, String numeroOperacion, String observacion, Usuario registradoPor,
			String claveIdempotencia) {
		Pago pago = new Pago();
		pago.setCliente(cliente);
		pago.setMembresia(membresia);
		pago.setVentaCredito(venta);
		pago.setMontoSoles(monto);
		pago.setMedioPago(medioPago);
		pago.setNumeroOperacion(numeroOperacion);
		pago.setFechaPago(LocalDateTime.now());
		pago.setEstado(EstadoPago.CONFIRMADO);
		pago.setObservacion(observacion);
		pago.setRegistradoPor(registradoPor);
		pago.setClaveIdempotencia(StringUtils.hasText(claveIdempotencia) ? claveIdempotencia.trim() : null);
		return pagoRepositorio.save(pago);
	}

	/**
	 * Si la clave de idempotencia ya fue procesada, reconstruye la respuesta de la
	 * compra original en lugar de cobrar dos veces.
	 */
	private Map<String, Object> respuestaCompraExistente(Cliente cliente, String claveIdempotencia) {
		if (!StringUtils.hasText(claveIdempotencia)) {
			return null;
		}
		Pago pago = pagoRepositorio.findByClaveIdempotencia(claveIdempotencia.trim()).orElse(null);
		if (pago == null) {
			return null;
		}
		if (!pago.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
			throw new NawinException(CodigoError.CON_002, "Clave de idempotencia ya usada por otro cliente.");
		}
		Map<String, Object> respuesta = new LinkedHashMap<>();
		respuesta.put("billetera", billeteraRespuesta(obtenerBilletera(cliente.getIdCliente())));
		if (pago.getVentaCredito() != null) {
			respuesta.put("ventaCredito", ventaRespuesta(pago.getVentaCredito()));
		}
		if (pago.getMembresia() != null) {
			respuesta.put("membresia", MapeadorRespuesta.membresia(pago.getMembresia()));
		}
		respuesta.put("pago", pagoRespuesta(pago));
		respuesta.put("comprobante", comprobanteServicio.emitirAutomatico(pago, pago.getRegistradoPor()));
		respuesta.put("duplicado", true);
		return respuesta;
	}

	/**
	 * Devuelve el monto de un pago MiPlata anulado a la billetera del cliente.
	 * Invocado por PagoServicio al anular pagos con medio MIPLATA.
	 */
	@Transactional
	public void devolverPorPagoAnulado(Pago pago, Usuario registradoPor, String motivo) {
		acreditar(pago.getCliente(), pago.getMontoSoles(), TipoMovimientoMiPlata.DEVOLUCION,
				"Devolución por anulación de pago" + (StringUtils.hasText(motivo) ? ": " + motivo : ""),
				null, pago.getVentaCredito(), pago.getMembresia(), registradoPor);
		notificar(pago.getCliente(), pago.getMembresia(), TipoNotificacion.RECARGA_MIPLATA,
				"Devolución a tu billetera MiPlata",
				"Se devolvió S/ " + pago.getMontoSoles() + " a tu billetera por la anulación de un pago.",
				registradoPor);
	}

	private void notificar(Cliente cliente, Membresia membresia, TipoNotificacion tipo, String titulo,
			String mensaje, Usuario creadoPor) {
		notificacionServicio.crearAutomatica(cliente, membresia, tipo, titulo, mensaje, creadoPor);
	}

	private BilleteraMiPlata acreditar(Cliente cliente, BigDecimal monto, TipoMovimientoMiPlata tipo, String descripcion,
			SolicitudRecargaMiPlata solicitud, VentaCredito venta, Membresia membresia, Usuario registradoPor) {
		BilleteraMiPlata billetera = obtenerBilleteraBloqueada(cliente.getIdCliente());
		BigDecimal anterior = billetera.getSaldoDisponible();
		BigDecimal posterior = anterior.add(monto);
		billetera.setSaldoDisponible(posterior);
		registrarMovimiento(billetera, cliente, solicitud, venta, membresia, tipo, monto, anterior, posterior, descripcion, registradoPor);
		return billetera;
	}

	private BilleteraMiPlata debitar(Cliente cliente, BigDecimal monto, TipoMovimientoMiPlata tipo, String descripcion,
			SolicitudRecargaMiPlata solicitud, VentaCredito venta, Membresia membresia, Usuario registradoPor) {
		validarMonto(monto);
		BilleteraMiPlata billetera = obtenerBilleteraBloqueada(cliente.getIdCliente());
		BigDecimal anterior = billetera.getSaldoDisponible();
		if (anterior.compareTo(monto) < 0) {
			throw new NawinException(CodigoError.CRE_001, "Saldo MiPlata insuficiente.");
		}
		BigDecimal posterior = anterior.subtract(monto);
		billetera.setSaldoDisponible(posterior);
		registrarMovimiento(billetera, cliente, solicitud, venta, membresia, tipo, monto, anterior, posterior, descripcion, registradoPor);
		return billetera;
	}

	private void registrarMovimiento(BilleteraMiPlata billetera, Cliente cliente, SolicitudRecargaMiPlata solicitud,
			VentaCredito venta, Membresia membresia, TipoMovimientoMiPlata tipo, BigDecimal monto,
			BigDecimal anterior, BigDecimal posterior, String descripcion, Usuario registradoPor) {
		MovimientoMiPlata movimiento = new MovimientoMiPlata();
		movimiento.setBilletera(billetera);
		movimiento.setCliente(cliente);
		movimiento.setSolicitudRecarga(solicitud);
		movimiento.setVentaCredito(venta);
		movimiento.setMembresia(membresia);
		movimiento.setTipoMovimiento(tipo);
		movimiento.setMontoSoles(monto);
		movimiento.setSaldoAnterior(anterior);
		movimiento.setSaldoPosterior(posterior);
		movimiento.setDescripcion(descripcion);
		movimiento.setRegistradoPor(registradoPor);
		movimientoRepositorio.save(movimiento);
	}

	private void registrarRechazo(SolicitudRecargaMiPlata solicitud, Usuario revisor) {
		BilleteraMiPlata billetera = obtenerBilleteraBloqueada(solicitud.getCliente().getIdCliente());
		BigDecimal saldo = billetera.getSaldoDisponible();
		registrarMovimiento(billetera, solicitud.getCliente(), solicitud, null, null,
				TipoMovimientoMiPlata.RECARGA_RECHAZADA, solicitud.getMontoSoles(), saldo, saldo,
				"Recarga MiPlata rechazada", revisor);
	}

	private Cliente clienteActual() {
		return clienteRepositorio.findByUsuario_IdUsuario(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.CLI_001));
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}

	private BilleteraMiPlata obtenerBilletera(Long idCliente) {
		return billeteraRepositorio.findByCliente_IdCliente(idCliente).orElseGet(() -> crearBilletera(idCliente));
	}

	private BilleteraMiPlata obtenerBilleteraBloqueada(Long idCliente) {
		return billeteraRepositorio.findWithLockByCliente_IdCliente(idCliente).orElseGet(() -> crearBilletera(idCliente));
	}

	private BilleteraMiPlata crearBilletera(Long idCliente) {
		Cliente cliente = clienteRepositorio.findById(idCliente).orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		BilleteraMiPlata billetera = new BilleteraMiPlata();
		billetera.setCliente(cliente);
		return billeteraRepositorio.save(billetera);
	}

	private SolicitudRecargaMiPlata obtenerSolicitud(Long id) {
		return solicitudRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private SolicitudRecargaMiPlata obtenerSolicitudBloqueada(Long id) {
		return solicitudRepositorio.findWithLockByIdSolicitudRecargaMiPlata(id)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private void validarClienteActivo(Cliente cliente) {
		if (cliente.getEstado() != EstadoCliente.ACTIVO) {
			throw new NawinException(CodigoError.GEN_001, "El cliente no está activo; no puede operar la billetera.");
		}
	}

	private Cliente resolverReferido(Cliente cliente, String codigoReferido) {
		String codigo = normalizarCodigo(codigoReferido);
		if (!StringUtils.hasText(codigo)) {
			return null;
		}
		return clienteRepositorio.findByCodigoReferido(codigo)
				.filter(referido -> !referido.getIdCliente().equals(cliente.getIdCliente()))
				.filter(referido -> referido.getEstado() == EstadoCliente.ACTIVO)
				.orElse(null);
	}

	private String normalizarCodigo(String codigoReferido) {
		return StringUtils.hasText(codigoReferido) ? codigoReferido.trim().toUpperCase() : null;
	}

	private void validarMonto(BigDecimal monto) {
		if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
			throw new NawinException(CodigoError.GEN_001, "El monto en soles debe ser mayor a cero.");
		}
	}

	private void validarMontoRecarga(BigDecimal monto) {
		validarMonto(monto);
		if (monto.compareTo(MONTO_MINIMO_RECARGA) < 0) {
			throw new NawinException(CodigoError.GEN_001,
					"El monto mínimo de recarga es S/ " + MONTO_MINIMO_RECARGA + ".");
		}
		if (monto.compareTo(MONTO_MAXIMO_RECARGA) > 0) {
			throw new NawinException(CodigoError.GEN_001,
					"El monto máximo de recarga por solicitud es S/ " + MONTO_MAXIMO_RECARGA + ".");
		}
		if (monto.scale() > 2) {
			throw new NawinException(CodigoError.GEN_001, "El monto no puede tener más de dos decimales.");
		}
	}

	private void validarBase64(String valor) {
		String base64 = valor;
		if (!StringUtils.hasText(base64)) {
			throw new NawinException(CodigoError.GEN_001, "El comprobante base64 es obligatorio.");
		}
		if (base64.length() > MAX_COMPROBANTE_BASE64_CARACTERES) {
			throw new NawinException(CodigoError.GEN_001,
					"El comprobante adjunto supera el tamaño máximo permitido (5 MB).");
		}
		int coma = base64.indexOf(',');
		if (base64.startsWith("data:") && coma > 0) {
			base64 = base64.substring(coma + 1);
		}
		try {
			Base64.getDecoder().decode(base64);
		} catch (IllegalArgumentException ex) {
			throw new NawinException(CodigoError.GEN_001, "El comprobante debe estar en formato base64 valido.");
		}
	}

	private Map<String, Object> billeteraRespuesta(BilleteraMiPlata billetera) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idBilleteraMiPlata", billetera.getIdBilleteraMiPlata());
		m.put("idCliente", billetera.getCliente().getIdCliente());
		m.put("saldoDisponible", billetera.getSaldoDisponible());
		m.put("moneda", billetera.getMoneda());
		m.put("codigoReferido", billetera.getCliente().getCodigoReferido());
		m.put("fechaActualizacion", billetera.getFechaActualizacion());
		return m;
	}

	private Map<String, Object> solicitudRespuesta(SolicitudRecargaMiPlata solicitud, boolean incluirComprobante) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idSolicitudRecargaMiPlata", solicitud.getIdSolicitudRecargaMiPlata());
		m.put("idCliente", solicitud.getCliente().getIdCliente());
		m.put("montoSoles", solicitud.getMontoSoles());
		m.put("moneda", "PEN");
		m.put("codigoOrden", solicitud.getCodigoOrden());
		m.put("codigoOperacion", solicitud.getCodigoOperacion());
		m.put("concepto", solicitud.getConcepto());
		m.put("idPlan", solicitud.getIdPlan());
		m.put("idPaqueteCredito", solicitud.getIdPaqueteCredito());
		m.put("fechaExpiracion", solicitud.getFechaExpiracion());
		m.put("tieneComprobante", solicitud.getComprobanteBase64() != null);
		m.put("codigoReferidoIngresado", solicitud.getCodigoReferidoIngresado());
		m.put("idClienteReferido", solicitud.getClienteReferido() == null ? null : solicitud.getClienteReferido().getIdCliente());
		m.put("estado", solicitud.getEstado());
		m.put("motivoRechazo", solicitud.getMotivoRechazo());
		m.put("apelacion", solicitud.getApelacion());
		m.put("fechaApelacion", solicitud.getFechaApelacion());
		m.put("apelacionRespondida", solicitud.isApelacionRespondida());
		m.put("fechaRevision", solicitud.getFechaRevision());
		m.put("fechaCreacion", solicitud.getFechaCreacion());
		if (incluirComprobante) {
			m.put("comprobanteBase64", solicitud.getComprobanteBase64());
		}
		return m;
	}

	private static final String ALFABETO_ORDEN = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private final java.security.SecureRandom aleatorioOrden = new java.security.SecureRandom();

	@Transactional
	public Map<String, Object> crearOrden(pe.nawin.dto.solicitud.CrearOrdenPagoRequest request) {
		Cliente cliente = clienteActual();
		validarClienteActivo(cliente);
		validarMontoRecarga(request.montoSoles());
		SolicitudRecargaMiPlata orden = new SolicitudRecargaMiPlata();
		orden.setCliente(cliente);
		orden.setMontoSoles(request.montoSoles());
		orden.setConcepto(request.concepto());
		orden.setIdPlan(request.idPlan());
		orden.setIdPaqueteCredito(request.idPaqueteCredito());
		orden.setEstado(EstadoSolicitudRecargaMiPlata.PENDIENTE);
		orden.setCodigoOrden(generarCodigoOrden());
		orden.setFechaExpiracion(LocalDateTime.now().plusHours(24));
		solicitudRepositorio.save(orden);
		return solicitudRespuesta(orden, false);
	}

	@Transactional
	public Map<String, Object> adjuntarComprobante(Long idOrden,
			pe.nawin.dto.solicitud.AdjuntarComprobanteRequest request) {
		Cliente cliente = clienteActual();
		SolicitudRecargaMiPlata orden = solicitudRepositorio.findWithLockByIdSolicitudRecargaMiPlata(idOrden)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002, "Orden no encontrada."));
		if (!orden.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
			throw new NawinException(CodigoError.GEN_001, "La orden no te pertenece.");
		}
		if (orden.getEstado() == EstadoSolicitudRecargaMiPlata.CANCELADO) {
			throw new NawinException(CodigoError.GEN_001,
					"La orden fue cancelada por vencimiento (24 h). Genera una nueva.");
		}
		if (orden.getEstado() != EstadoSolicitudRecargaMiPlata.PENDIENTE) {
			throw new NawinException(CodigoError.GEN_001, "La orden ya no admite comprobante.");
		}
		if (orden.getFechaExpiracion() != null && orden.getFechaExpiracion().isBefore(LocalDateTime.now())) {
			orden.setEstado(EstadoSolicitudRecargaMiPlata.CANCELADO);
			throw new NawinException(CodigoError.GEN_001, "La orden venció. Genera una nueva.");
		}
		validarBase64(request.comprobanteBase64());
		orden.setComprobanteBase64(request.comprobanteBase64());
		orden.setCodigoOperacion(request.codigoOperacion().trim());
		notificar(cliente, null, TipoNotificacion.RECARGA_MIPLATA, "Comprobante recibido",
				"Recibimos el comprobante de la orden " + orden.getCodigoOrden() + ". Está en revisión.",
				cliente.getUsuario());
		return solicitudRespuesta(orden, false);
	}

	@Transactional
	public int cancelarOrdenesVencidas() {
		List<SolicitudRecargaMiPlata> vencidas = solicitudRepositorio
				.findByEstadoAndComprobanteBase64IsNullAndFechaExpiracionBefore(
						EstadoSolicitudRecargaMiPlata.PENDIENTE, LocalDateTime.now());
		for (SolicitudRecargaMiPlata orden : vencidas) {
			orden.setEstado(EstadoSolicitudRecargaMiPlata.CANCELADO);
			notificar(orden.getCliente(), null, TipoNotificacion.RECARGA_MIPLATA, "Orden de pago cancelada",
					"Tu orden " + orden.getCodigoOrden() + " se canceló por no registrar el pago en 24 horas.",
					orden.getCliente().getUsuario());
		}
		return vencidas.size();
	}

	private String generarCodigoOrden() {
		String codigo;
		do {
			StringBuilder sb = new StringBuilder("NAWI-");
			for (int i = 0; i < 6; i++) {
				sb.append(ALFABETO_ORDEN.charAt(aleatorioOrden.nextInt(ALFABETO_ORDEN.length())));
			}
			codigo = sb.toString();
		} while (solicitudRepositorio.findByCodigoOrden(codigo).isPresent());
		return codigo;
	}

	private Map<String, Object> movimientoRespuesta(MovimientoMiPlata movimiento) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idMovimientoMiPlata", movimiento.getIdMovimientoMiPlata());
		m.put("idCliente", movimiento.getCliente().getIdCliente());
		m.put("tipoMovimiento", movimiento.getTipoMovimiento());
		m.put("montoSoles", movimiento.getMontoSoles());
		m.put("saldoAnterior", movimiento.getSaldoAnterior());
		m.put("saldoPosterior", movimiento.getSaldoPosterior());
		m.put("moneda", movimiento.getMoneda());
		m.put("descripcion", movimiento.getDescripcion());
		m.put("idSolicitudRecargaMiPlata", movimiento.getSolicitudRecarga() == null ? null : movimiento.getSolicitudRecarga().getIdSolicitudRecargaMiPlata());
		m.put("idVentaCredito", movimiento.getVentaCredito() == null ? null : movimiento.getVentaCredito().getIdVentaCredito());
		m.put("idMembresia", movimiento.getMembresia() == null ? null : movimiento.getMembresia().getIdMembresia());
		m.put("fechaCreacion", movimiento.getFechaCreacion());
		return m;
	}

	private Map<String, Object> pagoRespuesta(Pago pago) {
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

	private Map<String, Object> ventaRespuesta(VentaCredito venta) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idVentaCredito", venta.getIdVentaCredito());
		m.put("idCliente", venta.getCliente().getIdCliente());
		m.put("idPaqueteCredito", venta.getPaqueteCredito().getIdPaqueteCredito());
		m.put("cantidadPaquetes", venta.getCantidadPaquetes());
		m.put("creditosOtorgados", venta.getCreditosOtorgados());
		m.put("precioUnitario", venta.getPrecioUnitario());
		m.put("totalSoles", venta.getTotalSoles());
		m.put("fechaVencimientoCreditos", venta.getFechaVencimientoCreditos());
		m.put("estado", venta.getEstado());
		return m;
	}
}
