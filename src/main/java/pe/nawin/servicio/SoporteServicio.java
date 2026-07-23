package pe.nawin.servicio;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.dto.solicitud.ConfiguracionSoporteRequest;
import pe.nawin.entidad.ConfiguracionSoporte;
import pe.nawin.repositorio.ConfiguracionSoporteRepositorio;

/** Canales oficiales de soporte y redes (WhatsApp, correo, redes, términos). */
@Service
public class SoporteServicio {

	private final ConfiguracionSoporteRepositorio repositorio;

	public SoporteServicio(ConfiguracionSoporteRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	private ConfiguracionSoporte obtenerEntidad() {
		return repositorio.findFirstByOrderByIdConfiguracionSoporteAsc()
				.orElseGet(() -> repositorio.save(new ConfiguracionSoporte()));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> obtener() {
		return mapa(repositorio.findFirstByOrderByIdConfiguracionSoporteAsc()
				.orElseGet(ConfiguracionSoporte::new));
	}

	@Transactional
	public Map<String, Object> actualizar(ConfiguracionSoporteRequest request) {
		ConfiguracionSoporte config = obtenerEntidad();
		config.setWhatsapp(limpiar(request.whatsapp()));
		config.setCorreo(limpiar(request.correo()));
		config.setFacebook(limpiar(request.facebook()));
		config.setInstagram(limpiar(request.instagram()));
		config.setTiktok(limpiar(request.tiktok()));
		config.setTerminosUrl(limpiar(request.terminosUrl()));
		config.setPrivacidadUrl(limpiar(request.privacidadUrl()));
		return mapa(config);
	}

	private String limpiar(String valor) {
		if (valor == null) {
			return null;
		}
		String texto = valor.trim();
		return texto.isEmpty() ? null : texto;
	}

	private Map<String, Object> mapa(ConfiguracionSoporte c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("whatsapp", c.getWhatsapp());
		m.put("correo", c.getCorreo());
		m.put("facebook", c.getFacebook());
		m.put("instagram", c.getInstagram());
		m.put("tiktok", c.getTiktok());
		m.put("terminosUrl", c.getTerminosUrl());
		m.put("privacidadUrl", c.getPrivacidadUrl());
		m.put("fechaActualizacion", c.getFechaActualizacion());
		return m;
	}
}
