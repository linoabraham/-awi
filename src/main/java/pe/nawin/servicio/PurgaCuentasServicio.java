package pe.nawin.servicio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.configuracion.NawinPropiedades;

/**
 * Elimina las cuentas de cliente que se registraron pero nunca verificaron su
 * correo dentro del plazo configurado. Así se liberan el correo y el documento
 * (para que el dueño real pueda registrarse) y no se acumulan cuentas basura.
 *
 * <p>El borrado es en orden inverso a las llaves foráneas. Una cuenta no
 * verificada nunca pudo iniciar sesión, así que solo tiene filas en:
 * codigos_verificacion, clientes, saldos_creditos y billeteras_miplata. No se
 * tocan cuentas con actividad.
 */
@Service
public class PurgaCuentasServicio {

	private static final Logger log = LoggerFactory.getLogger(PurgaCuentasServicio.class);

	@PersistenceContext
	private EntityManager em;

	private final NawinPropiedades propiedades;

	public PurgaCuentasServicio(NawinPropiedades propiedades) {
		this.propiedades = propiedades;
	}

	@Transactional
	public int purgarNoVerificadas() {
		LocalDateTime limite = LocalDateTime.now().minusHours(propiedades.seguridad().horasPurgaNoVerificados());

		@SuppressWarnings("unchecked")
		List<Long> ids = em.createNativeQuery(
				"SELECT id_usuario FROM usuarios WHERE correo_verificado = false AND fecha_creacion < :limite")
				.setParameter("limite", limite)
				.getResultList()
				.stream()
				.map(o -> ((Number) o).longValue())
				.toList();

		if (ids.isEmpty()) {
			return 0;
		}

		ejecutar("DELETE FROM codigos_verificacion WHERE id_usuario IN (:ids)", ids);
		ejecutar("DELETE FROM tokens_refresco WHERE id_usuario IN (:ids)", ids);
		ejecutar("DELETE FROM billeteras_miplata WHERE id_cliente IN "
				+ "(SELECT id_cliente FROM clientes WHERE id_usuario IN (:ids))", ids);
		ejecutar("DELETE FROM saldos_creditos WHERE id_cliente IN "
				+ "(SELECT id_cliente FROM clientes WHERE id_usuario IN (:ids))", ids);
		ejecutar("DELETE FROM clientes WHERE id_usuario IN (:ids)", ids);
		int borradas = ejecutar("DELETE FROM usuarios WHERE id_usuario IN (:ids)", ids);

		log.info("Purga de cuentas no verificadas: {} cuenta(s) eliminada(s).", borradas);
		return borradas;
	}

	private int ejecutar(String sql, List<Long> ids) {
		return em.createNativeQuery(sql).setParameter("ids", ids).executeUpdate();
	}
}
