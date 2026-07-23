package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.*;
import pe.nawin.enumeracion.EstadoVentaCredito;
import pe.nawin.servicio.CreditoServicio;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "14 ADMIN - Creditos", description = "Paquetes, ventas, saldos, movimientos y ajustes de creditos.")
public class AdminCreditosControlador {

	private final CreditoServicio creditoServicio;

	public AdminCreditosControlador(CreditoServicio creditoServicio) {
		this.creditoServicio = creditoServicio;
	}

	@PostMapping("/paquetes-creditos")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crearPaquete(@Valid @RequestBody PaqueteCreditoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.crearPaquete(request)));
	}

	@GetMapping("/paquetes-creditos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listarPaquetes(@RequestParam(required = false) Boolean activo) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.listarPaquetes(activo)));
	}

	@GetMapping("/paquetes-creditos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> verPaquete(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.verPaquete(id)));
	}

	@PutMapping("/paquetes-creditos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizarPaquete(@PathVariable Long id,
			@Valid @RequestBody PaqueteCreditoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.actualizarPaquete(id, request)));
	}

	@DeleteMapping("/paquetes-creditos/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> inactivarPaquete(@PathVariable Long id) {
		creditoServicio.inactivarPaquete(id);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
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

	@GetMapping("/clientes/{id}/movimientos-creditos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> movimientos(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.movimientos(id)));
	}

	@PostMapping("/clientes/{id}/ajustes-creditos")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> ajustar(@PathVariable Long id,
			@Valid @RequestBody AjusteCreditoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(creditoServicio.ajustar(id, request)));
	}
}
