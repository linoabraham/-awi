package pe.nawin.dto.solicitud;

import jakarta.validation.constraints.NotBlank;

/**
 * Búsqueda por nombres (NM): el mínimo es un nombre y el apellido paterno;
 * el apellido materno es opcional (la API upstream admite ap2 vacío).
 */
public record ConsultaNombresRequest(
		@NotBlank String nombres,
		@NotBlank String apellidoPaterno,
		String apellidoMaterno
) implements SolicitudConsulta {
}
