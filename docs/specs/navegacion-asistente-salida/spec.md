# Especificación: alineación y navegación del indicador de pasos del Asistente de Salida

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Cerrada — implementación completada
**Fecha:** 27/10/2026  
**Alcance:** Indicador de pasos del Asistente de Salida y color de éxito compartido.

## Problem Statement

En el Asistente de Salida, el indicador de pasos no está alineado con las tarjetas del contenido: el círculo del primer paso queda desplazado hacia adentro respecto del borde izquierdo de la tarjeta y el del último respecto del derecho, y el desfase se repite en todos los pasos. Además, el check de los pasos completados usa un verde frío (`#188038`) ajeno a la paleta oliva de TrekRisk. Por último, el indicador es solo informativo: para corregir un paso anterior, o para saltar a Revisión cuando ya están completos los datos obligatorios, el guía debe usar Anterior/Siguiente repetidamente.

## Solution

El indicador ocupa exactamente el ancho de las tarjetas: "Datos" queda al ras del borde izquierdo y "Revisión" al ras del derecho, con los pasos intermedios repartidos y los conectores rellenando el espacio. El verde de éxito pasa a un verde oliva más cálido, coherente con la paleta, y se aplica en todo el sistema. Cada paso del indicador es clicable cuando corresponde: se puede volver siempre a un paso completado, y avanzar a un paso posterior cuando los pasos obligatorios previos (Datos y Ruta) están completos. Los pasos no alcanzables se ven deshabilitados.

## User Stories

1. Como guía, quiero que el indicador de pasos quede alineado con las tarjetas del contenido, para que la pantalla se vea ordenada y consistente.
2. Como guía, quiero que el primer paso quede al ras del borde izquierdo de la tarjeta, para reconocer un eje visual común con el contenido.
3. Como guía, quiero que el último paso quede al ras del borde derecho de la tarjeta, para que el indicador no parezca más angosto o ancho que el contenido.
4. Como guía, quiero que la alineación se mantenga en los cinco pasos y al cambiar el tamaño de la ventana, para no ver desfases en ningún momento.
5. Como guía, quiero que el check de los pasos completados use un verde acorde a la paleta de la aplicación, para que el indicador no contradiga el resto de la interfaz.
6. Como guía, quiero que el conector entre pasos completados use el mismo verde que el check, para que el progreso se lea como una sola unidad visual.
7. Como guía, quiero que los toasts de éxito y los estados de éxito de las tablas usen también ese verde, para que el color de éxito sea el mismo en toda la aplicación.
8. Como guía, quiero tocar un paso completado para volver a él, para corregir información sin pasar por todos los pasos intermedios.
9. Como guía, quiero que volver a un paso anterior conserve todo lo ingresado, incluso lo incompleto, para no perder trabajo.
10. Como guía, quiero que volver a un paso anterior nunca me muestre avisos de validación, para poder moverme libremente hacia atrás.
11. Como guía, quiero poder tocar un paso posterior cuando Datos y Ruta están completos, para llegar directamente a Revisión sin recorrer Equipamiento y Participantes si no los necesito.
12. Como guía, quiero que Equipamiento y Participantes no bloqueen la navegación, para poder crear una Salida sin ellos.
13. Como guía, quiero que los pasos a los que todavía no puedo ir se vean deshabilitados, para saber qué me falta sin recibir diálogos de error.
14. Como guía, quiero que un paso obligatorio inválido o sin completar bloquee el avance a los pasos posteriores, para no llegar a Revisión con una Salida que no se puede confirmar.
15. Como guía, quiero que Equipamiento y Participantes aparezcan como completados una vez que los visité, aunque estén vacíos, para distinguir lo que ya revisé de lo que no.
16. Como guía, quiero que un paso opcional no visitado aparezca como pendiente pero alcanzable cuando Datos y Ruta están completos, para saber que puedo ir a él cuando quiera.
17. Como guía, quiero que el paso actual siga destacado y mostrando "Paso N de 5", para ubicarme siempre.
18. Como guía, quiero que los botones Anterior, Siguiente y los accesos de edición de la Revisión sigan funcionando como hasta ahora, para no perder mis atajos habituales.
19. Como guía, quiero que si invalido un dato obligatorio ya completado (por ejemplo, borro la fecha), los pasos posteriores vuelvan a bloquearse, para que el indicador refleje el estado real.
20. Como guía, quiero que el paso Ruta se considere completo con una Ruta existente seleccionada o con los datos válidos de una Ruta nueva, para que ambos modos permitan avanzar.
21. Como guía, quiero que la protección contra pérdida de datos al cambiar de sección siga activa, para no descartar un borrador por accidente.
22. Como guía, quiero que la confirmación de la Salida siga guardando todo en una única operación, para que la navegación libre no altere las reglas de guardado.

## Implementation Decisions

- El indicador pasa a ocupar el ancho completo del área de contenido de las tarjetas. El ítem del primer paso se alinea a la izquierda, el del último a la derecha, y los intermedios quedan distribuidos con conectores flexibles entre ellos. El ancho de los ítems de los extremos se ajusta a su contenido (círculo y etiqueta), sin el ancho mínimo fijo actual que desplaza los círculos hacia adentro.
- El color de éxito global se cambia de `#188038` a `#5f7a2e`. Es un único token compartido; los toasts, los estados de éxito de las tablas, el check y el conector completo lo heredan. Se verifica visualmente que los estados de éxito sigan distinguiéndose de las acciones primarias (`#6e6928`) y de advertencia.
- Cada ítem del indicador se vuelve interactivo. Se calcula por paso un estado: completado, actual, alcanzable o bloqueado, y la interfaz refleja esos estados visualmente (cursor y aspecto de deshabilitado para los bloqueados).
- Reglas de navegación:
  - Hacia atrás: siempre permitido hacia pasos anteriores al actual, sin validar y conservando el borrador.
  - Hacia adelante: permitido a un paso posterior solo si todos los pasos obligatorios previos a él (Datos, Ruta) están completos. El paso Revisión es alcanzable bajo esa misma condición.
  - Un paso posterior bloqueado no es clicable y no muestra diálogos de error al tocarlo.
- Definición de "completo":
  - Datos: fecha seleccionada.
  - Ruta: Ruta existente seleccionada, o, al crear una Ruta nueva, nombre, altitud máxima válida (número mayor o igual a cero), tipo de terreno, dificultad técnica y dificultad física completos.
  - Equipamiento y Participantes (opcionales): completados una vez visitados, estén vacíos o no. Si no fueron visitados, aparecen como pendientes pero alcanzables cuando Datos y Ruta están completos.
- La validación se separa del avance: la verificación de completitud de Datos y Ruta, hoy dentro de la acción de avanzar con diálogos de aviso, pasa a ser una consulta reutilizable sin efectos visibles. Siguiente conserva sus avisos actuales; el indicador usa la consulta para habilitar o bloquear pasos sin mostrar avisos.
- El estado "visitado" de los pasos opcionales se registra durante la vida del asistente y se reinicia al abrir uno nuevo. No se persiste.
- El indicador se vuelve a calcular cuando cambian los datos de los que depende (fecha, selección o datos de la Ruta), de modo que invalidar un dato obligatorio vuelva a bloquear los pasos posteriores.
- Se mantiene el resto del asistente sin cambios: botones Anterior/Siguiente/Cancelar, accesos de edición de Revisión, protección contra descarte, confirmación atómica (ADR 0001 y 0003), vocabulario en español y "Paso N de 5".
- El término **Asistente de Salida** ya está registrado en el glosario; no se requieren ADR nuevos.

## Testing Decisions

- **Seam acordado:** el mismo de la spec del rediseño del asistente: recorrido manual de extremo a extremo en JavaFX desde Salidas, más compilación Maven (`mvn -f trekking-app/pom.xml compile`). Es el seam más alto disponible y el único; el proyecto no tiene infraestructura de pruebas automatizadas, por lo que no se afirma cobertura automatizada.
- Un buen escenario verifica comportamiento visible (qué pasos se ven habilitados, a dónde lleva cada clic, qué datos se conservan), sin acoplarse a la estructura interna.
- Escenarios a validar manualmente:
  - Alineación: el borde izquierdo de "Datos" y el derecho de "Revisión" coinciden con los de la tarjeta en los cinco pasos y al redimensionar la ventana.
  - Color: check, conector, toast de éxito y estados de éxito de tablas con el nuevo verde.
  - Estado inicial: solo el paso actual y los pasos anteriores son clicables; los posteriores están bloqueados.
  - Con fecha y Ruta existente completas: se puede saltar de Datos a Revisión; Equipamiento y Participantes aparecen pendientes y alcanzables.
  - Con una Ruta nueva incompleta: los pasos posteriores a Ruta están bloqueados; volver a Datos conserva lo ingresado y no muestra avisos.
  - Visitar Equipamiento o Participantes sin agregar nada: aparecen como completados.
  - Borrar la fecha tras completar los pasos: los pasos posteriores vuelven a bloquearse.
  - Navegar por el indicador y confirmar una Salida: se guarda con las reservas y copias operativas correctas.
  - Anterior, Siguiente, accesos de edición de Revisión y cancelación con confirmación siguen funcionando.
- Prior art: el recorrido manual descrito en la spec `wizard-salidas-uxui` y la verificación visual de `color-estados-tablas`.

## Out of Scope

- Cambios en las reglas de creación, validación o persistencia de Salidas, Rutas, Clientes, Equipamiento o reservas.
- Cambios en el flujo de edición de Salidas.
- Nuevos pasos, reordenamiento de pasos o cambios en sus nombres.
- Rediseño de otros componentes del asistente (tarjetas, barra inferior, Revisión).
- Cambios en otros colores de la paleta distintos del color de éxito.
- Atajos de teclado y accesibilidad del indicador más allá de la interacción con clic.

## Further Notes

- El color de éxito es global por decisión explícita: el cambio alcanza a toasts y badges de éxito de tablas, no solo al indicador.
- `#5f7a2e` es la propuesta aceptada; si en la verificación visual se confunde con el verde primario `#6e6928`, se ajusta la saturación manteniendo la temperatura cálida.
- Colores pendientes de `colors.md` (planificada, naranja distintivo, activo/inactivo) son un tema aparte y no forman parte de esta spec.
- Implementación completada. Verificación: compilación Maven correcta; revisión visual aprobada por el usuario el 27/10/2026. La spec queda cerrada.
