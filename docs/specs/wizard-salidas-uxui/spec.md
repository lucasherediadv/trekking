# Especificación: UX/UI del wizard de creación de Salidas

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Propuesta para revisión  
**Fecha:** 25/10/2026  
**Alcance:** Rediseño de la experiencia del wizard existente de creación de Salidas.

## Problem Statement

Al crear una Salida, el wizard ya divide el trabajo en cinco pasos, pero el indicador presenta los pasos como texto en línea y cada pantalla tiene poca jerarquía visual. Equipamiento y Participantes requieren controles verticales dispersos, y la Revisión concentra la información en texto corrido. Esto hace más difícil reconocer el progreso, completar asignaciones y detectar errores antes de confirmar.

## Solution

Refinar la experiencia visual del wizard en la ventana actual de la aplicación, manteniendo sus cinco pasos y reglas funcionales. El flujo presentará el progreso con estados claros, formularios agrupados en secciones, listas compactas para Equipamiento y Participantes, una revisión organizada por secciones y una barra inferior de navegación con acciones coherentes con el resto de la aplicación.

## User Stories

1. Como guía, quiero reconocer el paso actual y los pasos ya completados, para saber cuánto falta para confirmar una Salida.
2. Como guía, quiero ver el indicador de progreso como una secuencia de cinco pasos, para entender el orden del flujo de un vistazo.
3. Como guía, quiero que el paso Datos de la salida agrupe la fecha y su explicación, para entender qué información debo completar.
4. Como guía, quiero elegir una Ruta existente o crear una nueva dentro de una sección clara, para completar la planificación sin perder contexto.
5. Como guía, quiero consultar los datos principales de la Ruta elegida, para verificar que seleccioné la adecuada.
6. Como guía, quiero ver Equipamiento disponible y asignado junto, para elegir cantidades compatibles con el inventario.
7. Como guía, quiero agregar y quitar asignaciones de Equipamiento desde una lista compacta, para administrar lo que llevará la Salida.
8. Como guía, quiero buscar Clientes por DNI, nombre o apellido y ver los Participantes asignados en el mismo paso, para completar la lista sin recorrer controles separados.
9. Como guía, quiero acceder a la creación de un Cliente desde el paso Participantes, para agregarlo al borrador sin abandonar el wizard.
10. Como guía, quiero que la Revisión separe fecha, Ruta, Equipamiento y Participantes, para comprobar cada parte antes de confirmar.
11. Como guía, quiero poder volver desde la Revisión al paso que necesito corregir, para resolver errores sin reiniciar la planificación.
12. Como guía, quiero usar una barra inferior fija con Cancelar, Anterior y Siguiente o Confirmar, para mantener disponibles las acciones mientras recorro contenido extenso.
13. Como guía, quiero que la acción principal de avance o confirmación destaque visualmente, para identificar cómo continuar.
14. Como guía, quiero conservar los datos al avanzar y retroceder entre pasos, para no volver a ingresarlos.
15. Como guía, quiero cancelar con las protecciones actuales contra pérdida accidental, para descartar el borrador deliberadamente.

## Implementation Decisions

- Se conserva el wizard dentro de la ventana y la navegación actuales; no se cambia el patrón de navegación del shell.
- Se conservan cinco pasos y su orden: Datos de la salida, Ruta, Equipamiento, Participantes y Revisión.
- El indicador muestra los cinco pasos, distingue visualmente completados, actual y pendientes, y señala el progreso como “Paso N de 5”.
- Datos de la salida presenta la fecha dentro de una sección con título y breve descripción.
- Ruta presenta la elección entre Ruta existente y Ruta nueva como modos equivalentes y muestra el resumen de la Ruta seleccionada.
- Equipamiento organiza la disponibilidad y las asignaciones en una lista o tabla compacta. La disponibilidad sigue calculándose con las reglas actuales de reservas.
- Participantes prioriza búsqueda y alta de Cliente junto a una lista visible de Participantes asignados y su estado vacío.
- Revisión presenta tarjetas o secciones separadas para datos generales, Ruta, Equipamiento y Participantes, con acceso para volver a corregir los pasos correspondientes.
- La barra inferior permanece visible mientras se desplaza el contenido. Cancelar conserva su confirmación de descarte cuando hay cambios; la acción principal usa el estilo visual primario existente.
- La confirmación sigue guardando la Salida, sus asociaciones, copias operativas y reservas como una operación atómica. La presentación no altera las reglas del dominio ni agrega persistencia parcial.
- La creación de Clientes y Elementos de inventario mantiene sus diálogos y validaciones actuales; al volver, el borrador y el paso permanecen intactos.

## Testing Decisions

- **Seam acordado:** recorrido manual de extremo a extremo en JavaFX desde Salidas, más compilación Maven para detectar errores de integración.
- Un buen escenario verifica el comportamiento visible y los resultados persistidos, sin acoplarse a nombres de clases o estructura interna.
- El proyecto no cuenta con infraestructura de pruebas automatizadas de interfaz. No se afirma cobertura automatizada.
- Validar manualmente el indicador y los cinco pasos; avance y retroceso con datos conservados; búsqueda, asignación y eliminación de Equipamiento y Participantes; resumen de Revisión y retorno al paso elegido; diálogos auxiliares; desplazamiento con acciones inferiores accesibles; cancelación y confirmación; y persistencia y reservas correctas al confirmar.
- Compilar el módulo Maven. La validación funcional de la interfaz requiere ejecución manual de JavaFX.

## Out of Scope

- Cambios a las reglas de creación, validación o persistencia de Salidas, Rutas, Clientes, Equipamiento o reservas.
- Rediseño del flujo de edición de Salidas.
- Cambios de navegación, módulos o estructura del shell.
- Nuevas capacidades de inventario, participantes o checkpoints.
- Migración de datos o cambios de esquema.

## Further Notes

- Las referencias visuales proporcionadas muestran el indicador de progreso, secciones para Datos y Ruta, una vista tabular de Equipamiento, búsqueda y estado vacío para Participantes, una Revisión agrupada y una barra inferior con la acción primaria destacada.
- Respetar los ADR existentes sobre copia operativa reemplazable y reserva atómica de stock.
- La especificación funcional general de Salidas define las reglas de dominio que este rediseño conserva.
