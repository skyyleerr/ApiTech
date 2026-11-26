/**
 * Script de gestión de alertas en tiempo real
 * Carga y actualiza alertas desde obtener_alertas.php
 */

let alertasActivas = [];
let filtroActual = 'todas';

/**
 * Carga alertas desde el servidor
 */
function cargarAlertas() {
    fetch('../DASHBOARD/obtener_alertas.php')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.alertas) {
                alertasActivas = data.alertas;
                actualizarVistaBadge();
                actualizarModalAlertas();
            } else {
                console.warn('Respuesta sin alertas:', data);
            }
        })
        .catch(error => {
            console.error('Error al cargar alertas:', error);
        });
}

/**
 * Actualiza el badge de alertas en la topbar
 */
function actualizarVistaBadge() {
    const badgeAlertas = document.getElementById('badge-alertas');
    if (!badgeAlertas) return;
    
    // Contar solo alertas críticas para el badge
    const alertasCriticas = alertasActivas.filter(a => a.tipo === 'error').length;
    const total = alertasActivas.length;
    
    if (alertasCriticas > 0) {
        badgeAlertas.textContent = alertasCriticas;
        badgeAlertas.style.display = 'flex';
        badgeAlertas.title = `${alertasCriticas} alerta(s) crítica(s) de ${total} total`;
    } else if (total > 0) {
        badgeAlertas.textContent = total;
        badgeAlertas.style.display = 'flex';
        badgeAlertas.title = `${total} alerta(s) activa(s)`;
    } else {
        badgeAlertas.style.display = 'none';
    }
}

/**
 * Actualiza el modal/dropdown de alertas
 */
function actualizarModalAlertas() {
    const modalAlertas = document.getElementById('modal-alertas');
    if (!modalAlertas) return;
    
    const containerAlertas = modalAlertas.querySelector('.modal-alertas-content');
    if (!containerAlertas) return;
    
    if (alertasActivas.length === 0) {
        containerAlertas.innerHTML = `
            <div style="padding: 30px 20px; text-align: center; color: #999;">
                <i class="fa-solid fa-check-circle" style="font-size: 32px; margin-bottom: 10px; display: block;"></i>
                <p style="margin: 0; font-size: 13px;">No hay alertas activas</p>
            </div>
        `;
        return;
    }
    
    const html = alertasActivas.slice(0, 10).map(alerta => {
        const colorTipo = alerta.tipo === 'error' ? '#ef4444' : 
                         alerta.tipo === 'warning' ? '#f59e0b' : '#3b82f6';
        const iconoTipo = alerta.tipo === 'error' ? 'fa-circle-xmark' : 
                         alerta.tipo === 'warning' ? 'fa-triangle-exclamation' : 'fa-info-circle';
        
        return `
            <div style="padding: 12px; border-bottom: 1px solid #f0f0f0; border-left: 3px solid ${colorTipo}; cursor: pointer; transition: all 0.2s ease;" 
                 onmouseover="this.style.background='rgba(255,199,44,0.05)'" 
                 onmouseout="this.style.background='white'">
                <div style="display: flex; gap: 10px; align-items: flex-start;">
                    <i class="fa-solid ${iconoTipo}" style="color: ${colorTipo}; font-size: 16px; margin-top: 2px; flex-shrink: 0;"></i>
                    <div style="flex: 1; min-width: 0;">
                        <strong style="color: ${colorTipo}; font-size: 12px; display: block;">${alerta.titulo}</strong>
                        <p style="margin: 4px 0 0 0; color: #666; font-size: 11px; word-wrap: break-word; white-space: normal;">${alerta.mensaje}</p>
                        <span style="color: #999; font-size: 10px;">${alerta.timestamp}</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    const footer = alertasActivas.length > 10 ? 
        `<div style="padding: 10px; text-align: center; border-top: 1px solid #f0f0f0;">
            <a href="../DASHBOARD/alertas.php" style="color: #FFC72C; text-decoration: none; font-size: 12px; font-weight: 600;">
                Ver todas las alertas (${alertasActivas.length})
            </a>
         </div>` : '';
    
    containerAlertas.innerHTML = html + footer;
}

/**
 * Abre/cierra el modal de alertas
 */
function toggleModalAlertas() {
    const modalAlertas = document.getElementById('modal-alertas');
    if (!modalAlertas) return;
    
    const isVisible = modalAlertas.style.display === 'flex';
    modalAlertas.style.display = isVisible ? 'none' : 'flex';
}

/**
 * Cierra el modal de alertas
 */
function cerrarModalAlertas() {
    const modalAlertas = document.getElementById('modal-alertas');
    if (modalAlertas) {
        modalAlertas.style.display = 'none';
    }
}

/**
 * Filtra alertas por tipo
 */
function filtrarAlertas(tipo) {
    filtroActual = tipo;
    
    if (tipo === 'todas') {
        actualizarModalAlertas();
    } else {
        const modalAlertas = document.getElementById('modal-alertas');
        if (!modalAlertas) return;
        
        const containerAlertas = modalAlertas.querySelector('.modal-alertas-content');
        if (!containerAlertas) return;
        
        const alertasFiltradas = alertasActivas.filter(a => a.tipo === tipo);
        
        if (alertasFiltradas.length === 0) {
            containerAlertas.innerHTML = `
                <div style="padding: 30px 20px; text-align: center; color: #999;">
                    <i class="fa-solid fa-filter" style="font-size: 32px; margin-bottom: 10px; display: block;"></i>
                    <p style="margin: 0; font-size: 13px;">No hay alertas ${tipo}</p>
                </div>
            `;
            return;
        }
        
        const html = alertasFiltradas.slice(0, 10).map(alerta => {
            const colorTipo = alerta.tipo === 'error' ? '#ef4444' : 
                             alerta.tipo === 'warning' ? '#f59e0b' : '#3b82f6';
            const iconoTipo = alerta.tipo === 'error' ? 'fa-circle-xmark' : 
                             alerta.tipo === 'warning' ? 'fa-triangle-exclamation' : 'fa-info-circle';
            
            return `
                <div style="padding: 12px; border-bottom: 1px solid #f0f0f0; border-left: 3px solid ${colorTipo}; cursor: pointer; transition: all 0.2s ease;" 
                     onmouseover="this.style.background='rgba(255,199,44,0.05)'" 
                     onmouseout="this.style.background='white'">
                    <div style="display: flex; gap: 10px; align-items: flex-start;">
                        <i class="fa-solid ${iconoTipo}" style="color: ${colorTipo}; font-size: 16px; margin-top: 2px; flex-shrink: 0;"></i>
                        <div style="flex: 1; min-width: 0;">
                            <strong style="color: ${colorTipo}; font-size: 12px; display: block;">${alerta.titulo}</strong>
                            <p style="margin: 4px 0 0 0; color: #666; font-size: 11px; word-wrap: break-word; white-space: normal;">${alerta.mensaje}</p>
                            <span style="color: #999; font-size: 10px;">${alerta.timestamp}</span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
        containerAlertas.innerHTML = html;
    }
}

/**
 * Marca una alerta como leída
 */
function marcarAlertaLeida(alertaId) {
    console.log('Marca alerta como leída:', alertaId);
    // TODO: Implementar marca como leída en BD
}

/**
 * Descarta una alerta
 */
function descartarAlerta(alertaId) {
    console.log('Descarta alerta:', alertaId);
    // TODO: Implementar descarte en BD
}

/**
 * Inicializa el script
 */
document.addEventListener('DOMContentLoaded', function() {
    const btnAlertas = document.getElementById('btn-alertas');
    const modalAlertas = document.getElementById('modal-alertas');
    
    // Setup del botón de alertas
    if (btnAlertas && modalAlertas) {
        btnAlertas.addEventListener('click', function(e) {
            e.stopPropagation();
            toggleModalAlertas();
        });
        
        // Cerrar modal al hacer click fuera
        document.addEventListener('click', function(e) {
            if (!btnAlertas.contains(e.target) && !modalAlertas.contains(e.target)) {
                cerrarModalAlertas();
            }
        });
        
        // Evitar cerrar al hacer click dentro del modal
        modalAlertas.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    }
    
    // Cargar alertas inicialmente
    cargarAlertas();
    
    // Actualizar alertas cada 5 segundos
    setInterval(cargarAlertas, 5000);
    
    console.log('✓ Script de alertas.js cargado correctamente');
});

/**
 * Escuchar cambios de visibilidad de la página
 */
document.addEventListener('visibilitychange', function() {
    if (document.hidden) {
        console.log('Página oculta - pausando actualización de alertas');
    } else {
        console.log('Página visible - reanudando actualización de alertas');
        cargarAlertas();
    }
});
