package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resultados_consulta")
public class ResultadoConsulta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_resultado_consulta")
	private Long idResultadoConsulta;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_consulta", nullable = false, unique = true)
	private Consulta consulta;

	@Column(name = "resultado_json_cifrado", nullable = false, columnDefinition = "LONGTEXT")
	private String resultadoJsonCifrado;

	@Column(name = "resumen_json", columnDefinition = "LONGTEXT")
	private String resumenJson;

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
