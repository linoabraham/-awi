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
import pe.nawin.enumeracion.TipoArchivoConsulta;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "archivos_consulta")
public class ArchivoConsulta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_archivo_consulta")
	private Long idArchivoConsulta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_consulta", nullable = false)
	private Consulta consulta;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_archivo", nullable = false, length = 20)
	private TipoArchivoConsulta tipoArchivo;

	@Column(name = "nombre_archivo", nullable = false, length = 255)
	private String nombreArchivo;

	@Column(name = "tipo_mime", nullable = false, length = 100)
	private String tipoMime;

	@Column(name = "ruta_privada", nullable = false, length = 500)
	private String rutaPrivada;

	@Column(name = "tamano_bytes", nullable = false)
	private long tamanoBytes;

	@Column(name = "fecha_expiracion", nullable = false)
	private LocalDateTime fechaExpiracion;

	@Column(name = "eliminado_por_retencion", nullable = false)
	private boolean eliminadoPorRetencion;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@PrePersist
	void crearFecha() {
		fechaCreacion = LocalDateTime.now();
	}
}
