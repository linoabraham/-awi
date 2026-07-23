package pe.nawin.configuracion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguracion {

	@Bean
	OpenAPI openAPI() {
		String esquema = "bearer-jwt";
		return new OpenAPI()
				.info(new Info()
						.title("NAWIN Backend")
						.version("1.0.0")
						.description("""
								API backend de NAWIN para autenticacion, administracion, operacion de trabajadores y portal de clientes.
								El token del proveedor CODART no aparece en Swagger ni en respuestas; se configura solo como variable de entorno CODART_API_TOKEN.
								Use el selector de grupos de Swagger para revisar la API por rol y modulo.
								"""))
				.tags(List.of(
						new Tag().name("00 Autenticacion").description("Inicio de sesion, refresh token, cierre de sesion, cuenta actual, cambio de clave y MFA."),
						new Tag().name("10 ADMIN - Usuarios").description("Gestion de usuarios locales y roles fijos."),
						new Tag().name("11 ADMIN - Clientes").description("CRUD administrativo de clientes DNI/RUC."),
						new Tag().name("12 ADMIN - Catalogo y planes").description("Endpoints de busqueda, planes y reglas plan-endpoint."),
						new Tag().name("13 ADMIN - Membresias").description("Membresias, activacion, renovacion, suspension y accesos efectivos."),
						new Tag().name("14 ADMIN - Creditos").description("Paquetes, ventas, saldos, movimientos y ajustes de creditos."),
						new Tag().name("15 ADMIN - Pagos y comprobantes").description("Pagos, series, emision, anulacion y descarga de comprobantes."),
						new Tag().name("16 ADMIN - Operacion").description("Consultas globales, notificaciones, reportes y auditoria."),
						new Tag().name("17 ADMIN - MiPlata").description("Revision de recargas, billeteras y movimientos MiPlata."),
						new Tag().name("20 TRABAJADOR - Clientes").description("Operacion de clientes para trabajadores."),
						new Tag().name("21 TRABAJADOR - Membresias").description("Creacion, activacion y renovacion de membresias."),
						new Tag().name("22 TRABAJADOR - Creditos").description("Ventas de creditos y consulta de saldos."),
						new Tag().name("23 TRABAJADOR - Pagos y comprobantes").description("Cobros, comprobantes y descarga PDF."),
						new Tag().name("24 TRABAJADOR - Renovaciones").description("Clientes por vencer, vencidos y notificaciones de renovacion."),
						new Tag().name("25 TRABAJADOR - MiPlata").description("Revision operativa de recargas y consulta de billeteras MiPlata."),
						new Tag().name("30 CLIENTE - Inicio y perfil").description("Inicio, perfil, membresias, creditos, comprobantes y notificaciones propias."),
						new Tag().name("31 CLIENTE - Consultas").description("18 busquedas internas habilitadas por membresia, historial y archivos."),
						new Tag().name("32 CLIENTE - MiPlata").description("Billetera digital, recargas, compras con saldo y movimientos propios.")))
				.components(new Components().addSecuritySchemes(esquema,
						new SecurityScheme()
								.name(esquema)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(esquema));
	}

	@Bean
	GroupedOpenApi autenticacionApi() {
		return GroupedOpenApi.builder()
				.group("00-autenticacion")
				.pathsToMatch("/api/autenticacion/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminUsuariosApi() {
		return GroupedOpenApi.builder()
				.group("10-admin-usuarios")
				.pathsToMatch("/api/admin/usuarios/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminClientesApi() {
		return GroupedOpenApi.builder()
				.group("11-admin-clientes")
				.pathsToMatch("/api/admin/clientes/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminCatalogoPlanesApi() {
		return GroupedOpenApi.builder()
				.group("12-admin-catalogo-planes")
				.pathsToMatch("/api/admin/planes/**", "/api/admin/endpoints-busqueda/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminMembresiasApi() {
		return GroupedOpenApi.builder()
				.group("13-admin-membresias")
				.pathsToMatch("/api/admin/membresias/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminCreditosApi() {
		return GroupedOpenApi.builder()
				.group("14-admin-creditos")
				.pathsToMatch("/api/admin/paquetes-creditos/**", "/api/admin/ventas-creditos/**",
						"/api/admin/clientes/*/saldo-creditos", "/api/admin/clientes/*/movimientos-creditos",
						"/api/admin/clientes/*/ajustes-creditos")
				.build();
	}

	@Bean
	GroupedOpenApi adminPagosComprobantesApi() {
		return GroupedOpenApi.builder()
				.group("15-admin-pagos-comprobantes")
				.pathsToMatch("/api/admin/pagos/**", "/api/admin/series-comprobantes/**", "/api/admin/comprobantes/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminOperacionApi() {
		return GroupedOpenApi.builder()
				.group("16-admin-operacion")
				.pathsToMatch("/api/admin/consultas/**", "/api/admin/notificaciones/**",
						"/api/admin/reportes/**", "/api/admin/auditorias/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminMiPlataApi() {
		return GroupedOpenApi.builder()
				.group("17-admin-miplata")
				.pathsToMatch("/api/admin/miplata/**")
				.build();
	}

	@Bean
	GroupedOpenApi trabajadorApi() {
		return GroupedOpenApi.builder()
				.group("20-trabajador")
				.pathsToMatch("/api/trabajador/**")
				.pathsToExclude("/api/trabajador/miplata/**")
				.build();
	}

	@Bean
	GroupedOpenApi trabajadorMiPlataApi() {
		return GroupedOpenApi.builder()
				.group("25-trabajador-miplata")
				.pathsToMatch("/api/trabajador/miplata/**")
				.build();
	}

	@Bean
	GroupedOpenApi clientePortalApi() {
		return GroupedOpenApi.builder()
				.group("30-cliente-portal")
				.pathsToMatch("/api/cliente/**")
				.pathsToExclude("/api/cliente/consultas/**", "/api/cliente/miplata/**")
				.build();
	}

	@Bean
	GroupedOpenApi clienteConsultasApi() {
		return GroupedOpenApi.builder()
				.group("31-cliente-consultas")
				.pathsToMatch("/api/cliente/consultas/**")
				.build();
	}

	@Bean
	GroupedOpenApi clienteMiPlataApi() {
		return GroupedOpenApi.builder()
				.group("32-cliente-miplata")
				.pathsToMatch("/api/cliente/miplata/**")
				.build();
	}
}
