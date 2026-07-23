package pe.nawin.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.ActivarMembresiaRequest;
import pe.nawin.dto.solicitud.MembresiaActualizarRequest;
import pe.nawin.dto.solicitud.MembresiaCrearRequest;
import pe.nawin.dto.solicitud.MembresiaEndpointRequest;
import pe.nawin.dto.solicitud.MotivoRequest;
import pe.nawin.dto.solicitud.RenovarMembresiaRequest;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.EndpointBusqueda;
import pe.nawin.entidad.Membresia;
import pe.nawin.entidad.MembresiaEndpoint;
import pe.nawin.entidad.Pago;
import pe.nawin.entidad.Plan;
import pe.nawin.entidad.PlanEndpoint;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.enumeracion.EstadoPago;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.ConsultaRepositorio;
import pe.nawin.repositorio.EndpointBusquedaRepositorio;
import pe.nawin.repositorio.MembresiaEndpointRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.PagoRepositorio;
import pe.nawin.repositorio.PlanEndpointRepositorio;
import pe.nawin.repositorio.PlanRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;

@Service
public class MembresiaServicio {

	private final MembresiaRepositorio membresiaRepositorio;
	private final MembresiaEndpointRepositorio membresiaEndpointRepositorio;
	private final PlanEndpointRepositorio planEndpointRepositorio;
	private final ClienteRepositorio clienteRepositorio;
	private final PlanRepositorio planRepositorio;
	private final EndpointBusquedaRepositorio endpointRepositorio;
	private final PagoRepositorio pagoRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final ConsultaRepositorio consultaRepositorio;
	private final PlanServicio planServicio;

	public MembresiaServicio(MembresiaRepositorio membresiaRepositorio,
			MembresiaEndpointRepositorio membresiaEndpointRepositorio, PlanEndpointRepositorio planEndpointRepositorio,
			ClienteRepositorio clienteRepositorio, PlanRepositorio planRepositorio,
			EndpointBusquedaRepositorio endpointRepositorio, PagoRepositorio pagoRepositorio,
			UsuarioRepositorio usuarioRepositorio, ConsultaRepositorio consultaRepositorio, PlanServicio planServicio) {
		this.membresiaRepositorio = membresiaRepositorio;
		this.membresiaEndpointRepositorio = membresiaEndpointRepositorio;
		this.planEndpointRepositorio = planEndpointRepositorio;
		this.clienteRepositorio = clienteRepositorio;
		this.planRepositorio = planRepositorio;
		this.endpointRepositorio = endpointRepositorio;
		this.pagoRepositorio = pagoRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.consultaRepositorio = consultaRepositorio;
		this.planServicio = planServicio;
	}

	/**
	 * Mapea un acceso para el panel, calculando {@code consumidoTotal} en vivo por
	 * conteo de consultas (ya no existe el contador denormalizado; ver
	 * {@link ConsultaServicio}).
	 */
	private Map<String, Object> mapearAcceso(MembresiaEndpoint acceso) {
		Map<String, Object> m = MapeadorRespuesta.membresiaEndpoint(acceso);
		m.put("consumidoTotal", consultaRepositorio.countByMembresiaEndpoint_IdMembresiaEndpointAndEstadoIn(
				acceso.getIdMembresiaEndpoint(), ConsultaServicio.ESTADOS_CONSUMEN_CUOTA));
		return m;
	}

	@Transactional
	public Map<String, Object> crear(MembresiaCrearRequest request) {
		Cliente cliente = clienteRepositorio.findById(request.idCliente()).orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		Plan plan = planRepositorio.findById(request.idPlan()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		Membresia membresia = nuevaMembresia(cliente, plan, request.fechaInicio(), request.precioPagado(), request.observacion(), null);
		return MapeadorRespuesta.membresia(membresiaRepositorio.save(membresia));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listar(Long idCliente, EstadoMembresia estado) {
		List<Membresia> membresias = idCliente != null
				? membresiaRepositorio.findByCliente_IdCliente(idCliente)
				: estado != null ? membresiaRepositorio.findByEstado(estado) : membresiaRepositorio.findAll();
		return membresias.stream().map(MapeadorRespuesta::membresia).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> ver(Long id) {
		return MapeadorRespuesta.membresia(obtener(id));
	}

	@Transactional
	public Map<String, Object> actualizar(Long id, MembresiaActualizarRequest request) {
		Membresia membresia = obtener(id);
		if (membresia.getEstado() != EstadoMembresia.PENDIENTE) {
			throw new NawinException(CodigoError.MEM_001, "Solo se puede actualizar una membresía pendiente.");
		}
		if (request.fechaInicio() != null) {
			membresia.setFechaInicio(request.fechaInicio());
			membresia.setFechaFin(request.fechaInicio().plusDays(membresia.getDiasVigencia() - 1L));
		}
		if (request.precioPagado() != null) {
			membresia.setPrecioPagado(request.precioPagado());
		}
		membresia.setObservacion(request.observacion());
		return MapeadorRespuesta.membresia(membresia);
	}

	@Transactional
	public void cancelar(Long id, MotivoRequest request) {
		Membresia membresia = obtener(id);
		membresia.setEstado(EstadoMembresia.CANCELADA);
		membresia.setObservacion(request == null ? "Cancelada" : request.motivo());
	}

	@Transactional
	public Map<String, Object> activar(Long id, ActivarMembresiaRequest request) {
		Membresia membresia = obtener(id);
		Pago pago = pagoRepositorio.findById(request.idPago()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		if (pago.getEstado() != EstadoPago.CONFIRMADO || pago.getMontoSoles().compareTo(membresia.getPrecioPagado()) < 0) {
			throw new NawinException(CodigoError.PAG_001);
		}
		if (membresiaRepositorio.existsByCliente_IdClienteAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
				membresia.getCliente().getIdCliente(), EstadoMembresia.ACTIVA, membresia.getFechaFin(), membresia.getFechaInicio())) {
			throw new NawinException(CodigoError.MEM_001, "Ya existe una membresía activa superpuesta.");
		}
		membresia.setEstado(EstadoMembresia.ACTIVA);
		membresia.setFechaActivacion(LocalDateTime.now());
		copiarAccesosDelPlan(membresia);
		return MapeadorRespuesta.membresia(membresia);
	}

	@Transactional
	public Map<String, Object> renovar(Long id, RenovarMembresiaRequest request) {
		Membresia anterior = obtener(id);
		Plan plan = planRepositorio.findById(request.idPlan()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		Membresia nueva = nuevaMembresia(anterior.getCliente(), plan, request.fechaInicio(), plan.getPrecioSoles(),
				"Renovación de membresía " + anterior.getIdMembresia(), anterior);
		return MapeadorRespuesta.membresia(membresiaRepositorio.save(nueva));
	}

	@Transactional
	public void suspender(Long id, MotivoRequest request) {
		Membresia membresia = obtener(id);
		membresia.setEstado(EstadoMembresia.SUSPENDIDA);
		membresia.setObservacion(request == null ? "Suspendida" : request.motivo());
	}

	@Transactional
	public Map<String, Object> reactivar(Long id) {
		Membresia membresia = obtener(id);
		LocalDate hoy = LocalDate.now();
		if (hoy.isAfter(membresia.getFechaFin())) {
			throw new NawinException(CodigoError.MEM_002);
		}
		membresia.setEstado(EstadoMembresia.ACTIVA);
		return MapeadorRespuesta.membresia(membresia);
	}

	@Transactional
	public Map<String, Object> agregarAcceso(Long idMembresia, MembresiaEndpointRequest request) {
		Membresia membresia = obtener(idMembresia);
		EndpointBusqueda endpoint = endpointRepositorio.findById(request.idEndpoint()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		MembresiaEndpoint acceso = new MembresiaEndpoint();
		acceso.setMembresia(membresia);
		acceso.setEndpoint(endpoint);
		aplicarAcceso(acceso, request, endpoint);
		return mapearAcceso(membresiaEndpointRepositorio.save(acceso));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listarAccesos(Long idMembresia) {
		return membresiaEndpointRepositorio.findByMembresia_IdMembresia(idMembresia).stream()
				.map(this::mapearAcceso)
				.toList();
	}

	@Transactional
	public Map<String, Object> actualizarAcceso(Long idMembresia, Long idAcceso, MembresiaEndpointRequest request) {
		MembresiaEndpoint acceso = membresiaEndpointRepositorio.findByMembresia_IdMembresiaAndIdMembresiaEndpoint(idMembresia, idAcceso)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		EndpointBusqueda endpoint = endpointRepositorio.findById(request.idEndpoint()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		acceso.setEndpoint(endpoint);
		aplicarAcceso(acceso, request, endpoint);
		return mapearAcceso(acceso);
	}

	@Transactional
	public void deshabilitarAcceso(Long idMembresia, Long idAcceso) {
		membresiaEndpointRepositorio.findByMembresia_IdMembresiaAndIdMembresiaEndpoint(idMembresia, idAcceso)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002))
				.setHabilitado(false);
	}

	public Membresia obtener(Long id) {
		return membresiaRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private Membresia nuevaMembresia(Cliente cliente, Plan plan, LocalDate fechaInicio,
			java.math.BigDecimal precioPagado, String observacion, Membresia anterior) {
		Membresia membresia = new Membresia();
		membresia.setCliente(cliente);
		membresia.setPlan(plan);
		membresia.setMembresiaAnterior(anterior);
		membresia.setFechaInicio(fechaInicio);
		membresia.setDiasVigencia(plan.getDiasVigencia());
		membresia.setFechaFin(fechaInicio.plusDays(plan.getDiasVigencia() - 1L));
		membresia.setPrecioPagado(precioPagado);
		membresia.setEstado(EstadoMembresia.PENDIENTE);
		membresia.setObservacion(observacion);
		membresia.setCreadoPor(usuarioActual());
		return membresia;
	}

	private void copiarAccesosDelPlan(Membresia membresia) {
		if (!membresiaEndpointRepositorio.findByMembresia_IdMembresia(membresia.getIdMembresia()).isEmpty()) {
			return;
		}
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

	private void aplicarAcceso(MembresiaEndpoint acceso, MembresiaEndpointRequest request, EndpointBusqueda endpoint) {
		planServicio.validarRegla(endpoint.isEsCritico(), request.modalidadAcceso(), request.costoCreditosCliente(),
				request.requiereMfa(), request.requiereFinalidad(), request.requiereJustificacion());
		acceso.setHabilitado(request.habilitado());
		acceso.setModalidadAcceso(request.modalidadAcceso());
		acceso.setLimiteDiario(request.limiteDiario());
		acceso.setLimiteTotal(request.limiteTotal());
		acceso.setCostoCreditosCliente(request.costoCreditosCliente());
		acceso.setRequiereMfa(request.requiereMfa());
		acceso.setRequiereFinalidad(request.requiereFinalidad());
		acceso.setRequiereJustificacion(request.requiereJustificacion());
		acceso.setPermiteExportar(request.permiteExportar());
		acceso.setDiasRetencion(request.diasRetencion());
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}
}
