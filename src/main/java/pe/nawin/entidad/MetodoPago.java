package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Método de pago configurable desde el panel (Yape, Izipay, etc.). Para
 * billeteras/QR usa número, titular y QR; para pasarelas usa credenciales_json.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "metodos_pago")
public class MetodoPago extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_metodo_pago")
	private Long idMetodoPago;

	@Column(name = "codigo", nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	// BILLETERA_QR (Yape/Plin) o PASARELA (Izipay/Culqi/...).
	@Column(name = "tipo", nullable = false, length = 30)
	private String tipo;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	@Column(name = "orden", nullable = false)
	private int orden;

	@Column(name = "numero", length = 20)
	private String numero;

	@Column(name = "titular", length = 120)
	private String titular;

	@Lob
	@Column(name = "qr_base64", columnDefinition = "LONGTEXT")
	private String qrBase64;

	@Lob
	@Column(name = "credenciales_json", columnDefinition = "LONGTEXT")
	private String credencialesJson;

	@Column(name = "instrucciones", length = 500)
	private String instrucciones;

	@Column(name = "validacion_texto", nullable = false, length = 60)
	private String validacionTexto = "5 a 10 min";

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actualizado_por")
	private Usuario actualizadoPor;
}
