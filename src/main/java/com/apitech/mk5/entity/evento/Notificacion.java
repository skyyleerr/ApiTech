package com.apitech.mk5.entity.evento;

import com.apitech.mk5.entity.apiario.Alerta;
import com.apitech.mk5.entity.empresa.Empresa;
import com.apitech.mk5.entity.suscripcion.Pago;
import com.apitech.mk5.entity.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/** Entidad JPA para las notificaciones dirigidas a usuarios. */
@Entity
@Table(name = "notificaciones")
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa")
    private Empresa empresa;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fecha", insertable = false, updatable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alerta")
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago")
    private Pago pago;

    protected Notificacion() {
    }

    public Notificacion(Usuario usuario, Empresa empresa, String tipo, String titulo,
                        String mensaje, boolean leida, Alerta alerta, Pago pago) {
        this.usuario = usuario;
        this.empresa = empresa;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.leida = leida;
        this.alerta = alerta;
        this.pago = pago;
    }

    public Integer getIdNotificacion() { return idNotificacion; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getFecha() { return fecha; }
    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }
    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notificacion other)) return false;
        return idNotificacion != null && idNotificacion.equals(other.idNotificacion);
    }

    @Override
    public int hashCode() { return Objects.hashCode(getClass()); }

    @Override
    public String toString() {
        return "Notificacion{" + "idNotificacion=" + idNotificacion +
                ", tipo='" + tipo + '\'' + ", titulo='" + titulo + '\'' +
                ", leida=" + leida + '}';
    }
}
