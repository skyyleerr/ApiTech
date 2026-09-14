/**
 * Script principal del Dashboard
 * Gestiona gráficas, actualizaciones en tiempo real y búsqueda
 */

let chartTemp = null;
let chartProduccion = null;
let chartEstados = null;

const datosTemperatura = [33, 34, 35, 36, 35, 34, 33];
let datosProduccion = [];
let colmenasActuales = [];
let ultimasColmenas = 0;

// Configuración de colores
const COLORES = {
    primary: '#FFC72C',
    success: '#10b981',
    warning: '#f59e0b',
    danger: '#ef4444',
    info: '#3b82f6',
    secondary: 'rgba(255, 199, 44, 0.1)'
};

/**
 * Actualiza todos los datos del dashboard
 */
function actualizarDatos() {
    fetch('generar_datos.php')
        .then(response => {
            if (!response.ok) {
                console.error('❌ Error HTTP:', response.status);
                throw new Error('Error en la respuesta: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (!data.success) {
                console.warn('⚠️ Respuesta sin datos válidos');
                return;
            }

            console.log('✓ Datos recibidos:', data);
            
            // Actualizar tarjetas de estadísticas
            actualizarTarjetas(data);
            
            // Detectar cambios en colmenas
            detectarCambiosColmenas(data);
            
            // Actualizar tabla de colmenas
            actualizarTabla(data);
            
            // Actualizar gráficas
            actualizarGraficas(data);
        })
        .catch(error => {
            console.error('❌ Error al obtener datos:', error);
            mostrarErrorDatos();
        });
}

/**
 * Actualiza las tarjetas de estadísticas
 */
function actualizarTarjetas(data) {
    const elementos = {
        'temperatura-promedio': data.temperatura_promedio + ' <span style="font-size: 14px;">°C</span>',
        'produccion-total': data.produccion_total + ' <span style="font-size: 14px;">kg</span>',
        'alertas-activas': data.alertas_activas,
        'colmenas-activas': data.colmenas_count
    };

    for (const [id, valor] of Object.entries(elementos)) {
        const elemento = document.getElementById(id);
        if (elemento) {
            elemento.innerHTML = valor;
        }
    }
}

/**
 * Detecta cambios en el número de colmenas
 */
function detectarCambiosColmenas(data) {
    if (data.colmenas.length !== ultimasColmenas) {
        console.log('🐝 Cambio en el número de colmenas detectado!');
        console.log(`   Anterior: ${ultimasColmenas}, Nuevo: ${data.colmenas.length}`);
        
        ultimasColmenas = data.colmenas.length;
        colmenasActuales = data.colmenas;
        
        // Reinicializar datos de producción
        datosProduccion = data.colmenas.map(c => c.produccion);
        
        // Actualizar gráfica de producción
        actualizarGraficaProduccion(data);
    }
}

/**
 * Actualiza la tabla de colmenas
 */
function actualizarTabla(data) {
    if (!data.colmenas || data.colmenas.length === 0) {
        console.warn('⚠️ No hay datos de colmenas');
        return;
    }

    data.colmenas.forEach(colmena => {
        let row = document.querySelector(`tr[data-id="${colmena.id}"]`);
        
        if (row) {
            // Actualizar celda de temperatura
            const tempCell = row.querySelector('.temp-cell');
            if (tempCell) {
                tempCell.textContent = colmena.temperatura + '°C';
                tempCell.style.color = obtenerColorTemperatura(colmena.temperatura);
            }

            // Actualizar celda de producción
            const prodCell = row.querySelector('.prod-cell');
            if (prodCell) {
                prodCell.textContent = colmena.produccion + ' kg';
            }

            // Actualizar celda de estado
            const estadoCell = row.querySelector('.estado-cell');
            if (estadoCell) {
                const estadoClass = obtenerClaseEstado(colmena.estado);
                estadoCell.innerHTML = `<span class="${estadoClass}">${colmena.estado}</span>`;
            }
        }
    });
}

/**
 * Obtiene el color según la temperatura
 */
function obtenerColorTemperatura(temperatura) {
    if (temperatura < 36) {
        return COLORES.success; // Verde
    } else if (temperatura < 38) {
        return COLORES.warning; // Ámbar
    } else {
        return COLORES.danger; // Rojo
    }
}

/**
 * Obtiene la clase de estado
 */
function obtenerClaseEstado(estado) {
    switch (estado) {
        case 'Estable':
            return 'estado-ok';
        case 'Advertencia':
            return 'estado-warn';
        case 'Problema':
            return 'estado-bad';
        default:
            return 'estado-ok';
    }
}

/**
 * Actualiza todas las gráficas
 */
function actualizarGraficas(data) {
    if (chartTemp) {
        actualizarGraficaTemperatura(data);
    }
    
    if (chartProduccion) {
        actualizarGraficaProduccion(data);
    }
}

/**
 * Actualiza la gráfica de temperatura
 */
function actualizarGraficaTemperatura(data) {
    if (!chartTemp) return;
    
    datosTemperatura.shift();
    datosTemperatura.push(data.temperatura_promedio);
    
    chartTemp.data.datasets[0].data = [...datosTemperatura];
    chartTemp.update('none');
}

/**
 * Actualiza la gráfica de producción
 */
function actualizarGraficaProduccion(data) {
    if (!chartProduccion || !data.colmenas) return;
    
    const labels = data.colmenas.map(c => c.nombre);
    const datos = data.colmenas.map(c => c.produccion);
    
    chartProduccion.data.labels = labels;
    chartProduccion.data.datasets[0].data = datos;
    chartProduccion.data.datasets[0].backgroundColor = data.colmenas.map((_, idx) => {
        const opacity = 0.6 + (idx * 0.15);
        return `rgba(255, 199, 44, ${Math.min(opacity, 1)})`;
    });
    
    chartProduccion.update('none');
}

/**
 * Muestra mensaje de error en datos
 */
function mostrarErrorDatos() {
    const contenedor = document.querySelector('.dashboard');
    if (contenedor) {
        console.error('Error al cargar datos del dashboard');
    }
}

/**
 * Inicializa la búsqueda en tabla
 */
function inicializarBusqueda() {
    const searchInput = document.getElementById('search');
    if (!searchInput) return;
    
    searchInput.addEventListener('keyup', function(e) {
        const searchTerm = e.target.value.toLowerCase();
        const tbody = document.querySelector('tbody');
        
        if (!tbody) return;
        
        tbody.querySelectorAll('tr').forEach(row => {
            const texto = row.textContent.toLowerCase();
            const visible = texto.includes(searchTerm);
            row.style.display = visible ? '' : 'none';
        });
    });
}

/**
 * Inicializa la gráfica de temperatura
 */
function inicializarGraficaTemperatura() {
    const ctx = document.getElementById('graficaTemp');
    if (!ctx) return;
    
    chartTemp = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sab', 'Dom'],
            datasets: [{
                label: 'Temperatura (°C)',
                data: [...datosTemperatura],
                borderColor: COLORES.primary,
                backgroundColor: COLORES.secondary,
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: COLORES.primary,
                pointBorderColor: '#fff',
                pointBorderWidth: 2,
                pointRadius: 5,
                pointHoverRadius: 7,
                pointStyle: 'circle'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            animation: { duration: 300 },
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    labels: { 
                        font: { size: 12, weight: '600' }, 
                        color: '#666',
                        usePointStyle: true
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    titleFont: { size: 13, weight: '600' },
                    bodyFont: { size: 12 },
                    callbacks: {
                        label: function(context) {
                            return context.parsed.y.toFixed(1) + '°C';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: false,
                    min: 30,
                    max: 40,
                    ticks: { 
                        color: '#999', 
                        font: { size: 12 },
                        callback: function(value) {
                            return value + '°C';
                        }
                    },
                    grid: { color: 'rgba(0, 0, 0, 0.05)' }
                },
                x: {
                    ticks: { color: '#999', font: { size: 12 } },
                    grid: { display: false }
                }
            }
        }
    });
}

/**
 * Inicializa la gráfica de producción
 */
function inicializarGraficaProduccion() {
    const ctx = document.getElementById('graficaProduccion');
    if (!ctx) return;
    
    chartProduccion = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: [],
            datasets: [{
                label: 'Producción (kg)',
                data: [],
                backgroundColor: [],
                borderColor: COLORES.primary,
                borderWidth: 2,
                borderRadius: 8,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            animation: { duration: 300 },
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    labels: { 
                        font: { size: 12, weight: '600' }, 
                        color: '#666',
                        usePointStyle: true
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    titleFont: { size: 13, weight: '600' },
                    bodyFont: { size: 12 },
                    callbacks: {
                        label: function(context) {
                            return context.parsed.y.toFixed(1) + ' kg';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { 
                        color: '#999', 
                        font: { size: 12 },
                        callback: function(value) {
                            return value + ' kg';
                        }
                    },
                    grid: { color: 'rgba(0, 0, 0, 0.05)' }
                },
                x: {
                    ticks: { color: '#999', font: { size: 12 } },
                    grid: { display: false }
                }
            }
        }
    });
}

/**
 * Inicialización al cargar el DOM
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Inicializando Dashboard...');
    
    // Inicializar gráficas
    inicializarGraficaTemperatura();
    inicializarGraficaProduccion();
    
    // Inicializar búsqueda
    inicializarBusqueda();
    
    // Cargar datos iniciales
    console.log('📊 Cargando datos iniciales...');
    actualizarDatos();
    
    // Actualizar datos cada 5 segundos
    const intervalo = setInterval(actualizarDatos, 5000);
    console.log('⏰ Actualizaciones cada 5 segundos activadas');
    
    // Pausar actualizaciones si la página está oculta
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            console.log('📴 Página oculta - pausando actualizaciones');
            clearInterval(intervalo);
        } else {
            console.log('📱 Página visible - reanudando actualizaciones');
            actualizarDatos();
            setInterval(actualizarDatos, 5000);
        }
    });
    
    console.log('✓ Dashboard inicializado correctamente');
});

/**
 * Descarga datos del dashboard como JSON
 */
function descargarDatos() {
    fetch('generar_datos.php')
        .then(response => response.json())
        .then(data => {
            const elemento = document.createElement('a');
            elemento.href = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(data, null, 2));
            elemento.download = `dashboard_${new Date().toISOString()}.json`;
            elemento.click();
        })
        .catch(error => console.error('Error descargando datos:', error));
}

/**
 * Recarga el dashboard
 */
function recargarDashboard() {
    console.log('🔄 Recargando dashboard...');
    actualizarDatos();
}
