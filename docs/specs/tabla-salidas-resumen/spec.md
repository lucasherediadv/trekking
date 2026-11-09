# Especificación: presentación de la tabla de Salidas

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Borrador para aprobación  
**Fecha:** 27/10/2026  
**Alcance:** Orden visible de las columnas y resumen de Participantes en la tabla de Salidas.

## Problema

La columna Participantes muestra nombres junto con DNI, por lo que agrega información que no hace falta para reconocer quién participa y recarga visualmente la tabla.

## Solución

La tabla conserva este orden de lectura: **Fecha → Ruta → Participantes → Equipamiento asignado → Estado → Acciones**. La columna Participantes muestra únicamente los nombres de las personas, en formato **Nombre Apellido**, separados por coma. Cuando no haya participantes, se conserva el marcador vacío actual (`—`).

## Criterios de aceptación

1. Las columnas se presentan en el orden acordado: Fecha, Ruta, Participantes, Equipamiento asignado, Estado y Acciones.
2. Participantes contiene solo nombres en formato Nombre Apellido, separados por coma; no muestra DNI ni otros datos personales.
3. Una Salida sin participantes muestra `—`.
4. Se mantienen los datos y comportamientos actuales de las otras columnas, los filtros y las acciones por fila.

## Fuera de alcance

- Cambiar el orden de los nombres o el separador acordado.
- Cambiar los datos de participantes almacenados o el resumen usado en otras pantallas o documentos.
- Rediseñar filtros, anchos de columna, estilos, acciones o el resto de la pantalla.
- Cambiar el modelo de dominio, persistencia o reglas de Salidas.

## Validación prevista

La implementación debe compilar y verificarse manualmente en JavaFX. Comprobar una Salida con varios participantes, que solo se muestren sus nombres, y otra sin participantes, que conserve `—`. No hay infraestructura de pruebas automatizadas para esta interfaz.

Esta especificación no crea ni aprueba un ticket de implementación. El desglose de tickets es una etapa separada y requiere aprobación explícita de esta especificación.
