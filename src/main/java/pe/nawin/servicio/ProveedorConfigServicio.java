package pe.nawin.servicio;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.entidad.ConfiguracionProveedor;
import pe.nawin.repositorio.ConfiguracionProveedorRepositorio;
import pe.nawin.utilidad.CifradoServicio;

/**
 * Credenciales del proveedor CODART con prioridad: valores guardados desde el
 * panel (token cifrado en BD) y, si están vacíos, las variables de entorno /
 * application.yml. Cachea en memoria para no tocar la BD en cada consulta; el
 * caché se invalida al guardar desde el panel (efecto inmediato, sin reiniciar).
 */
@Service
public class ProveedorConfigServicio {

	private static final Logger log = LoggerFactory.getLogger(ProveedorConfigServicio.class);

	private final ConfiguracionProveedorRepositorio repositorio;
	private final CifradoServicio cifradoServicio;
	private final NawinPropiedades propiedades;

	// Caché en memoria del token/URL efectivos (null = aún no cargado).
	private volatile String tokenCache;
	private volatile String baseUrlCache;
	private volatile boolean cargado;

	public ProveedorConfigServicio(ConfiguracionProveedorRepositorio repositorio,
			CifradoServicio cifradoServicio, NawinPropiedades propiedades) {
		this.repositorio = repositorio;
		this.cifradoServicio = cifradoServicio;
		this.propiedades = propiedades;
	}

	/** Token efectivo: el del panel si existe; si no, el de la configuración/env. */
	public String tokenActual() {
		cargarSiHaceFalta();
		return StringUtils.hasText(tokenCache) ? tokenCache : propiedades.codart().apiToken();
	}

	/** Base URL efectiva: la del panel si existe; si no, la de la configuración/env. */
	public String baseUrlActual() {
		cargarSiHaceFalta();
		return StringUtils.hasText(baseUrlCache) ? baseUrlCache : propiedades.codart().baseUrl();
	}

	private synchronized void cargarSiHaceFalta() {
		if (cargado) {
			return;
		}
		try {
			ConfiguracionProveedor config = repositorio.findFirstByOrderByIdConfiguracionProveedorAsc()
					.orElse(null);
			if (config != null) {
				baseUrlCache = config.getBaseUrl();
				tokenCache = StringUtils.hasText(config.getApiTokenCifrado())
						? cifradoServicio.descifrar(config.getApiTokenCifrado())
						: null;
			}
			cargado = true;
		} catch (RuntimeException ex) {
			// BD no disponible: se usa el fallback de propiedades sin marcar cargado.
			log.warn("No se pudo leer la configuración del proveedor: {}", ex.getMessage());
		}
	}

	/** Estado para el panel: token enmascarado, nunca en claro. */
	@Transactional(readOnly = true)
	public Map<String, Object> obtenerParaPanel() {
		ConfiguracionProveedor config = repositorio.findFirstByOrderByIdConfiguracionProveedorAsc()
				.orElse(null);
		boolean tokenDePanel = config != null && StringUtils.hasText(config.getApiTokenCifrado());
		String tokenEfectivo = tokenActual();
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("baseUrl", baseUrlActual());
		m.put("baseUrlPersonalizada", config != null && StringUtils.hasText(config.getBaseUrl()));
		m.put("tokenMascara", enmascarar(tokenEfectivo));
		m.put("tokenFuente", tokenDePanel ? "PANEL" : "VARIABLES");
		m.put("fechaActualizacion", config == null ? null : config.getFechaActualizacion());
		return m;
	}

	/**
	 * Guarda desde el panel. Token en blanco = conservar el actual; baseUrl en
	 * blanco = volver al valor de las variables de entorno.
	 */
	@Transactional
	public Map<String, Object> actualizar(String apiToken, String baseUrl) {
		ConfiguracionProveedor config = repositorio.findFirstByOrderByIdConfiguracionProveedorAsc()
				.orElseGet(ConfiguracionProveedor::new);
		if (StringUtils.hasText(apiToken)) {
			config.setApiTokenCifrado(cifradoServicio.cifrar(apiToken.trim()));
		}
		config.setBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl.trim() : null);
		repositorio.save(config);
		invalidarCache();
		return obtenerParaPanel();
	}

	private synchronized void invalidarCache() {
		cargado = false;
		tokenCache = null;
		baseUrlCache = null;
	}

	private String enmascarar(String token) {
		if (!StringUtils.hasText(token)) {
			return null;
		}
		if (token.length() <= 6) {
			return "••••••";
		}
		return "••••••••" + token.substring(token.length() - 4);
	}
}
