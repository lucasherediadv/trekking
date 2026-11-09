# Especificación funcional: rediseño de la gestión del Itinerario

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Cerrada — implementación completada
**Fecha:** 26/10/2026
**Alcance:** Presentación de la gestión del Itinerario de una Ruta y formulario de alta/edición de sus Checkpoints.

## Problem Statement

La gestión del Itinerario presenta los Checkpoints en una lista simple y ofrece sus acciones fuera de cada fila. El alta y la edición abren un diálogo separado, con una distribución distinta a los formularios ya rediseñados. Esto reduce el espacio disponible, oculta el contexto de la Ruta y hace menos consistente el flujo de gestión.

## Solution

Rediseñar la pantalla del Itinerario con una lista compacta de Checkpoints, basada en la referencia visual y adaptada a la identidad clara de TrekRisk. Cada fila permitirá reconocer hora, nombre, altitud y coordenadas, además de editar o quitar ese Checkpoint. La acción para agregar estará junto al encabezado de la lista y Guardar Itinerario permanecerá claramente identificable.

El alta y la edición reemplazarán el contenido central dentro de la ventana actual, manteniendo la shell y la navegación visibles. El formulario seguirá el patrón de `rediseno-formularios`: título y descripción, tarjeta, labels sobre controles, alturas y espaciado uniformes, ayudas pertinentes y errores de campo visibles. Se conservarán los campos y reglas vigentes para un Checkpoint de Ruta.

Los Checkpoints se mostrarán por hora ascendente. No habrá controles para reordenarlos manualmente; los que compartan hora conservarán su orden actual. Los cambios seguirán pendientes hasta guardar el Itinerario completo.

## User Stories

1. Como guía, quiero ver el nombre y el resumen técnico de la Ruta en la pantalla del Itinerario, para saber qué plantilla estoy editando.
2. Como guía, quiero ver la cantidad de Checkpoints junto a la lista, para dimensionar rápidamente el Itinerario.
3. Como guía, quiero ver cada Checkpoint en una fila compacta con hora, nombre, altitud y coordenadas, para consultar sus datos sin abrir el formulario.
4. Como guía, quiero que los Checkpoints aparezcan ordenados por hora ascendente, para leer el Itinerario en la secuencia del día operativo.
5. Como guía, quiero que los Checkpoints con igual hora conserven su orden actual, para que guardar o editar otros datos no cambie su secuencia.
6. Como guía, quiero agregar un Checkpoint desde la pantalla del Itinerario, para completar la Ruta sin volver al listado de Rutas.
7. Como guía, quiero editar o quitar un Checkpoint desde su fila, para encontrar la acción junto al dato correspondiente.
8. Como guía, quiero editar Checkpoints sin controles de reordenamiento manual, para mantener una lista ordenada por horario.
9. Como guía, quiero identificar una Ruta sin Checkpoints como borrador, para saber que todavía no puede usarse en una Salida.
10. Como guía, quiero que el formulario de Checkpoint ocupe el contenido central de la ventana y conserve visible la shell, para seguir orientado dentro de la aplicación.
11. Como guía, quiero que el formulario use el mismo patrón visual que los formularios de Ruta, Inventario y Cliente, para reconocer cómo completar y guardar datos.
12. Como guía, quiero conservar nombre, hora, latitud, longitud, altitud y descripción del Checkpoint, para registrar toda la información de la Ruta.
13. Como guía, quiero ver cuáles campos son obligatorios y las reglas pertinentes de formato y rango, para ingresar valores válidos.
14. Como guía, quiero ver los errores junto al campo correspondiente y que se actualicen al corregirlo, para resolver los problemas sin buscar un mensaje general.
15. Como guía, quiero poder cancelar el alta o edición y regresar al Itinerario sin aplicar esos cambios, para corregir mi decisión antes de guardar.
16. Como guía, quiero que los cambios confirmados en el formulario permanezcan pendientes en el Itinerario, para guardar o descartar el conjunto completo de una sola vez.
17. Como guía, quiero guardar el Itinerario con cero Checkpoints o con dos o más, para conservar la regla de borrador y evitar una Ruta con un único punto.
18. Como guía, quiero que guardar un Itinerario con exactamente un Checkpoint muestre la validación y mantenga mis cambios pendientes, para completar o corregir la lista.
19. Como guía, quiero confirmar, descartar o seguir editando cuando intento salir con cambios pendientes, para decidir qué ocurre con el Itinerario.
20. Como guía, quiero que el Itinerario anterior se conserve si falla el guardado, para no perder la configuración válida previamente persistida.

## Implementation Decisions

- El rediseño se limita a la pantalla de gestión del Itinerario de una Ruta y al formulario que se usa para agregar o editar sus Checkpoints.
- Se conserva la identidad visual clara y la paleta actual de TrekRisk. La referencia guía la composición de la lista y sus acciones, no introduce un tema oscuro.
- La pantalla mantiene la shell y la barra de navegación. El formulario reemplaza el contenido central en la misma ventana; no se abre una ventana ni un diálogo independiente.
- El resumen de Ruta, el encabezado de Checkpoints, la cantidad y la acción «Agregar checkpoint» comparten una jerarquía visual clara. El resumen mantiene el nombre, la altitud máxima, el terreno y las dificultades.
- La lista se presenta como una tarjeta con filas separadas. Cada fila muestra hora, nombre, altitud y coordenadas, con acciones de editar y quitar junto al Checkpoint.
- La acción «Guardar Itinerario» queda visible en la pantalla de gestión. Los cambios de cada formulario se incorporan a la edición pendiente y no se persisten hasta guardar el Itinerario.
- La pantalla ordena los Checkpoints por hora ascendente. No ofrece controles para mover filas manualmente. Para horas iguales mantiene la posición explícita ya persistida; al agregar un Checkpoint con una hora empatada, este se incorpora después de los Checkpoints existentes de esa hora.
- Esta interacción actualiza la presentación aprobada previamente para el Itinerario: elimina la operación manual de reordenar empates. Se conserva el orden explícito persistido del dominio y no se cambian el esquema, el modelo ni la persistencia. La regla de desempate existente en el ADR del Itinerario permanece vigente como orden estable.
- El formulario aplica el patrón establecido en `rediseno-formularios`: título y descripción alineados con la tarjeta, labels sobre controles, tarjeta centrada y adaptable, separación suficiente, acciones secundarias/principales diferenciadas y errores junto al campo.
- El formulario conserva los campos de un Checkpoint de Ruta: nombre, hora `HH:mm`, latitud, longitud, altitud en metros y descripción opcional. Nombre, hora, coordenadas y altitud son obligatorios. Se conservan los rangos actuales de coordenadas y altitud, incluida la validación contra la altitud máxima de la Ruta.
- Cancelar el formulario vuelve a la pantalla del Itinerario sin aplicar lo editado en ese formulario. Confirmar el formulario actualiza la edición pendiente del Itinerario; no guarda por separado el Checkpoint.
- Se conservan las reglas ya acordadas: el Itinerario puede guardarse con cero Checkpoints o con dos o más, pero no con exactamente uno; los cambios del conjunto se guardan atómicamente; una salida con cambios pendientes permite guardar, descartar o seguir editando.
- Se mantienen navegación, breadcrumbs y comportamiento de guardado/cancelación de la shell según las convenciones existentes. El breadcrumb no incorpora dinámicamente el nombre de la Ruta.
- No se cambian campos, validaciones de negocio, modelos, APIs ni persistencia. No se agregan dependencias.

## Testing Decisions

- Un buen escenario verifica el comportamiento observable de la pantalla y los datos que conserva el Itinerario. No depende de nombres de métodos, clases, consultas ni estructura interna de controladores.
- Seam: recorrido manual de extremo a extremo en la aplicación JavaFX, desde «Ver checkpoints» en una Ruta hasta regresar al listado, reutilizando el seam de gestión del Itinerario ya definido en la spec de Salidas, Rutas e Inventario.
- El recorrido comprueba la presentación de la Ruta, filas y acciones; orden cronológico y desempate estable; alta, edición y cancelación del formulario dentro del contenido central; y guardado o descarte del conjunto pendiente.
- También comprueba campos obligatorios, formato de hora, rangos de coordenadas y altitud máxima, errores junto al campo, casos de cero/uno/dos o más Checkpoints, aviso de salida con cambios pendientes y conservación del Itinerario anterior ante un fallo de guardado.
- Complementar la validación visual con la compilación Maven. El proyecto no tiene árbol de pruebas ni framework automatizado configurado; la compilación no se considera cobertura de comportamiento.
- No se propone un seam nuevo ni se cambia la infraestructura de pruebas.

## Out of Scope

- Cambiar el modelo de dominio o la posición explícita persistida para desempatar horas.
- Agregar reordenamiento manual de Checkpoints.
- Cambiar las reglas de negocio, validaciones, campos, guardado atómico o confirmación ante cambios pendientes.
- Modificar cómo una Salida conserva sus copias operativas de Ruta y Checkpoints.
- Rediseñar el formulario general de Ruta, los listados de otros módulos o la shell completa.
- Cambiar la identidad visual de TrekRisk, agregar dependencias o implementar pruebas automatizadas.

## Further Notes

- Vocabulario del glosario: **Ruta**, **Itinerario** y **Checkpoint**.
- El ADR `0002-itinerario-con-orden-explicito` define que la posición de Checkpoints con igual hora es explícita y persistida. Esta spec conserva esa posición como desempate estable, aunque ya no presenta controles para cambiarla manualmente.
- `docs/specs/rediseno-formularios/spec.md` es la referencia para el patrón visual del formulario.
- La especificación funcional de Salidas, Rutas e Inventario ya contiene las reglas de dominio y guardado que este rediseño conserva. Esta spec precisa la presentación de la pantalla y reemplaza su interacción de reordenamiento manual.

## Implementación

- La gestión del Itinerario presenta filas compactas con hora como texto libre, alineada con el nombre, altitud, coordenadas y acciones de edición/quitar. La acción de alta usa `ADD.svg`; las acciones por fila usan `EDIT.svg` y `DELETE.svg`, y siguen el patrón de visibilidad de las tablas. No se usan cápsulas para las horas ni glifos de texto.
- El formulario de Checkpoint reemplaza el contenido central dentro de la shell. Su confirmación actualiza el borrador del Itinerario; la persistencia sigue ocurriendo al guardar el conjunto completo.
- Verificación: compilación Maven correcta; `mvn test` terminó correctamente e informó que no hay pruebas que ejecutar; los dos FXML pasaron validación XML y `git diff --check` no reportó errores.
- Revisión visual aprobada por el usuario el 26/10/2026. La spec queda cerrada.
