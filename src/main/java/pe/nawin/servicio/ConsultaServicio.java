package pe.nawin.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.dto.respuesta.ConsultaRespuesta;
import pe.nawin.dto.solicitud.SolicitudConsulta;
import pe.nawin.dto.solicitud.SolicitudConsultaCritica;
import pe.nawin.entidad.ArchivoConsulta;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Consulta;
import pe.nawin.entidad.EndpointBusqueda;
import pe.nawin.entidad.Membresia;
import pe.nawin.entidad.MembresiaEndpoint;
import pe.nawin.entidad.ResultadoConsulta;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.CodigoEndpoint;
import pe.nawin.enumeracion.EstadoCliente;
import pe.nawin.enumeracion.EstadoConsulta;
import pe.nawin.enumeracion.EstadoMembresia;
import pe.nawin.enumeracion.ModalidadAccesoEndpoint;
import pe.nawin.enumeracion.OrigenConsumoConsulta;
import pe.nawin.enumeracion.TipoArchivoConsulta;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.integracion.codart.ClienteCodart;
import pe.nawin.integracion.codart.RespuestaProveedor;
import pe.nawin.repositorio.ArchivoConsultaRepositorio;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.ConsultaRepositorio;
import pe.nawin.repositorio.MembresiaEndpointRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.ResultadoConsultaRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.utilidad.CifradoServicio;
import pe.nawin.utilidad.ContextoSeguridad;
import pe.nawin.utilidad.TextoSeguro;

@Service
public class ConsultaServicio {

	/** Estados de una consulta que "cuentan" para cuota/consumo (reservada o resuelta con datos). */
	public static final List<EstadoConsulta> ESTADOS_CONSUMEN_CUOTA =
			List.of(EstadoConsulta.PROCESANDO, EstadoConsulta.EXITOSA, EstadoConsulta.SIN_RESULTADOS);

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConsultaServicio.class);

	private final ClienteRepositorio clienteRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final MembresiaRepositorio membresiaRepositorio;
	private final MembresiaEndpointRepositorio membresiaEndpointRepositorio;
	private final pe.nawin.repositorio.EndpointBusquedaRepositorio endpointRepositorio;
	private final ConsultaRepositorio consultaRepositorio;
	private final ResultadoConsultaRepositorio resultadoRepositorio;
	private final ArchivoConsultaRepositorio archivoRepositorio;
	private final CreditoServicio creditoServicio;
	private final ReferidoServicio referidoServicio;
	private final ClienteCodart clienteCodart;
	private final CifradoServicio cifradoServicio;
	private final ObjectMapper objectMapper;
	private final NawinPropiedades propiedades;

	public ConsultaServicio(ClienteRepositorio clienteRepositorio, UsuarioRepositorio usuarioRepositorio,
			MembresiaRepositorio membresiaRepositorio, MembresiaEndpointRepositorio membresiaEndpointRepositorio,
			pe.nawin.repositorio.EndpointBusquedaRepositorio endpointRepositorio,
			ConsultaRepositorio consultaRepositorio, ResultadoConsultaRepositorio resultadoRepositorio,
			ArchivoConsultaRepositorio archivoRepositorio, CreditoServicio creditoServicio,
			ReferidoServicio referidoServicio, ClienteCodart clienteCodart,
			CifradoServicio cifradoServicio, ObjectMapper objectMapper, NawinPropiedades propiedades) {
		this.clienteRepositorio = clienteRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.membresiaRepositorio = membresiaRepositorio;
		this.membresiaEndpointRepositorio = membresiaEndpointRepositorio;
		this.endpointRepositorio = endpointRepositorio;
		this.consultaRepositorio = consultaRepositorio;
		this.resultadoRepositorio = resultadoRepositorio;
		this.archivoRepositorio = archivoRepositorio;
		this.creditoServicio = creditoServicio;
		this.referidoServicio = referidoServicio;
		this.clienteCodart = clienteCodart;
		this.cifradoServicio = cifradoServicio;
		this.objectMapper = objectMapper;
		this.propiedades = propiedades;
	}

	/** Acceso resuelto a un endpoint: por membresía (ilimitada o descuento) o SOLO por créditos (free). */
	private record AccesoConsulta(
			EndpointBusqueda endpoint,
			Membresia membresia,
			MembresiaEndpoint membresiaEndpoint,
			boolean incluidoMembresia,
			int costoCreditos,
			int diasRetencion,
			boolean permiteExportar,
			boolean requiereMfa,
			boolean requiereFinalidad,
			boolean requiereJustificacion) {
	}

	@Transactional
	public ConsultaRespuesta ejecutar(CodigoEndpoint codigo, SolicitudConsulta solicitud, String ip, String agenteUsuario, String idempotencyKey) {
		Usuario usuario = usuarioActual();
		if (StringUtils.hasText(idempotencyKey)) {
			var existente = consultaRepositorio.findByUsuario_IdUsuarioAndClaveIdempotencia(usuario.getIdUsuario(), idempotencyKey);
			if (existente.isPresent()) {
				return respuestaDesdeConsulta(existente.get());
			}
		}
		Cliente cliente = validarCliente(usuario);
		AccesoConsulta acceso = resolverAcceso(cliente, codigo);
		validarReglasCriticas(acceso, solicitud, usuario);
		Reserva reserva = reservar(acceso, cliente);
		Consulta consulta = crearConsulta(usuario, cliente, acceso, solicitud, reserva, ip, agenteUsuario, idempotencyKey);
		if (reserva.origen() == OrigenConsumoConsulta.CREDITOS) {
			creditoServicio.reservarCreditos(cliente, consulta, reserva.cantidad());
		}
		try {
			RespuestaProveedor respuestaProveedor = clienteCodart.consultar(acceso.endpoint(), solicitud);
			return guardarRespuestaYConfirmar(consulta, cliente, acceso, reserva, respuestaProveedor);
		} catch (NawinException ex) {
			liberarReserva(acceso, cliente, consulta, reserva);
			marcarFallida(consulta, ex.getMessage());
			throw ex;
		}
	}

	@Transactional
	public ConsultaRespuesta ejecutarFacial(MultipartFile imagen, String finalidad, String justificacion, String codigoMfa,
			String ip, String agenteUsuario, String idempotencyKey) {
		validarImagenFacial(imagen);
		Usuario usuario = usuarioActual();
		if (StringUtils.hasText(idempotencyKey)) {
			var existente = consultaRepositorio.findByUsuario_IdUsuarioAndClaveIdempotencia(usuario.getIdUsuario(), idempotencyKey);
			if (existente.isPresent()) {
				return respuestaDesdeConsulta(existente.get());
			}
		}
		Cliente cliente = validarCliente(usuario);
		AccesoConsulta acceso = resolverAcceso(cliente, CodigoEndpoint.FACIAL_TOP);
		validarCriticoFacial(acceso, usuario, finalidad, justificacion, codigoMfa);
		ObjectNode parametros = objectMapper.createObjectNode();
		parametros.put("image_facial", imagen.getOriginalFilename());
		parametros.put("tipoMime", imagen.getContentType());
		parametros.put("tamanoBytes", imagen.getSize());
		parametros.put("finalidad", finalidad);
		parametros.put("justificacion", justificacion);

		Reserva reserva = reservar(acceso, cliente);
		Consulta consulta = crearConsultaDesdeJson(usuario, cliente, acceso, parametros, TextoSeguro.mascara(imagen.getOriginalFilename()),
				finalidad, justificacion, reserva, ip, agenteUsuario, idempotencyKey);
		if (reserva.origen() == OrigenConsumoConsulta.CREDITOS) {
			creditoServicio.reservarCreditos(cliente, consulta, reserva.cantidad());
		}
		try {
			RespuestaProveedor respuestaProveedor = clienteCodart.consultarFacial(acceso.endpoint(), imagen);
			return guardarRespuestaYConfirmar(consulta, cliente, acceso, reserva, respuestaProveedor);
		} catch (NawinException ex) {
			liberarReserva(acceso, cliente, consulta, reserva);
			marcarFallida(consulta, ex.getMessage());
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> historialCliente() {
		Cliente cliente = clienteRepositorio.findByUsuario_IdUsuario(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		return consultaRepositorio.findByCliente_IdClienteAndVisibleClienteTrueOrderByFechaInicioDesc(cliente.getIdCliente())
				.stream().map(this::consultaResumen).toList();
	}

	@Transactional(readOnly = true)
	public ConsultaRespuesta detalleCliente(String codigoConsulta) {
		Cliente cliente = clienteRepositorio.findByUsuario_IdUsuario(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		Consulta consulta = consultaRepositorio.findByCodigoConsultaAndCliente_IdCliente(codigoConsulta, cliente.getIdCliente())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		return respuestaDesdeConsulta(consulta);
	}

	@Transactional
	public void ocultarCliente(String codigoConsulta) {
		Cliente cliente = clienteRepositorio.findByUsuario_IdUsuario(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		Consulta consulta = consultaRepositorio.findByCodigoConsultaAndCliente_IdCliente(codigoConsulta, cliente.getIdCliente())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		consulta.setVisibleCliente(false);
	}

	@Transactional(readOnly = true)
	public Resource archivoCliente(String codigoConsulta, Long idArchivo) {
		Cliente cliente = clienteRepositorio.findByUsuario_IdUsuario(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		Consulta consulta = consultaRepositorio.findByCodigoConsultaAndCliente_IdCliente(codigoConsulta, cliente.getIdCliente())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		boolean permiteExportar = consulta.getMembresiaEndpoint() != null
				? consulta.getMembresiaEndpoint().isPermiteExportar()
				: propiedades.consultas().permiteExportarCreditos();
		if (!permiteExportar) {
			throw new NawinException(CodigoError.AUTH_003);
		}
		ArchivoConsulta archivo = archivoRepositorio.findByIdArchivoConsultaAndConsulta_Cliente_IdCliente(idArchivo, cliente.getIdCliente())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		if (!archivo.getConsulta().getCodigoConsulta().equals(codigoConsulta) || archivo.isEliminadoPorRetencion()
				|| archivo.getFechaExpiracion().isBefore(LocalDateTime.now())) {
			throw new NawinException(CodigoError.GEN_002);
		}
		return new FileSystemResource(archivo.getRutaPrivada());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> historialGlobal() {
		return consultaRepositorio.findAll().stream().map(this::consultaResumen).toList();
	}

	/**
	 * Ranking de consumo por cliente en los últimos {@code dias} días (para vigilar
	 * abuso, sobre todo de membresías ilimitadas). Marca si el cliente tiene
	 * membresía activa (ilimitado) y cuántas de sus consultas fueron por membresía.
	 */
	@Transactional(readOnly = true)
	public List<Map<String, Object>> consumoPorCliente(int dias) {
		int rango = Math.max(1, dias);
		LocalDateTime hasta = LocalDate.now().atTime(LocalTime.MAX);
		LocalDateTime desde = LocalDate.now().minusDays(rango - 1L).atStartOfDay();
		List<Map<String, Object>> salida = new java.util.ArrayList<>();
		for (Object[] fila : consultaRepositorio.consumoPorCliente(desde, hasta, ESTADOS_CONSUMEN_CUOTA)) {
			Long idCliente = ((Number) fila[0]).longValue();
			long total = ((Number) fila[1]).longValue();
			long porMembresia = ((Number) fila[2]).longValue();
			Object ultima = fila[3];
			Cliente cliente = clienteRepositorio.findById(idCliente).orElse(null);
			boolean ilimitado = cliente != null && obtenerMembresiaActivaOpcional(cliente) != null;
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("idCliente", idCliente);
			item.put("cliente", nombreClienteCorto(cliente));
			item.put("documento", cliente == null ? null : cliente.getNumeroDocumento());
			item.put("consultas", total);
			item.put("consultasMembresia", porMembresia);
			item.put("consultasCreditos", total - porMembresia);
			item.put("membresiaActiva", ilimitado);
			item.put("ultimaConsulta", ultima);
			salida.add(item);
		}
		return salida;
	}

	private String nombreClienteCorto(Cliente cliente) {
		if (cliente == null) {
			return "Cliente";
		}
		String razon = cliente.getRazonSocial();
		if (StringUtils.hasText(razon)) {
			return razon;
		}
		String nombre = ((cliente.getNombres() == null ? "" : cliente.getNombres()) + " "
				+ (cliente.getApellidos() == null ? "" : cliente.getApellidos())).trim();
		return StringUtils.hasText(nombre) ? nombre
				: (cliente.getNumeroDocumento() == null ? "Cliente #" + cliente.getIdCliente() : cliente.getNumeroDocumento());
	}

	@Transactional(readOnly = true)
	public ConsultaRespuesta detalleGlobal(String codigoConsulta) {
		return respuestaDesdeConsulta(consultaRepositorio.findByCodigoConsulta(codigoConsulta)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002)));
	}

	private ConsultaRespuesta guardarRespuestaYConfirmar(Consulta consulta, Cliente cliente, AccesoConsulta acceso,
			Reserva reserva, RespuestaProveedor respuestaProveedor) {
		LocalDateTime fin = LocalDateTime.now();
		List<Map<String, Object>> archivos = guardarArchivos(consulta, respuestaProveedor.cuerpo(), acceso.diasRetencion());
		ResultadoConsulta resultado = new ResultadoConsulta();
		resultado.setConsulta(consulta);
		resultado.setResultadoJsonCifrado(cifradoServicio.cifrar(respuestaProveedor.cuerpo().toString()));
		resultado.setResumenJson(resumenSeguro(respuestaProveedor.cuerpo()));
		resultado.setFechaExpiracion(fin.plusDays(acceso.diasRetencion()));
		resultadoRepositorio.save(resultado);
		consulta.setCodigoHttpProveedor(respuestaProveedor.codigoHttp());
		consulta.setDuracionMilisegundos(respuestaProveedor.duracionMilisegundos());
		consulta.setEstado(esSinResultados(respuestaProveedor.cuerpo()) ? EstadoConsulta.SIN_RESULTADOS : EstadoConsulta.EXITOSA);
		consulta.setFechaFin(fin);
		if (reserva.origen() == OrigenConsumoConsulta.CREDITOS) {
			creditoServicio.confirmarCreditos(cliente, consulta, reserva.cantidad());
		}
		// Primera consulta exitosa de un invitado: libera el bono pendiente de su
		// referente (anti-granjas de referidos). Best-effort, nunca rompe la consulta.
		referidoServicio.acreditarBonoPorConsulta(cliente);
		return new ConsultaRespuesta(consulta.getCodigoConsulta(), consulta.getEstado(), consulta.getOrigenConsumo(),
				consulta.getCantidadConsumida(), acceso.permiteExportar(), respuestaProveedor.cuerpo(), archivos, consulta.getFechaInicio());
	}

	private Consulta crearConsulta(Usuario usuario, Cliente cliente, AccesoConsulta acceso,
			SolicitudConsulta solicitud, Reserva reserva, String ip, String agenteUsuario, String idempotencyKey) {
		JsonNode parametros = objectMapper.valueToTree(solicitud);
		return crearConsultaDesdeJson(usuario, cliente, acceso, parametros, mascara(solicitud),
				finalidad(solicitud), justificacion(solicitud), reserva, ip, agenteUsuario, idempotencyKey);
	}

	private Consulta crearConsultaDesdeJson(Usuario usuario, Cliente cliente, AccesoConsulta acceso,
			JsonNode parametros, String mascara, String finalidad, String justificacion, Reserva reserva,
			String ip, String agenteUsuario, String idempotencyKey) {
		Consulta consulta = new Consulta();
		consulta.setCodigoConsulta(UUID.randomUUID().toString());
		consulta.setCliente(cliente);
		consulta.setUsuario(usuario);
		consulta.setMembresia(acceso.membresia());
		consulta.setMembresiaEndpoint(acceso.membresiaEndpoint());
		consulta.setEndpoint(acceso.endpoint());
		consulta.setParametroCifrado(cifradoServicio.cifrar(parametros.toString()));
		consulta.setParametroMascara(mascara);
		consulta.setFinalidad(finalidad);
		consulta.setJustificacion(justificacion);
		consulta.setOrigenConsumo(reserva.origen());
		consulta.setCantidadConsumida(reserva.cantidad());
		consulta.setEstado(EstadoConsulta.PROCESANDO);
		consulta.setDireccionIp(ip == null ? "0.0.0.0" : ip);
		consulta.setAgenteUsuario(agenteUsuario);
		consulta.setFechaInicio(LocalDateTime.now());
		consulta.setClaveIdempotencia(StringUtils.hasText(idempotencyKey) ? idempotencyKey : null);
		return consultaRepositorio.save(consulta);
	}

	/**
	 * Resuelve cómo se accede al endpoint: si el cliente tiene membresía activa se
	 * usa su configuración (ilimitada o descuento de créditos); si NO tiene, la
	 * consulta va SOLO por créditos (cliente free) usando el costo del endpoint.
	 */
	private AccesoConsulta resolverAcceso(Cliente cliente, CodigoEndpoint codigo) {
		Membresia membresia = obtenerMembresiaActivaOpcional(cliente);
		if (membresia != null) {
			MembresiaEndpoint acceso = obtenerAcceso(membresia, codigo);
			boolean incluido = acceso.getModalidadAcceso() == ModalidadAccesoEndpoint.INCLUIDO_MEMBRESIA;
			boolean creditos = acceso.getModalidadAcceso() == ModalidadAccesoEndpoint.DESCUENTO_CREDITOS;
			if (!incluido && !creditos) {
				throw new NawinException(CodigoError.END_001);
			}
			int costo = acceso.getCostoCreditosCliente() == null
					? acceso.getEndpoint().getCostoProveedor()
					: acceso.getCostoCreditosCliente();
			return new AccesoConsulta(acceso.getEndpoint(), membresia, acceso, incluido, costo,
					acceso.getDiasRetencion(), acceso.isPermiteExportar(),
					acceso.isRequiereMfa(), acceso.isRequiereFinalidad(), acceso.isRequiereJustificacion());
		}
		// Sin membresía activa: acceso SOLO por créditos (free).
		EndpointBusqueda endpoint = endpointRepositorio.findByCodigo(codigo)
				.orElseThrow(() -> new NawinException(CodigoError.END_001));
		if (!endpoint.isActivo()) {
			throw new NawinException(CodigoError.END_001);
		}
		int costo = endpoint.getCostoCreditosCliente() == null
				? endpoint.getCostoProveedor()
				: endpoint.getCostoCreditosCliente();
		boolean critico = endpoint.isEsCritico();
		return new AccesoConsulta(endpoint, null, null, false, costo,
				propiedades.consultas().diasRetencionCreditos(), propiedades.consultas().permiteExportarCreditos(),
				false, critico, critico);
	}

	/** Membresía activa hoy, o null si no tiene (ya no lanza MEM_001/MEM_002; el free cae a créditos). */
	private Membresia obtenerMembresiaActivaOpcional(Cliente cliente) {
		LocalDate hoy = LocalDate.now();
		return membresiaRepositorio
				.findFirstByCliente_IdClienteAndEstadoAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaFinDesc(
						cliente.getIdCliente(), EstadoMembresia.ACTIVA, hoy, hoy)
				.orElse(null);
	}

	private Reserva reservar(AccesoConsulta acceso, Cliente cliente) {
		if (acceso.incluidoMembresia()) {
			// Ya NO se incrementa el contador denormalizado consumido_total: ese UPDATE
			// sobre membresias_endpoints, sostenido durante la llamada HTTP a CODART,
			// serializaba (y deadlockeaba) las consultas concurrentes de la misma
			// membresía. El consumo se cuenta en vivo (la Consulta se inserta en estado
			// PROCESANDO, que ya cuenta como reservada). Así el árbol genealógico puede
			// resolver varios familiares realmente en paralelo.
			validarCuota(acceso, cliente);
			return new Reserva(OrigenConsumoConsulta.MEMBRESIA, 1);
		}
		return new Reserva(OrigenConsumoConsulta.CREDITOS, acceso.costoCreditos());
	}

	private void validarCuota(AccesoConsulta acceso, Cliente cliente) {
		MembresiaEndpoint me = acceso.membresiaEndpoint();
		if (me.getLimiteTotal() != null) {
			long consumoTotal = consultaRepositorio.countByMembresiaEndpoint_IdMembresiaEndpointAndEstadoIn(
					me.getIdMembresiaEndpoint(), ESTADOS_CONSUMEN_CUOTA);
			if (consumoTotal >= me.getLimiteTotal()) {
				throw new NawinException(CodigoError.CUO_002);
			}
		}
		LocalDateTime desde = LocalDate.now().atStartOfDay();
		LocalDateTime hasta = LocalDate.now().atTime(LocalTime.MAX);
		if (me.getLimiteDiario() != null) {
			long consumoDiario = consultaRepositorio.countByMembresiaEndpoint_IdMembresiaEndpointAndEstadoInAndFechaInicioBetween(
					me.getIdMembresiaEndpoint(), ESTADOS_CONSUMEN_CUOTA, desde, hasta);
			if (consumoDiario >= me.getLimiteDiario()) {
				throw new NawinException(CodigoError.CUO_001);
			}
		}
		// Anti-abuso de membresías ilimitadas: corte de seguridad + aviso de uso anómalo por cliente/día.
		int tope = propiedades.consultas().topeDiarioIlimitado();
		int alerta = propiedades.consultas().alertaConsumoDiario();
		if (tope > 0 || alerta > 0) {
			long consumoClienteDia = consultaRepositorio.countByCliente_IdClienteAndEstadoInAndFechaInicioBetween(
					cliente.getIdCliente(), ESTADOS_CONSUMEN_CUOTA, desde, hasta);
			if (tope > 0 && consumoClienteDia >= tope) {
				throw new NawinException(CodigoError.CUO_001,
						"Alcanzaste el límite diario de seguridad. Intenta nuevamente mañana.");
			}
			if (alerta > 0 && consumoClienteDia + 1 == alerta) {
				log.warn("Uso anómalo: cliente {} alcanzó {} consultas ilimitadas hoy.", cliente.getIdCliente(), alerta);
			}
		}
	}

	private void liberarReserva(AccesoConsulta acceso, Cliente cliente, Consulta consulta, Reserva reserva) {
		if (reserva.origen() == OrigenConsumoConsulta.CREDITOS) {
			creditoServicio.liberarCreditos(cliente, consulta, reserva.cantidad());
		}
		// Membresía: no hay contador que revertir. Al marcarse FALLIDA, la consulta
		// sale de ESTADOS_CONSUMEN_CUOTA y deja de contar en el consumo automáticamente.
	}

	private Cliente validarCliente(Usuario usuario) {
		Cliente cliente = clienteRepositorio.findByUsuario_IdUsuario(usuario.getIdUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		if (cliente.getEstado() != EstadoCliente.ACTIVO || usuario.getEstado() != pe.nawin.enumeracion.EstadoUsuario.ACTIVO) {
			throw new NawinException(CodigoError.USR_001);
		}
		return cliente;
	}

	private MembresiaEndpoint obtenerAcceso(Membresia membresia, CodigoEndpoint codigo) {
		MembresiaEndpoint acceso = membresiaEndpointRepositorio
				.findByMembresia_IdMembresiaAndEndpoint_Codigo(membresia.getIdMembresia(), codigo)
				.orElseThrow(() -> new NawinException(CodigoError.END_001));
		if (!acceso.isHabilitado() || !acceso.getEndpoint().isActivo()) {
			throw new NawinException(CodigoError.END_001);
		}
		return acceso;
	}

	private void validarReglasCriticas(AccesoConsulta acceso, SolicitudConsulta solicitud, Usuario usuario) {
		if (!acceso.requiereMfa() && !acceso.requiereFinalidad() && !acceso.requiereJustificacion()) {
			return;
		}
		if (!(solicitud instanceof SolicitudConsultaCritica critica)) {
			throw new NawinException(CodigoError.MFA_001);
		}
		if (acceso.requiereFinalidad() && !StringUtils.hasText(critica.finalidad())) {
			throw new NawinException(CodigoError.MFA_001);
		}
		if (acceso.requiereJustificacion() && !StringUtils.hasText(critica.justificacion())) {
			throw new NawinException(CodigoError.MFA_001);
		}
		if (acceso.requiereMfa() && (!usuario.isMfaHabilitado() || !"123456".equals(critica.codigoMfa()))) {
			throw new NawinException(CodigoError.MFA_001);
		}
	}

	private void validarCriticoFacial(AccesoConsulta acceso, Usuario usuario, String finalidad, String justificacion, String codigoMfa) {
		if (acceso.requiereFinalidad() && !StringUtils.hasText(finalidad)) {
			throw new NawinException(CodigoError.MFA_001);
		}
		if (acceso.requiereJustificacion() && !StringUtils.hasText(justificacion)) {
			throw new NawinException(CodigoError.MFA_001);
		}
		if (acceso.requiereMfa() && (!usuario.isMfaHabilitado() || !"123456".equals(codigoMfa))) {
			throw new NawinException(CodigoError.MFA_001);
		}
	}

	private void validarImagenFacial(MultipartFile imagen) {
		if (imagen == null || imagen.isEmpty() || imagen.getSize() > 5_000_000L) {
			throw new NawinException(CodigoError.CON_001, "Imagen facial inválida.");
		}
		String tipo = imagen.getContentType();
		if (!List.of("image/jpeg", "image/jpg", "image/png").contains(tipo)) {
			throw new NawinException(CodigoError.CON_001, "Solo se aceptan imágenes jpg, jpeg o png.");
		}
	}

	private List<Map<String, Object>> guardarArchivos(Consulta consulta, JsonNode json, int diasRetencion) {
		List<Map<String, Object>> archivos = new ArrayList<>();
		List<DataUri> dataUris = new ArrayList<>();
		recolectarDataUris(json, dataUris);
		int contador = 1;
		for (DataUri dataUri : dataUris) {
			try {
				byte[] bytes = Base64.getDecoder().decode(dataUri.base64());
				TipoArchivoConsulta tipo = dataUri.mime().contains("pdf") ? TipoArchivoConsulta.PDF : TipoArchivoConsulta.IMAGEN;
				String extension = tipo == TipoArchivoConsulta.PDF ? ".pdf" : ".bin";
				String nombre = "archivo-" + contador + extension;
				Path carpeta = Path.of(propiedades.almacenamientoPrivadoRuta(), "consultas", consulta.getCodigoConsulta());
				Files.createDirectories(carpeta);
				Path ruta = carpeta.resolve(nombre);
				Files.write(ruta, bytes);
				ArchivoConsulta archivo = new ArchivoConsulta();
				archivo.setConsulta(consulta);
				archivo.setTipoArchivo(tipo);
				archivo.setNombreArchivo(nombre);
				archivo.setTipoMime(dataUri.mime());
				archivo.setRutaPrivada(ruta.toAbsolutePath().toString());
				archivo.setTamanoBytes(bytes.length);
				archivo.setFechaExpiracion(LocalDateTime.now().plusDays(diasRetencion));
				archivoRepositorio.save(archivo);
				archivos.add(Map.of("idArchivoConsulta", archivo.getIdArchivoConsulta(), "nombreArchivo", nombre,
						"tipoArchivo", tipo, "tipoMime", dataUri.mime(), "tamanoBytes", bytes.length));
				contador++;
			} catch (Exception ignored) {
				// Si un proveedor envía un data_uri mal formado, se conserva el JSON cifrado y se omite el archivo.
			}
		}
		return archivos;
	}

	private void recolectarDataUris(JsonNode nodo, List<DataUri> dataUris) {
		if (nodo == null) {
			return;
		}
		if (nodo.isTextual() && nodo.asText().startsWith("data:") && nodo.asText().contains(";base64,")) {
			String texto = nodo.asText();
			int idxMime = texto.indexOf(';');
			int idxData = texto.indexOf(";base64,");
			dataUris.add(new DataUri(texto.substring(5, idxMime), texto.substring(idxData + 8)));
			return;
		}
		if (nodo.isObject()) {
			nodo.fields().forEachRemaining(entry -> recolectarDataUris(entry.getValue(), dataUris));
		} else if (nodo.isArray()) {
			nodo.forEach(item -> recolectarDataUris(item, dataUris));
		}
	}

	private ConsultaRespuesta respuestaDesdeConsulta(Consulta consulta) {
		JsonNode resultado = objectMapper.createObjectNode();
		var resultadoOpt = resultadoRepositorio.findByConsulta_CodigoConsulta(consulta.getCodigoConsulta());
		if (resultadoOpt.isPresent() && !resultadoOpt.get().isEliminadoPorRetencion()) {
			try {
				resultado = objectMapper.readTree(cifradoServicio.descifrar(resultadoOpt.get().getResultadoJsonCifrado()));
			} catch (Exception ignored) {
				resultado = objectMapper.createObjectNode();
			}
		}
		List<Map<String, Object>> archivos = archivoRepositorio.findByConsulta_IdConsulta(consulta.getIdConsulta()).stream()
				.map(a -> Map.<String, Object>of("idArchivoConsulta", a.getIdArchivoConsulta(), "nombreArchivo", a.getNombreArchivo(),
						"tipoArchivo", a.getTipoArchivo(), "tipoMime", a.getTipoMime(), "tamanoBytes", a.getTamanoBytes()))
				.toList();
		boolean permiteExportar = consulta.getMembresiaEndpoint() != null
				? consulta.getMembresiaEndpoint().isPermiteExportar()
				: propiedades.consultas().permiteExportarCreditos();
		return new ConsultaRespuesta(consulta.getCodigoConsulta(), consulta.getEstado(), consulta.getOrigenConsumo(),
				consulta.getCantidadConsumida(), permiteExportar, resultado, archivos, consulta.getFechaInicio());
	}

	private Map<String, Object> consultaResumen(Consulta consulta) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("codigoConsulta", consulta.getCodigoConsulta());
		m.put("cliente", consulta.getCliente().getIdCliente());
		m.put("endpoint", consulta.getEndpoint().getCodigo());
		m.put("parametroMascara", consulta.getParametroMascara());
		m.put("estado", consulta.getEstado());
		m.put("origenConsumo", consulta.getOrigenConsumo());
		m.put("cantidadConsumida", consulta.getCantidadConsumida());
		m.put("fechaInicio", consulta.getFechaInicio());
		m.put("fechaFin", consulta.getFechaFin());
		return m;
	}

	private String mascara(SolicitudConsulta solicitud) {
		JsonNode json = objectMapper.valueToTree(solicitud);
		if (json.hasNonNull("dni")) return TextoSeguro.mascara(json.get("dni").asText());
		if (json.hasNonNull("ruc")) return TextoSeguro.mascara(json.get("ruc").asText());
		if (json.hasNonNull("numero")) return TextoSeguro.mascara(json.get("numero").asText());
		if (json.hasNonNull("placa")) return TextoSeguro.mascara(json.get("placa").asText());
		if (json.hasNonNull("nombres")) return TextoSeguro.mascara(json.get("nombres").asText());
		return "consulta";
	}

	private String finalidad(SolicitudConsulta solicitud) {
		return solicitud instanceof SolicitudConsultaCritica critica ? critica.finalidad() : null;
	}

	private String justificacion(SolicitudConsulta solicitud) {
		return solicitud instanceof SolicitudConsultaCritica critica ? critica.justificacion() : null;
	}

	private String resumenSeguro(JsonNode json) {
		ObjectNode resumen = objectMapper.createObjectNode();
		resumen.put("success", json.path("success").asBoolean(true));
		if (json.has("source")) {
			resumen.put("source", json.path("source").asText());
		}
		return resumen.toString();
	}

	private boolean esSinResultados(JsonNode json) {
		return json.has("success") && !json.path("success").asBoolean();
	}

	private void marcarFallida(Consulta consulta, String mensaje) {
		consulta.setEstado(EstadoConsulta.FALLIDA);
		consulta.setMensajeError(mensaje);
		consulta.setFechaFin(LocalDateTime.now());
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}

	private record Reserva(OrigenConsumoConsulta origen, int cantidad) {
	}

	private record DataUri(String mime, String base64) {
	}
}
