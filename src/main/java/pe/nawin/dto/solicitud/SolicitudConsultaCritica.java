package pe.nawin.dto.solicitud;

public sealed interface SolicitudConsultaCritica extends SolicitudConsulta permits ConsultaCriticaDniRequest,
		ConsultaCriticaNumeroRequest, ConsultaCriticaPlacaRequest {
	String finalidad();

	String justificacion();

	String codigoMfa();
}
