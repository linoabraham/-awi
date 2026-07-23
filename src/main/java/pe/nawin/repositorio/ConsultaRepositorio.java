package pe.nawin.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.nawin.entidad.Consulta;
import pe.nawin.enumeracion.EstadoConsulta;

public interface ConsultaRepositorio extends JpaRepository<Consulta, Long> {
	Optional<Consulta> findByCodigoConsulta(String codigoConsulta);

	Optional<Consulta> findByCodigoConsultaAndCliente_IdCliente(String codigoConsulta, Long idCliente);

	Optional<Consulta> findByUsuario_IdUsuarioAndClaveIdempotencia(Long idUsuario, String claveIdempotencia);

	List<Consulta> findByCliente_IdClienteAndVisibleClienteTrueOrderByFechaInicioDesc(Long idCliente);

	long countByMembresiaEndpoint_IdMembresiaEndpointAndEstadoInAndFechaInicioBetween(
			Long idMembresiaEndpoint,
			Collection<EstadoConsulta> estados,
			LocalDateTime desde,
			LocalDateTime hasta);

	/**
	 * Consumo histórico (todas las fechas) de un endpoint de membresía. Reemplaza
	 * al contador denormalizado {@code consumido_total}: se cuenta en vivo por el
	 * estado de las consultas, evitando el UPDATE en caliente que serializaba /
	 * deadlockeaba las consultas concurrentes de la misma membresía (árbol
	 * genealógico). Las PROCESANDO cuentan como "reservadas"; una FALLIDA sale del
	 * conteo sola, sin necesidad de revertir nada.
	 */
	long countByMembresiaEndpoint_IdMembresiaEndpointAndEstadoIn(
			Long idMembresiaEndpoint,
			Collection<EstadoConsulta> estados);

	/** Consultas de un cliente en un rango (para el tope diario de seguridad de ilimitados). */
	long countByCliente_IdClienteAndEstadoInAndFechaInicioBetween(
			Long idCliente,
			Collection<EstadoConsulta> estados,
			LocalDateTime desde,
			LocalDateTime hasta);

	/** Consumo agrupado por cliente en un rango (para el ranking de consumo del panel). */
	@org.springframework.data.jpa.repository.Query("""
			select c.cliente.idCliente, count(c),
			       sum(case when c.origenConsumo = pe.nawin.enumeracion.OrigenConsumoConsulta.MEMBRESIA then 1 else 0 end),
			       max(c.fechaInicio)
			from Consulta c
			where c.fechaInicio between :desde and :hasta and c.estado in :estados
			group by c.cliente.idCliente
			order by count(c) desc""")
	List<Object[]> consumoPorCliente(
			@org.springframework.data.repository.query.Param("desde") LocalDateTime desde,
			@org.springframework.data.repository.query.Param("hasta") LocalDateTime hasta,
			@org.springframework.data.repository.query.Param("estados") Collection<EstadoConsulta> estados);
}
