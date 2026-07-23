package pe.nawin.controlador.cliente;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.AdjuntarComprobanteRequest;
import pe.nawin.dto.solicitud.ComprarCreditosMiPlataRequest;
import pe.nawin.dto.solicitud.ComprarPlanMiPlataRequest;
import pe.nawin.dto.solicitud.CrearOrdenPagoRequest;
import pe.nawin.dto.solicitud.SolicitudRecargaMiPlataRequest;
import pe.nawin.servicio.MetodoPagoServicio;
import pe.nawin.servicio.MiPlataServicio;

@RestController
@RequestMapping("/api/cliente/miplata")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "32 CLIENTE - MiPlata", description = "Billetera digital, recargas, compras con saldo y movimientos propios.")
public class ClienteMiPlataControlador {

	private final MiPlataServicio miPlataServicio;
	private final MetodoPagoServicio metodoPagoServicio;

	public ClienteMiPlataControlador(MiPlataServicio miPlataServicio,
			MetodoPagoServicio metodoPagoServicio) {
		this.miPlataServicio = miPlataServicio;
		this.metodoPagoServicio = metodoPagoServicio;
	}

	@GetMapping("/configuracion-yape")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> configuracionYape() {
		return ResponseEntity.ok(RespuestaApi.ok(metodoPagoServicio.yapePublico()));
	}

	@GetMapping("/metodos-pago")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> metodosPago() {
		return ResponseEntity.ok(RespuestaApi.ok(metodoPagoServicio.activosPublico()));
	}

	@PostMapping("/ordenes")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crearOrden(
			@Valid @RequestBody CrearOrdenPagoRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.crearOrden(request)));
	}

	@PostMapping("/ordenes/{id}/comprobante")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> adjuntarComprobante(
			@PathVariable Long id, @Valid @RequestBody AdjuntarComprobanteRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.adjuntarComprobante(id, request)));
	}

	@GetMapping("/billetera")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> billetera() {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.miBilletera()));
	}

	@GetMapping("/movimientos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> movimientos() {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.misMovimientos()));
	}

	@PostMapping("/recargas")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> solicitarRecarga(@Valid @RequestBody SolicitudRecargaMiPlataRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String claveIdempotencia) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.solicitarRecarga(request, claveIdempotencia)));
	}

	@GetMapping("/recargas")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> recargas() {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.misSolicitudes()));
	}

	/** Apelar una recarga rechazada (pedir reconsideración). */
	@PostMapping("/recargas/{id}/apelar")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> apelarRecarga(
			@PathVariable Long id,
			@Valid @RequestBody pe.nawin.dto.solicitud.ApelarRecargaMiPlataRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.apelarRecarga(id, request)));
	}

	@GetMapping("/paquetes-creditos")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> paquetesCreditos() {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.paquetesCreditosDisponibles()));
	}

	@GetMapping("/planes")
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> planes() {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.planesDisponibles()));
	}

	@PostMapping("/compras/creditos")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> comprarCreditos(@Valid @RequestBody ComprarCreditosMiPlataRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String claveIdempotencia) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.comprarCreditos(request, claveIdempotencia)));
	}

	@PostMapping("/compras/planes")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> comprarPlan(@Valid @RequestBody ComprarPlanMiPlataRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String claveIdempotencia) {
		return ResponseEntity.ok(RespuestaApi.ok(miPlataServicio.comprarPlan(request, claveIdempotencia)));
	}
}
