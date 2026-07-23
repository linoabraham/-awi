package pe.nawin.servicio;

import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.UsuarioActualizarRequest;
import pe.nawin.dto.solicitud.UsuarioCrearRequest;
import pe.nawin.entidad.Rol;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.mapper.MapeadorRespuesta;
import pe.nawin.repositorio.RolRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;

@Service
public class UsuarioServicio {

	private final UsuarioRepositorio usuarioRepositorio;
	private final RolRepositorio rolRepositorio;
	private final PasswordEncoder passwordEncoder;
	private final AutenticacionServicio autenticacionServicio;

	public UsuarioServicio(UsuarioRepositorio usuarioRepositorio, RolRepositorio rolRepositorio,
			PasswordEncoder passwordEncoder, AutenticacionServicio autenticacionServicio) {
		this.usuarioRepositorio = usuarioRepositorio;
		this.rolRepositorio = rolRepositorio;
		this.passwordEncoder = passwordEncoder;
		this.autenticacionServicio = autenticacionServicio;
	}

	@Transactional
	public Map<String, Object> crear(UsuarioCrearRequest request) {
		if (usuarioRepositorio.existsByNombreUsuario(request.nombreUsuario()) || usuarioRepositorio.existsByCorreo(request.correo())) {
			throw new NawinException(CodigoError.GEN_001, "Usuario o correo ya existe.");
		}
		autenticacionServicio.validarClave(request.claveTemporal());
		Rol rol = rolRepositorio.findByCodigo(request.rol()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		Usuario usuario = new Usuario();
		usuario.setRol(rol);
		usuario.setNombres(request.nombres());
		usuario.setApellidos(request.apellidos());
		usuario.setCorreo(request.correo());
		usuario.setCelular(request.celular());
		usuario.setNombreUsuario(request.nombreUsuario());
		usuario.setClaveHash(passwordEncoder.encode(request.claveTemporal()));
		usuario.setEstado(EstadoUsuario.ACTIVO);
		return MapeadorRespuesta.usuario(usuarioRepositorio.save(usuario));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listar() {
		return usuarioRepositorio.findAll().stream().map(MapeadorRespuesta::usuario).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> ver(Long id) {
		return MapeadorRespuesta.usuario(obtener(id));
	}

	@Transactional
	public Map<String, Object> actualizar(Long id, UsuarioActualizarRequest request) {
		Usuario usuario = obtener(id);
		Rol rol = rolRepositorio.findByCodigo(request.rol()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		usuario.setRol(rol);
		usuario.setNombres(request.nombres());
		usuario.setApellidos(request.apellidos());
		usuario.setCorreo(request.correo());
		usuario.setCelular(request.celular());
		usuario.setEstado(request.estado());
		return MapeadorRespuesta.usuario(usuario);
	}

	@Transactional
	public void desactivar(Long id) {
		obtener(id).setEstado(EstadoUsuario.INACTIVO);
	}

	public Usuario obtener(Long id) {
		return usuarioRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}
}
