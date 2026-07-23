package pe.nawin.configuracion;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.nawin.seguridad.JwtFiltro;
import pe.nawin.seguridad.LimitadorTasaFiltro;

@Configuration
@EnableMethodSecurity
public class SeguridadConfiguracion {

	private final NawinPropiedades propiedades;
	private final JwtFiltro jwtFiltro;
	private final LimitadorTasaFiltro limitadorTasaFiltro;
	private final UserDetailsService userDetailsService;

	public SeguridadConfiguracion(NawinPropiedades propiedades, JwtFiltro jwtFiltro,
			LimitadorTasaFiltro limitadorTasaFiltro, UserDetailsService userDetailsService) {
		this.propiedades = propiedades;
		this.jwtFiltro = jwtFiltro;
		this.limitadorTasaFiltro = limitadorTasaFiltro;
		this.userDetailsService = userDetailsService;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers(HttpMethod.POST,
								"/api/autenticacion/iniciar-sesion",
								"/api/autenticacion/dispositivos-en-espera",
								"/api/autenticacion/dispositivos-en-espera/cerrar",
								"/api/autenticacion/renovar-token",
								"/api/autenticacion/registro-cliente",
								"/api/autenticacion/verificar-correo",
								"/api/autenticacion/reenviar-codigo",
								"/api/autenticacion/recuperar-clave/solicitar",
								"/api/autenticacion/recuperar-clave/confirmar").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(limitadorTasaFiltro, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origenes = Arrays.stream(propiedades.frontendOrigin().split(","))
				.map(String::trim)
				.filter(o -> !o.isEmpty())
				.toList();
		config.setAllowedOrigins(origenes);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
		config.setExposedHeaders(List.of("Content-Disposition"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
