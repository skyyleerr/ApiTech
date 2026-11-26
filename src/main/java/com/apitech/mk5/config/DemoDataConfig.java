package com.apitech.mk5.config;

import com.apitech.mk5.entity.apiario.*;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.usuario.Rol;
import com.apitech.mk5.entity.usuario.Usuario;
import com.apitech.mk5.entity.suscripcion.PlanSuscripcion;
import com.apitech.mk5.repository.apiario.*;
import com.apitech.mk5.repository.empresa.EmpresaRepository;
import com.apitech.mk5.repository.usuario.RolRepository;
import com.apitech.mk5.repository.usuario.UsuarioRepository;
import com.apitech.mk5.repository.suscripcion.PlanSuscripcionRepository;
import com.apitech.mk5.repository.suscripcion.SuscripcionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;

@Configuration
public class DemoDataConfig {
    @Bean
    CommandLineRunner cargarDatosDemo(EmpresaRepository empresas, RolRepository roles,
                                      UsuarioRepository usuarios, ColmenaRepository colmenas,
                                      PlanSuscripcionRepository planes, SuscripcionRepository suscripciones,
                                      SensorRepository sensores, SensorColmenaRepository asociaciones,
                                      MonitoreoRepository monitoreos, MedicionRepository mediciones,
                                      PasswordEncoder encoder) {
        return args -> {
            Rol adminApi = roles.findByNombreRol("Admin_ApiTech")
                    .orElseGet(() -> roles.save(new Rol("Admin_ApiTech", "Administrador de la plataforma")));
            Rol empleadoApi = roles.findByNombreRol("Empleado_ApiTech")
                    .orElseGet(() -> roles.save(new Rol("Empleado_ApiTech", "Operador de la plataforma")));
            Rol adminCliente = roles.findByNombreRol("Admin_Cliente")
                    .orElseGet(() -> roles.save(new Rol("Admin_Cliente", "Administrador de empresa cliente")));
            Rol empleadoCliente = roles.findByNombreRol("Empleado_Cliente")
                    .orElseGet(() -> roles.save(new Rol("Empleado_Cliente", "Operador de empresa cliente")));
            Empresa empresa = empresas.findByNit("900123456-7")
                    .orElseGet(() -> empresas.save(new Empresa("900123456-7", "Apícola El Panal S.A.S.",
                            "contacto@elpanal.co", "3001234567", "Vereda El Roble", "activa")));
            // ApiTech ofrece únicamente dos planes. Un plan antiguo se deja inactivo
            // para no romper suscripciones históricas.
            var basico = planes.findByNombrePlan("Plan Básico").orElseGet(() ->
                    planes.save(new PlanSuscripcion("Plan Básico", new BigDecimal("49000"), 30, "activo")));
            var profesional = planes.findByNombrePlan("Plan Profesional").orElseGet(() ->
                    planes.save(new PlanSuscripcion("Plan Profesional", new BigDecimal("99000"), 30, "activo")));
            planes.findAll().stream()
                    .filter(p -> !"Plan Básico".equals(p.getNombrePlan()) && !"Plan Profesional".equals(p.getNombrePlan()))
                    .forEach(p -> p.setEstado("inactivo"));
            planes.saveAll(planes.findAll());
            crearUsuario(usuarios, encoder, "admin@apitech.com", "Administrador", "ApiTech", adminApi, null);
            crearUsuario(usuarios, encoder, "admin.etapaa@apitech.com", "Administrador", "Etapa A", adminApi, null);
            crearUsuario(usuarios, encoder, "operador@apitech.com", "Operador", "ApiTech", empleadoApi, null);
            Usuario cliente = crearUsuario(usuarios, encoder, "admin@apitech-cliente.com", "Administradora", "El Panal", adminCliente, empresa);
            crearUsuario(usuarios, encoder, "usuario@apitech-cliente.com", "Operario", "El Panal", empleadoCliente, empresa);
            if (suscripciones.findFirstByEmpresa_IdEmpresaAndEstadoOrderByIdSuscripcionDesc(
                    empresa.getIdEmpresa(), com.apitech.mk5.entity.suscripcion.Suscripcion.EstadoSuscripcion.ACTIVA).isEmpty()) {
                var demoSub = new com.apitech.mk5.entity.suscripcion.Suscripcion(empresa, basico);
                demoSub.activar();
                suscripciones.save(demoSub);
            }
            if (colmenas.findByEmpresa_IdEmpresa(empresa.getIdEmpresa()).isEmpty()) {
                colmenas.save(new Colmena(empresa, cliente, "Panal Norte", "Sector La Montaña", "Estable"));
                colmenas.save(new Colmena(empresa, cliente, "Panal Sur", "Sector El Roble", "Advertencia"));
            }
            Colmena colmena = colmenas.findByEmpresa_IdEmpresaAndEstado(empresa.getIdEmpresa(), "Estable")
                    .stream().findFirst().orElse(null);
            if (colmena != null) {
                crearSensorYDatos(empresa, colmena, "SIM-TEMP-001", TipoMedicion.TEMPERATURA, 28.5f, "°C", sensores, asociaciones, monitoreos, mediciones);
                crearSensorYDatos(empresa, colmena, "SIM-HUM-001", TipoMedicion.HUMEDAD, 64.0f, "%", sensores, asociaciones, monitoreos, mediciones);
                crearSensorYDatos(empresa, colmena, "SIM-PESO-001", TipoMedicion.PESO, 18.2f, "kg", sensores, asociaciones, monitoreos, mediciones);
            }
        };
    }

    private void crearSensorYDatos(Empresa empresa, Colmena colmena, String codigo, TipoMedicion tipo,
                                   float valor, String unidad, SensorRepository sensores,
                                   SensorColmenaRepository asociaciones, MonitoreoRepository monitoreos,
                                   MedicionRepository mediciones) {
        Sensor sensor = sensores.findByCodigo(codigo).orElseGet(() ->
                sensores.save(new Sensor(empresa, codigo, tipo, "ApiTech Demo", "ApiTech", true, "activo")));
        SensorColmena asociacion = asociaciones.findBySensor_IdSensorAndActivoTrue(sensor.getIdSensor())
                .orElseGet(() -> asociaciones.save(new SensorColmena(sensor, colmena, empresa.getIdEmpresa())));
        if (!monitoreos.existsByAsociacion_IdAsociacionAndEstado(asociacion.getIdAsociacion(), "activo")) {
            monitoreos.save(new Monitoreo(asociacion, "activo"));
        }
        if (mediciones.findTopByAsociacion_IdAsociacionOrderByFechaDesc(asociacion.getIdAsociacion()).isEmpty()) {
            mediciones.save(new Medicion(asociacion, tipo, valor, unidad, OrigenMedicion.SIMULADO));
        }
    }

    private Usuario crearUsuario(UsuarioRepository repo, PasswordEncoder encoder, String correo,
                                 String nombre, String apellido, Rol rol, Empresa empresa) {
        return repo.findByCorreo(correo).orElseGet(() -> repo.save(new Usuario(
                empresa, rol, nombre, apellido, correo, encoder.encode(correo.equals("admin.etapaa@apitech.com") ? "ApiTechA2026!" : "123456"), "activo")));
    }
}
