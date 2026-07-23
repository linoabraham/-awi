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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.EstadoSolicitudRecargaMiPlata;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitudes_recarga_miplata")
public class SolicitudRecargaMiPlata extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_solicitud_recarga_miplata")
	private Long idSolicitudRecargaMiPlata;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@Column(name = "monto_soles", nullable = false, precision = 12, scale = 2)
	private BigDecimal montoSoles;

	@Column(name = "comprobante_base64", columnDefinition = "LONGTEXT")
	private String comprobanteBase64;

	@Column(name = "codigo_orden", length = 30, unique = true)
	private String codigoOrden;

	@Column(name = "codigo_operacion", length = 60)
	private String codigoOperacion;

	@Column(name = "concepto", length = 200)
	private String concepto;

	@Column(name = "id_plan")
	private Long idPlan;

	@Column(name = "id_paquete_credito")
	private Long idPaqueteCredito;

	@Column(name = "fecha_expiracion")
	private LocalDateTime fechaExpiracion;

	@Column(name = "codigo_referido_ingresado", length = 20)
	private String codigoReferidoIngresado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_cliente_referido")
	private Cliente clienteReferido;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoSolicitudRecargaMiPlata estado = EstadoSolicitudRecargaMiPlata.PENDIENTE;

	@Column(name = "motivo_rechazo", length = 500)
	private String motivoRechazo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "revisado_por")
	private Usuario revisadoPor;

	@Column(name = "fecha_revision")
	private LocalDateTime fechaRevision;

	@Column(name = "clave_idempotencia", length = 80, unique = true)
	private String claveIdempotencia;

	/** Apelación del cliente cuando la recarga fue rechazada (mensaje de reconsideración). */
	@Column(name = "apelacion", length = 500)
	private String apelacion;

	@Column(name = "fecha_apelacion")
	private LocalDateTime fechaApelacion;

	@Column(name = "apelacion_respondida", nullable = false)
	private boolean apelacionRespondida;
}
