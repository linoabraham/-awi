package pe.nawin.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.nawin.configuracion.NawinPropiedades;

/**
 * Rate limiting en memoria por IP para los endpoints públicos de autenticación,
 * como freno a fuerza bruta de contraseñas/códigos y a spam de registro y
 * correos. Ventana fija: N solicitudes por IP cada X segundos. Suficiente para
 * un despliegue de una sola instancia; para varias instancias conviene mover el
 * contador a Redis.
 */
@Component
public class LimitadorTasaFiltro extends OncePerRequestFilter {

	private static final List<String> RUTAS_LIMITADAS = List.of(
			"/api/autenticacion/iniciar-sesion",
			"/api/autenticacion/registro-cliente",
			"/api/autenticacion/verificar-correo",
			"/api/autenticacion/reenviar-codigo",
			"/api/autenticacion/recuperar-clave/solicitar",
			"/api/autenticacion/recuperar-clave/confirmar");

	private final NawinPropiedades propiedades;
	private final ConcurrentHashMap<String, Ventana> contadores = new ConcurrentHashMap<>();

	public LimitadorTasaFiltro(NawinPropiedades propiedades) {
		this.propiedades = propiedades;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return RUTAS_LIMITADAS.stream().noneMatch(path::equals);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String clave = ip(request) + "|" + request.getRequestURI();
		long ahora = System.currentTimeMillis();
		long ventanaMs = propiedades.seguridad().rateLimitVentanaSegundos() * 1000;
		int maximo = propiedades.seguridad().rateLimitSolicitudes();

		Ventana ventana = contadores.compute(clave, (k, actual) -> {
			if (actual == null || ahora - actual.inicio >= ventanaMs) {
				return new Ventana(ahora);
			}
			return actual;
		});
		int usados = ventana.contador.incrementAndGet();

		if (contadores.size() > 10_000) {
			contadores.entrySet().removeIf(e -> ahora - e.getValue().inicio >= ventanaMs);
		}

		if (usados > maximo) {
			responderLimite(response);
			return;
		}
		chain.doFilter(request, response);
	}

	private void responderLimite(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(
				"{\"exito\":false,\"mensaje\":\"Demasiadas solicitudes. Espera unos segundos e intenta de nuevo.\","
						+ "\"datos\":{\"codigo\":\"AUTH_005\"}}");
	}

	private String ip(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private static final class Ventana {
		private final long inicio;
		private final AtomicInteger contador = new AtomicInteger(0);

		private Ventana(long inicio) {
			this.inicio = inicio;
		}
	}
}
