package pe.nawin.controlador.cliente;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.nawin.dto.respuesta.ConsultaRespuesta;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.*;
import pe.nawin.enumeracion.CodigoEndpoint;
import pe.nawin.servicio.ConsultaServicio;

@RestController
@RequestMapping("/api/cliente/consultas")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "31 CLIENTE - Consultas", description = "18 busquedas internas con membresia, cuota/creditos, idempotencia y MFA en endpoints criticos.")
public class ClienteConsultasControlador {

	private final ConsultaServicio consultaServicio;

	public ClienteConsultasControlador(ConsultaServicio consultaServicio) {
		this.consultaServicio = consultaServicio;
	}

	@PostMapping("/ruc")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> ruc(@Valid @RequestBody ConsultaRucRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.RUC, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/dni-basico")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> dniBasico(@Valid @RequestBody ConsultaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DNI_BASICO, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/dniv")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> dniv(@Valid @RequestBody ConsultaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DNIV, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/dnivel")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> dnivel(@Valid @RequestBody ConsultaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DNIVEL, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/dni-completo")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> dniCompleto(@Valid @RequestBody ConsultaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DNI_COMPLETO, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/dnit")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> dnit(@Valid @RequestBody ConsultaCriticaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DNIT, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/nombres")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> nombres(@Valid @RequestBody ConsultaNombresRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.NM, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/familiares")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> familiares(@Valid @RequestBody ConsultaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.AG, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/telefonos-por-dni")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> telefonosPorDni(@Valid @RequestBody ConsultaCriticaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.TELP_DNI, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/titular-celular")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> titularCelular(@Valid @RequestBody ConsultaCriticaNumeroRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.TELP_CEL, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/denuncias-resumen")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> denunciasResumen(@Valid @RequestBody ConsultaCriticaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DEN, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/denuncias-pdf")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> denunciasPdf(@Valid @RequestBody ConsultaCriticaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DENUNCIAS, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/requisitorias")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> requisitorias(@Valid @RequestBody ConsultaCriticaDniRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.RQH, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping(value = "/facial-top", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> facialTop(@RequestPart("image_facial") MultipartFile imageFacial,
			@RequestParam(required = false) String finalidad, @RequestParam(required = false) String justificacion,
			@RequestParam(required = false) String codigoMfa,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutarFacial(imageFacial, finalidad, justificacion, codigoMfa, ip(servlet),
				servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/placa")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> placa(@Valid @RequestBody ConsultaPlacaRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.PLA, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/denuncias-placa")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> denunciasPlaca(@Valid @RequestBody ConsultaCriticaPlacaRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.DENPLA, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/vehiculo-propietarios")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> vehiculoPropietarios(@Valid @RequestBody ConsultaPlacaRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.PLAT, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@PostMapping("/historial-soat")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> historialSoat(@Valid @RequestBody ConsultaPlacaRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, HttpServletRequest servlet) {
		return ok(consultaServicio.ejecutar(CodigoEndpoint.HSOAT, request, ip(servlet), servlet.getHeader("User-Agent"), idempotencyKey));
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> historial() {
		return ResponseEntity.ok(RespuestaApi.ok(consultaServicio.historialCliente()));
	}

	@GetMapping("/{codigo}")
	public ResponseEntity<RespuestaApi<ConsultaRespuesta>> detalle(@PathVariable String codigo) {
		return ok(consultaServicio.detalleCliente(codigo));
	}

	@DeleteMapping("/{codigo}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> ocultar(@PathVariable String codigo) {
		consultaServicio.ocultarCliente(codigo);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@GetMapping("/{codigo}/archivos/{id}")
	public ResponseEntity<Resource> archivo(@PathVariable String codigo, @PathVariable Long id) {
		Resource resource = consultaServicio.archivoCliente(codigo, id);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	private ResponseEntity<RespuestaApi<ConsultaRespuesta>> ok(ConsultaRespuesta respuesta) {
		return ResponseEntity.ok(RespuestaApi.ok(respuesta));
	}

	private String ip(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
	}
}
