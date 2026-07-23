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

/** Preferencias de notificación por usuario (una fila por usuario). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "preferencias_notificacion")
public class PreferenciaNotificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_preferencia")
	private Long idPreferencia;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	/** Interruptor maestro: si está apagado, no se envía ninguna push. */
	@Column(name = "push", nullable = false)
	private boolean push = true;

	@Column(name = "promos", nullable = false)
	private boolean promos = true;

	@Column(name = "pagos", nullable = false)
	private boolean pagos = true;

	@Column(name = "consultas", nullable = false)
	private boolean consultas = true;

	@Column(name = "referidos", nullable = false)
	private boolean referidos = true;

	@Column(name = "seguridad", nullable = false)
	private boolean seguridad = true;

	@Column(name = "fecha_actualizacion", nullable = false)
	private LocalDateTime fechaActualizacion;

	@PrePersist
	@PreUpdate
	void marcarFecha() {
		fechaActualizacion = LocalDateTime.now();
	}
}
