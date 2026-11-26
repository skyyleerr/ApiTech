package com.apitech.mk5.controller;

import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.repository.apiario.AlertaRepository;
import com.apitech.mk5.repository.apiario.ColmenaRepository;
import com.apitech.mk5.repository.apiario.MedicionRepository;
import com.apitech.mk5.repository.apiario.SensorRepository;
import com.apitech.mk5.repository.apiario.SensorColmenaRepository;
import com.apitech.mk5.security.UsuarioContexto;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Dashboard del cliente (Admin_Cliente y Empleado_Cliente).
 *
 * <p>Muestra resumen de colmenas, sensores y alertas activas
 * de la empresa del usuario autenticado.</p>
 */
@Controller
@RequestMapping("/app")
@PreAuthorize("hasAnyRole('Admin_Cliente','Empleado_Cliente')")
public class AppDashboardController {

    private final ColmenaRepository colmenaRepository;
    private final SensorRepository sensorRepository;
    private final AlertaRepository alertaRepository;
    private final SensorColmenaRepository sensorColmenaRepository;
    private final MedicionRepository medicionRepository;
    private final UsuarioContexto usuarioContexto;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;

    public AppDashboardController(ColmenaRepository colmenaRepository,
                                  SensorRepository sensorRepository,
                                  AlertaRepository alertaRepository,
                                  SensorColmenaRepository sensorColmenaRepository,
                                  MedicionRepository medicionRepository,
                                  UsuarioContexto usuarioContexto,
                                  com.apitech.mk5.service.EmpresaOperacionService empresaOperacion) {
        this.colmenaRepository = colmenaRepository;
        this.sensorRepository = sensorRepository;
        this.alertaRepository = alertaRepository;
        this.sensorColmenaRepository = sensorColmenaRepository;
        this.medicionRepository = medicionRepository;
        this.usuarioContexto = usuarioContexto;
        this.empresaOperacion = empresaOperacion;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Usuario usuario = usuarioContexto.getUsuarioActual();
        Integer idEmpresa = usuarioContexto.getIdEmpresaActual();

        model.addAttribute("usuario", usuario);
        model.addAttribute("totalColmenas",
                colmenaRepository.findByEmpresa_IdEmpresa(idEmpresa).size());
        model.addAttribute("colmenas",
                colmenaRepository.findByEmpresa_IdEmpresa(idEmpresa));
        model.addAttribute("totalSensores",
                sensorRepository.findByEmpresa_IdEmpresa(idEmpresa).size());
        model.addAttribute("empresaPausada", empresaOperacion.estaPausada(idEmpresa));
        model.addAttribute("limiteColmenas", empresaOperacion.limiteColmenas(idEmpresa));
        model.addAttribute("limiteSensores", empresaOperacion.limiteSensores(idEmpresa));
        model.addAttribute("alertasActivas",
                alertaRepository.findByIdEmpresaAndEstado(
                        idEmpresa, "ACTIVA",
                        PageRequest.of(0, 5)).getTotalElements());
        model.addAttribute("ultimasAlertas",
                alertaRepository.findByIdEmpresaAndEstado(
                        idEmpresa, "ACTIVA",
                        PageRequest.of(0, 5)).getContent());

        if (usuarioContexto.tieneRol("Admin_Cliente")) {
            return "app/dashboard-admin-cliente";
        }
        return "app/dashboard-empleado-cliente";
    }

    @GetMapping("/generar_datos.php")
    @ResponseBody
    public Map<String,Object> datosDashboard() {
        Integer idEmpresa = usuarioContexto.getIdEmpresaActual();
        List<Map<String, Object>> datos = new ArrayList<>();
        double sumaTemperatura = 0;
        int temperaturas = 0;
        double produccion = 0;
        for (var colmena : colmenaRepository.findByEmpresa_IdEmpresa(idEmpresa)) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id", colmena.getIdColmena());
            fila.put("nombre", colmena.getNombre());
            fila.put("estado", colmena.getEstado());
            Double temperatura = null;
            Double humedad = null;
            Double peso = null;
            for (var asociacion : sensorColmenaRepository.findByColmena_IdColmenaAndActivoTrue(colmena.getIdColmena())) {
                var ultima = medicionRepository.findTopByAsociacion_IdAsociacionOrderByFechaDesc(asociacion.getIdAsociacion());
                if (ultima.isEmpty()) continue;
                switch (ultima.get().getTipoMedicion()) {
                    case TEMPERATURA -> temperatura = (double) ultima.get().getValor();
                    case HUMEDAD -> humedad = (double) ultima.get().getValor();
                    case PESO -> { peso = (double) ultima.get().getValor(); produccion += peso; }
                }
            }
            fila.put("temperatura", temperatura == null ? 0 : temperatura);
            fila.put("humedad", humedad == null ? 0 : humedad);
            fila.put("peso", peso == null ? 0 : peso);
            fila.put("produccion", peso == null ? 0 : peso);
            datos.add(fila);
            if (temperatura != null) { sumaTemperatura += temperatura; temperaturas++; }
        }
        return Map.of("success", true, "colmenas", datos,
                "colmenas_count", datos.size(),
                "temperatura_promedio", temperaturas == 0 ? 0 : sumaTemperatura / temperaturas,
                "produccion_total", produccion,
                "alertas_activas", alertaRepository.findByIdEmpresaAndEstado(idEmpresa, "ACTIVA", PageRequest.of(0, 5)).getTotalElements());
    }
}
