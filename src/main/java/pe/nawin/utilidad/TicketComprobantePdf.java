package pe.nawin.utilidad;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

/**
 * Genera el comprobante en formato de ticket para impresora térmica de 80 mm:
 * cabecera de la empresa, datos del cliente, detalle con IGV y código de
 * barras Code128 con la serie y el número del comprobante.
 */
public final class TicketComprobantePdf {

	private static final float ANCHO_PAGINA = 226.77f; // 80 mm en puntos (72 dpi)
	private static final float MARGEN = 10f;
	private static final float ANCHO_UTIL = ANCHO_PAGINA - 2 * MARGEN;
	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private TicketComprobantePdf() {
	}

	public record DatosTicket(
			String empresaNombre,
			String empresaRuc,
			String empresaDireccion,
			String empresaTelefono,
			String tituloComprobante,
			String serieNumero,
			LocalDateTime fechaEmision,
			String clienteNombre,
			String clienteTipoDocumento,
			String clienteNumeroDocumento,
			String concepto,
			BigDecimal subtotal,
			BigDecimal igv,
			BigDecimal total,
			String medioPago,
			String numeroOperacion,
			String atendidoPor,
			boolean anulado) {
	}

	private static final int TIPO_TEXTO = 0;
	private static final int TIPO_DOBLE = 1;
	private static final int TIPO_SEPARADOR = 2;
	private static final int TIPO_BARRAS = 3;
	private static final int TIPO_ESPACIO = 4;

	private static final class Elemento {
		int tipo;
		String texto;
		String textoDerecha;
		PDFont fuente;
		float tamano;
		boolean centrado;
		float alto;
		BufferedImage imagen;
	}

	public static byte[] generar(DatosTicket datos) {
		try (PDDocument documento = new PDDocument()) {
			List<Elemento> elementos = construirElementos(datos);
			float altoTotal = 2 * MARGEN;
			for (Elemento elemento : elementos) {
				altoTotal += elemento.alto;
			}

			PDPage pagina = new PDPage(new PDRectangle(ANCHO_PAGINA, altoTotal));
			documento.addPage(pagina);
			try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
				float y = altoTotal - MARGEN;
				for (Elemento elemento : elementos) {
					y = dibujar(documento, contenido, elemento, y);
				}
			}
			ByteArrayOutputStream salida = new ByteArrayOutputStream();
			documento.save(salida);
			return salida.toByteArray();
		} catch (IOException ex) {
			throw new IllegalStateException("No fue posible generar el PDF del comprobante.", ex);
		}
	}

	private static List<Elemento> construirElementos(DatosTicket datos) throws IOException {
		List<Elemento> elementos = new ArrayList<>();

		agregarTextoCentrado(elementos, datos.empresaNombre(), PDType1Font.HELVETICA_BOLD, 11f);
		agregarTextoCentrado(elementos, "RUC " + datos.empresaRuc(), PDType1Font.HELVETICA, 8f);
		agregarTextoCentrado(elementos, datos.empresaDireccion(), PDType1Font.HELVETICA, 7f);
		if (tiene(datos.empresaTelefono())) {
			agregarTextoCentrado(elementos, "Tel. " + datos.empresaTelefono(), PDType1Font.HELVETICA, 7f);
		}
		agregarSeparador(elementos);

		agregarTextoCentrado(elementos, datos.tituloComprobante(), PDType1Font.HELVETICA_BOLD, 9f);
		agregarTextoCentrado(elementos, datos.serieNumero(), PDType1Font.HELVETICA_BOLD, 12f);
		agregarSeparador(elementos);

		agregarTexto(elementos, "Fecha: " + datos.fechaEmision().format(FORMATO_FECHA), PDType1Font.HELVETICA, 8f);
		if (tiene(datos.atendidoPor())) {
			agregarTexto(elementos, "Atendido por: " + datos.atendidoPor(), PDType1Font.HELVETICA, 8f);
		}
		agregarTexto(elementos, "Cliente: " + datos.clienteNombre(), PDType1Font.HELVETICA, 8f);
		agregarTexto(elementos, datos.clienteTipoDocumento() + ": " + datos.clienteNumeroDocumento(),
				PDType1Font.HELVETICA, 8f);
		agregarSeparador(elementos);

		agregarDoble(elementos, "DESCRIPCION", "IMPORTE", PDType1Font.HELVETICA_BOLD, 8f);
		agregarDoble(elementos, datos.concepto(), moneda(datos.total()), PDType1Font.HELVETICA, 8f);
		agregarSeparador(elementos);

		agregarDoble(elementos, "OP. GRAVADA", moneda(datos.subtotal()), PDType1Font.HELVETICA, 8f);
		agregarDoble(elementos, "IGV (18%)", moneda(datos.igv()), PDType1Font.HELVETICA, 8f);
		agregarDoble(elementos, "TOTAL", moneda(datos.total()), PDType1Font.HELVETICA_BOLD, 11f);
		agregarSeparador(elementos);

		agregarTexto(elementos, "Medio de pago: " + datos.medioPago(), PDType1Font.HELVETICA, 8f);
		if (tiene(datos.numeroOperacion())) {
			agregarTexto(elementos, "N. operacion: " + datos.numeroOperacion(), PDType1Font.HELVETICA, 8f);
		}
		if (datos.anulado()) {
			agregarEspacio(elementos, 4f);
			agregarTextoCentrado(elementos, "*** ANULADO ***", PDType1Font.HELVETICA_BOLD, 11f);
		}
		agregarEspacio(elementos, 6f);

		Elemento barras = new Elemento();
		barras.tipo = TIPO_BARRAS;
		barras.imagen = generarCodigoBarras(datos.serieNumero());
		barras.alto = 40f;
		elementos.add(barras);
		agregarTextoCentrado(elementos, datos.serieNumero(), PDType1Font.HELVETICA, 8f);

		agregarEspacio(elementos, 6f);
		agregarTextoCentrado(elementos, "Gracias por su preferencia!", PDType1Font.HELVETICA_BOLD, 8f);
		agregarTextoCentrado(elementos, "Representacion impresa del comprobante.", PDType1Font.HELVETICA, 6.5f);
		return elementos;
	}

	private static float dibujar(PDDocument documento, PDPageContentStream contenido, Elemento elemento, float y)
			throws IOException {
		switch (elemento.tipo) {
			case TIPO_SEPARADOR -> {
				float yLinea = y - elemento.alto / 2;
				contenido.setLineDashPattern(new float[] {2f, 2f}, 0);
				contenido.setLineWidth(0.6f);
				contenido.moveTo(MARGEN, yLinea);
				contenido.lineTo(ANCHO_PAGINA - MARGEN, yLinea);
				contenido.stroke();
				contenido.setLineDashPattern(new float[] {}, 0);
			}
			case TIPO_TEXTO -> {
				float x = MARGEN;
				if (elemento.centrado) {
					float anchoTexto = ancho(elemento.fuente, elemento.tamano, elemento.texto);
					x = (ANCHO_PAGINA - anchoTexto) / 2;
				}
				escribir(contenido, elemento.fuente, elemento.tamano, x, y - elemento.tamano, elemento.texto);
			}
			case TIPO_DOBLE -> {
				float anchoDerecha = ancho(elemento.fuente, elemento.tamano, elemento.textoDerecha);
				escribir(contenido, elemento.fuente, elemento.tamano, MARGEN, y - elemento.tamano, elemento.texto);
				escribir(contenido, elemento.fuente, elemento.tamano, ANCHO_PAGINA - MARGEN - anchoDerecha,
						y - elemento.tamano, elemento.textoDerecha);
			}
			case TIPO_BARRAS -> {
				PDImageXObject imagen = LosslessFactory.createFromImage(documento, elemento.imagen);
				float anchoBarras = Math.min(ANCHO_UTIL, 170f);
				float altoBarras = elemento.alto - 6f;
				contenido.drawImage(imagen, (ANCHO_PAGINA - anchoBarras) / 2, y - elemento.alto + 3f,
						anchoBarras, altoBarras);
			}
			default -> {
				// TIPO_ESPACIO: solo desplaza el cursor
			}
		}
		return y - elemento.alto;
	}

	private static void escribir(PDPageContentStream contenido, PDFont fuente, float tamano, float x, float y,
			String texto) throws IOException {
		contenido.beginText();
		contenido.setFont(fuente, tamano);
		contenido.newLineAtOffset(x, y);
		contenido.showText(texto);
		contenido.endText();
	}

	private static void agregarTexto(List<Elemento> elementos, String texto, PDFont fuente, float tamano)
			throws IOException {
		for (String linea : partir(sanear(texto), fuente, tamano, ANCHO_UTIL)) {
			Elemento elemento = new Elemento();
			elemento.tipo = TIPO_TEXTO;
			elemento.texto = linea;
			elemento.fuente = fuente;
			elemento.tamano = tamano;
			elemento.alto = tamano + 3f;
			elementos.add(elemento);
		}
	}

	private static void agregarTextoCentrado(List<Elemento> elementos, String texto, PDFont fuente, float tamano)
			throws IOException {
		for (String linea : partir(sanear(texto), fuente, tamano, ANCHO_UTIL)) {
			Elemento elemento = new Elemento();
			elemento.tipo = TIPO_TEXTO;
			elemento.texto = linea;
			elemento.fuente = fuente;
			elemento.tamano = tamano;
			elemento.centrado = true;
			elemento.alto = tamano + 3f;
			elementos.add(elemento);
		}
	}

	private static void agregarDoble(List<Elemento> elementos, String izquierda, String derecha, PDFont fuente,
			float tamano) throws IOException {
		String textoDerecha = sanear(derecha);
		float anchoDerecha = ancho(fuente, tamano, textoDerecha) + 6f;
		List<String> lineas = partir(sanear(izquierda), fuente, tamano, ANCHO_UTIL - anchoDerecha);
		for (int i = 0; i < lineas.size(); i++) {
			Elemento elemento = new Elemento();
			elemento.fuente = fuente;
			elemento.tamano = tamano;
			elemento.alto = tamano + 3f;
			if (i == 0) {
				elemento.tipo = TIPO_DOBLE;
				elemento.texto = lineas.get(i);
				elemento.textoDerecha = textoDerecha;
			} else {
				elemento.tipo = TIPO_TEXTO;
				elemento.texto = lineas.get(i);
			}
			elementos.add(elemento);
		}
	}

	private static void agregarSeparador(List<Elemento> elementos) {
		Elemento elemento = new Elemento();
		elemento.tipo = TIPO_SEPARADOR;
		elemento.alto = 9f;
		elementos.add(elemento);
	}

	private static void agregarEspacio(List<Elemento> elementos, float alto) {
		Elemento elemento = new Elemento();
		elemento.tipo = TIPO_ESPACIO;
		elemento.alto = alto;
		elementos.add(elemento);
	}

	private static BufferedImage generarCodigoBarras(String contenido) {
		Code128Bean codigo = new Code128Bean();
		codigo.setModuleWidth(0.30);
		codigo.setBarHeight(11.0);
		codigo.setQuietZone(2.0);
		codigo.doQuietZone(true);
		codigo.setMsgPosition(HumanReadablePlacement.HRP_NONE);
		BitmapCanvasProvider lienzo = new BitmapCanvasProvider(300, BufferedImage.TYPE_BYTE_BINARY, false, 0);
		codigo.generateBarcode(lienzo, contenido);
		try {
			lienzo.finish();
		} catch (IOException ex) {
			throw new IllegalStateException("No fue posible generar el código de barras.", ex);
		}
		return lienzo.getBufferedImage();
	}

	private static List<String> partir(String texto, PDFont fuente, float tamano, float anchoMaximo)
			throws IOException {
		List<String> lineas = new ArrayList<>();
		if (texto == null || texto.isBlank()) {
			return lineas;
		}
		StringBuilder actual = new StringBuilder();
		for (String palabra : texto.trim().split("\\s+")) {
			String candidata = actual.isEmpty() ? palabra : actual + " " + palabra;
			if (ancho(fuente, tamano, candidata) <= anchoMaximo) {
				actual = new StringBuilder(candidata);
				continue;
			}
			if (!actual.isEmpty()) {
				lineas.add(actual.toString());
			}
			// Palabra más ancha que la línea: se corta por caracteres.
			while (ancho(fuente, tamano, palabra) > anchoMaximo && palabra.length() > 1) {
				int corte = palabra.length() - 1;
				while (corte > 1 && ancho(fuente, tamano, palabra.substring(0, corte)) > anchoMaximo) {
					corte--;
				}
				lineas.add(palabra.substring(0, corte));
				palabra = palabra.substring(corte);
			}
			actual = new StringBuilder(palabra);
		}
		if (!actual.isEmpty()) {
			lineas.add(actual.toString());
		}
		return lineas;
	}

	private static float ancho(PDFont fuente, float tamano, String texto) throws IOException {
		return fuente.getStringWidth(texto) / 1000f * tamano;
	}

	/** Sustituye caracteres fuera de WinAnsi para que Helvetica pueda dibujarlos. */
	private static String sanear(String texto) {
		if (texto == null) {
			return "";
		}
		StringBuilder limpio = new StringBuilder(texto.length());
		for (char c : texto.toCharArray()) {
			if (c == '\n' || c == '\r' || c == '\t') {
				limpio.append(' ');
			} else if (c >= 32 && c <= 255) {
				limpio.append(c);
			} else {
				limpio.append('?');
			}
		}
		return limpio.toString();
	}

	private static String moneda(BigDecimal valor) {
		BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
		return "S/ " + seguro.setScale(2, RoundingMode.HALF_UP);
	}

	private static boolean tiene(String valor) {
		return valor != null && !valor.isBlank();
	}
}
