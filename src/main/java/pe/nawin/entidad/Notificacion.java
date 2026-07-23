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
import pe.nawin.enumeracion.CanalNotificacion;
import pe.nawin.enumeracion.EstadoNotificacion;
import pe.nawin.enumeracion.TipoNotificacion;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notificaciones")
public class Notificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_notificacion")
	private Long idNotificacion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_membresia")
	private Membresia membresia;

	@Enumerated(EnumType.STRING)
	@Column(name = "canal", nullable = false, length = 20)
	private CanalNotificacion canal;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 30)
	private TipoNotificacion tipo;

	@Column(name = "titulo", nullable = false, length = 150)
	private String titulo;

	@Column(name = "mensaje", nullable = false, length = 1000)
	private String mensaje;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

	@Column(name = "fecha_programada")
	private LocalDateTime fechaProgramada;

	@Column(name = "fecha_envio")
	private LocalDateTime fechaEnvio;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creado_por")
	private Usuario creadoPor;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@PrePersist
	void crearFecha() {
		fechaCreacion = LocalDateTime.now();
	}
}
