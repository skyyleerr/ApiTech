package com.apitech.mk5.controller;

import com.apitech.mk5.entity.suscripcion.*;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.security.UsuarioContexto;
import com.apitech.mk5.service.PagoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PagoController {
    private final PagoService service; private final UsuarioContexto contexto; private final EmpresaRepository empresas;
    private final com.apitech.mk5.service.EmpresaOperacionService empresaOperacion;
    public PagoController(PagoService service, UsuarioContexto contexto, EmpresaRepository empresas,
                          com.apitech.mk5.service.EmpresaOperacionService empresaOperacion){
        this.service=service;this.contexto=contexto;this.empresas=empresas;this.empresaOperacion=empresaOperacion;
    }
    @GetMapping("/planes") public String planes(Model model){model.addAttribute("planes",service.planesActivos());return "planes";}
    @GetMapping("/app/pagos")
    public String pagos(Model model){
        Integer id=empresaActual();
        var suscripcion=empresaOperacion.suscripcionActual(id);
        model.addAttribute("suscripcion", suscripcion);
        model.addAttribute("suscripcionVencida", empresaOperacion.suscripcionVencida(id));
        model.addAttribute("empresaPausada", empresaOperacion.estaPausada(id));
        return "app/pagos";
    }
    @PostMapping("/app/pagos/checkout")
    public String checkout(){
        throw new com.apitech.mk5.exception.ReglaDeNegocioException("Los cambios de plan y pagos desde el panel están deshabilitados. Contacta a ApiTech.");
    }
        private Integer empresaActual(){Integer id=contexto.getIdEmpresaActual();if(id!=null)return id;return empresas.findAll().stream().findFirst().orElseThrow().getIdEmpresa();}
    private com.apitech.mk5.entity.empresa.Empresa empresaActualEntity(){return empresas.findById(empresaActual()).orElseThrow();}
}
