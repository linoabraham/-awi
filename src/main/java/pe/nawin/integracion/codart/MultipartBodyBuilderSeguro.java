package pe.nawin.integracion.codart;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;

/**
 * Arma el cuerpo multipart con las clases de Spring MVC (LinkedMultiValueMap +
 * HttpEntity). No usa MultipartBodyBuilder porque esa clase depende de
 * reactive-streams (WebFlux), que no está en el classpath del proyecto.
 */
class MultipartBodyBuilderSeguro {

	private final LinkedMultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();

	MultipartBodyBuilderSeguro archivo(String nombre, MultipartFile archivo) {
		try {
			HttpHeaders cabeceras = new HttpHeaders();
			cabeceras.setContentDispositionFormData(nombre, archivo.getOriginalFilename());
			if (archivo.getContentType() != null) {
				cabeceras.setContentType(MediaType.parseMediaType(archivo.getContentType()));
			}
			partes.add(nombre, new HttpEntity<>(archivo.getResource(), cabeceras));
			return this;
		} catch (Exception ex) {
			throw new NawinException(CodigoError.CON_001);
		}
	}

	MultiValueMap<String, Object> build() {
		return partes;
	}
}
