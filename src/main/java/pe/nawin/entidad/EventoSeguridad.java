package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.TipoEventoSeguridad;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "eventos_seguridad")
public class EventoSeguridad {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_evento_seguridad")
	private Long idEventoSeguridad;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 40)
	private TipoEventoSeguridad tipo;

	@Column(name = "installation_id", length = 64)
	private String installationId;

	@Column(name = "direccion_ip", length = 45)
	private String direccionIp;

	@Column(name = "detalle", length = 255)
	private String detalle;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@PrePersist
	void crearFecha() {
		fechaCreacion = LocalDateTime.now();
	}
}
