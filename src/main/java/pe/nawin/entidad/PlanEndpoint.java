package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.ModalidadAccesoEndpoint;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "planes_endpoints", uniqueConstraints = @UniqueConstraint(name = "uk_planes_endpoints_plan_endpoint", columnNames = {"id_plan", "id_endpoint"}))
public class PlanEndpoint extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_plan_endpoint")
	private Long idPlanEndpoint;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_plan", nullable = false)
	private Plan plan;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_endpoint", nullable = false)
	private EndpointBusqueda endpoint;

	@Column(name = "habilitado", nullable = false)
	private boolean habilitado;

	@Enumerated(EnumType.STRING)
	@Column(name = "modalidad_acceso", nullable = false, length = 30)
	private ModalidadAccesoEndpoint modalidadAcceso;

	@Column(name = "limite_diario")
	private Integer limiteDiario;

	@Column(name = "limite_ciclo")
	private Integer limiteCiclo;

	@Column(name = "costo_creditos_cliente")
	private Integer costoCreditosCliente;

	@Column(name = "requiere_mfa", nullable = false)
	private boolean requiereMfa;

	@Column(name = "requiere_finalidad", nullable = false)
	private boolean requiereFinalidad;

	@Column(name = "requiere_justificacion", nullable = false)
	private boolean requiereJustificacion;

	@Column(name = "permite_exportar", nullable = false)
	private boolean permiteExportar;

	@Column(name = "dias_retencion", nullable = false)
	private int diasRetencion;
}
