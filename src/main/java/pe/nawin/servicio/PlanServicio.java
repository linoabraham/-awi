package pe.nawin.servicio;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.configuracion.CacheConfiguracion;
import pe.nawin.dto.solicitud.EndpointBusquedaRequest;
import pe.nawin.dto.solicitud.PlanEndpointRequest;
import pe.nawin.dto.solicitud.PlanRequest;
import pe.nawin.entidad.EndpointBusqueda;
import pe.nawin.entidad.Plan;
import pe.nawin.entidad.PlanEndpoint;
import pe.nawin.enumeracion.EstadoPlan;
import pe.nawin.enumeracion.ModalidadAccesoEndpoint;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.EndpointBusquedaRepositorio;
import pe.nawin.repositorio.PlanEndpointRepositorio;
import pe.nawin.repositorio.PlanRepositorio;

@Service
public class PlanServicio {

	private final PlanRepositorio planRepositorio;
	private final EndpointBusquedaRepositorio endpointRepositorio;
	private final PlanEndpointRepositorio planEndpointRepositorio;

	public PlanServicio(PlanRepositorio planRepositorio, EndpointBusquedaRepositorio endpointRepositorio,
			PlanEndpointRepositorio planEndpointRepositorio) {
		this.planRepositorio = planRepositorio;
		this.endpointRepositorio = endpointRepositorio;
		this.planEndpointRepositorio = planEndpointRepositorio;
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_PLANES, allEntries = true)
	public Map<String, Object> crearPlan(PlanRequest request) {
		if (planRepositorio.existsByCodigo(request.codigo())) {
			throw new NawinException(CodigoError.GEN_001, "CÃ³digo de plan ya existe.");
		}
		Plan plan = new Plan();
		aplicarPlan(plan, request);
		return MapeadorRespuesta.plan(planRepositorio.save(plan));
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheConfiguracion.CACHE_PLANES, key = "#estado == null ? 'TODOS' : #estado.name()")
	public List<Map<String, Object>> listarPlanes(EstadoPlan estado) {
		List<Plan> planes = estado == null ? planRepositorio.findAll() : planRepositorio.findByEstado(estado);
		return planes.stream().map(MapeadorRespuesta::plan).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verPlan(Long id) {
		return MapeadorRespuesta.plan(obtenerPlan(id));
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_PLANES, allEntries = true)
	public Map<String, Object> actualizarPlan(Long id, PlanRequest request) {
		Plan plan = obtenerPlan(id);
		aplicarPlan(plan, request);
		return MapeadorRespuesta.plan(plan);
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_PLANES, allEntries = true)
	public void inactivarPlan(Long id) {
		obtenerPlan(id).setEstado(EstadoPlan.INACTIVO);
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_ENDPOINTS, allEntries = true)
	public Map<String, Object> crearEndpoint(EndpointBusquedaRequest request) {
		EndpointBusqueda endpoint = new EndpointBusqueda();
		aplicarEndpoint(endpoint, request);
		return MapeadorRespuesta.endpoint(endpointRepositorio.save(endpoint));
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheConfiguracion.CACHE_ENDPOINTS,
			key = "(#activo == null ? 'N' : #activo.toString()) + '-' + (#critico == null ? 'N' : #critico.toString())")
	public List<Map<String, Object>> listarEndpoints(Boolean activo, Boolean critico) {
		List<EndpointBusqueda> endpoints;
		if (activo != null) {
			endpoints = endpointRepositorio.findByActivo(activo);
		} else if (critico != null) {
			endpoints = endpointRepositorio.findByEsCritico(critico);
		} else {
			endpoints = endpointRepositorio.findAll();
		}
		return endpoints.stream().map(MapeadorRespuesta::endpoint).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verEndpoint(Long id) {
		return MapeadorRespuesta.endpoint(obtenerEndpoint(id));
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_ENDPOINTS, allEntries = true)
	public Map<String, Object> actualizarEndpoint(Long id, EndpointBusquedaRequest request) {
		EndpointBusqueda endpoint = obtenerEndpoint(id);
		aplicarEndpoint(endpoint, request);
		return MapeadorRespuesta.endpoint(endpoint);
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_ENDPOINTS, allEntries = true)
	public void inactivarEndpoint(Long id) {
		obtenerEndpoint(id).setActivo(false);
	}

	@Transactional
	public Map<String, Object> agregarRegla(Long idPlan, PlanEndpointRequest request) {
		Plan plan = obtenerPlan(idPlan);
		EndpointBusqueda endpoint = obtenerEndpoint(request.idEndpoint());
		planEndpointRepositorio.findByPlan_IdPlanAndEndpoint_IdEndpoint(idPlan, request.idEndpoint())
				.ifPresent(regla -> {
					throw new NawinException(CodigoError.GEN_001, "El endpoint ya estÃ¡ configurado en el plan.");
				});
		PlanEndpoint regla = new PlanEndpoint();
		regla.setPlan(plan);
		regla.setEndpoint(endpoint);
		aplicarRegla(regla, request, endpoint);
		return planEndpointRespuesta(regla);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listarReglas(Long idPlan) {
		return planEndpointRepositorio.findByPlan_IdPlan(idPlan).stream()
				.map(this::planEndpointRespuesta)
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verRegla(Long idPlan, Long idRegla) {
		return planEndpointRespuesta(obtenerRegla(idPlan, idRegla));
	}

	@Transactional
	public Map<String, Object> actualizarRegla(Long idPlan, Long idRegla, PlanEndpointRequest request) {
		PlanEndpoint regla = obtenerRegla(idPlan, idRegla);
		EndpointBusqueda endpoint = obtenerEndpoint(request.idEndpoint());
		regla.setEndpoint(endpoint);
		aplicarRegla(regla, request, endpoint);
		return planEndpointRespuesta(regla);
	}

	@Transactional
	public void quitarRegla(Long idPlan, Long idRegla) {
		PlanEndpoint regla = obtenerRegla(idPlan, idRegla);
		if (regla.getPlan().getEstado() == EstadoPlan.BORRADOR) {
			planEndpointRepositorio.delete(regla);
		} else {
			regla.setHabilitado(false);
		}
	}

	public Plan obtenerPlan(Long id) {
		return planRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	public EndpointBusqueda obtenerEndpoint(Long id) {
		return endpointRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private PlanEndpoint obtenerRegla(Long idPlan, Long idRegla) {
		return planEndpointRepositorio.findByPlan_IdPlanAndIdPlanEndpoint(idPlan, idRegla)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private void aplicarPlan(Plan plan, PlanRequest request) {
		if (request.precioSoles().signum() < 0 || request.diasVigencia() <= 0) {
			throw new NawinException(CodigoError.GEN_001);
		}
		plan.setCodigo(request.codigo());
		plan.setNombre(request.nombre());
		plan.setDescripcion(request.descripcion());
		plan.setPrecioSoles(request.precioSoles());
		plan.setDiasVigencia(request.diasVigencia());
		plan.setEstado(request.estado());
	}

	private void aplicarEndpoint(EndpointBusqueda endpoint, EndpointBusquedaRequest request) {
		endpoint.setCodigo(request.codigo());
		endpoint.setNombre(request.nombre());
		endpoint.setDescripcion(request.descripcion());
		endpoint.setMetodoProveedor(request.metodoProveedor().toUpperCase());
		endpoint.setRutaProveedor(request.rutaProveedor());
		endpoint.setParametroPrincipal(request.parametroPrincipal());
		endpoint.setTipoConsumoProveedor(request.tipoConsumoProveedor());
		endpoint.setCostoProveedor(request.costoProveedor());
		endpoint.setEsCritico(request.esCritico());
		endpoint.setActivo(request.activo());
	}

	private void aplicarRegla(PlanEndpoint regla, PlanEndpointRequest request, EndpointBusqueda endpoint) {
		validarRegla(endpoint.isEsCritico(), request.modalidadAcceso(), request.costoCreditosCliente(),
				request.requiereMfa(), request.requiereFinalidad(), request.requiereJustificacion());
		regla.setHabilitado(request.habilitado());
		regla.setModalidadAcceso(request.modalidadAcceso());
		regla.setLimiteDiario(request.limiteDiario());
		regla.setLimiteCiclo(request.limiteCiclo());
		regla.setCostoCreditosCliente(request.costoCreditosCliente());
		regla.setRequiereMfa(request.requiereMfa());
		regla.setRequiereFinalidad(request.requiereFinalidad());
		regla.setRequiereJustificacion(request.requiereJustificacion());
		regla.setPermiteExportar(request.permiteExportar());
		regla.setDiasRetencion(request.diasRetencion());
		planEndpointRepositorio.save(regla);
	}

	public void validarRegla(boolean critico, ModalidadAccesoEndpoint modalidad, Integer costoCreditos,
			boolean requiereMfa, boolean requiereFinalidad, boolean requiereJustificacion) {
		if (modalidad == ModalidadAccesoEndpoint.DESCUENTO_CREDITOS && (costoCreditos == null || costoCreditos <= 0)) {
			throw new NawinException(CodigoError.GEN_001, "La modalidad DESCUENTO_CREDITOS requiere costo mayor a cero.");
		}
	}

	private Map<String, Object> planEndpointRespuesta(PlanEndpoint regla) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idPlanEndpoint", regla.getIdPlanEndpoint());
		m.put("idPlan", regla.getPlan().getIdPlan());
		m.put("endpoint", MapeadorRespuesta.endpoint(regla.getEndpoint()));
		m.put("habilitado", regla.isHabilitado());
		m.put("modalidadAcceso", regla.getModalidadAcceso());
		m.put("limiteDiario", regla.getLimiteDiario());
		m.put("limiteCiclo", regla.getLimiteCiclo());
		m.put("costoCreditosCliente", regla.getCostoCreditosCliente());
		m.put("requiereMfa", regla.isRequiereMfa());
		m.put("requiereFinalidad", regla.isRequiereFinalidad());
		m.put("requiereJustificacion", regla.isRequiereJustificacion());
		m.put("permiteExportar", regla.isPermiteExportar());
		m.put("diasRetencion", regla.getDiasRetencion());
		return m;
	}
}
