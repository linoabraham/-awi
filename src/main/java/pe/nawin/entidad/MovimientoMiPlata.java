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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.TipoMovimientoMiPlata;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movimientos_miplata")
public class MovimientoMiPlata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento_miplata")
	private Long idMovimientoMiPlata;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_billetera_miplata", nullable = false)
	private BilleteraMiPlata billetera;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_solicitud_recarga_miplata")
	private SolicitudRecargaMiPlata solicitudRecarga;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_venta_credito")
	private VentaCredito ventaCredito;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_membresia")
	private Membresia membresia;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_movimiento", nullable = false, length = 30)
	private TipoMovimientoMiPlata tipoMovimiento;

	@Column(name = "monto_soles", nullable = false, precision = 12, scale = 2)
	private BigDecimal montoSoles;

	@Column(name = "saldo_anterior", nullable = false, precision = 12, scale = 2)
	private BigDecimal saldoAnterior;

	@Column(name = "saldo_posterior", nullable = false, precision = 12, scale = 2)
	private BigDecimal saldoPosterior;

	@Column(name = "moneda", nullable = false, length = 3)
	private String moneda = "PEN";

	@Column(name = "descripcion", nullable = false, length = 300)
	private String descripcion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "registrado_por")
	private Usuario registradoPor;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@PrePersist
	void crearFecha() {
		fechaCreacion = LocalDateTime.now();
	}
}
