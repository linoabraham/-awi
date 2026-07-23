package pe.nawin.servicio;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.ClienteActualizarRequest;
import pe.nawin.dto.solicitud.ClienteCrearRequest;
import pe.nawin.dto.solicitud.RegistroClienteRequest;
import pe.nawin.entidad.BilleteraMiPlata;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Rol;
import pe.nawin.entidad.SaldoCredito;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.EstadoCliente;
import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.enumeracion.RolCodigo;
import pe.nawin.enumeracion.TipoDocumento;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.BilleteraMiPlataRepositorio;
import pe.nawin.repositorio.RolRepositorio;
import pe.nawin.repositorio.SaldoCreditoRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;

@Service
public class ClienteServicio {

	private final ClienteRepositorio clienteRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final RolRepositorio rolRepositorio;
	private final SaldoCreditoRepositorio saldoCreditoRepositorio;
	private final BilleteraMiPlataRepositorio billeteraMiPlataRepositorio;
	private final PasswordEncoder passwordEncoder;
	private final AutenticacionServicio autenticacionServicio;
	private final SecureRandom secureRandom = new SecureRandom();

	public ClienteServicio(ClienteRepositorio clienteRepositorio, UsuarioRepositorio usuarioRepositorio,
			RolRepositorio rolRepositorio, SaldoCreditoRepositorio saldoCreditoRepositorio,
			BilleteraMiPlataRepositorio billeteraMiPlataRepositorio,
			PasswordEncoder passwordEncoder, AutenticacionServicio autenticacionServicio) {
		this.clienteRepositorio = clienteRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.rolRepositorio = rolRepositorio;
		this.saldoCreditoRepositorio = saldoCreditoRepositorio;
		this.billeteraMiPlataRepositorio = billeteraMiPlataRepositorio;
		this.passwordEncoder = passwordEncoder;
		this.autenticacionServicio = autenticacionServicio;
	}

	@Transactional
	public Map<String, Object> crear(ClienteCrearRequest request) {
		validarDocumento(request.tipoDocumento(), request.numeroDocumento(), request.nombres(), request.apellidos(), request.razonSocial());
		if (clienteRepositorio.existsByNumeroDocumento(request.numeroDocumento())) {
			throw new NawinException(CodigoError.GEN_001, "Documento de cliente ya existe.");
		}
		if (usuarioRepositorio.existsByNombreUsuario(request.nombreUsuario()) || usuarioRepositorio.existsByCorreo(request.correo())) {
			throw new NawinException(CodigoError.GEN_001, "Usuario o correo ya existe.");
		}
		autenticacionServicio.validarClave(request.claveTemporal());
		Rol rolCliente = rolRepositorio.findByCodigo(RolCodigo.CLIENTE).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		Usuario usuario = new Usuario();
		usuario.setRol(rolCliente);
		usuario.setNombres(request.nombres() == null ? request.razonSocial() : request.nombres());
		usuario.setApellidos(request.apellidos() == null ? "-" : request.apellidos());
		usuario.setCorreo(request.correo());
		usuario.setCelular(request.celular());
		usuario.setNombreUsuario(request.nombreUsuario());
		usuario.setClaveHash(passwordEncoder.encode(request.claveTemporal()));
		usuario.setEstado(EstadoUsuario.ACTIVO);
		usuarioRepositorio.save(usuario);

		Cliente cliente = new Cliente();
		cliente.setUsuario(usuario);
		cliente.setTipoDocumento(request.tipoDocumento());
		cliente.setNumeroDocumento(request.numeroDocumento());
		cliente.setNombres(request.nombres());
		cliente.setApellidos(request.apellidos());
		cliente.setRazonSocial(request.razonSocial());
		cliente.setDireccion(request.direccion());
		cliente.setCodigoReferido(generarCodigoReferido());
		cliente.setEstado(EstadoCliente.ACTIVO);
		cliente.setCreadoPor(usuarioActual());
		clienteRepositorio.save(cliente);

		SaldoCredito saldo = new SaldoCredito();
		saldo.setCliente(cliente);
		saldoCreditoRepositorio.save(saldo);

		BilleteraMiPlata billetera = new BilleteraMiPlata();
		billetera.setCliente(cliente);
		billeteraMiPlataRepositorio.save(billetera);
		return MapeadorRespuesta.cliente(cliente);
	}

	/**
	 * Registro público (autoservicio) de un cliente con rol CLIENTE. A diferencia
	 * de {@link #crear}, no requiere usuario autenticado: el propio usuario creado
	 * queda como creador del registro.
	 */
	@Transactional
	public Map<String, Object> registrarPublico(RegistroClienteRequest request) {
		String correoNormalizado = request.correo().trim().toLowerCase();
		String documento = limpiar(request.numeroDocumento());
		autenticacionServicio.validarClave(request.clave());
		// Nombre y apellidos: se divide el campo "Nombres y apellidos" del registro.
		String[] partes = dividirNombre(request.nombres(), request.apellidos());

		// Reintento de registro: si el correo ya existe pero NO está verificado, la
		// cuenta técnicamente no quedó creada. Se actualizan sus datos y el
		// controlador reenvía un código (no se manda al login). Solo si el correo YA
		// está verificado se rechaza, con mensaje genérico (anti-enumeración).
		Usuario existente = usuarioRepositorio.findByCorreoIgnoreCase(correoNormalizado).orElse(null);
		// Anti alias (tucorreo+1@gmail.com, tu.correo@gmail.com): el mismo buzón
		// real no puede crear otra cuenta, aunque el texto del correo sea distinto.
		String correoCanonico = pe.nawin.utilidad.CorreoNormalizador.normalizar(correoNormalizado);
		if (existente == null && usuarioRepositorio.existsByCorreoNormalizado(correoCanonico)) {
			throw new NawinException(CodigoError.GEN_001,
					"No se pudo completar el registro con los datos ingresados. Verifica tu información o inicia sesión.");
		}
		if (existente != null) {
			if (existente.isCorreoVerificado()) {
				throw new NawinException(CodigoError.GEN_001,
						"No se pudo completar el registro con los datos ingresados. Verifica tu información o inicia sesión.");
			}
			existente.setNombres(partes[0]);
			existente.setApellidos(partes[1]);
			existente.setClaveHash(passwordEncoder.encode(request.clave()));
			Cliente clienteExistente = clienteRepositorio.findByUsuario_IdUsuario(existente.getIdUsuario()).orElse(null);
			if (clienteExistente != null) {
				clienteExistente.setNombres(partes[0]);
				clienteExistente.setApellidos(partes[1]);
				return MapeadorRespuesta.cliente(clienteExistente);
			}
			return Map.of("correo", correoNormalizado);
		}

		if (documento != null) {
			if (clienteRepositorio.existsByNumeroDocumento(documento)) {
				throw new NawinException(CodigoError.GEN_001,
						"No se pudo completar el registro con los datos ingresados. Verifica tu información o inicia sesión.");
			}
			validarDocumento(request.tipoDocumento(), documento, request.nombres(), request.apellidos(),
					request.razonSocial());
		}
		Rol rolCliente = rolRepositorio.findByCodigo(RolCodigo.CLIENTE)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));

		String nombreUsuario = resolverNombreUsuario(request.nombreUsuario(), correoNormalizado);

		Usuario usuario = new Usuario();
		usuario.setRol(rolCliente);
		usuario.setNombres(partes[0]);
		usuario.setApellidos(partes[1]);
		usuario.setCorreo(correoNormalizado);
		usuario.setCorreoNormalizado(correoCanonico);
		usuario.setCelular(limpiar(request.celular()));
		usuario.setNombreUsuario(nombreUsuario);
		usuario.setClaveHash(passwordEncoder.encode(request.clave()));
		usuario.setEstado(EstadoUsuario.ACTIVO);
		// La cuenta creada desde la app queda pendiente de verificar el correo.
		usuario.setCorreoVerificado(false);
		usuarioRepositorio.save(usuario);

		Cliente cliente = new Cliente();
		cliente.setUsuario(usuario);
		cliente.setTipoDocumento(request.tipoDocumento());
		cliente.setNumeroDocumento(documento);
		cliente.setNombres(partes[0]);
		cliente.setApellidos(partes[1]);
		cliente.setRazonSocial(limpiar(request.razonSocial()));
		cliente.setDireccion(limpiar(request.direccion()));
		cliente.setCodigoReferido(generarCodigoReferido());
		cliente.setEstado(EstadoCliente.ACTIVO);
		cliente.setCreadoPor(usuario);
		clienteRepositorio.save(cliente);

		SaldoCredito saldo = new SaldoCredito();
		saldo.setCliente(cliente);
		saldoCreditoRepositorio.save(saldo);

		BilleteraMiPlata billetera = new BilleteraMiPlata();
		billetera.setCliente(cliente);
		billeteraMiPlataRepositorio.save(billetera);
		return MapeadorRespuesta.cliente(cliente);
	}

	/** Divide "Nombres y apellidos" en dos: primer token = nombres, resto = apellidos. */
	private String[] dividirNombre(String nombreCompleto, String apellidosExplicitos) {
		String completo = nombreCompleto == null ? "" : nombreCompleto.trim().replaceAll("\\s+", " ");
		if (apellidosExplicitos != null && !apellidosExplicitos.isBlank()) {
			return new String[] {completo.isEmpty() ? "-" : completo, apellidosExplicitos.trim()};
		}
		int espacio = completo.indexOf(' ');
		if (espacio < 0) {
			return new String[] {completo.isEmpty() ? "-" : completo, "-"};
		}
		return new String[] {completo.substring(0, espacio), completo.substring(espacio + 1)};
	}

	/** Usa el nombre de usuario dado o genera uno único a partir del correo. */
	private String resolverNombreUsuario(String propuesto, String correo) {
		if (propuesto != null && !propuesto.isBlank() && !usuarioRepositorio.existsByNombreUsuario(propuesto.trim())) {
			return propuesto.trim();
		}
		String base = correo.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
		if (base.isBlank()) {
			base = "cliente";
		}
		if (base.length() > 50) {
			base = base.substring(0, 50);
		}
		String candidato = base;
		int intento = 0;
		while (usuarioRepositorio.existsByNombreUsuario(candidato)) {
			candidato = base + (1000 + secureRandom.nextInt(9000));
			if (++intento > 20) {
				candidato = base + System.nanoTime();
				break;
			}
		}
		return candidato;
	}

	private String limpiar(String valor) {
		if (valor == null) {
			return null;
		}
		String t = valor.trim();
		return t.isEmpty() ? null : t;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listar(EstadoCliente estado) {
		List<Cliente> clientes = estado == null ? clienteRepositorio.findAll() : clienteRepositorio.findByEstado(estado);
		return clientes.stream().map(MapeadorRespuesta::cliente).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> ver(Long id) {
		return MapeadorRespuesta.cliente(obtener(id));
	}

	@Transactional
	public Map<String, Object> actualizar(Long id, ClienteActualizarRequest request) {
		Cliente cliente = obtener(id);
		validarDocumento(cliente.getTipoDocumento(), cliente.getNumeroDocumento(), request.nombres(), request.apellidos(), request.razonSocial());
		cliente.setNombres(request.nombres());
		cliente.setApellidos(request.apellidos());
		cliente.setRazonSocial(request.razonSocial());
		cliente.setDireccion(request.direccion());
		if (request.estado() != null) {
			cliente.setEstado(request.estado());
			cliente.getUsuario().setEstado(request.estado() == EstadoCliente.ACTIVO ? EstadoUsuario.ACTIVO : EstadoUsuario.INACTIVO);
		}
		cliente.getUsuario().setCorreo(request.correo());
		cliente.getUsuario().setCelular(request.celular());
		return MapeadorRespuesta.cliente(cliente);
	}

	@Transactional
	public void desactivar(Long id) {
		Cliente cliente = obtener(id);
		cliente.setEstado(EstadoCliente.INACTIVO);
		cliente.getUsuario().setEstado(EstadoUsuario.INACTIVO);
	}

	@Transactional(readOnly = true)
	public Cliente clienteActual() {
		return clienteRepositorio.findByUsuario_IdUsuario(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.CLI_001));
	}

	public Cliente obtener(Long id) {
		return clienteRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.CLI_001));
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}

	private void validarDocumento(TipoDocumento tipoDocumento, String numero, String nombres, String apellidos, String razonSocial) {
		if (tipoDocumento == TipoDocumento.DNI) {
			if (numero == null || !numero.matches("\\d{8}") || estaVacio(nombres) || estaVacio(apellidos) || !estaVacio(razonSocial)) {
				throw new NawinException(CodigoError.GEN_001, "Cliente DNI requiere 8 dígitos, nombres y apellidos; razón social debe ser nula.");
			}
			return;
		}
		if (numero == null || !numero.matches("\\d{11}") || estaVacio(razonSocial)) {
			throw new NawinException(CodigoError.GEN_001, "Cliente RUC requiere 11 dígitos y razón social.");
		}
	}

	private boolean estaVacio(String valor) {
		return valor == null || valor.isBlank();
	}

	private static final String ALFABETO_REFERIDO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	/** Genera un código de referido aleatorio de 5 caracteres (A-Z y 0-9, sin Ñ). */
	private String generarCodigoReferido() {
		String codigo;
		do {
			StringBuilder sb = new StringBuilder(5);
			for (int i = 0; i < 5; i++) {
				sb.append(ALFABETO_REFERIDO.charAt(secureRandom.nextInt(ALFABETO_REFERIDO.length())));
			}
			codigo = sb.toString();
		} while (clienteRepositorio.findByCodigoReferido(codigo).isPresent());
		return codigo;
	}
}
