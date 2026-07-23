package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Canales oficiales de soporte y redes de la app (fila única, editable desde el panel). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion_soporte")
public class ConfiguracionSoporte {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_configuracion_soporte")
	private Long idConfiguracionSoporte;

	@Column(name = "whatsapp", length = 20)
	private String whatsapp;

	@Column(name = "correo", length = 120)
	private String correo;

	@Column(name = "facebook", length = 255)
	private String facebook;

	@Column(name = "instagram", length = 255)
	private String instagram;

	@Column(name = "tiktok", length = 255)
	private String tiktok;

	@Column(name = "terminos_url", length = 255)
	private String terminosUrl;

	@Column(name = "privacidad_url", length = 255)
	private String privacidadUrl;

	@Column(name = "fecha_actualizacion", nullable = false)
	private LocalDateTime fechaActualizacion;

	@PrePersist
	@PreUpdate
	void marcarFecha() {
		fechaActualizacion = LocalDateTime.now();
	}
}
