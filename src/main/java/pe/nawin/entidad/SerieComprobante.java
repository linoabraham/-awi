package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.TipoComprobante;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "series_comprobantes")
public class SerieComprobante extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_serie_comprobante")
	private Long idSerieComprobante;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_comprobante", nullable = false, length = 30)
	private TipoComprobante tipoComprobante;

	@Column(name = "serie", nullable = false, unique = true, length = 10)
	private String serie;

	@Column(name = "ultimo_numero", nullable = false)
	private long ultimoNumero;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;
}
