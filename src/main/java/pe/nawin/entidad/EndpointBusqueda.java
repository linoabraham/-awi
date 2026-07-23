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
import pe.nawin.enumeracion.CodigoEndpoint;
import pe.nawin.enumeracion.TipoConsumoProveedor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "endpoints_busqueda")
public class EndpointBusqueda extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_endpoint")
	private Long idEndpoint;

	@Enumerated(EnumType.STRING)
	@Column(name = "codigo", nullable = false, unique = true, length = 40)
	private CodigoEndpoint codigo;

	@Column(name = "nombre", nullable = false, length = 120)
	private String nombre;

	@Column(name = "descripcion", length = 500)
	private String descripcion;

	@Column(name = "metodo_proveedor", nullable = false, length = 10)
	private String metodoProveedor;

	@Column(name = "ruta_proveedor", nullable = false, length = 300)
	private String rutaProveedor;

	@Column(name = "parametro_principal", nullable = false, length = 50)
	private String parametroPrincipal;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_consumo_proveedor", nullable = false, length = 20)
	private TipoConsumoProveedor tipoConsumoProveedor;

	@Column(name = "costo_proveedor", nullable = false)
	private int costoProveedor;

	/** Costo en créditos que se cobra al cliente free (sin membresía). Si es null, usa costoProveedor. */
	@Column(name = "costo_creditos_cliente")
	private Integer costoCreditosCliente;

	@Column(name = "es_critico", nullable = false)
	private boolean esCritico;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;
}
