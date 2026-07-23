package pe.nawin.controlador.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.dto.solicitud.UsuarioActualizarRequest;
import pe.nawin.dto.solicitud.UsuarioCrearRequest;
import pe.nawin.servicio.UsuarioServicio;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "10 ADMIN - Usuarios", description = "Usuarios locales, roles, activacion, bloqueo y desactivacion.")
public class AdminUsuariosControlador {

	private final UsuarioServicio usuarioServicio;

	public AdminUsuariosControlador(UsuarioServicio usuarioServicio) {
		this.usuarioServicio = usuarioServicio;
	}

	@PostMapping
	public ResponseEntity<RespuestaApi<Map<String, Object>>> crear(@Valid @RequestBody UsuarioCrearRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(usuarioServicio.crear(request)));
	}

	@GetMapping
	public ResponseEntity<RespuestaApi<List<Map<String, Object>>>> listar() {
		return ResponseEntity.ok(RespuestaApi.ok(usuarioServicio.listar()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> ver(@PathVariable Long id) {
		return ResponseEntity.ok(RespuestaApi.ok(usuarioServicio.ver(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> actualizar(@PathVariable Long id,
			@Valid @RequestBody UsuarioActualizarRequest request) {
		return ResponseEntity.ok(RespuestaApi.ok(usuarioServicio.actualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<RespuestaApi<Map<String, Object>>> desactivar(@PathVariable Long id) {
		usuarioServicio.desactivar(id);
		return ResponseEntity.ok(RespuestaApi.ok(Map.of()));
	}
}
