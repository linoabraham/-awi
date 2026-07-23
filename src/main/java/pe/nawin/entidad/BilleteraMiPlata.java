package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "billeteras_miplata")
public class BilleteraMiPlata extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_billetera_miplata")
	private Long idBilleteraMiPlata;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false, unique = true)
	private Cliente cliente;

	@Column(name = "saldo_disponible", nullable = false, precision = 12, scale = 2)
	private BigDecimal saldoDisponible = BigDecimal.ZERO;

	@Column(name = "moneda", nullable = false, length = 3)
	private String moneda = "PEN";

	@Version
	@Column(name = "version", nullable = false)
	private Long version;
}
