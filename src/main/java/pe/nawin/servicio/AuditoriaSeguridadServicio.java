package pe.nawin.servicio;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.entidad.EventoSeguridad;
import pe.nawin.enumeracion.TipoEventoSeguridad;
import pe.nawin.repositorio.EventoSeguridadRepositorio;

/**
 * Registra eventos de auditoría de seguridad. El registro es "best-effort":
 * corre en su propia transacción ({@code REQUIRES_NEW}) y captura cualquier
 * error, de modo que un fallo al auditar nunca rompe el flujo principal (login,
 * cambio de clave, etc.).
 */
@Service
public class AuditoriaSeguridadServicio {

	private static final Logger log = LoggerFactory.getLogger(AuditoriaSeguridadServicio.class);

	private final EventoSeguridadRepositorio repositorio;

	public AuditoriaSeguridadServicio(EventoSeguridadRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrar(Long idUsuario, TipoEventoSeguridad tipo, String installationId, String ip, String detalle) {
		if (idUsuario == null || tipo == null) {
			return;
		}
		try {
			EventoSeguridad evento = new EventoSeguridad();
			evento.setIdUsuario(idUsuario);
			evento.setTipo(tipo);
			evento.setInstallationId(installationId);
			evento.setDireccionIp(ip);
			evento.setDetalle(detalle);
			repositorio.save(evento);
		} catch (RuntimeException ex) {
			log.warn("No se pudo registrar el evento de seguridad {} del usuario {}", tipo, idUsuario, ex);
		}
	}

	public void registrar(Long idUsuario, TipoEventoSeguridad tipo, String installationId, String ip) {
		registrar(idUsuario, tipo, installationId, ip, null);
	}

	@Transactional(readOnly = true)
	public List<EventoSeguridad> ultimosEventos(Long idUsuario, int limite) {
		return repositorio.findByIdUsuarioOrderByFechaCreacionDesc(idUsuario, PageRequest.of(0, limite));
	}
}
