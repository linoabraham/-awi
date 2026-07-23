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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.EstadoBonoReferente;

/**
 * Registro del canje de un código de referido: quién lo canjeó (invitado),
 * de quién es el código (referente) y los créditos otorgados a cada uno.
 * Sirve para trazabilidad y para las estadísticas del centro de referidos.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "canjes_referido")
public class CanjeReferido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_canje_referido")
	private Long idCanjeReferido;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente_referente", nullable = false)
	private Cliente referente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente_invitado", nullable = false, unique = true)
	private Cliente invitado;

	@Column(name = "codigo_referido", nullable = false, length = 20)
	private String codigoReferido;

	@Column(name = "creditos_invitado", nullable = false)
	private int creditosInvitado;

	@Column(name = "creditos_referente", nullable = false)
	private int creditosReferente;

	@Column(name = "fecha_canje", nullable = false)
	private LocalDateTime fechaCanje;

	/** Dispositivo desde el que se canjeó (1 canje por dispositivo, anti-granjas). */
	@Column(name = "installation_id", length = 64)
	private String installationId;

	@Column(name = "direccion_ip", length = 45)
	private String direccionIp;

	/** El bono del referente queda pendiente hasta la 1.ª consulta exitosa del invitado. */
	@Enumerated(EnumType.STRING)
	@Column(name = "estado_bono_referente", nullable = false, length = 20)
	private EstadoBonoReferente estadoBonoReferente = EstadoBonoReferente.ACREDITADO;

	@Column(name = "fecha_acreditacion")
	private LocalDateTime fechaAcreditacion;

	@PrePersist
	void crearFecha() {
		fechaCanje = LocalDateTime.now();
	}
}
