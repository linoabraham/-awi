package pe.nawin.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.nawin.dto.respuesta.ErrorRespuesta;
import pe.nawin.dto.respuesta.RespuestaApi;
import pe.nawin.entidad.TokenRefresco;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.repositorio.TokenRefrescoRepositorio;

@Component
public class JwtFiltro extends OncePerRequestFilter {

	private final JwtServicio jwtServicio;
	private final DetalleUsuarioServicio detalleUsuarioServicio;
	private final TokenRefrescoRepositorio tokenRefrescoRepositorio;
	private final ObjectMapper objectMapper;

	public JwtFiltro(JwtServicio jwtServicio, DetalleUsuarioServicio detalleUsuarioServicio,
			TokenRefrescoRepositorio tokenRefrescoRepositorio, ObjectMapper objectMapper) {
		this.jwtServicio = jwtServicio;
		this.detalleUsuarioServicio = detalleUsuarioServicio;
		this.tokenRefrescoRepositorio = tokenRefrescoRepositorio;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = header.substring(7);
		try {
			Claims claims = jwtServicio.validar(token);
			// Revocación inmediata: si el token trae sid, la sesión debe seguir vigente.
			// Al cerrar sesión en remoto ese token de refresco queda revocado y aquí se
			// rechaza el acceso ("esta sesión fue cerrada desde otro dispositivo").
			if (claims.get("sid") != null && sesionRevocada(claims)) {
				escribirSesionRevocada(response);
				return;
			}
			String nombreUsuario = claims.get("usuario", String.class);
			if (nombreUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UsuarioPrincipal principal = (UsuarioPrincipal) detalleUsuarioServicio.loadUserByUsername(nombreUsuario);
				UsernamePasswordAuthenticationToken autenticacion =
						new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
				autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(autenticacion);
			}
		} catch (JwtException | IllegalArgumentException ignored) {
			SecurityContextHolder.clearContext();
		}
		filterChain.doFilter(request, response);
	}

	/** true si la sesión referida por el claim sid ya no está activa (revocada, vencida o inexistente). */
	private boolean sesionRevocada(Claims claims) {
		long sid = ((Number) claims.get("sid")).longValue();
		Optional<TokenRefresco> sesion = tokenRefrescoRepositorio.findById(sid);
		if (sesion.isEmpty()) {
			return true;
		}
		TokenRefresco s = sesion.get();
		return s.isRevocado() || s.getFechaExpiracion().isBefore(LocalDateTime.now());
	}

	private void escribirSesionRevocada(HttpServletResponse response) throws IOException {
		SecurityContextHolder.clearContext();
		response.setStatus(CodigoError.DEV_002.estadoHttp().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		RespuestaApi<ErrorRespuesta> cuerpo = RespuestaApi.error(
				CodigoError.DEV_002.mensaje(), ErrorRespuesta.simple(CodigoError.DEV_002.name()));
		objectMapper.writeValue(response.getWriter(), cuerpo);
	}
}
