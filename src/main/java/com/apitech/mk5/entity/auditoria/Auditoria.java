package com.apitech.mk5.entity.auditoria;

import com.apitech.mk5.entity.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/** Traza de acciones de usuarios o del actor automático Sistema. */
@Entity
@Table(name = "auditoria")
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_tipo", nullable = false, length = 7)
    private ActorTipo actorTipo;

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "modulo", length = 50)
    private String modulo;

    @Column(name = "entidad_tipo", length = 50)
    private String entidadTipo;

    @Column(name = "id_entidad")
    private Long idEntidad;

    @Column(name = "registro_afectado", length = 100)
    private String registroAfectado;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha", insertable = false, updatable = false)
    private LocalDateTime fecha;

    protected Auditoria() {
    }

    public Auditoria(Usuario usuario, ActorTipo actorTipo, String accion, String modulo,
                     String entidadTipo, Long idEntidad, String registroAfectado,
                     String descripcion) {
        this.usuario = usuario;
        this.actorTipo = actorTipo;
        this.accion = accion;
        this.modulo = modulo;
        this.entidadTipo = entidadTipo;
        this.idEntidad = idEntidad;
        this.registroAfectado = registroAfectado;
        this.descripcion = descripcion;
    }

    public Integer getIdAuditoria() { return idAuditoria; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public ActorTipo getActorTipo() { return actorTipo; }
    public void setActorTipo(ActorTipo actorTipo) { this.actorTipo = actorTipo; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getEntidadTipo() { return entidadTipo; }
    public void setEntidadTipo(String entidadTipo) { this.entidadTipo = entidadTipo; }
    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public String getRegistroAfectado() { return registroAfectado; }
    public void setRegistroAfectado(String registroAfectado) { this.registroAfectado = registroAfectado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Auditoria other)) return false;
        return idAuditoria != null && idAuditoria.equals(other.idAuditoria);
    }

    @Override
    public int hashCode() { return Objects.hashCode(getClass()); }
}
