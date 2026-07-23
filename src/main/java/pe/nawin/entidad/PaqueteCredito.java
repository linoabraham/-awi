package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paquetes_creditos")
public class PaqueteCredito extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_paquete_credito")
	private Long idPaqueteCredito;

	@Column(name = "codigo", nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "cantidad_creditos", nullable = false)
	private int cantidadCreditos;

	@Column(name = "precio_soles", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioSoles;

	@Column(name = "dias_vigencia", nullable = false)
	private int diasVigencia;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;
}
