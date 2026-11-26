package com.apitech.mk5.entity.apiario;

/**
 * Tipos de medicion soportados por el sistema, reflejando exactamente
 * el {@code ENUM('TEMPERATURA','HUMEDAD','PESO')} usado en varias
 * columnas del esquema SQL: {@code sensores.tipo},
 * {@code mediciones.tipo_medicion} y {@code rangos_umbrales.tipo_medicion}.
 *
 * <p>Se define una sola vez, en un unico archivo, y se reutiliza en
 * todas las entidades que lo necesitan -- evita que cada entidad
 * declare su propio enum con los mismos tres valores (lo que tarde o
 * temprano llevaria a inconsistencias, por ejemplo si alguien escribe
 * "Temperatura" en una entidad y "TEMPERATURA" en otra).</p>
 *
 * <p>Los nombres de las constantes deben coincidir EXACTAMENTE (mismas
 * mayusculas) con los valores del {@code ENUM} de MySQL, porque se
 * mapean con {@code @Enumerated(EnumType.STRING)}: Hibernate guarda y
 * compara el nombre de la constante como texto, no su posicion
 * numerica -- eso es intencional, para que el valor guardado en la
 * base de datos sea legible directamente (p. ej. {@code 'TEMPERATURA'})
 * y no dependa del orden en que se declararon las constantes aqui.</p>
 */
public enum TipoMedicion {
    TEMPERATURA,
    HUMEDAD,
    PESO
}
