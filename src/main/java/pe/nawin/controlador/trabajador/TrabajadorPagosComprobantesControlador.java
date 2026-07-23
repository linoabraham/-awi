package pe.nawin.controlador.trabajador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.*;
import pe.nawin.enumeracion.EstadoPago;
import pe.nawin.servicio.ComprobanteServicio;
import pe.nawin.servicio.PagoServicio;

@RestController
@RequestMapping("/api/trabajador")
@PreAuthorize("hasRole('TRABAJADOR')")
@Tag(name = "23 TRABAJADOR - Pagos y comprobantes", description = "Registro de pagos, emision y descarga de comprobantes.")
public class TrabajadorPagosComprobantesControlador {

	private final PagoServicio pagoServicio;
	private final ComprobanteServicio comprobanteServicio;

	public TrabajadorPagosComprobantesControlador(PagoServicio pagoServicio, ComprobanteServicio comprobanteServicio) {
		this.pagoServicio = pagoServicio;
		this.comprobanteServicio = comprobanteServicio;
	}

	@PostMapping("/pagos")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crearPago(@Valid @RequestBody PagoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(pagoServicio.crear(request)));
	}

	@GetMapping("/pagos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarPagos(@RequestParam(required = false) Long cliente,
			@RequestParam(required = false) EstadoPago estado) {
		return ResponseEntity.ok(RespuestaApi.ok(pagoServicio.listar(cliente, estado)));
	}

	@GetMapping("/pagos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verPago(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(pagoServicio.ver(id)));
	}

	@PutMapping("/pagos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarPago(@PathVariable Long id, @Valid @RequestBody PagoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(pagoServicio.actualizar(id, request)));
	}

	@DeleteMapping("/pagos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> anularPago(@PathVariable Long id, @RequestBody(required = false) MotivoRequest request) {
		pagoServicio.anular(id, request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@PostMapping("/comprobantes")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> emitir(@Valid @RequestBody ComprobanteEmitirRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(comprobanteServicio.emitir(request)));
	}

	@GetMapping("/comprobantes")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarComprobantes(@RequestParam(required = false) Long cliente) {
		return ResponseEntity.ok(RespuestaApi.ok(comprobanteServicio.listar(cliente)));
	}

	@GetMapping("/comprobantes/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verComprobante(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(comprobanteServicio.ver(id)));
	}

	@DeleteMapping("/comprobantes/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> anularComprobante(@PathVariable Long id,
			@RequestBody(required = false) MotivoRequest request) {
		comprobanteServicio.anular(id, request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@GetMapping("/comprobantes/{id}/pdf")
	public ResponseEntity<Resource> pdf(@PathVariable Long id) {
		Resource resource = comprobanteServicio.pdf(id);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comprobante-" + id + ".pdf\"")
				.body(resource);
	}
}
