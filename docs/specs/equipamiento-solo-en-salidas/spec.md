# Especificación funcional: Equipamiento solo en Salidas

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Borrador para revisión  
**Fecha:** 25/10/2026  
**Alcance:** Retirar de las Rutas la gestión de Equipamiento y mantener las asignaciones de Equipamiento en las Salidas.

## Problem Statement

La aplicación permite asociar Equipamiento a una Ruta, aunque la planificación y la reserva de los elementos corresponden a cada Salida. Esto mezcla una plantilla técnica reutilizable con una asignación operativa que puede variar entre Salidas.

## Solution

Las Rutas dejan de gestionar Equipamiento. Cada Salida mantiene su propia asignación de Elementos de inventario y cantidades, independiente de la Ruta elegida. Al cambiar o actualizar la Ruta de una Salida, su Equipamiento asignado se conserva.

## User Stories

1. Como guía, quiero crear o editar una Ruta sin asociarle Equipamiento, para mantener la Ruta como plantilla técnica reutilizable.
2. Como guía, quiero consultar una Ruta sin una sección de Equipamiento, para distinguir sus datos técnicos de la planificación operativa.
3. Como guía, quiero asignar Elementos de inventario y cantidades a una Salida, para registrar el Equipamiento que se llevará en esa ocasión.
4. Como guía, quiero que cada Salida conserve su propia asignación de Equipamiento, para que dos Salidas de una misma Ruta puedan llevar elementos o cantidades diferentes.
5. Como guía, quiero que cambiar la Ruta de una Salida conserve su Equipamiento, para no perder la asignación operativa al reemplazar la plantilla.
6. Como guía, quiero que actualizar una Salida desde su Ruta conserve su Equipamiento, para refrescar los datos de la Ruta sin alterar la planificación de elementos.
7. Como guía, quiero consultar el Equipamiento guardado en una Salida aunque cambie el Inventario de origen, para conservar una copia operativa de lo planificado.

## Implementation Decisions

- La relación entre Ruta y Equipamiento se elimina de la interfaz y del comportamiento de la aplicación.
- El alta, edición y detalle de Ruta no ofrecen acciones ni secciones para consultar o modificar Equipamiento.
- El Inventario sigue siendo el catálogo de Elementos disponibles.
- Las asignaciones de Equipamiento se realizan desde una Salida y pertenecen exclusivamente a esa Salida.
- Cada asignación de Salida conserva el Elemento y la cantidad planificada como parte de su copia operativa.
- Cambiar la Ruta o usar “Actualizar desde ruta” no modifica las asignaciones de Equipamiento de la Salida.
- No se migran asociaciones Ruta–Equipamiento existentes. El alcance supone iniciar con una base nueva, de acuerdo con la especificación vigente de Salidas, Rutas e Inventario.

## Testing Decisions

- El seam de verificación es un recorrido manual de extremo a extremo en JavaFX, desde las pantallas de Ruta hasta el alta y edición de una Salida.
- Un buen escenario verifica el comportamiento visible y los datos operativos guardados: una Ruta se crea y edita sin Equipamiento; una Salida permite asignar un Elemento y una cantidad; el detalle de la Salida muestra esa asignación; cambiar la Ruta y actualizar desde Ruta la conservan.
- La verificación cubre las pantallas de Rutas, el flujo de alta/edición de Salidas y el detalle de Salida.
- El repositorio no tiene infraestructura de pruebas automatizadas ni un árbol de pruebas existente para este flujo. La spec vigente prevé compilación y validación manual de JavaFX.

## Out of Scope

- Migrar o transformar datos de bases existentes.
- Crear plantillas de Equipamiento por Ruta o heredar Equipamiento automáticamente desde una Ruta.
- Cambiar el catálogo de Inventario, las cantidades disponibles, las reservas de stock o sus reglas.
- Cambiar otros datos que una Salida copia desde su Ruta.

## Further Notes

El glosario y la especificación vigente de Salidas, Rutas e Inventario ya definen el Equipamiento como asignación de una Salida y excluyen las plantillas de Equipamiento por Ruta. Esta spec concreta la retirada de la asociación que todavía ofrece la aplicación.
