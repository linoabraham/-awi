package pe.nawin.dto.solicitud;

public sealed interface SolicitudConsulta permits ConsultaRucRequest, ConsultaDniRequest, ConsultaNumeroRequest,
		ConsultaPlacaRequest, ConsultaNombresRequest, SolicitudConsultaCritica {
}
