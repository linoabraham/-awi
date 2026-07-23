package pe.nawin.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import pe.nawin.entidad.*;

public final class MapeadorRespuesta {

	private MapeadorRespuesta() {
	}

	public static Map<String, Object> usuario(Usuario usuario) {
		Map<String, Object> m = base();
		m.put("idUsuario", usuario.getIdUsuario());
		m.put("rol", usuario.getRol().getCodigo());
		m.put("nombres", usuario.getNombres());
		m.put("apellidos", usuario.getApellidos());
		m.put("correo", usuario.getCorreo());
		m.put("celular", usuario.getCelular());
		m.put("nombreUsuario", usuario.getNombreUsuario());
		m.put("estado", usuario.getEstado());
		m.put("mfaHabilitado", usuario.isMfaHabilitado());
		return m;
	}

	public static Map<String, Object> cliente(Cliente cliente) {
		Map<String, Object> m = base();
		m.put("idCliente", cliente.getIdCliente());
		m.put("idUsuario", cliente.getUsuario().getIdUsuario());
		m.put("tipoDocumento", cliente.getTipoDocumento());
		m.put("numeroDocumento", cliente.getNumeroDocumento());
		m.put("nombres", cliente.getNombres());
		m.put("apellidos", cliente.getApellidos());
		m.put("razonSocial", cliente.getRazonSocial());
		m.put("direccion", cliente.getDireccion());
		m.put("codigoReferido", cliente.getCodigoReferido());
		m.put("estado", cliente.getEstado());
		return m;
	}

	public static Map<String, Object> plan(Plan plan) {
		Map<String, Object> m = base();
		m.put("idPlan", plan.getIdPlan());
		m.put("codigo", plan.getCodigo());
		m.put("nombre", plan.getNombre());
		m.put("descripcion", plan.getDescripcion());
		m.put("precioSoles", plan.getPrecioSoles());
		m.put("diasVigencia", plan.getDiasVigencia());
		m.put("estado", plan.getEstado());
		return m;
	}

	public static Map<String, Object> endpoint(EndpointBusqueda endpoint) {
		Map<String, Object> m = base();
		m.put("idEndpoint", endpoint.getIdEndpoint());
		m.put("codigo", endpoint.getCodigo());
		m.put("nombre", endpoint.getNombre());
		m.put("descripcion", endpoint.getDescripcion());
		m.put("metodoProveedor", endpoint.getMetodoProveedor());
		m.put("rutaProveedor", endpoint.getRutaProveedor());
		m.put("parametroPrincipal", endpoint.getParametroPrincipal());
		m.put("tipoConsumoProveedor", endpoint.getTipoConsumoProveedor());
		m.put("costoProveedor", endpoint.getCostoProveedor());
		m.put("esCritico", endpoint.isEsCritico());
		m.put("activo", endpoint.isActivo());
		return m;
	}

	public static Map<String, Object> membresia(Membresia membresia) {
		Map<String, Object> m = base();
		m.put("idMembresia", membresia.getIdMembresia());
		m.put("idCliente", membresia.getCliente().getIdCliente());
		m.put("idPlan", membresia.getPlan().getIdPlan());
		m.put("planCodigo", membresia.getPlan().getCodigo());
		m.put("planNombre", membresia.getPlan().getNombre());
		m.put("fechaInicio", membresia.getFechaInicio());
		m.put("fechaFin", membresia.getFechaFin());
		m.put("diasVigencia", membresia.getDiasVigencia());
		m.put("precioPagado", membresia.getPrecioPagado());
		m.put("estado", membresia.getEstado());
		m.put("observacion", membresia.getObservacion());
		return m;
	}

	public static Map<String, Object> membresiaEndpoint(MembresiaEndpoint acceso) {
		Map<String, Object> m = base();
		m.put("idMembresiaEndpoint", acceso.getIdMembresiaEndpoint());
		m.put("idMembresia", acceso.getMembresia().getIdMembresia());
		m.put("endpoint", endpoint(acceso.getEndpoint()));
		m.put("habilitado", acceso.isHabilitado());
		m.put("modalidadAcceso", acceso.getModalidadAcceso());
		m.put("limiteDiario", acceso.getLimiteDiario());
		m.put("limiteTotal", acceso.getLimiteTotal());
		m.put("consumidoTotal", acceso.getConsumidoTotal());
		m.put("costoCreditosCliente", acceso.getCostoCreditosCliente());
		m.put("requiereMfa", acceso.isRequiereMfa());
		m.put("requiereFinalidad", acceso.isRequiereFinalidad());
		m.put("requiereJustificacion", acceso.isRequiereJustificacion());
		m.put("permiteExportar", acceso.isPermiteExportar());
		m.put("diasRetencion", acceso.getDiasRetencion());
		return m;
	}

	public static Map<String, Object> paquete(PaqueteCredito paquete) {
		Map<String, Object> m = base();
		m.put("idPaqueteCredito", paquete.getIdPaqueteCredito());
		m.put("codigo", paquete.getCodigo());
		m.put("nombre", paquete.getNombre());
		m.put("cantidadCreditos", paquete.getCantidadCreditos());
		m.put("precioSoles", paquete.getPrecioSoles());
		m.put("diasVigencia", paquete.getDiasVigencia());
		m.put("activo", paquete.isActivo());
		return m;
	}

	public static Map<String, Object> saldo(SaldoCredito saldo) {
		Map<String, Object> m = base();
		m.put("idCliente", saldo.getCliente().getIdCliente());
		m.put("creditosDisponibles", saldo.getCreditosDisponibles());
		m.put("creditosReservados", saldo.getCreditosReservados());
		m.put("creditosConsumidos", saldo.getCreditosConsumidos());
		m.put("fechaActualizacion", saldo.getFechaActualizacion());
		return m;
	}

	private static Map<String, Object> base() {
		return new LinkedHashMap<>();
	}
}
