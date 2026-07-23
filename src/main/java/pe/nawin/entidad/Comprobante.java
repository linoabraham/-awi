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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.EstadoComprobante;
import pe.nawin.enumeracion.TipoComprobante;
import pe.nawin.enumeracion.TipoDocumento;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comprobantes", uniqueConstraints = @UniqueConstraint(name = "uk_comprobantes_serie_numero", columnNames = {"serie", "numero"}))
public class Comprobante {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_comprobante")
	private Long idComprobante;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_pago", nullable = false, unique = true)
	private Pago pago;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_serie_comprobante", nullable = false)
	private SerieComprobante serieComprobante;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_comprobante", nullable = false, length = 30)
	private TipoComprobante tipoComprobante;

	@Column(name = "serie", nullable = false, length = 10)
	private String serie;

	@Column(name = "numero", nullable = false)
	private long numero;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento_cliente", nullable = false, length = 10)
	private TipoDocumento tipoDocumentoCliente;

	@Column(name = "numero_documento_cliente", nullable = false, length = 11)
	private String numeroDocumentoCliente;

	@Column(name = "nombre_cliente", nullable = false, length = 200)
	private String nombreCliente;

	@Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "igv", nullable = false, precision = 12, scale = 2)
	private BigDecimal igv;

	@Column(name = "total", nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoComprobante estado = EstadoComprobante.EMITIDO;

	@Column(name = "ruta_pdf", nullable = false, length = 500)
	private String rutaPdf;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "emitido_por", nullable = false)
	private Usuario emitidoPor;

	@Column(name = "fecha_emision", nullable = false)
	private LocalDateTime fechaEmision;

	@Column(name = "fecha_anulacion")
	private LocalDateTime fechaAnulacion;

	@Column(name = "motivo_anulacion", length = 300)
	private String motivoAnulacion;
}
