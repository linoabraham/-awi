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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.EstadoCliente;
import pe.nawin.enumeracion.TipoDocumento;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cliente")
	private Long idCliente;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, unique = true)
	private Usuario usuario;

	// Documento opcional en el registro público; se completa luego en el perfil.
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", length = 10)
	private TipoDocumento tipoDocumento;

	@Column(name = "numero_documento", unique = true, length = 11)
	private String numeroDocumento;

	@Column(name = "nombres", length = 100)
	private String nombres;

	@Column(name = "apellidos", length = 120)
	private String apellidos;

	@Column(name = "razon_social", length = 200)
	private String razonSocial;

	@Column(name = "direccion", length = 250)
	private String direccion;

	@Column(name = "codigo_referido", nullable = false, unique = true, length = 20)
	private String codigoReferido;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoCliente estado = EstadoCliente.ACTIVO;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creado_por", nullable = false)
	private Usuario creadoPor;
}
