package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "saldos_creditos")
public class SaldoCredito {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_saldo_credito")
	private Long idSaldoCredito;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false, unique = true)
	private Cliente cliente;

	@Column(name = "creditos_disponibles", nullable = false)
	private int creditosDisponibles;

	@Column(name = "creditos_reservados", nullable = false)
	private int creditosReservados;

	@Column(name = "creditos_consumidos", nullable = false)
	private int creditosConsumidos;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@Column(name = "fecha_actualizacion", nullable = false)
	private LocalDateTime fechaActualizacion;

	@PrePersist
	@PreUpdate
	void actualizarFecha() {
		fechaActualizacion = LocalDateTime.now();
	}
}
