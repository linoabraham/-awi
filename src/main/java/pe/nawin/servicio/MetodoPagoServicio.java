package pe.nawin.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.MetodoPagoRequest;
import pe.nawin.entidad.MetodoPago;
import pe.nawin.entidad.Usuario;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.repositorio.MetodoPagoRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;

/**
 * Gestiona los métodos de pago. Lectura pública para el cliente (sin
 * credenciales) y CRUD para el administrador desde el panel.
 */
@Service
public class MetodoPagoServicio {

	private final MetodoPagoRepositorio repositorio;
	private final UsuarioRepositorio usuarioRepositorio;

	public MetodoPagoServicio(MetodoPagoRepositorio repositorio, UsuarioRepositorio usuarioRepositorio) {
		this.repositorio = repositorio;
		this.usuarioRepositorio = usuarioRepositorio;
	}

	// ---- Cliente (público, sin credenciales) ----

	/** Método Yape activo con la forma que consume la app (compatibilidad). */
	@Transactional(readOnly = true)
	public Map<String, Object> yapePublico() {
		MetodoPago metodo = repositorio.findByCodigo("YAPE")
				.filter(MetodoPago::isActivo)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002, "Yape no está configurado."));
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("numero", metodo.getNumero());
		m.put("titular", metodo.getTitular());
		m.put("qrBase64", metodo.getQrBase64());
		m.put("validacionTexto", metodo.getValidacionTexto());
		return m;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> activosPublico() {
		return repositorio.findByActivoTrueOrderByOrdenAscIdMetodoPagoAsc().stream()
				.map(this::respuestaPublica)
				.toList();
	}

	// ---- Admin (con credenciales) ----

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listar() {
		return repositorio.findAllByOrderByOrdenAscIdMetodoPagoAsc().stream()
				.map(this::respuestaAdmin)
				.toList();
	}

	@Transactional
	public Map<String, Object> crear(MetodoPagoRequest request) {
		String codigo = request.codigo().trim().toUpperCase();
		if (repositorio.existsByCodigo(codigo)) {
			throw new NawinException(CodigoError.GEN_001, "Ya existe un método con ese código.");
		}
		MetodoPago metodo = new MetodoPago();
		metodo.setCodigo(codigo);
		aplicar(metodo, request);
		return respuestaAdmin(repositorio.save(metodo));
	}

	@Transactional
	public Map<String, Object> actualizar(Long id, MetodoPagoRequest request) {
		MetodoPago metodo = repositorio.findById(id)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002, "Método no encontrado."));
		String codigo = request.codigo().trim().toUpperCase();
		if (!codigo.equals(metodo.getCodigo()) && repositorio.existsByCodigo(codigo)) {
			throw new NawinException(CodigoError.GEN_001, "Ya existe un método con ese código.");
		}
		metodo.setCodigo(codigo);
		aplicar(metodo, request);
		return respuestaAdmin(metodo);
	}

	@Transactional
	public Map<String, Object> cambiarEstado(Long id, boolean activo) {
		MetodoPago metodo = repositorio.findById(id)
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002, "Método no encontrado."));
		metodo.setActivo(activo);
		metodo.setActualizadoPor(usuarioActual());
		return respuestaAdmin(metodo);
	}

	private void aplicar(MetodoPago metodo, MetodoPagoRequest request) {
		metodo.setNombre(request.nombre().trim());
		metodo.setTipo(request.tipo().trim().toUpperCase());
		if (request.activo() != null) {
			metodo.setActivo(request.activo());
		}
		if (request.orden() != null) {
			metodo.setOrden(request.orden());
		}
		metodo.setNumero(vacioANull(request.numero()));
		metodo.setTitular(vacioANull(request.titular()));
		if (request.qrBase64() != null) {
			metodo.setQrBase64(request.qrBase64().isBlank() ? null : request.qrBase64());
		}
		if (request.credencialesJson() != null) {
			metodo.setCredencialesJson(
					request.credencialesJson().isBlank() ? null : request.credencialesJson());
		}
		metodo.setInstrucciones(vacioANull(request.instrucciones()));
		if (request.validacionTexto() != null && !request.validacionTexto().isBlank()) {
			metodo.setValidacionTexto(request.validacionTexto().trim());
		}
		metodo.setActualizadoPor(usuarioActual());
	}

	private String vacioANull(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}

	private Map<String, Object> respuestaPublica(MetodoPago m) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("codigo", m.getCodigo());
		r.put("nombre", m.getNombre());
		r.put("tipo", m.getTipo());
		r.put("numero", m.getNumero());
		r.put("titular", m.getTitular());
		r.put("qrBase64", m.getQrBase64());
		r.put("instrucciones", m.getInstrucciones());
		r.put("validacionTexto", m.getValidacionTexto());
		return r;
	}

	private Map<String, Object> respuestaAdmin(MetodoPago m) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("idMetodoPago", m.getIdMetodoPago());
		r.put("codigo", m.getCodigo());
		r.put("nombre", m.getNombre());
		r.put("tipo", m.getTipo());
		r.put("activo", m.isActivo());
		r.put("orden", m.getOrden());
		r.put("numero", m.getNumero());
		r.put("titular", m.getTitular());
		r.put("qrBase64", m.getQrBase64());
		r.put("credencialesJson", m.getCredencialesJson());
		r.put("instrucciones", m.getInstrucciones());
		r.put("validacionTexto", m.getValidacionTexto());
		r.put("fechaActualizacion", m.getFechaActualizacion());
		return r;
	}
}
