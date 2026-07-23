package pe.nawin.servicio;

import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.configuracion.CacheConfiguracion;
import pe.nawin.dto.solicitud.AjusteCreditoRequest;
import pe.nawin.dto.solicitud.MotivoRequest;
import pe.nawin.dto.solicitud.PaqueteCreditoRequest;
import pe.nawin.dto.solicitud.VentaCreditoRequest;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Consulta;
import pe.nawin.entidad.MovimientoCredito;
import pe.nawin.entidad.PaqueteCredito;
import pe.nawin.entidad.SaldoCredito;
import pe.nawin.entidad.Usuario;
import pe.nawin.entidad.VentaCredito;
import pe.nawin.enumeracion.EstadoVentaCredito;
import pe.nawin.enumeracion.TipoMovimientoCredito;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.MovimientoCreditoRepositorio;
import pe.nawin.repositorio.PaqueteCreditoRepositorio;
import pe.nawin.repositorio.SaldoCreditoRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.repositorio.VentaCreditoRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;

@Service
public class CreditoServicio {

	private final PaqueteCreditoRepositorio paqueteRepositorio;
	private final VentaCreditoRepositorio ventaRepositorio;
	private final SaldoCreditoRepositorio saldoRepositorio;
	private final MovimientoCreditoRepositorio movimientoRepositorio;
	private final ClienteRepositorio clienteRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;

	public CreditoServicio(PaqueteCreditoRepositorio paqueteRepositorio, VentaCreditoRepositorio ventaRepositorio,
			SaldoCreditoRepositorio saldoRepositorio, MovimientoCreditoRepositorio movimientoRepositorio,
			ClienteRepositorio clienteRepositorio, UsuarioRepositorio usuarioRepositorio) {
		this.paqueteRepositorio = paqueteRepositorio;
		this.ventaRepositorio = ventaRepositorio;
		this.saldoRepositorio = saldoRepositorio;
		this.movimientoRepositorio = movimientoRepositorio;
		this.clienteRepositorio = clienteRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_PAQUETES, allEntries = true)
	public Map<String, Object> crearPaquete(PaqueteCreditoRequest request) {
		if (paqueteRepositorio.existsByCodigo(request.codigo())) {
			throw new NawinException(CodigoError.GEN_001, "Código de paquete ya existe.");
		}
		PaqueteCredito paquete = new PaqueteCredito();
		aplicarPaquete(paquete, request);
		return MapeadorRespuesta.paquete(paqueteRepositorio.save(paquete));
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheConfiguracion.CACHE_PAQUETES, key = "#activo == null ? 'TODOS' : #activo.toString()")
	public List<Map<String, Object>> listarPaquetes(Boolean activo) {
		List<PaqueteCredito> paquetes = activo == null ? paqueteRepositorio.findAll() : paqueteRepositorio.findByActivo(activo);
		return paquetes.stream().map(MapeadorRespuesta::paquete).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verPaquete(Long id) {
		return MapeadorRespuesta.paquete(obtenerPaquete(id));
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_PAQUETES, allEntries = true)
	public Map<String, Object> actualizarPaquete(Long id, PaqueteCreditoRequest request) {
		PaqueteCredito paquete = obtenerPaquete(id);
		aplicarPaquete(paquete, request);
		return MapeadorRespuesta.paquete(paquete);
	}

	@Transactional
	@CacheEvict(value = CacheConfiguracion.CACHE_PAQUETES, allEntries = true)
	public void inactivarPaquete(Long id) {
		obtenerPaquete(id).setActivo(false);
	}

	@Transactional
	public Map<String, Object> crearVenta(VentaCreditoRequest request) {
		Cliente cliente = clienteRepositorio.findById(request.idCliente()).orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		PaqueteCredito paquete = obtenerPaquete(request.idPaqueteCredito());
		if (!paquete.isActivo()) {
			throw new NawinException(CodigoError.GEN_001, "Paquete inactivo.");
		}
		VentaCredito venta = new VentaCredito();
		venta.setCliente(cliente);
		venta.setPaqueteCredito(paquete);
		venta.setCantidadPaquetes(request.cantidadPaquetes());
		venta.setCreditosOtorgados(paquete.getCantidadCreditos() * request.cantidadPaquetes());
		venta.setPrecioUnitario(paquete.getPrecioSoles());
		venta.setTotalSoles(paquete.getPrecioSoles().multiply(java.math.BigDecimal.valueOf(request.cantidadPaquetes())));
		venta.setFechaVencimientoCreditos(java.time.LocalDate.now().plusDays(paquete.getDiasVigencia()));
		venta.setEstado(EstadoVentaCredito.PENDIENTE);
		venta.setCreadoPor(usuarioActual());
		return ventaRespuesta(ventaRepositorio.save(venta));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listarVentas(Long idCliente, EstadoVentaCredito estado) {
		List<VentaCredito> ventas = idCliente != null
				? ventaRepositorio.findByCliente_IdCliente(idCliente)
				: estado != null ? ventaRepositorio.findByEstado(estado) : ventaRepositorio.findAll();
		return ventas.stream().map(this::ventaRespuesta).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verVenta(Long id) {
		return ventaRespuesta(obtenerVenta(id));
	}

	@Transactional
	public Map<String, Object> actualizarVenta(Long id, VentaCreditoRequest request) {
		VentaCredito venta = obtenerVenta(id);
		if (venta.getEstado() != EstadoVentaCredito.PENDIENTE) {
			throw new NawinException(CodigoError.GEN_001, "Solo se puede actualizar una venta pendiente.");
		}
		PaqueteCredito paquete = obtenerPaquete(request.idPaqueteCredito());
		venta.setPaqueteCredito(paquete);
		venta.setCantidadPaquetes(request.cantidadPaquetes());
		venta.setCreditosOtorgados(paquete.getCantidadCreditos() * request.cantidadPaquetes());
		venta.setPrecioUnitario(paquete.getPrecioSoles());
		venta.setTotalSoles(paquete.getPrecioSoles().multiply(java.math.BigDecimal.valueOf(request.cantidadPaquetes())));
		return ventaRespuesta(venta);
	}

	@Transactional
	public void anularVenta(Long id, MotivoRequest request) {
		VentaCredito venta = obtenerVenta(id);
		if (venta.getEstado() == EstadoVentaCredito.PAGADA) {
			throw new NawinException(CodigoError.GEN_001, "No se anula una venta pagada sin devolución controlada.");
		}
		venta.setEstado(EstadoVentaCredito.ANULADA);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> saldo(Long idCliente) {
		return MapeadorRespuesta.saldo(obtenerSaldo(idCliente));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> movimientos(Long idCliente) {
		return movimientoRepositorio.findByCliente_IdClienteOrderByFechaCreacionDesc(idCliente).stream()
				.map(this::movimientoRespuesta)
				.toList();
	}

	@Transactional
	public Map<String, Object> ajustar(Long idCliente, AjusteCreditoRequest request) {
		if (request.tipo() != TipoMovimientoCredito.AJUSTE && request.tipo() != TipoMovimientoCredito.DEVOLUCION) {
			throw new NawinException(CodigoError.GEN_001, "Solo se permiten AJUSTE o DEVOLUCION por esta ruta.");
		}
		SaldoCredito saldo = obtenerSaldoBloqueado(idCliente);
		int anterior = saldo.getCreditosDisponibles();
		if (request.tipo() == TipoMovimientoCredito.DEVOLUCION) {
			saldo.setCreditosDisponibles(anterior + request.cantidad());
		} else {
			saldo.setCreditosDisponibles(Math.max(0, anterior + request.cantidad()));
		}
		registrarMovimiento(saldo.getCliente(), null, null, request.tipo(), request.cantidad(), anterior,
				saldo.getCreditosDisponibles(), request.motivo(), usuarioActual());
		return MapeadorRespuesta.saldo(saldo);
	}

	@Transactional
	public void cargarPorVentaPagada(VentaCredito venta, Usuario registradoPor) {
		if (venta.getEstado() == EstadoVentaCredito.PAGADA) {
			return;
		}
		SaldoCredito saldo = obtenerSaldoBloqueado(venta.getCliente().getIdCliente());
		int anterior = saldo.getCreditosDisponibles();
		saldo.setCreditosDisponibles(anterior + venta.getCreditosOtorgados());
		venta.setEstado(EstadoVentaCredito.PAGADA);
		registrarMovimiento(venta.getCliente(), venta, null, TipoMovimientoCredito.CARGA, venta.getCreditosOtorgados(),
				anterior, saldo.getCreditosDisponibles(), "Carga por venta de créditos", registradoPor);
	}

	@Transactional
	public void revertirPorVentaAnulada(VentaCredito venta, Usuario registradoPor, String motivo) {
		if (venta == null || venta.getEstado() != EstadoVentaCredito.PAGADA) {
			return;
		}
		SaldoCredito saldo = obtenerSaldoBloqueado(venta.getCliente().getIdCliente());
		int anterior = saldo.getCreditosDisponibles();
		if (anterior < venta.getCreditosOtorgados()) {
			throw new NawinException(CodigoError.GEN_001,
					"No se puede anular: los créditos de la venta ya fueron consumidos.");
		}
		saldo.setCreditosDisponibles(anterior - venta.getCreditosOtorgados());
		venta.setEstado(EstadoVentaCredito.ANULADA);
		registrarMovimiento(venta.getCliente(), venta, null, TipoMovimientoCredito.AJUSTE, venta.getCreditosOtorgados(),
				anterior, saldo.getCreditosDisponibles(),
				"Reverso de créditos por anulación de pago" + (motivo == null || motivo.isBlank() ? "" : ": " + motivo),
				registradoPor);
	}

	/**
	 * Otorga créditos como bono (p. ej. programa de referidos) al cliente y
	 * registra el movimiento. Devuelve el saldo actualizado.
	 */
	@Transactional
	public SaldoCredito otorgarBono(Cliente cliente, int cantidad, String descripcion) {
		SaldoCredito saldo = obtenerSaldoBloqueado(cliente.getIdCliente());
		int anterior = saldo.getCreditosDisponibles();
		saldo.setCreditosDisponibles(anterior + cantidad);
		registrarMovimiento(cliente, null, null, TipoMovimientoCredito.CARGA, cantidad, anterior,
				saldo.getCreditosDisponibles(), descripcion, cliente.getUsuario());
		return saldo;
	}

	@Transactional
	public void reservarCreditos(Cliente cliente, Consulta consulta, int cantidad) {
		SaldoCredito saldo = obtenerSaldoBloqueado(cliente.getIdCliente());
		if (saldo.getCreditosDisponibles() < cantidad) {
			throw new NawinException(CodigoError.CRE_001);
		}
		int anterior = saldo.getCreditosDisponibles();
		saldo.setCreditosDisponibles(anterior - cantidad);
		saldo.setCreditosReservados(saldo.getCreditosReservados() + cantidad);
		registrarMovimiento(cliente, null, consulta, TipoMovimientoCredito.RESERVA, cantidad, anterior,
				saldo.getCreditosDisponibles(), "Reserva de créditos por consulta", cliente.getUsuario());
	}

	@Transactional
	public void confirmarCreditos(Cliente cliente, Consulta consulta, int cantidad) {
		SaldoCredito saldo = obtenerSaldoBloqueado(cliente.getIdCliente());
		saldo.setCreditosReservados(Math.max(0, saldo.getCreditosReservados() - cantidad));
		saldo.setCreditosConsumidos(saldo.getCreditosConsumidos() + cantidad);
		registrarMovimiento(cliente, null, consulta, TipoMovimientoCredito.CONSUMO, cantidad, saldo.getCreditosDisponibles(),
				saldo.getCreditosDisponibles(), "Consumo confirmado por consulta", cliente.getUsuario());
	}

	@Transactional
	public void liberarCreditos(Cliente cliente, Consulta consulta, int cantidad) {
		SaldoCredito saldo = obtenerSaldoBloqueado(cliente.getIdCliente());
		int anterior = saldo.getCreditosDisponibles();
		saldo.setCreditosReservados(Math.max(0, saldo.getCreditosReservados() - cantidad));
		saldo.setCreditosDisponibles(anterior + cantidad);
		registrarMovimiento(cliente, null, consulta, TipoMovimientoCredito.LIBERACION, cantidad, anterior,
				saldo.getCreditosDisponibles(), "Liberación por falla técnica del proveedor", cliente.getUsuario());
	}

	private void aplicarPaquete(PaqueteCredito paquete, PaqueteCreditoRequest request) {
		paquete.setCodigo(request.codigo());
		paquete.setNombre(request.nombre());
		paquete.setCantidadCreditos(request.cantidadCreditos());
		paquete.setPrecioSoles(request.precioSoles());
		paquete.setDiasVigencia(request.diasVigencia());
		paquete.setActivo(request.activo());
	}

	private PaqueteCredito obtenerPaquete(Long id) {
		return paqueteRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private VentaCredito obtenerVenta(Long id) {
		return ventaRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private SaldoCredito obtenerSaldo(Long idCliente) {
		return saldoRepositorio.findByCliente_IdCliente(idCliente).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private SaldoCredito obtenerSaldoBloqueado(Long idCliente) {
		return saldoRepositorio.findWithLockByCliente_IdCliente(idCliente).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}

	private void registrarMovimiento(Cliente cliente, VentaCredito venta, Consulta consulta, TipoMovimientoCredito tipo,
			int cantidad, int anterior, int posterior, String descripcion, Usuario registradoPor) {
		MovimientoCredito movimiento = new MovimientoCredito();
		movimiento.setCliente(cliente);
		movimiento.setVentaCredito(venta);
		movimiento.setConsulta(consulta);
		movimiento.setTipoMovimiento(tipo);
		movimiento.setCantidad(cantidad);
		movimiento.setSaldoAnterior(anterior);
		movimiento.setSaldoPosterior(posterior);
		movimiento.setDescripcion(descripcion);
		movimiento.setRegistradoPor(registradoPor);
		movimientoRepositorio.save(movimiento);
	}

	private Map<String, Object> ventaRespuesta(VentaCredito venta) {
		return Map.of(
				"idVentaCredito", venta.getIdVentaCredito(),
				"idCliente", venta.getCliente().getIdCliente(),
				"idPaqueteCredito", venta.getPaqueteCredito().getIdPaqueteCredito(),
				"cantidadPaquetes", venta.getCantidadPaquetes(),
				"creditosOtorgados", venta.getCreditosOtorgados(),
				"precioUnitario", venta.getPrecioUnitario(),
				"totalSoles", venta.getTotalSoles(),
				"fechaVencimientoCreditos", venta.getFechaVencimientoCreditos(),
				"estado", venta.getEstado());
	}

	private Map<String, Object> movimientoRespuesta(MovimientoCredito movimiento) {
		return Map.of(
				"idMovimientoCredito", movimiento.getIdMovimientoCredito(),
				"tipoMovimiento", movimiento.getTipoMovimiento(),
				"cantidad", movimiento.getCantidad(),
				"saldoAnterior", movimiento.getSaldoAnterior(),
				"saldoPosterior", movimiento.getSaldoPosterior(),
				"descripcion", movimiento.getDescripcion(),
				"fechaCreacion", movimiento.getFechaCreacion());
	}
}
