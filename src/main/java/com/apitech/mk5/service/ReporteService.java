package com.apitech.mk5.service;

import com.apitech.mk5.entity.apiario.Medicion;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/** Servicio para generar reportes de mediciones en PDF, Excel y CSV. */
public interface ReporteService {
    void generarPdf(List<Medicion> mediciones, OutputStream out) throws IOException;
    void generarExcel(List<Medicion> mediciones, OutputStream out) throws IOException;
    void generarCsv(List<Medicion> mediciones, PrintWriter writer);
}
