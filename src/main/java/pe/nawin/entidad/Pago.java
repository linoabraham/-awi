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
import pe.nawin.enumeracion.EstadoPago;
import pe.nawin.enumeracion.MedioPago;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pagos")
public class Pago extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pago")
	private Long idPago;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_membresia")
	private Membresia membresia;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_venta_credito")
	private VentaCredito ventaCredito;

	@Column(name = "monto_soles", nullable = false, precision = 12, scale = 2)
	private BigDecimal montoSoles;

	@Enumerated(EnumType.STRING)
	@Column(name = "medio_pago", nullable = false, length = 30)
	private MedioPago medioPago;

	@Column(name = "numero_operacion", length = 100)
	private String numeroOperacion;

	@Column(name = "fecha_pago", nullable = false)
	private LocalDateTime fechaPago;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoPago estado = EstadoPago.PENDIENTE;

	@Column(name = "observacion", length = 500)
	private String observacion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registrado_por", nullable = false)
	private Usuario registradoPor;

	@Column(name = "clave_idempotencia", length = 80, unique = true)
	private String claveIdempotencia;
}
