package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.ConsultaRespuesta;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.MotivoRequest;
import pe.nawin.dto.solicitud.NotificacionRequest;
import pe.nawin.entidad.Auditoria;
import pe.nawin.enumeracion.EstadoNotificacion;
import pe.nawin.repositorio.AuditoriaRepositorio;
import pe.nawin.servicio.ConsultaServicio;
import pe.nawin.servicio.NotificacionServicio;
import pe.nawin.servicio.ReporteServicio;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "16 ADMIN - Operacion", description = "Historial global, notificaciones, reportes y auditoria.")
public class AdminOperacionControlador {

	private final ConsultaServicio consultaServicio;
	private final NotificacionServicio notificacionServicio;
	private final ReporteServicio reporteServicio;
	private final AuditoriaRepositorio auditoriaRepositorio;

	public AdminOperacionControlador(ConsultaServicio consultaServicio, NotificacionServicio notificacionServicio,
			ReporteServicio reporteServicio, AuditoriaRepositorio auditoriaRepositorio) {
		this.consultaServicio = consultaServicio;
		this.notificacionServicio = notificacionServicio;
		this.reporteServicio = reporteServicio;
		this.auditoriaRepositorio = auditoriaRepositorio;
	}

	@GetMapping("/consultas")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> consultas() {
		return ResponseEntity.ok(RespuestaApi.ok(consultaServicio.historialGlobal()));
	}

	@GetMapping("/consultas/{codigo}")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> detalleConsulta(@PathVariable String codigo) {
		return ResponseEntity.ok(RespuestaApi.ok(consultaServicio.detalleGlobal(codigo)));
	}

	/** Ranking de consumo por cliente (para vigilar abuso de ilimitados). dias=1 por defecto (hoy). */
	@GetMapping("/reportes/consumo")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> consumoPorCliente(
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int dias) {
		return ResponseEntity.ok(RespuestaApi.ok(consultaServicio.consumoPorCliente(dias)));
	}

	@PostMapping("/notificaciones")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crearNotificacion(@Valid @RequestBody NotificacionRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(notificacionServicio.crear(request)));
	}

	@GetMapping("/notificaciones")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarNotificaciones(@RequestParam(required = false) Long cliente,
			@RequestParam(required = false) EstadoNotificacion estado) {
		return ResponseEntity.ok(RespuestaApi.ok(notificacionServicio.listar(cliente, estado)));
	}

	@GetMapping("/notificaciones/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verNotificacion(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(notificacionServicio.ver(id)));
	}

	@PutMapping("/notificaciones/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarNotificacion(@PathVariable Long id,
			@Valid @RequestBody NotificacionRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(notificacionServicio.actualizar(id, request)));
	}

	@DeleteMapping("/notificaciones/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> cancelarNotificacion(@PathVariable Long id,
			@RequestBody(required = false) MotivoRequest request) {
		notificacionServicio.cancelar(id, request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@GetMapping("/reportes/resumen")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> resumen() {
		return ResponseEntity.ok(RespuestaApi.ok(reporteServicio.resumen()));
	}

	@GetMapping({"/reportes/ventas", "/reportes/membresias", "/reportes/consultas"})
	public ResponseEntity<RespuestaApi<Map<String, Object>>> reporteGenerico() {
		return ResponseEntity.ok(RespuestaApi.ok(reporteServicio.resumen()));
	}

	@GetMapping("/auditorias")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> auditorias(@RequestParam(required = false) String entidad) {
		List<Auditoria> auditorias = entidad == null ? auditoriaRepositorio.findAll() : auditoriaRepositorio.findByEntidad(entidad);
		return ResponseEntity.ok(RespuestaApi.ok(auditorias.stream()
				.map(a -> {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("idAuditoria", a.getIdAuditoria());
					m.put("accion", a.getAccion());
					m.put("entidad", a.getEntidad());
					m.put("idEntidad", a.getIdEntidad());
					m.put("fechaCreacion", a.getFechaCreacion());
					return m;
				})
				.toList()));
	}
}
