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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.TipoCliente;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tokens_refresco")
public class TokenRefresco {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_token_refresco")
	private Long idTokenRefresco;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false)
	private Usuario usuario;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "fecha_expiracion", nullable = false)
	private LocalDateTime fechaExpiracion;

	@Column(name = "revocado", nullable = false)
	private boolean revocado;

	@Column(name = "direccion_ip", nullable = false, length = 45)
	private String direccionIp;

	@Column(name = "agente_usuario", length = 500)
	private String agenteUsuario;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_cliente", nullable = false, length = 10)
	private TipoCliente tipoCliente = TipoCliente.WEB;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	/** UUID generado por la app en la primera instalación; identifica al dispositivo. */
	@Column(name = "installation_id", length = 64)
	private String installationId;

	@Column(name = "nombre_dispositivo", length = 120)
	private String nombreDispositivo;

	@Column(name = "modelo", length = 120)
	private String modelo;

	@Column(name = "plataforma", length = 20)
	private String plataforma;

	@Column(name = "ultima_actividad")
	private LocalDateTime ultimaActividad;

	@PrePersist
	void crearFecha() {
		fechaCreacion = LocalDateTime.now();
	}
}
