# Especificación: interfaz web

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Status:** closed
**Fecha:** 06/11/2026
**Alcance:** Tercera fase de la migración a web. Una SPA que consume la API REST ya cerrada (spec `api-rest`) y reproduce el comportamiento de la app de escritorio, por rebanadas. Respeta los ADR 0004, 0005, 0006 y 0007.

## Problem Statement

Ya existe una API REST que expone todos los casos de uso del núcleo, pero la única forma de usar la aplicación sigue siendo el escritorio JavaFX. No hay ninguna pantalla en un navegador: no puedo administrar Rutas, Inventario y Clientes, ni planificar, editar, cerrar y exportar Salidas desde la web. Tampoco quiero reabrir las decisiones de UX que el escritorio ya resolvió, ni duplicar reglas de negocio en el navegador, ni dejar datos sensibles de Clientes (fichas médicas) en el almacenamiento del navegador.

## Solution

Una aplicación web de una sola página, en español, con la misma estructura y comportamiento que el escritorio: menú lateral con Salidas, Rutas, Inventario y Clientes; listados con acciones por fila; altas y ediciones en formularios con URL propia; y el Asistente de Salida de cinco pasos. Se entrega por rebanadas verticales, cada una utilizable por separado:

1. Esqueleto (navegación, breadcrumbs, cliente de la API, manejo de errores) y Rutas con Itinerario.
2. Inventario.
3. Clientes con fichas médicas.
4. Salidas: listado con indicadores, Asistente de Salida, edición, cierre y Ficha operativa.

Toda regla de negocio sigue en el núcleo; la web solo presenta, valida formato para dar retroalimentación inmediata y traduce los errores de la API a mensajes por campo.

## User Stories

### Arquitectura y operación

1. Como desarrollador, quiero que la web sea un proyecto propio que solo hable con la API por HTTP, para que escritorio y web evolucionen por separado.
2. Como desarrollador, quiero que la web no contenga reglas de negocio, para que el comportamiento siga definido en un único lugar.
3. Como desarrollador, quiero levantar la web con un solo comando de npm, para probarla sin pasos manuales.
4. Como desarrollador, quiero que en desarrollo la web llame a `/api` mediante un proxy hacia `127.0.0.1:8080`, para no necesitar CORS.
5. Como desarrollador, quiero configurar la URL base de la API por variable de entorno, para poder apuntarla a otro lugar más adelante.
6. Como desarrollador, quiero que los tipos de petición y respuesta se generen desde el contrato OpenAPI de la API, para que el contrato no se desincronice.
7. Como desarrollador, quiero que la documentación del proyecto (`AGENTS.md`) incluya los comandos de la web, para que cualquiera pueda ejecutarla y probarla.
8. Como operador, quiero que la web funcione mientras el escritorio está abierto sobre la misma base de datos, para migrar sin congelar el uso actual.
9. Como desarrollador, quiero que la construcción Maven del resto del proyecto no dependa de la web, para no alterar las dependencias entre módulos.

### Navegación y estructura

10. Como guía, quiero un menú lateral con Salidas, Rutas, Inventario y Clientes, para moverme entre secciones como en el escritorio.
11. Como guía, quiero ver la marca TrekRisk con su logo al tope del menú lateral, para identificar la aplicación.
12. Como guía, quiero contraer el menú lateral a una barra de iconos con tooltips, para ganar espacio horizontal.
13. Como guía, quiero distinguir la sección activa en el menú, para saber dónde estoy.
14. Como guía, quiero breadcrumbs en las pantallas secundarias, para entender mi ubicación y volver.
15. Como guía, quiero que cada pantalla y cada formulario tengan URL propia, para recargar, volver con el navegador y compartir el enlace dentro de mi equipo.
16. Como guía, quiero que la raíz de la aplicación me lleve a Salidas, para empezar donde empieza el escritorio.
17. Como guía, quiero que, mientras Salidas no esté disponible, la raíz me lleve a Rutas, para no ver una pantalla vacía en las primeras entregas.
18. Como guía, quiero que una URL inexistente muestre un aviso claro con un enlace de regreso, para no quedar perdido.
19. Como guía, quiero que, si un formulario tiene cambios sin guardar y trato de salir (menú, breadcrumb o atrás del navegador), se me pida confirmación, para no perder trabajo por accidente.
20. Como guía, quiero que, sin cambios pendientes, salir de un formulario no pida confirmación, para no ser interrumpido.

### Comportamiento común de listados y formularios

21. Como guía, quiero que los listados de Rutas, Inventario y Clientes tengan un selector de estado (activas, bajas, todas) con activas por defecto, para ver registros dados de Baja y reactivarlos.
22. Como guía, quiero que baja y reactivación sean acciones por fila y no un borrado, para respetar que una Baja no elimina el registro.
23. Como guía, quiero que las acciones por fila ofrezcan solo las válidas para el estado del registro, para no ver opciones inútiles.
24. Como guía, quiero que las altas y ediciones se hagan en formularios con Guardar y Cancelar, para mantener en un flujo las validaciones y los datos relacionados.
25. Como guía, quiero ver un estado vacío claro en una tabla sin registros, con la acción de crear cuando corresponda, para saber cómo empezar.
26. Como guía, quiero que los estados de las tablas usen los mismos colores que el escritorio, para reconocerlos sin esfuerzo.
27. Como guía, quiero ver un indicador de carga mientras se obtienen los datos y un aviso con opción de reintentar si la carga falla, para saber qué ocurre.
28. Como guía, quiero ver una confirmación breve (toast) tras guardar, dar de baja o reactivar, para saber que la acción funcionó.

### Manejo de errores

29. Como guía, quiero que un error de validación (422) marque cada campo afectado con su mensaje, para corregirlo sin adivinar.
30. Como guía, quiero que un error sin campo asociado se muestre como aviso general del formulario, para no perder el mensaje.
31. Como guía, quiero que un recurso inexistente (404) muestre un aviso claro y me devuelva al listado, para entender que ya no existe.
32. Como guía, quiero que un conflicto de estado (409) muestre el mensaje, recargue el recurso y no reintente solo, para ver el estado real antes de decidir.
33. Como guía, quiero que un conflicto por stock insuficiente indique el elemento y la cantidad disponible, para corregir la cantidad.
34. Como guía, quiero que un cuerpo rechazado (400) o un error inesperado (500) muestre un mensaje genérico sin detalles internos, para no ver información confusa o sensible.
35. Como guía, quiero un aviso claro cuando la API no responde, para distinguir un problema de conexión de un error de negocio.

### Rutas e Itinerario

36. Como guía, quiero listar Rutas activas, dadas de Baja o todas, para elegir con cuál trabajar.
37. Como guía, quiero filtrar el listado de Rutas por terreno, dificultad técnica y dificultad física, para encontrar la adecuada.
38. Como guía, quiero que esos filtros se apliquen sobre el listado ya cargado, para no depender de nuevas consultas.
39. Como guía, quiero ver en el listado si una Ruta es Ruta borrador, para saber que todavía no puedo usarla en una Salida.
40. Como guía, quiero crear una Ruta en un formulario, para registrar un recorrido nuevo.
41. Como guía, quiero editar una Ruta en un formulario, para corregir sus datos.
42. Como guía, quiero dar de baja y reactivar una Ruta desde su fila, para sacarla del uso sin perder su historia.
43. Como guía, quiero abrir el Itinerario de una Ruta y ver sus Checkpoints en orden, para revisar el recorrido.
44. Como guía, quiero agregar, editar, quitar y reordenar Checkpoints y guardar el Itinerario completo de una vez, para ajustarlo con el orden explícito.
45. Como guía, quiero ver un aviso de error por campo si un Checkpoint es inválido, para corregirlo.
46. Como guía, quiero que una Ruta deje de aparecer como borrador cuando guardo dos o más Checkpoints, para poder usarla en una Salida.

### Inventario

47. Como encargado de equipo, quiero listar elementos activos, dados de Baja o todos, para ver qué hay en el catálogo.
48. Como encargado de equipo, quiero crear y editar un elemento en un formulario con su cantidad total, Estado del elemento y vigencia, para mantener el catálogo.
49. Como encargado de equipo, quiero dar de baja y reactivar un elemento desde su fila, para retirar equipo sin borrarlo.
50. Como encargado de equipo, quiero ver el Estado del elemento de cada fila con su color, para detectar equipo en reparación, fuera de servicio o extraviado.
51. Como encargado de equipo, quiero que los elementos que no se pueden asignar no se ofrezcan al armar una Salida, para no asignarlos por error.

### Clientes y fichas médicas

52. Como administrador, quiero listar Clientes activos, dados de Baja o todos, para encontrar a quién busco.
53. Como administrador, quiero crear y editar un Cliente en un formulario, con validación por campo, para mantener datos correctos.
54. Como administrador, quiero recibir retroalimentación de validación mientras escribo, usando la validación sin guardar de la API, para corregir antes de enviar.
55. Como administrador, quiero ver si un Cliente es menor y las reglas que eso implica, para aplicarlas al cargarlo.
56. Como administrador, quiero dar de baja y reactivar un Cliente desde su fila, para excluirlo sin borrar su registro.
57. Como administrador, quiero adjuntar o reemplazar la ficha médica de un Cliente eligiendo un archivo, para tenerla disponible.
58. Como administrador, quiero que se rechace en el navegador un archivo mayor de 10 MB antes de enviarlo, para no esperar una subida que va a fallar.
59. Como administrador, quiero descargar la ficha médica con su nombre original solo mediante una acción explícita, para revisarla sin que se incruste ni se previsualice.
60. Como administrador, quiero quitar la ficha médica de un Cliente tras una confirmación, para eliminarla cuando ya no corresponda.
61. Como administrador, quiero un aviso claro si el Cliente no tiene ficha médica, para no intentar descargar algo inexistente.
62. Como administrador, quiero que la web no guarde datos de Clientes ni fichas médicas en el navegador entre sesiones, para proteger datos sensibles.

### Salidas

63. Como guía, quiero que Salidas sea la pantalla de inicio, con sus indicadores y la lista de Salidas, para ver el estado general al abrir la aplicación.
64. Como guía, quiero ver para cada Salida su resumen operativo y la condición de Salida vencida, para saber cuáles esperan cierre.
65. Como guía, quiero ver las próximas Salidas con ruta, fecha y horarios, para anticiparme.
66. Como guía, quiero que las filas de Salida muestren solo las acciones válidas para su estado, para no ofrecer cambios en una Salida final.
67. Como guía, quiero ver el detalle de una Salida y su Copia operativa vigente, para consultar exactamente lo planificado.
68. Como guía, quiero crear una Salida con el Asistente de Salida de cinco pasos (Datos, Ruta, Equipamiento, Participantes y Revisión), para planificarla paso a paso.
69. Como guía, quiero que Datos y Ruta sean obligatorios y Equipamiento y Participantes opcionales, para crear una Salida sin ellos.
70. Como guía, quiero que cada paso del indicador sea clicable cuando corresponda, para corregir o saltar a Revisión sin recorrer todo.
71. Como guía, quiero poder volver siempre a un paso anterior sin avisos de validación y sin perder lo ingresado, para moverme libremente hacia atrás.
72. Como guía, quiero que los pasos no alcanzables aparezcan deshabilitados, sin diálogos de error, para saber qué me falta.
73. Como guía, quiero que Equipamiento y Participantes figuren como completados una vez visitados, estén vacíos o no, para distinguir lo revisado de lo pendiente.
74. Como guía, quiero que el paso Ruta admita elegir una Ruta existente o crear una nueva, para no salir del asistente.
75. Como guía, quiero que no se pueda elegir una Ruta borrador para una Salida, para respetar la regla del dominio.
76. Como guía, quiero elegir equipamiento existente con su cantidad y poder crear elementos nuevos desde el asistente, para armar la Salida de una vez.
77. Como guía, quiero elegir Clientes existentes y crear Clientes nuevos en el asistente, con su ficha médica opcional, para sumar Participantes de una vez.
78. Como guía, quiero que la ficha médica de un Cliente nuevo se lea en el navegador y se envíe con la confirmación, para que se guarde en la misma operación.
79. Como guía, quiero revisar todos los datos en el paso Revisión, con accesos de edición a cada paso, para confirmar con seguridad.
80. Como guía, quiero que confirmar la Salida se haga en una única petición atómica, para que un fallo de stock no deje datos a medias.
81. Como guía, quiero que el estado del asistente se conserve mientras navego dentro de él y se reinicie al recargar, para no dejar borradores con datos de Clientes en el navegador.
82. Como guía, quiero que se me pida confirmación antes de abandonar un asistente con datos ingresados, para no perderlos por accidente.
83. Como guía, quiero editar una Salida planificada en un formulario completo, con Guardar y Cancelar, para reemplazar su Copia operativa vigente.
84. Como guía, quiero que el formulario de edición ofrezca solo los elementos que puedo asignar, para no elegir los no disponibles.
85. Como guía, quiero marcar una Salida como realizada, para cerrarla.
86. Como guía, quiero cancelar una Salida tras confirmarlo, para liberar su reserva de stock.
87. Como guía, quiero que una Salida realizada o cancelada no admita nuevas acciones, para respetar los estados finales.
88. Como guía, quiero que, si la Salida cambió de estado desde otro cliente, la web me lo informe, recargue y no reintente, para no actuar sobre datos viejos.
89. Como guía, quiero descargar la Ficha operativa en PDF con un nombre reconocible mediante una acción explícita, para imprimirla.
90. Como guía, quiero que la Ficha operativa no incorpore adjuntos médicos, para no divulgar datos sensibles.

### Idioma, accesibilidad y presentación

91. Como guía, quiero toda la interfaz en español con los términos del glosario (Ruta, Salida, Copia operativa, Baja...), para que coincida con mi vocabulario.
92. Como guía, quiero la misma paleta y el mismo logo del escritorio, para reconocer la aplicación.
93. Como guía que usa teclado o tecnologías de asistencia, quiero que los controles, tablas, formularios y errores tengan nombres y asociaciones accesibles, para poder operar sin ratón.
94. Como guía, quiero que la interfaz se use bien en una ventana de escritorio típica y degrade de forma razonable en anchos menores, para no depender de una sola resolución.

## Implementation Decisions

### Estructura y herramientas

- La web es un proyecto propio de Node en una carpeta hermana de los módulos Maven, gestionado solo con npm y fuera del reactor Maven (ADR 0007). No cambia el `pom.xml` padre ni la regla de dependencias entre módulos Java. `AGENTS.md` se actualiza con sus comandos de instalación, desarrollo, prueba y compilación.
- Stack: Vite, React y TypeScript. Las librerías concretas de enrutado, de formularios, de estado remoto y de generación de tipos son un detalle de implementación y se eligen en el primer ticket, sin ADR, porque son reversibles.
- Los tipos de petición y respuesta se generan desde el contrato OpenAPI de la API; no se escriben a mano. El contrato lo publica la API en ejecución.
- En desarrollo, un proxy de Vite reenvía `/api` hacia `127.0.0.1:8080`, por lo que la web no necesita CORS y `trekking.api.origen-web` queda vacío. La URL base de la API se configura por variable de entorno.
- Se ejecutan dos procesos en desarrollo (la API con `spring-boot:run` y la web con npm). No se empaqueta ni se sirve la web desde Spring.

### Rutas de navegador

- Cada pantalla y cada paso del Asistente de Salida tienen URL propia. La raíz redirige a Salidas; mientras Salidas no esté entregada, a Rutas.
- Las altas y ediciones son rutas propias (formulario con Guardar y Cancelar), no diálogos modales, siguiendo el ADR 0004.
- Un mecanismo común detecta cambios sin guardar en formularios y asistente y pide confirmación al salir por menú, breadcrumb o historial del navegador.

### Estructura de pantallas

- Menú lateral expandible con Salidas, Rutas, Inventario y Clientes, marca TrekRisk con el logo de montaña arriba y control de expansión al pie, tal como en el escritorio. Breadcrumbs en las pantallas secundarias.
- Cada listado ofrece el filtro `estado` de la API con `activas` por defecto. Los filtros de terreno y de dificultad técnica y física de Rutas se aplican en el cliente sobre el listado completo (la API no los ofrece y no se agregan).
- Las acciones por fila siguen el ADR 0004, con las filas de Salida limitadas a las acciones válidas para su estado.
- Los colores de estado, el verde de éxito y la paleta base se toman de los specs visuales del escritorio, adaptados a CSS.
- El comportamiento del escritorio es la referencia de UX. Cualquier diferencia deliberada se anota en el ticket correspondiente.

### Datos sensibles

- La web no escribe en `localStorage`, `sessionStorage` ni IndexedDB, ni conserva datos de Clientes o fichas médicas entre sesiones. El estado del asistente vive solo en memoria y se reinicia al recargar.
- Las fichas médicas y la Ficha operativa se entregan únicamente mediante una descarga explícita; no se incrustan ni se previsualizan.
- El límite de 10 MB para fichas médicas se valida en el navegador antes de enviar; la API sigue siendo la autoridad.

### Errores

- Un único punto interpreta el cuerpo de error uniforme de la API: 422 se asigna a los campos del formulario por el nombre del campo (los sobrantes pasan a un aviso general); 404 lleva al listado con aviso; 409 muestra el mensaje, recarga el recurso y no reintenta, y en el caso de stock insuficiente indica el elemento y la cantidad disponible; 400 y 500 muestran un mensaje genérico; una falla de red muestra un aviso de conexión.

### Asistente de Salida

- Conserva los cinco pasos y las reglas de navegación del glosario y del spec `navegacion-asistente-salida`: hacia atrás siempre y sin validar; hacia adelante solo si Datos y Ruta están completos; Equipamiento y Participantes opcionales y completados al visitarlos.
- Cada paso tiene su URL, pero el estado vive en memoria compartida entre pasos; una recarga lo reinicia, con confirmación previa al abandonar si hay datos.
- Permite crear Ruta nueva, elementos nuevos y Clientes nuevos (con ficha médica como nombre y contenido en base64). La confirmación es una única petición a la API y no envía los Checkpoints: la Salida copia el Itinerario vigente.

### Orden de entrega

1. Esqueleto: proyecto web, proxy, tipos generados, cliente de la API, manejo de errores, shell con menú y breadcrumbs, confirmación de salida con cambios sin guardar; Rutas con su Itinerario. Primer hito utilizable.
2. Inventario.
3. Clientes y fichas médicas.
4. Salidas: listado con indicadores y próximas Salidas, detalle y Copia operativa, Asistente de Salida, edición, realizar y cancelar, Ficha operativa.

## Testing Decisions

- **Seam único:** la aplicación web completa renderizada con su enrutador, con la API simulada en la frontera HTTP (MSW). Cada test monta la app en una URL, actúa como lo haría el usuario y observa solo lo visible: texto, filas, errores por campo, URL resultante, descargas disparadas y las peticiones HTTP emitidas. No se prueban componentes, hooks ni el cliente generado de forma aislada.
- Un buen test observa comportamiento externo, no detalles de implementación; no depende de nombres de componentes ni de la estructura interna.
- Por rebanada: el camino feliz de cada pantalla y un test por cada error que esa pantalla puede mostrar (422 en el campo, 404, 409 con recarga y 409 de stock con elemento y cantidad disponible).
- Pruebas específicas: filtro `estado` y filtros de Rutas en el cliente; baja y reactivación por fila; confirmación al salir de un formulario con cambios; ausencia de escritura en el almacenamiento del navegador; descarga de ficha médica y de Ficha operativa; rechazo de ficha mayor de 10 MB sin llamar a la API; reglas de navegación de los cinco pasos del asistente; confirmación en una única petición; estado del asistente reiniciado tras recarga.
- Las reglas de negocio se prueban en `trekking-core` y la API en `trekking-api`; la web no las repite.
- Prior art: los tests de `trekking-api` (comportamiento externo, un test por error mapeado). Es la primera suite de la web y fija el estilo.
- La integración real con la API en marcha y la concurrencia con el escritorio se validan a mano; no hay E2E con navegador real en esta etapa.

## Out of Scope

- Autenticación, autorización, usuarios y roles (ADR 0006), y exponer la API o la web fuera de `127.0.0.1`.
- Despliegue, empaquetado de la web con la API y servir estáticos desde Spring.
- Pruebas E2E con navegador real.
- Pantalla de Dashboard con los indicadores generales (Rutas activas, Clientes activos, equipo no disponible): el escritorio no la expone y requeriría un diseño nuevo. La API ya los ofrece para una etapa posterior.
- Filtros, búsqueda de texto, paginación y ordenamiento nuevos más allá de los que ya tiene el escritorio.
- Cambios en la API, el núcleo, el esquema o el glosario.
- Retirar o modificar la app de escritorio.
- Funcionamiento sin conexión, notificaciones en tiempo real y aplicación móvil nativa.
- Internacionalización a otros idiomas.

## Further Notes

- `GLOSSARY.md` no cambia: "web", "SPA" y "API" son términos técnicos. Toda la interfaz usa los términos definidos allí.
- Si durante la implementación aparece una regla que hoy solo vive en un controlador de desktop, se mueve primero al núcleo con su test, se expone en la API y recién entonces se usa desde la web; no se reimplementa en el navegador.
- Los filtros de Rutas por terreno y dificultad son un criterio de presentación sobre el listado completo; si el volumen de datos hiciera inviable ese enfoque, se revisa con un ticket propio.
- Riesgo asumido y conocido: la API sigue sin autenticación y expone fichas médicas; por eso la web solo funciona en local hasta que exista esa etapa (ADR 0006).
- Corrige un supuesto de la conversación: el ADR 0004 establece formularios para altas y ediciones; la edición inline de celdas no forma parte de la web.
