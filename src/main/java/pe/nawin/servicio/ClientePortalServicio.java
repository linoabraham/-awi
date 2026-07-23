package pe.nawin.servicio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.PerfilClienteRequest;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Membresia;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.ComprobanteRepositorio;
import pe.nawin.repositorio.MembresiaEndpointRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.MovimientoCreditoRepositorio;
import pe.nawin.repositorio.NotificacionRepositorio;
import pe.nawin.repositorio.SaldoCreditoRepositorio;

@Service
public class ClientePortalServicio {

	private final ClienteServicio clienteServicio;
	private final pe.nawin.repositorio.ClienteRepositorio clienteRepositorio;
	private final pe.nawin.repositorio.UsuarioRepositorio usuarioRepositorio;
	private final MembresiaRepositorio membresiaRepositorio;
	private final MembresiaEndpointRepositorio membresiaEndpointRepositorio;
	private final SaldoCreditoRepositorio saldoCreditoRepositorio;
	private final MovimientoCreditoRepositorio movimientoCreditoRepositorio;
	private final ComprobanteRepositorio comprobanteRepositorio;
	private final NotificacionRepositorio notificacionRepositorio;

	public ClientePortalServicio(ClienteServicio clienteServicio,
			pe.nawin.repositorio.ClienteRepositorio clienteRepositorio,
			pe.nawin.repositorio.UsuarioRepositorio usuarioRepositorio, MembresiaRepositorio membresiaRepositorio,
			MembresiaEndpointRepositorio membresiaEndpointRepositorio, SaldoCreditoRepositorio saldoCreditoRepositorio,
			MovimientoCreditoRepositorio movimientoCreditoRepositorio, ComprobanteRepositorio comprobanteRepositorio,
			NotificacionRepositorio notificacionRepositorio) {
		this.clienteServicio = clienteServicio;
		this.clienteRepositorio = clienteRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.membresiaRepositorio = membresiaRepositorio;
		this.membresiaEndpointRepositorio = membresiaEndpointRepositorio;
		this.saldoCreditoRepositorio = saldoCreditoRepositorio;
		this.movimientoCreditoRepositorio = movimientoCreditoRepositorio;
		this.comprobanteRepositorio = comprobanteRepositorio;
		this.notificacionRepositorio = notificacionRepositorio;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> inicio() {
		// Freemium: la app se usa con créditos, sin membresía obligatoria. El
		// inicio no debe fallar cuando el cliente aún no tiene una membresía
		// activa; en ese caso devolvemos membresía vacía, 0 días y sin endpoints
		// de plan (el acceso a las consultas se rige por el saldo de créditos).
		Cliente cliente = clienteServicio.clienteActual();
		Optional<Membresia> membresia = membresiaActivaOpt(cliente);
		long diasRestantes = membresia
				.map(m -> Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), m.getFechaFin()) + 1))
				.orElse(0L);
		return Map.of(
				"cliente", MapeadorRespuesta.cliente(cliente),
				"membresia", membresia.map(MapeadorRespuesta::membresia).orElse(Map.of()),
				"diasRestantes", diasRestantes,
				"saldoCreditos", saldoCreditoRepositorio.findByCliente_IdCliente(cliente.getIdCliente()).map(MapeadorRespuesta::saldo).orElse(Map.of()),
				"endpoints", membresia.map(this::endpointsDeMembresia).orElse(List.of()));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> perfil() {
		return MapeadorRespuesta.cliente(clienteServicio.clienteActual());
	}

	@Transactional
	public Map<String, Object> actualizarPerfil(PerfilClienteRequest request) {
		Cliente cliente = clienteServicio.clienteActual();
		cliente.setDireccion(request.direccion());
		// Cambio de correo: valida que el buzón real (sin alias) no pertenezca a otra cuenta.
		String canonico = pe.nawin.utilidad.CorreoNormalizador.normalizar(request.correo());
		var usuario = cliente.getUsuario();
		if (!canonico.equals(usuario.getCorreoNormalizado())
				&& usuarioRepositorio.existsByCorreoNormalizado(canonico)) {
			throw new NawinException(CodigoError.GEN_001, "Ese correo ya está registrado en otra cuenta.");
		}
		usuario.setCorreo(request.correo().trim().toLowerCase());
		usuario.setCorreoNormalizado(canonico);
		usuario.setCelular(request.celular());
		completarDocumento(cliente, request);
		return MapeadorRespuesta.cliente(cliente);
	}

	/**
	 * Completa el documento del cliente (freemium se registra sin él). Solo se
	 * permite UNA vez: si ya hay documento registrado no se puede cambiar desde la
	 * app (se usa en comprobantes); en ese caso debe gestionarse con soporte.
	 */
	private void completarDocumento(Cliente cliente, PerfilClienteRequest request) {
		String numero = request.numeroDocumento() == null ? null : request.numeroDocumento().trim();
		if (numero == null || numero.isEmpty()) {
			return; // No se está enviando documento: no hay nada que completar.
		}
		if (cliente.getNumeroDocumento() != null) {
			if (numero.equals(cliente.getNumeroDocumento())) {
				return; // Mismo documento: sin cambios.
			}
			throw new NawinException(CodigoError.GEN_001,
					"Tu documento ya está registrado y no puede cambiarse desde la app. Escríbenos a soporte.");
		}
		pe.nawin.enumeracion.TipoDocumento tipo = request.tipoDocumento() == null
				? pe.nawin.enumeracion.TipoDocumento.DNI
				: request.tipoDocumento();
		if (tipo == pe.nawin.enumeracion.TipoDocumento.DNI && !numero.matches("\\d{8}")) {
			throw new NawinException(CodigoError.GEN_001, "El DNI debe tener 8 dígitos.");
		}
		if (tipo == pe.nawin.enumeracion.TipoDocumento.RUC && !numero.matches("\\d{11}")) {
			throw new NawinException(CodigoError.GEN_001, "El RUC debe tener 11 dígitos.");
		}
		if (clienteRepositorio.existsByNumeroDocumento(numero)) {
			throw new NawinException(CodigoError.GEN_001, "Ese documento ya está registrado en otra cuenta.");
		}
		cliente.setTipoDocumento(tipo);
		cliente.setNumeroDocumento(numero);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> membresiaActual() {
		return MapeadorRespuesta.membresia(membresiaActualEntidad(clienteServicio.clienteActual()));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> membresias() {
		Cliente cliente = clienteServicio.clienteActual();
		return membresiaRepositorio.findByCliente_IdCliente(cliente.getIdCliente()).stream().map(MapeadorRespuesta::membresia).toList();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> endpointsDisponibles() {
		// Sin membresía activa no hay endpoints de plan; el listado queda vacío en
		// lugar de fallar (freemium).
		return membresiaActivaOpt(clienteServicio.clienteActual())
				.map(this::endpointsDeMembresia)
				.orElse(List.of());
	}

	@Transactional(readOnly = true)
	public Map<String, Object> saldo() {
		Cliente cliente = clienteServicio.clienteActual();
		return saldoCreditoRepositorio.findByCliente_IdCliente(cliente.getIdCliente()).map(MapeadorRespuesta::saldo)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> movimientos() {
		Cliente cliente = clienteServicio.clienteActual();
		return movimientoCreditoRepositorio.findByCliente_IdClienteOrderByFechaCreacionDesc(cliente.getIdCliente()).stream()
				.map(m -> Map.<String, Object>of("tipoMovimiento", m.getTipoMovimiento(), "cantidad", m.getCantidad(),
						"saldoAnterior", m.getSaldoAnterior(), "saldoPosterior", m.getSaldoPosterior(),
						"descripcion", m.getDescripcion(), "fechaCreacion", m.getFechaCreacion()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> comprobantes() {
		Cliente cliente = clienteServicio.clienteActual();
		return comprobanteRepositorio.findByPago_Cliente_IdCliente(cliente.getIdCliente()).stream()
				.map(c -> Map.<String, Object>of("idComprobante", c.getIdComprobante(), "tipoComprobante", c.getTipoComprobante(),
						"serie", c.getSerie(), "numero", c.getNumero(), "total", c.getTotal(), "estado", c.getEstado()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> notificaciones() {
		Cliente cliente = clienteServicio.clienteActual();
		return notificacionRepositorio.findByCliente_IdClienteOrderByFechaCreacionDesc(cliente.getIdCliente()).stream()
				.map(n -> Map.<String, Object>of("idNotificacion", n.getIdNotificacion(), "canal", n.getCanal(), "tipo", n.getTipo(),
						"titulo", n.getTitulo(), "mensaje", n.getMensaje(), "estado", n.getEstado(), "fechaCreacion", n.getFechaCreacion()))
				.toList();
	}

	private Membresia membresiaActualEntidad(Cliente cliente) {
		return membresiaActivaOpt(cliente).orElseThrow(() -> new NawinException(CodigoError.MEM_001));
	}

	private Optional<Membresia> membresiaActivaOpt(Cliente cliente) {
		LocalDate hoy = LocalDate.now();
		return membresiaRepositorio
				.findFirstByCliente_IdClienteAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaFinDesc(
						cliente.getIdCliente(), EstadoMembresia.ACTIVA, hoy, hoy);
	}

	private List<Map<String, Object>> endpointsDeMembresia(Membresia membresia) {
		return membresiaEndpointRepositorio.findByMembresia_IdMembresia(membresia.getIdMembresia()).stream()
				.filter(acceso -> acceso.isHabilitado() && acceso.getEndpoint().isActivo())
				.map(MapeadorRespuesta::membresiaEndpoint)
				.toList();
	}
}
