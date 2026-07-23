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

/** Credenciales del proveedor CODART (fila única, editable desde el panel; token cifrado). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion_proveedor")
public class ConfiguracionProveedor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_configuracion_proveedor")
	private Long idConfiguracionProveedor;

	@Column(name = "api_token_cifrado", columnDefinition = "TEXT")
	private String apiTokenCifrado;

	@Column(name = "base_url", length = 255)
	private String baseUrl;

	@Column(name = "fecha_actualizacion", nullable = false)
	private LocalDateTime fechaActualizacion;

	@PrePersist
	@PreUpdate
	void marcarFecha() {
		fechaActualizacion = LocalDateTime.now();
	}
}
