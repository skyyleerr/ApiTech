package com.apitech.mk5.service.impl;

import com.apitech.mk5.service.ReporteService;

import com.apitech.mk5.entity.apiario.Medicion;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de generación de reportes (ítem 6).
 *
 * <p>Soporta tres formatos de exportación:</p>
 * <ul>
 *   <li>PDF — usando iText 8</li>
 *   <li>Excel (.xlsx) — usando Apache POI</li>
 *   <li>CSV — con BOM UTF-8 para compatibilidad con Excel</li>
 * </ul>
 *
 * <p>Todos los métodos reciben la lista ya filtrada de mediciones
 * (el filtrado multicriterio se aplica en el repositorio antes de
 * llegar aquí) y generan el archivo directamente sobre el
 * {@code OutputStream} de la respuesta HTTP, sin crear archivos
 * temporales en disco.</p>
 */
@Service
public class ReporteServiceImpl implements ReporteService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String[] HEADERS = {
        "ID Medición", "Tipo", "Valor", "Unidad", "Origen", "Fecha"
    };

    // ─────────────────────────────────────────────────────────────
    // PDF
    // ─────────────────────────────────────────────────────────────

    /**
     * Genera un PDF con la tabla de mediciones filtradas.
     *
     * @param mediciones lista de mediciones a incluir.
     * @param out        stream de salida (response de HTTP).
     */
    public void generarPdf(List<Medicion> mediciones, OutputStream out)
            throws IOException {

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Título
        document.add(new Paragraph("Reporte de Mediciones — ApiTech")
                .setFontSize(18).setBold());
        document.add(new Paragraph(
                "Total de registros: " + mediciones.size())
                .setFontSize(11).setMarginBottom(12));

        // Tabla
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{10, 15, 10, 8, 12, 20}))
                .useAllAvailableWidth();

        // Encabezados
        for (String header : HEADERS) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(10)));
        }

        // Filas
        for (Medicion m : mediciones) {
            table.addCell(String.valueOf(m.getIdMedicion()));
            table.addCell(m.getTipoMedicion().name());
            table.addCell(String.valueOf(m.getValor()));
            table.addCell(m.getUnidad());
            table.addCell(m.getOrigen().name());
            table.addCell(m.getFecha() != null
                    ? m.getFecha().format(FMT) : "—");
        }

        document.add(table);
        document.close();
    }

    // ─────────────────────────────────────────────────────────────
    // Excel
    // ─────────────────────────────────────────────────────────────

    /**
     * Genera un archivo Excel (.xlsx) con la tabla de mediciones.
     */
    public void generarExcel(List<Medicion> mediciones, OutputStream out)
            throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mediciones");

            // Estilo encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fila de encabezados
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filas de datos
            int rowNum = 1;
            for (Medicion m : mediciones) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(m.getIdMedicion());
                row.createCell(1).setCellValue(m.getTipoMedicion().name());
                row.createCell(2).setCellValue(m.getValor());
                row.createCell(3).setCellValue(m.getUnidad());
                row.createCell(4).setCellValue(m.getOrigen().name());
                row.createCell(5).setCellValue(
                        m.getFecha() != null ? m.getFecha().format(FMT) : "");
            }

            // Ajustar ancho de columnas automáticamente
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CSV
    // ─────────────────────────────────────────────────────────────

    /**
     * Genera un CSV separado por punto y coma (compatible con Excel
     * en español / Colombia).
     */
    public void generarCsv(List<Medicion> mediciones, PrintWriter writer) {
        // Encabezados
        writer.println(String.join(";", HEADERS));

        // Filas
        for (Medicion m : mediciones) {
            writer.println(String.join(";",
                    String.valueOf(m.getIdMedicion()),
                    m.getTipoMedicion().name(),
                    String.valueOf(m.getValor()),
                    m.getUnidad(),
                    m.getOrigen().name(),
                    m.getFecha() != null ? m.getFecha().format(FMT) : ""
            ));
        }

        writer.flush();
    }
}
