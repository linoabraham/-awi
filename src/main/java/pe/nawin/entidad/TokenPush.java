package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Token FCM de un dispositivo (uno por usuario + installationId). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tokens_push")
public class TokenPush {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_token_push")
	private Long idTokenPush;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "installation_id", nullable = false, length = 64)
	private String installationId;

	@Column(name = "fcm_token", nullable = false, length = 500)
	private String fcmToken;

	@Column(name = "plataforma", length = 20)
	private String plataforma;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	@Column(name = "fecha_actualizacion", nullable = false)
	private LocalDateTime fechaActualizacion;

	@PrePersist
	@PreUpdate
	void marcarFecha() {
		fechaActualizacion = LocalDateTime.now();
	}
}
