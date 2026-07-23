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
import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.enumeracion.TipoMfa;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario extends EntidadAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Long idUsuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_rol", nullable = false)
	private Rol rol;

	@Column(name = "nombres", nullable = false, length = 100)
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = 120)
	private String apellidos;

	@Column(name = "correo", nullable = false, unique = true, length = 150)
	private String correo;

	/** Correo sin alias (+sufijo, puntos en Gmail) para detectar cuentas duplicadas. */
	@Column(name = "correo_normalizado", length = 160)
	private String correoNormalizado;

	// Verificado por defecto: solo el registro público de clientes lo pone en false.
	@Column(name = "correo_verificado", nullable = false)
	private boolean correoVerificado = true;

	// Opcional: el registro público no lo pide; se completa luego en el perfil.
	@Column(name = "celular", length = 15)
	private String celular;

	@Column(name = "nombre_usuario", nullable = false, unique = true, length = 60)
	private String nombreUsuario;

	@Column(name = "clave_hash", nullable = false)
	private String claveHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoUsuario estado = EstadoUsuario.ACTIVO;

	@Column(name = "mfa_habilitado", nullable = false)
	private boolean mfaHabilitado;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_mfa", length = 20)
	private TipoMfa tipoMfa;

	@Column(name = "mfa_secreto_cifrado", length = 500)
	private String mfaSecretoCifrado;

	@Column(name = "intentos_fallidos", nullable = false)
	private int intentosFallidos;

	@Column(name = "bloqueado_hasta")
	private LocalDateTime bloqueadoHasta;

	@Column(name = "ultimo_acceso")
	private LocalDateTime ultimoAcceso;
}
