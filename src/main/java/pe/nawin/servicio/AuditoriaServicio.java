package pe.nawin.servicio;

import org.springframework.stereotype.Service;
import pe.nawin.entidad.Auditoria;
import pe.nawin.entidad.Usuario;
import pe.nawin.repositorio.AuditoriaRepositorio;

@Service
public class AuditoriaServicio {

	private final AuditoriaRepositorio auditoriaRepositorio;

	public AuditoriaServicio(AuditoriaRepositorio auditoriaRepositorio) {
		this.auditoriaRepositorio = auditoriaRepositorio;
	}

	public void registrar(Usuario usuario, String accion, String entidad, Object idEntidad, String detalleJson, String ip) {
		Auditoria auditoria = new Auditoria();
		auditoria.setUsuario(usuario);
		auditoria.setAccion(accion);
		auditoria.setEntidad(entidad);
		auditoria.setIdEntidad(idEntidad == null ? null : String.valueOf(idEntidad));
		auditoria.setDetalleJson(detalleJson);
		auditoria.setDireccionIp(ip);
		auditoriaRepositorio.save(auditoria);
	}
}
