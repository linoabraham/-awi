package pe.nawin.configuracion;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguracion {

	public static final String CACHE_PLANES = "catalogoPlanes";
	public static final String CACHE_ENDPOINTS = "catalogoEndpoints";
	public static final String CACHE_PAQUETES = "catalogoPaquetes";

	@Bean
	CacheManager cacheManager() {
		return new ConcurrentMapCacheManager(CACHE_PLANES, CACHE_ENDPOINTS, CACHE_PAQUETES);
	}
}
