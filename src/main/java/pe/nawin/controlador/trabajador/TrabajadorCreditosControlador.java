package pe.nawin.controlador.trabajador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.MotivoRequest;
import pe.nawin.dto.solicitud.VentaCreditoRequest;
import pe.nawin.enumeracion.EstadoVentaCredito;
import pe.nawin.servicio.CreditoServicio;

@RestController
@RequestMapping("/api/trabajador")
@PreAuthorize("hasRole('TRABAJADOR')")
@Tag(name = "22 TRABAJADOR - Creditos", description = "Venta de paquetes de creditos y consulta de saldo del cliente.")
public class TrabajadorCreditosControlador {

	private final CreditoServicio creditoServicio;

	public TrabajadorCreditosControlador(CreditoServicio creditoServicio) {
		this.creditoServicio = creditoServicio;
	}

	@PostMapping("/ventas-creditos")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crearVenta(@Valid @RequestBody VentaCreditoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.crearVenta(request)));
	}

	@GetMapping("/ventas-creditos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarVentas(@RequestParam(required = false) Long cliente,
			@RequestParam(required = false) EstadoVentaCredito estado) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.listarVentas(cliente, estado)));
	}

	@GetMapping("/ventas-creditos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verVenta(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.verVenta(id)));
	}

	@PutMapping("/ventas-creditos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarVenta(@PathVariable Long id,
			@Valid @RequestBody VentaCreditoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.actualizarVenta(id, request)));
	}

	@DeleteMapping("/ventas-creditos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> anularVenta(@PathVariable Long id, @RequestBody(required = false) MotivoRequest request) {
		creditoServicio.anularVenta(id, request);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}

	@GetMapping("/clientes/{id}/saldo-creditos")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> saldo(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.saldo(id)));
	}
}
