package pe.nawin.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.nawin.enumeracion.RolCodigo;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Rol {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_rol")
	private Long idRol;

	@Enumerated(EnumType.STRING)
	@Column(name = "codigo", nullable = false, unique = true, length = 20)
	private RolCodigo codigo;

	@Column(name = "nombre", nullable = false, length = 50)
	private String nombre;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;
}
