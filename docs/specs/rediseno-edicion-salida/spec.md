# Especificación funcional: rediseño de la edición de Salida

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Cerrada — implementación completada  
**Fecha:** 27/10/2026  
**Alcance:** Pantalla de edición de una Salida existente.

## Problem Statement

La pantalla de edición de Salida no sigue la estructura de los demás formularios de edición (Ruta, Inventario, Cliente). Los datos de la Salida aparecen como una columna de labels y controles sueltos sin agrupación; los botones («Agregar checkpoint», «Editar seleccionado», «Quitar seleccionado», «Quitar equipamiento seleccionado», «Quitar participante seleccionado») tienen un estilo anterior y están ubicados sin criterio, obligando a seleccionar una fila y luego buscar el botón que la afecta. «Actualizar desde Ruta» es un botón naranja aislado entre listas. Las acciones finales usan la barra inferior del asistente, con «Cancelar | Guardar» en un orden distinto al del resto de formularios. Fecha y Ruta no se identifican como obligatorios ni muestran errores junto al campo, y el título («Editar Salida 9») y el breadcrumb («Edición de Salida») no coinciden con el patrón «Editar ruta», «Editar cliente».

## Solution

Rediseñar la edición de Salida con la misma estructura y estilo que los formularios de edición existentes: título y subtítulo alineados con una tarjeta blanca de ancho máximo 760 px, centrada, dividida en cuatro secciones: **Datos de la Salida**, **Ruta e itinerario**, **Equipamiento** y **Participantes**. Las listas ofrecen acciones por fila en lugar de botones «… seleccionado»; las acciones de agregado aparecen en el encabezado o junto al control correspondiente de su sección. Las acciones finales, «Guardar cambios» y «Cancelar», se ubican al pie de la tarjeta. Fecha y Ruta se marcan como obligatorios y muestran sus errores junto al campo.

La reorganización conserva todos los datos editables, reglas de guardado y comportamientos: la edición sigue siendo un formulario completo en la ventana actual que reemplaza la copia operativa de la Salida (ADR 0001, ADR 0004).

## User Stories

1. Como guía, quiero que la edición de una Salida tenga la misma estructura que Editar ruta, Editar elemento y Editar cliente, para reconocer el mismo patrón en toda la aplicación.
2. Como guía, quiero ver el título «Editar salida» y un subtítulo descriptivo alineados con la tarjeta, para identificar el propósito de la pantalla.
3. Como guía, quiero ver el breadcrumb «Salidas › Editar salida», para saber dónde estoy y volver al listado.
4. Como guía, quiero conservar la sidebar visible con Salidas activa, para mantener el contexto de la aplicación mientras edito.
5. Como guía, quiero que todo el contenido esté en una tarjeta blanca con borde sutil y esquinas redondeadas, para distinguir el formulario del fondo.
6. Como guía, quiero que la tarjeta tenga un ancho máximo de 760 px y esté centrada, para leerla cómodamente en pantallas amplias.
7. Como guía, quiero que los datos estén agrupados en las secciones Datos de la Salida, Ruta e itinerario, Equipamiento y Participantes, para encontrar cada grupo de información con facilidad.
8. Como guía, quiero ver los labels encima de sus controles y controles de altura uniforme, para completar el formulario con una apariencia consistente.
9. Como guía, quiero que Fecha y Ruta se identifiquen con asterisco, para saber qué datos son obligatorios.
10. Como guía, quiero ver el error de Fecha o Ruta junto al campo al intentar guardar, para localizar y corregir el dato que impide continuar.
11. Como guía, quiero que esos errores se actualicen mientras corrijo el campo, para saber cuándo el dato cumple la regla.
12. Como guía, quiero que los errores generales de guardado sigan mostrándose como alertas, para distinguirlos de los errores de un campo.
13. Como guía, quiero ver el resumen de la Ruta seleccionada (altitud máxima, terreno y dificultades) dentro de la sección Ruta e itinerario, para confirmar sus datos sin salir del formulario.
14. Como guía, quiero que la sección Ruta e itinerario indique que la lista de checkpoints es la copia operativa de la Salida y puede tener cualquier cantidad, para no confundirla con las reglas de la plantilla de Ruta.
15. Como guía, quiero ver cada checkpoint con su hora, nombre y altitud en una fila con acciones propias de editar y quitar, para operar sobre un checkpoint sin seleccionarlo antes.
16. Como guía, quiero un botón «Agregar checkpoint» en el encabezado de la sección, para encontrar la acción de alta donde corresponde.
17. Como guía, quiero que «Actualizar desde Ruta» sea un botón secundario con icono dentro de la sección Ruta e itinerario, para reconocerlo como una acción de esa sección y no como un botón aislado.
18. Como guía, quiero que «Actualizar desde Ruta» mantenga su confirmación actual, para no reemplazar la copia operativa por error.
19. Como guía, quiero que, al cambiar de Ruta, los checkpoints se reemplacen como ocurre hoy, para conservar el comportamiento de la copia operativa.
20. Como guía, quiero que no se pueda cambiar a una Ruta inactiva y se me avise, para conservar la regla actual.
21. Como guía, quiero ver una lista vacía de checkpoints con un mensaje corto, para entender que no hay puntos cargados en lugar de ver una caja en blanco.
22. Como guía, quiero agregar equipamiento eligiendo un elemento y una cantidad, con labels sobre cada control y un botón «Agregar», para completar la asignación de forma clara.
23. Como guía, quiero ver el equipamiento asignado con su cantidad y una acción de quitar por fila, para retirar un elemento sin seleccionarlo antes.
24. Como guía, quiero ver el mensaje «Sin equipamiento asignado» cuando no hay elementos, para entender el estado de la lista.
25. Como guía, quiero que se mantengan los avisos de cantidad inválida y la suma de cantidades al agregar un elemento ya asignado, para conservar las reglas actuales.
26. Como guía, quiero agregar un Participante eligiendo un Cliente y pulsando «Agregar», con su label encima del selector, para completar la asignación de forma clara.
27. Como guía, quiero ver los Participantes con su nombre, apellido y DNI y una acción de quitar por fila, para retirar un Participante sin seleccionarlo antes.
28. Como guía, quiero ver el mensaje «Sin participantes asignados» cuando no hay ninguno, para entender el estado de la lista.
29. Como guía, quiero que se mantenga el aviso cuando intento agregar un Cliente ya asociado, para conservar la regla actual.
30. Como guía, quiero que los Participantes y el equipamiento dados de baja que ya estaban asociados sigan visibles en la edición, para conservar la copia operativa existente.
31. Como guía, quiero ver «Guardar cambios» y «Cancelar» al final de la tarjeta, con una línea sutil de separación y alineados a la derecha, para reconocer el cierre del formulario como en los demás.
32. Como guía, quiero que «Guardar cambios» sea la acción primaria en verde y «Cancelar» la secundaria, para distinguir el guardado de la salida.
33. Como guía, quiero que «Guardar cambios» y «Cancelar» regresen al listado de Salidas, y que el listado se actualice y muestre una notificación después de guardar, para conservar el flujo actual.
34. Como guía, quiero que se me pida confirmación al cancelar o cambiar de sección con cambios sin guardar, para no perder datos por error.
35. Como guía, quiero que, sin cambios pendientes, cancelar o cambiar de sección no pida confirmación, para no interrumpir el flujo.
36. Como guía, quiero poder recorrer y activar todos los controles y acciones con teclado, para completar la edición sin depender del mouse.
37. Como guía, quiero que, en un ancho estrecho, el formulario siga siendo utilizable y sin desbordamiento horizontal, para editar una Salida en ventanas pequeñas.
38. Como guía, quiero que el diálogo de agregar y editar checkpoint siga funcionando como hoy, para que el rediseño no altere ese flujo.

## Implementation Decisions

- Se rediseña únicamente la presentación de la edición de Salida. Se reutilizan las convenciones visuales de los formularios de edición existentes: página con contenido centrado, tarjeta con ancho máximo de 760 px, padding de 24–32 px, borde sutil y radio de 8 px, títulos de sección, labels de 14 px sobre controles de 40 px y separación de 20–24 px entre campos.
- El título será «Editar salida», con un subtítulo breve del tipo «Modifica la fecha, la Ruta, el equipamiento y los Participantes de la Salida.». El número de la Salida deja de mostrarse. El breadcrumb será «Salidas › Editar salida».
- La tarjeta se organiza en cuatro secciones, en este orden:
  - **Datos de la Salida:** fecha, a ancho completo, con asterisco.
  - **Ruta e itinerario:** selector de Ruta con asterisco, resumen de la Ruta seleccionada y lista de checkpoints operativos. El encabezado de la sección ofrece «Agregar checkpoint» y «Actualizar desde Ruta», ambos botones secundarios con icono. Una ayuda breve indica que es la copia operativa de la Salida y admite cualquier cantidad de checkpoints.
  - **Equipamiento:** fila de agregado (elemento, cantidad y «Agregar») con labels sobre los controles, y lista de elementos asignados.
  - **Participantes:** fila de agregado (Cliente y «Agregar») con label sobre el selector, y lista de Participantes asignados.
- Se eliminan los botones «Editar seleccionado», «Quitar seleccionado», «Quitar equipamiento seleccionado» y «Quitar participante seleccionado». En su lugar, cada fila de las listas incluye sus acciones, con los mismos iconos y estilo de las acciones por fila de los listados: editar y quitar en checkpoints, y quitar en equipamiento y Participantes. Las acciones permanecen en sus paneles, de acuerdo con el ADR 0004. Las acciones por fila deben ser accesibles por teclado y tener nombre accesible.
- Las listas vacías muestran un mensaje corto («Sin checkpoints», «Sin equipamiento asignado», «Sin participantes asignados»).
- «Actualizar desde Ruta» deja de ser un botón de advertencia naranja y pasa a ser secundario con icono; conserva el texto y la confirmación actuales.
- Fecha y Ruta son obligatorias. Al intentar guardar sin ellas, el error aparece junto al campo y se actualiza mientras se corrige el campo con el que el usuario ya interactuó. No se muestran errores antes de la interacción. Los errores generales de guardado (por ejemplo, fallo de persistencia) siguen mostrándose con alertas.
- Las acciones finales se ubican al pie de la tarjeta, bajo una línea sutil y alineadas a la derecha en escritorio: «Guardar cambios» (primaria) y «Cancelar» (secundaria). En un ancho estrecho se apilan y ocupan el ancho disponible. Se elimina la barra inferior del asistente.
- Se conservan sin cambios: la carga de datos al abrir la edición, el reemplazo de la copia operativa al guardar (ADR 0001), las reservas de stock (ADR 0003), la regla de Ruta inactiva, la protección contra descarte de cambios, el guard de navegación al cambiar de sección, la actualización del listado y el aviso de éxito tras guardar.
- El diálogo de agregar y editar checkpoint no se modifica.
- El vocabulario sigue el glosario: **Salida**, **Ruta**, **Checkpoint**, **Itinerario**, **Copia operativa**, **Participante** y **Equipamiento de salida**. No se agregan términos al glosario ni ADR nuevos.
- No se agregan dependencias ni componentes externos; se usan las convenciones JavaFX existentes.

## Testing Decisions

- Una buena verificación comprueba comportamiento observable de pantalla (qué se ve, qué hace cada acción y a dónde lleva), sin depender de nombres de métodos ni de la estructura interna del controlador.
- Seam acordado, el mismo que las especificaciones anteriores del proyecto: recorrido manual en la interfaz JavaFX desde Salidas hasta la edición, más compilación Maven (`mvn -f trekking-app/pom.xml compile`). Es el seam más alto disponible y el único. El proyecto no tiene pruebas automatizadas, por lo que no se afirma cobertura automatizada.
- Escenarios a validar manualmente:
  - Estructura: título, subtítulo, breadcrumb, tarjeta de 760 px centrada, cuatro secciones y acciones finales al pie, comparados con Editar ruta.
  - Guardar sin cambios y con cambios: se regresa al listado, se actualiza y aparece la notificación.
  - Cancelar y cambiar de sección con y sin cambios pendientes.
  - Fecha vacía o Ruta sin seleccionar: error junto al campo, actualizado al corregir.
  - Cambiar de Ruta: se reemplazan los checkpoints; elegir una Ruta inactiva muestra el aviso y no cambia la selección.
  - «Actualizar desde Ruta»: confirmación y reemplazo; cancelar la confirmación no cambia nada.
  - Checkpoints: agregar, editar y quitar con acciones por fila, incluida la opción de guardar también en la Ruta; lista vacía.
  - Equipamiento: agregar, sumar cantidad a un elemento ya asignado, cantidad inválida, quitar y lista vacía; elementos dados de baja ya asignados.
  - Participantes: agregar, Cliente ya asociado, quitar y lista vacía; Clientes inactivos ya asociados.
  - Recorrido por teclado de todos los controles y acciones por fila.
  - Ancho de escritorio y ancho estrecho: una columna, botones accesibles, sin desbordamiento horizontal.
- Prior art: la verificación manual descrita en `rediseno-formularios`, `navegacion-asistente-salida` y `acciones-inline-tablas`.

## Out of Scope

- El diálogo de agregar y editar checkpoint (queda para un ticket aparte).
- El Asistente de Salida, el listado de Salidas y cualquier otro formulario.
- Cambios en los datos editables, reglas de validación de negocio, persistencia, copia operativa, reservas de stock o modelos.
- Cambios en la navegación general, la sidebar o la identidad visual de TrekRisk.
- Convertir la edición de Salida en un flujo multipaso.
- Nuevas dependencias o infraestructura de pruebas automatizadas.

## Further Notes

- La edición completa en la ventana actual, con scroll, Guardar y Cancelar que regresan al listado, y confirmación ante cambios sin guardar, ya está decidida en el ADR 0004 y se respeta; esta spec solo cambia la presentación.
- La única mejora de comportamiento incluida es la validación visible de Fecha y Ruta, hoy inexistente.
- El desglose en tickets es una etapa separada y requiere aprobación explícita después de revisar esta spec.
