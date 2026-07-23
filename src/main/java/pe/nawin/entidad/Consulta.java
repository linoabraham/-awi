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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.EstadoConsulta;
import pe.nawin.enumeracion.OrigenConsumoConsulta;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consultas")
public class Consulta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_consulta")
	private Long idConsulta;

	@Column(name = "codigo_consulta", nullable = false, unique = true, length = 36)
	private String codigoConsulta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false)
	private Usuario usuario;

	// Nullable: una consulta SOLO por créditos (cliente free) no tiene membresía.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_membresia")
	private Membresia membresia;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_membresia_endpoint")
	private MembresiaEndpoint membresiaEndpoint;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_endpoint", nullable = false)
	private EndpointBusqueda endpoint;

	@Column(name = "parametro_cifrado", nullable = false, columnDefinition = "LONGTEXT")
	private String parametroCifrado;

	@Column(name = "parametro_mascara", nullable = false, length = 100)
	private String parametroMascara;

	@Column(name = "finalidad", length = 200)
	private String finalidad;

	@Column(name = "justificacion", length = 500)
	private String justificacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "origen_consumo", nullable = false, length = 20)
	private OrigenConsumoConsulta origenConsumo;

	@Column(name = "cantidad_consumida", nullable = false)
	private int cantidadConsumida;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 30)
	private EstadoConsulta estado;

	@Column(name = "codigo_http_proveedor")
	private Integer codigoHttpProveedor;

	@Column(name = "duracion_milisegundos")
	private Long duracionMilisegundos;

	@Column(name = "mensaje_error", length = 500)
	private String mensajeError;

	@Column(name = "direccion_ip", nullable = false, length = 45)
	private String direccionIp;

	@Column(name = "agente_usuario", length = 500)
	private String agenteUsuario;

	@Column(name = "visible_cliente", nullable = false)
	private boolean visibleCliente = true;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDateTime fechaInicio;

	@Column(name = "fecha_fin")
	private LocalDateTime fechaFin;

	@Column(name = "clave_idempotencia", length = 120)
	private String claveIdempotencia;
}
