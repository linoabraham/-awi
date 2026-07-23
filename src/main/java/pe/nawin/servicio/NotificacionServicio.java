package pe.nawin.servicio;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.MotivoRequest;
import pe.nawin.dto.solicitud.NotificacionRequest;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Membresia;
import pe.nawin.entidad.Notificacion;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.CanalNotificacion;
import pe.nawin.enumeracion.EstadoNotificacion;
import pe.nawin.enumeracion.TipoNotificacion;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.repositorio.ClienteRepositorio;
import pe.nawin.repositorio.MembresiaRepositorio;
import pe.nawin.repositorio.NotificacionRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;

@Service
public class NotificacionServicio {

	private final NotificacionRepositorio notificacionRepositorio;
	private final ClienteRepositorio clienteRepositorio;
	private final MembresiaRepositorio membresiaRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;

	public NotificacionServicio(NotificacionRepositorio notificacionRepositorio, ClienteRepositorio clienteRepositorio,
			MembresiaRepositorio membresiaRepositorio, UsuarioRepositorio usuarioRepositorio) {
		this.notificacionRepositorio = notificacionRepositorio;
		this.clienteRepositorio = clienteRepositorio;
		this.membresiaRepositorio = membresiaRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
	}

	@Transactional
	public Map<String, Object> crear(NotificacionRequest request) {
		Cliente cliente = clienteRepositorio.findById(request.idCliente()).orElseThrow(() -> new NawinException(CodigoError.CLI_001));
		Membresia membresia = request.idMembresia() == null ? null
				: membresiaRepositorio.findById(request.idMembresia()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		Notificacion notificacion = new Notificacion();
		notificacion.setCliente(cliente);
		notificacion.setMembresia(membresia);
		notificacion.setCanal(request.canal());
		notificacion.setTipo(request.tipo());
		notificacion.setTitulo(request.titulo());
		notificacion.setMensaje(request.mensaje());
		notificacion.setFechaProgramada(request.fechaProgramada());
		notificacion.setEstado(EstadoNotificacion.PENDIENTE);
		notificacion.setCreadoPor(usuarioActual());
		return respuesta(notificacionRepositorio.save(notificacion));
	}

	@Transactional
	public Map<String, Object> crearAutomatica(Cliente cliente, Membresia membresia, TipoNotificacion tipo,
			String titulo, String mensaje, Usuario creadoPor) {
		Notificacion notificacion = new Notificacion();
		notificacion.setCliente(cliente);
		notificacion.setMembresia(membresia);
		notificacion.setCanal(CanalNotificacion.SISTEMA);
		notificacion.setTipo(tipo);
		notificacion.setTitulo(titulo);
		notificacion.setMensaje(mensaje);
		notificacion.setEstado(EstadoNotificacion.PENDIENTE);
		notificacion.setFechaProgramada(LocalDateTime.now());
		notificacion.setCreadoPor(creadoPor);
		return respuesta(notificacionRepositorio.save(notificacion));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listar(Long idCliente, EstadoNotificacion estado) {
		List<Notificacion> notificaciones = idCliente != null
				? notificacionRepositorio.findByCliente_IdClienteOrderByFechaCreacionDesc(idCliente)
				: estado != null ? notificacionRepositorio.findByEstado(estado) : notificacionRepositorio.findAll();
		return notificaciones.stream().map(this::respuesta).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> ver(Long id) {
		return respuesta(obtener(id));
	}

	@Transactional
	public Map<String, Object> actualizar(Long id, NotificacionRequest request) {
		Notificacion notificacion = obtener(id);
		if (notificacion.getEstado() != EstadoNotificacion.PENDIENTE) {
			throw new NawinException(CodigoError.GEN_001, "Solo se actualizan notificaciones pendientes.");
		}
		notificacion.setCanal(request.canal());
		notificacion.setTipo(request.tipo());
		notificacion.setTitulo(request.titulo());
		notificacion.setMensaje(request.mensaje());
		notificacion.setFechaProgramada(request.fechaProgramada());
		return respuesta(notificacion);
	}

	@Transactional
	public void cancelar(Long id, MotivoRequest request) {
		Notificacion notificacion = obtener(id);
		if (notificacion.getEstado() == EstadoNotificacion.PENDIENTE) {
			notificacion.setEstado(EstadoNotificacion.FALLIDA);
		}
	}

	@Transactional
	public void marcarLeida(Long id, Long idCliente) {
		Notificacion notificacion = obtener(id);
		if (idCliente == null) {
			idCliente = pe.nawin.utilidad.ContextoSeguridad.usuarioActual().idUsuario();
		}
		if (!notificacion.getCliente().getIdCliente().equals(idCliente)) {
			Long idUsuarioCliente = notificacion.getCliente().getUsuario().getIdUsuario();
			if (!idUsuarioCliente.equals(idCliente)) {
				throw new NawinException(CodigoError.AUTH_003);
			}
		}
		notificacion.setEstado(EstadoNotificacion.LEIDA);
	}

	@Transactional
	public void marcarEnviada(Notificacion notificacion) {
		notificacion.setEstado(EstadoNotificacion.ENVIADA);
		notificacion.setFechaEnvio(LocalDateTime.now());
	}

	private Notificacion obtener(Long id) {
		return notificacionRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario()).orElse(null);
	}

	private Map<String, Object> respuesta(Notificacion notificacion) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idNotificacion", notificacion.getIdNotificacion());
		m.put("idCliente", notificacion.getCliente().getIdCliente());
		m.put("idMembresia", notificacion.getMembresia() == null ? null : notificacion.getMembresia().getIdMembresia());
		m.put("canal", notificacion.getCanal());
		m.put("tipo", notificacion.getTipo());
		m.put("titulo", notificacion.getTitulo());
		m.put("mensaje", notificacion.getMensaje());
		m.put("estado", notificacion.getEstado());
		m.put("fechaProgramada", notificacion.getFechaProgramada());
		m.put("fechaEnvio", notificacion.getFechaEnvio());
		m.put("fechaCreacion", notificacion.getFechaCreacion());
		return m;
	}
}
