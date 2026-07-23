package pe.nawin.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nawin.configuracion.NawinPropiedades;
import pe.nawin.dto.solicitud.ComprobanteEmitirRequest;
import pe.nawin.dto.solicitud.MotivoRequest;
import pe.nawin.dto.solicitud.SerieComprobanteRequest;
import pe.nawin.entidad.Cliente;
import pe.nawin.entidad.Comprobante;
import pe.nawin.entidad.Pago;
import pe.nawin.entidad.SerieComprobante;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.EstadoComprobante;
import pe.nawin.enumeracion.EstadoPago;
import pe.nawin.enumeracion.TipoComprobante;
import pe.nawin.enumeracion.TipoDocumento;
import pe.nawin.excepcion.CodigoError;
import pe.nawin.excepcion.NawinException;
import pe.nawin.repositorio.ComprobanteRepositorio;
import pe.nawin.repositorio.PagoRepositorio;
import pe.nawin.repositorio.SerieComprobanteRepositorio;
import pe.nawin.repositorio.UsuarioRepositorio;
import pe.nawin.utilidad.ContextoSeguridad;
import pe.nawin.utilidad.TicketComprobantePdf;

@Service
public class ComprobanteServicio {

	private final ComprobanteRepositorio comprobanteRepositorio;
	private final SerieComprobanteRepositorio serieRepositorio;
	private final PagoRepositorio pagoRepositorio;
	private final UsuarioRepositorio usuarioRepositorio;
	private final NawinPropiedades propiedades;

	public ComprobanteServicio(ComprobanteRepositorio comprobanteRepositorio, SerieComprobanteRepositorio serieRepositorio,
			PagoRepositorio pagoRepositorio, UsuarioRepositorio usuarioRepositorio, NawinPropiedades propiedades) {
		this.comprobanteRepositorio = comprobanteRepositorio;
		this.serieRepositorio = serieRepositorio;
		this.pagoRepositorio = pagoRepositorio;
		this.usuarioRepositorio = usuarioRepositorio;
		this.propiedades = propiedades;
	}

	@Transactional
	public Map<String, Object> crearSerie(SerieComprobanteRequest request) {
		SerieComprobante serie = new SerieComprobante();
		serie.setTipoComprobante(request.tipoComprobante());
		serie.setSerie(request.serie());
		serie.setActivo(request.activo());
		return serieRespuesta(serieRepositorio.save(serie));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listarSeries(Boolean activo) {
		List<SerieComprobante> series = activo == null ? serieRepositorio.findAll() : serieRepositorio.findByActivo(activo);
		return series.stream().map(this::serieRespuesta).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verSerie(Long id) {
		return serieRespuesta(serieRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002)));
	}

	@Transactional
	public Map<String, Object> actualizarSerie(Long id, SerieComprobanteRequest request) {
		SerieComprobante serie = serieRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		serie.setTipoComprobante(request.tipoComprobante());
		serie.setSerie(request.serie());
		serie.setActivo(request.activo());
		return serieRespuesta(serie);
	}

	@Transactional
	public void inactivarSerie(Long id) {
		serieRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002)).setActivo(false);
	}

	@Transactional
	public Map<String, Object> emitir(ComprobanteEmitirRequest request) {
		if (comprobanteRepositorio.findByPago_IdPago(request.idPago()).isPresent()) {
			throw new NawinException(CodigoError.COM_001);
		}
		Pago pago = pagoRepositorio.findById(request.idPago()).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		if (pago.getEstado() != EstadoPago.CONFIRMADO) {
			throw new NawinException(CodigoError.PAG_001);
		}
		SerieComprobante serie = serieRepositorio.findById(request.idSerieComprobante())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		if (!serie.isActivo() || serie.getTipoComprobante() != request.tipoComprobante()) {
			throw new NawinException(CodigoError.GEN_001, "Serie no disponible para el tipo de comprobante.");
		}
		return respuesta(emitirConSerie(pago, serie, request.tipoComprobante(), usuarioActual()));
	}

	@Transactional
	public Map<String, Object> emitirAutomatico(Pago pago, Usuario emitidoPor) {
		return comprobanteRepositorio.findByPago_IdPago(pago.getIdPago())
				.map(this::respuesta)
				.orElseGet(() -> {
					SerieComprobante serie = seleccionarSerieAutomatica(pago);
					return respuesta(emitirConSerie(pago, serie, serie.getTipoComprobante(), emitidoPor));
				});
	}

	private Comprobante emitirConSerie(Pago pago, SerieComprobante serie, TipoComprobante tipoComprobante, Usuario emitidoPor) {
		if (pago.getEstado() != EstadoPago.CONFIRMADO) {
			throw new NawinException(CodigoError.PAG_001);
		}
		// Bloqueo pesimista de la serie: dos emisiones simultáneas no pueden
		// obtener el mismo número correlativo.
		SerieComprobante serieBloqueada = serieRepositorio.findWithLockByIdSerieComprobante(serie.getIdSerieComprobante())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_002));
		serieBloqueada.setUltimoNumero(serieBloqueada.getUltimoNumero() + 1);
		Cliente cliente = pago.getCliente();
		BigDecimal total = pago.getMontoSoles();
		BigDecimal subtotal = total.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
		BigDecimal igv = total.subtract(subtotal);

		Comprobante comprobante = new Comprobante();
		comprobante.setPago(pago);
		comprobante.setSerieComprobante(serieBloqueada);
		comprobante.setTipoComprobante(tipoComprobante);
		comprobante.setSerie(serieBloqueada.getSerie());
		comprobante.setNumero(serieBloqueada.getUltimoNumero());
		comprobante.setTipoDocumentoCliente(cliente.getTipoDocumento());
		comprobante.setNumeroDocumentoCliente(cliente.getNumeroDocumento());
		comprobante.setNombreCliente(cliente.getRazonSocial() == null ? cliente.getNombres() + " " + cliente.getApellidos() : cliente.getRazonSocial());
		comprobante.setSubtotal(subtotal);
		comprobante.setIgv(igv);
		comprobante.setTotal(total);
		comprobante.setEstado(EstadoComprobante.EMITIDO);
		comprobante.setEmitidoPor(emitidoPor);
		comprobante.setFechaEmision(LocalDateTime.now());
		comprobante.setRutaPdf(crearArchivoComprobante(comprobante));
		return comprobanteRepositorio.save(comprobante);
	}

	private SerieComprobante seleccionarSerieAutomatica(Pago pago) {
		TipoComprobante tipo = pago.getCliente().getTipoDocumento() == TipoDocumento.RUC
				? TipoComprobante.FACTURA
				: TipoComprobante.BOLETA;
		return serieRepositorio.findByTipoComprobante(tipo).stream()
				.filter(SerieComprobante::isActivo)
				.findFirst()
				.or(() -> serieRepositorio.findByTipoComprobante(TipoComprobante.RECIBO_INTERNO).stream()
						.filter(SerieComprobante::isActivo)
						.findFirst())
				.orElseThrow(() -> new NawinException(CodigoError.GEN_001, "No hay serie activa para emitir comprobantes."));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> listar(Long idCliente) {
		List<Comprobante> comprobantes = idCliente == null ? comprobanteRepositorio.findAll() : comprobanteRepositorio.findByPago_Cliente_IdCliente(idCliente);
		return comprobantes.stream().map(this::respuesta).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Object> ver(Long id) {
		return respuesta(obtener(id));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> verPropio(Long id) {
		Comprobante comprobante = obtener(id);
		validarPropiedadCliente(comprobante);
		return respuesta(comprobante);
	}

	@Transactional
	public void anular(Long id, MotivoRequest request) {
		Comprobante comprobante = obtener(id);
		if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
			return;
		}
		comprobante.setEstado(EstadoComprobante.ANULADO);
		comprobante.setFechaAnulacion(LocalDateTime.now());
		comprobante.setMotivoAnulacion(request == null ? "Anulado" : request.motivo());
		regenerarPdf(comprobante);
	}

	/** Anula el comprobante ligado a un pago (si existe). Usado al anular pagos. */
	@Transactional
	public void anularPorPago(Long idPago, String motivo) {
		comprobanteRepositorio.findByPago_IdPago(idPago).ifPresent(comprobante -> {
			if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
				return;
			}
			comprobante.setEstado(EstadoComprobante.ANULADO);
			comprobante.setFechaAnulacion(LocalDateTime.now());
			comprobante.setMotivoAnulacion(motivo == null || motivo.isBlank() ? "Pago anulado" : motivo);
			regenerarPdf(comprobante);
		});
	}

	@Transactional(readOnly = true)
	public Resource pdf(Long id) {
		return recursoPdf(obtener(id));
	}

	@Transactional(readOnly = true)
	public Resource pdfPropio(Long id) {
		Comprobante comprobante = obtener(id);
		validarPropiedadCliente(comprobante);
		return recursoPdf(comprobante);
	}

	/**
	 * Devuelve el PDF del comprobante; si el archivo no existe en disco (por
	 * migración o pérdida del almacenamiento), lo regenera desde la base de datos.
	 */
	private Resource recursoPdf(Comprobante comprobante) {
		Resource resource = new FileSystemResource(comprobante.getRutaPdf());
		if (!resource.exists()) {
			regenerarPdf(comprobante);
			resource = new FileSystemResource(comprobante.getRutaPdf());
			if (!resource.exists()) {
				throw new NawinException(CodigoError.GEN_002);
			}
		}
		return resource;
	}

	private void regenerarPdf(Comprobante comprobante) {
		try {
			Path archivo = Path.of(comprobante.getRutaPdf());
			Files.createDirectories(archivo.getParent());
			Files.write(archivo, TicketComprobantePdf.generar(datosTicket(comprobante)));
		} catch (Exception ex) {
			// La regeneración es de mejor esfuerzo: si falla, la descarga reportará GEN_002.
		}
	}

	private Comprobante obtener(Long id) {
		return comprobanteRepositorio.findById(id).orElseThrow(() -> new NawinException(CodigoError.GEN_002));
	}

	private String crearArchivoComprobante(Comprobante comprobante) {
		try {
			Path carpeta = Path.of(propiedades.almacenamientoPrivadoRuta(), "comprobantes");
			Files.createDirectories(carpeta);
			Path archivo = carpeta.resolve(serieNumero(comprobante) + ".pdf");
			Files.write(archivo, TicketComprobantePdf.generar(datosTicket(comprobante)));
			return archivo.toAbsolutePath().toString();
		} catch (Exception ex) {
			throw new NawinException(CodigoError.GEN_001, "No fue posible crear el comprobante privado.");
		}
	}

	private TicketComprobantePdf.DatosTicket datosTicket(Comprobante comprobante) {
		Pago pago = comprobante.getPago();
		Usuario emisor = comprobante.getEmitidoPor();
		NawinPropiedades.Empresa empresa = propiedades.empresa();
		return new TicketComprobantePdf.DatosTicket(
				empresa == null || empresa.nombre() == null ? "NAWIN" : empresa.nombre(),
				empresa == null || empresa.ruc() == null ? "-" : empresa.ruc(),
				empresa == null || empresa.direccion() == null ? "-" : empresa.direccion(),
				empresa == null ? null : empresa.telefono(),
				tituloComprobante(comprobante.getTipoComprobante()),
				serieNumero(comprobante),
				comprobante.getFechaEmision(),
				comprobante.getNombreCliente(),
				comprobante.getTipoDocumentoCliente() == null ? "DOC" : comprobante.getTipoDocumentoCliente().name(),
				comprobante.getNumeroDocumentoCliente(),
				conceptoPago(pago),
				comprobante.getSubtotal(),
				comprobante.getIgv(),
				comprobante.getTotal(),
				pago.getMedioPago() == null ? "-" : pago.getMedioPago().name(),
				pago.getNumeroOperacion(),
				emisor == null ? null : (emisor.getNombres() + " " + emisor.getApellidos()).trim(),
				comprobante.getEstado() == EstadoComprobante.ANULADO);
	}

	private String serieNumero(Comprobante comprobante) {
		return comprobante.getSerie() + "-" + String.format("%08d", comprobante.getNumero());
	}

	private String tituloComprobante(TipoComprobante tipo) {
		return switch (tipo) {
			case BOLETA -> "BOLETA DE VENTA ELECTRONICA";
			case FACTURA -> "FACTURA ELECTRONICA";
			case RECIBO_INTERNO -> "RECIBO INTERNO";
		};
	}

	private String conceptoPago(Pago pago) {
		if (pago.getVentaCredito() != null) {
			return pago.getVentaCredito().getCreditosOtorgados() + " créditos de consulta ("
					+ pago.getVentaCredito().getCantidadPaquetes() + " paquete(s))";
		}
		if (pago.getMembresia() != null && pago.getMembresia().getPlan() != null) {
			return "Plan " + pago.getMembresia().getPlan().getNombre() + " ("
					+ pago.getMembresia().getDiasVigencia() + " días)";
		}
		if (pago.getNumeroOperacion() != null && pago.getNumeroOperacion().startsWith("MIPLATA-RECARGA")) {
			return "Recarga de billetera MiPlata";
		}
		return pago.getObservacion() == null || pago.getObservacion().isBlank()
				? "Pago de servicios NAWIN"
				: pago.getObservacion();
	}

	private Usuario usuarioActual() {
		return usuarioRepositorio.findById(ContextoSeguridad.usuarioActual().idUsuario())
				.orElseThrow(() -> new NawinException(CodigoError.AUTH_002));
	}

	private void validarPropiedadCliente(Comprobante comprobante) {
		Long idUsuarioActual = ContextoSeguridad.usuarioActual().idUsuario();
		Long idUsuarioCliente = comprobante.getPago().getCliente().getUsuario().getIdUsuario();
		if (!idUsuarioActual.equals(idUsuarioCliente)) {
			throw new NawinException(CodigoError.AUTH_003);
		}
	}

	private Map<String, Object> serieRespuesta(SerieComprobante serie) {
		return Map.of(
				"idSerieComprobante", serie.getIdSerieComprobante(),
				"tipoComprobante", serie.getTipoComprobante(),
				"serie", serie.getSerie(),
				"ultimoNumero", serie.getUltimoNumero(),
				"activo", serie.isActivo());
	}

	private Map<String, Object> respuesta(Comprobante comprobante) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("idComprobante", comprobante.getIdComprobante());
		m.put("idPago", comprobante.getPago().getIdPago());
		m.put("tipoComprobante", comprobante.getTipoComprobante());
		m.put("serie", comprobante.getSerie());
		m.put("numero", comprobante.getNumero());
		m.put("tipoDocumentoCliente", comprobante.getTipoDocumentoCliente());
		m.put("numeroDocumentoCliente", comprobante.getNumeroDocumentoCliente());
		m.put("nombreCliente", comprobante.getNombreCliente());
		m.put("subtotal", comprobante.getSubtotal());
		m.put("igv", comprobante.getIgv());
		m.put("total", comprobante.getTotal());
		m.put("estado", comprobante.getEstado());
		m.put("fechaEmision", comprobante.getFechaEmision());
		m.put("fechaAnulacion", comprobante.getFechaAnulacion());
		m.put("motivoAnulacion", comprobante.getMotivoAnulacion());
		return m;
	}
}
