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
import pe.nawin.enumeracion.TipoMovimientoCredito;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movimientos_creditos")
public class MovimientoCredito {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento_credito")
	private Long idMovimientoCredito;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_venta_credito")
	private VentaCredito ventaCredito;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_consulta")
	private Consulta consulta;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_movimiento", nullable = false, length = 20)
	private TipoMovimientoCredito tipoMovimiento;

	@Column(name = "cantidad", nullable = false)
	private int cantidad;

	@Column(name = "saldo_anterior", nullable = false)
	private int saldoAnterior;

	@Column(name = "saldo_posterior", nullable = false)
	private int saldoPosterior;

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
