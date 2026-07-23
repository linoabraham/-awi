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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.EstadoVentaCredito;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ventas_creditos")
public class VentaCredito extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_venta_credito")
	private Long idVentaCredito;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_paquete_credito", nullable = false)
	private PaqueteCredito paqueteCredito;

	@Column(name = "cantidad_paquetes", nullable = false)
	private int cantidadPaquetes;

	@Column(name = "creditos_otorgados", nullable = false)
	private int creditosOtorgados;

	@Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioUnitario;

	@Column(name = "total_soles", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalSoles;

	@Column(name = "fecha_vencimiento_creditos", nullable = false)
	private LocalDate fechaVencimientoCreditos;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoVentaCredito estado = EstadoVentaCredito.PENDIENTE;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creado_por", nullable = false)
	private Usuario creadoPor;
}
