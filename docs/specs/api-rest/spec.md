# Especificación técnica: API REST del backend

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Status:** closed
**Fecha:** 05/11/2026
**Alcance:** Segunda fase de la migración a web. Exponer los casos de uso de `trekking-core` mediante una API REST en un módulo nuevo, para que la futura interfaz web pueda consumirlos. Respeta los ADR 0001, 0003, 0005 y 0006.

## Problem Statement

Tengo un backend (`trekking-core` + `trekking-sqlite`) que ya concentra todas las reglas de negocio, pero solo se puede usar desde la app de escritorio JavaFX. Para construir la interfaz web no existe ningún punto de entrada accesible desde un navegador: no hay contrato de datos, no hay forma de que una página consulte Rutas, administre Clientes o confirme una Salida, ni de descargar la Ficha operativa o una ficha médica. Mientras tanto, la app de escritorio sigue en uso y no puedo romperla ni duplicar reglas de negocio en otro lugar.

## Solution

Un servicio HTTP, ejecutable por separado, que traduce peticiones REST a los casos de uso existentes y devuelve JSON con el vocabulario del dominio. Al terminar esta fase:

- La interfaz web puede hacer, por HTTP, todo lo que hoy hace la app de escritorio: administrar Rutas con su Itinerario, Inventario y Clientes (con fichas médicas), ver el Dashboard, y crear, editar, cerrar y exportar Salidas.
- Todas las reglas siguen en el núcleo; la API solo traduce formatos y errores.
- Los errores de negocio llegan a la interfaz como respuestas HTTP uniformes y fáciles de mostrar por campo.
- La API y la app de escritorio pueden usarse a la vez sobre la misma base de datos.
- Hay un contrato OpenAPI generado para construir la interfaz web.
- La API escucha solo en la máquina local y no autentica (ADR 0006).

## User Stories

### Arquitectura y operación

1. Como desarrollador, quiero que la API sea un módulo propio que no dependa de la app de escritorio ni de JavaFX, para que ambas presentaciones evolucionen por separado.
2. Como desarrollador, quiero que las anotaciones del framework web queden solo en el módulo de la API, para que el núcleo siga siendo Java plano.
3. Como desarrollador, quiero que la API reutilice los casos de uso del núcleo sin reimplementar reglas, para que la web y el escritorio se comporten igual.
4. Como operador, quiero iniciar la API con un solo comando Maven, para probarla sin pasos manuales.
5. Como operador, quiero que la API use el mismo archivo de base de datos que la app de escritorio, para trabajar con los mismos datos mientras dura la migración.
6. Como operador, quiero indicar otra base de datos con la misma propiedad de sistema que ya usa el escritorio, para no aprender otra configuración.
7. Como operador, quiero que la API escuche por defecto solo en `127.0.0.1:8080`, para no exponer datos sensibles por accidente.
8. Como operador, quiero cambiar el puerto por configuración, para evitar conflictos con otros servicios.
9. Como operador, quiero que la API cree o actualice el esquema al iniciar, para no depender de haber abierto antes la app de escritorio.
10. Como desarrollador de la interfaz web, quiero configurar un único origen permitido por CORS, para que solo mi página pueda llamar a la API.
11. Como desarrollador de la interfaz web, quiero que sin origen configurado el navegador no pueda llamar a la API desde otros orígenes, para no tener una política abierta por defecto.
12. Como desarrollador de la interfaz web, quiero un contrato OpenAPI generado y una página de documentación, para conocer rutas, formatos y errores sin leer el código.
13. Como desarrollador, quiero que la API y el escritorio puedan escribir al mismo tiempo sin corromper datos ni sobrevender stock, para migrar sin congelar el escritorio.

### Convenciones del contrato

14. Como desarrollador de la interfaz web, quiero recursos y campos con nombres en español del dominio, para que coincidan con el vocabulario de la UI.
15. Como desarrollador de la interfaz web, quiero que las fechas viajen en ISO-8601 y las horas como `HH:mm`, para no convertir formatos propios.
16. Como desarrollador de la interfaz web, quiero ids enteros estables, para enlazar recursos entre sí.
17. Como desarrollador de la interfaz web, quiero DTOs propios de la API y no los tipos internos del núcleo, para que cambiar el núcleo no rompa el contrato.
18. Como desarrollador de la interfaz web, quiero listados completos sin paginación, para mostrar tablas como en el escritorio.
19. Como usuario, quiero filtrar listados por Baja con `estado=activas|bajas|todas` donde el dominio lo permita, para ver los registros dados de baja y reactivarlos.
20. Como usuario, quiero que la baja y la reactivación sean acciones explícitas y no un borrado, para respetar que una Baja no elimina el registro.

### Manejo de errores

21. Como usuario de la interfaz web, quiero recibir los errores de validación con el campo afectado y su mensaje, para marcarlos en el formulario.
22. Como usuario de la interfaz web, quiero que un recurso inexistente responda como no encontrado, para mostrar un aviso claro.
23. Como usuario de la interfaz web, quiero que cerrar una Salida que ya no está planificada responda como conflicto, para entender que el estado cambió.
24. Como usuario de la interfaz web, quiero que editar una Salida que ya no está planificada responda como conflicto, para no perder la noticia de que es final.
25. Como usuario de la interfaz web, quiero que reservar más stock del disponible responda como conflicto e indique el elemento y la cantidad disponible, para corregir la cantidad.
26. Como desarrollador de la interfaz web, quiero un cuerpo de error uniforme para todos los casos, para manejarlos en un solo lugar.
27. Como desarrollador, quiero que un error inesperado responda 500 con un mensaje genérico y sin detalles internos, para no filtrar información.
28. Como desarrollador de la interfaz web, quiero que un cuerpo mal formado o con tipos incorrectos responda 400, para distinguirlo de una regla de negocio incumplida.

### Rutas e Itinerario

29. Como guía, quiero listar Rutas activas, dadas de baja o todas, para elegir sobre cuál trabajar.
30. Como guía, quiero ver el detalle de una Ruta, para consultar sus datos y si es Ruta borrador.
31. Como guía, quiero crear una Ruta, para registrar un recorrido nuevo.
32. Como guía, quiero editar una Ruta, para corregir sus datos.
33. Como guía, quiero dar de baja y reactivar una Ruta, para sacarla del uso sin perder su historia.
34. Como guía, quiero consultar el Itinerario de una Ruta, para ver sus Checkpoints en orden.
35. Como guía, quiero guardar el Itinerario completo de una Ruta con el orden explícito de los Checkpoints, para reordenar y editar de una vez.
36. Como guía, quiero que una Ruta pase de borrador cuando guardo dos o más Checkpoints, para poder usarla en una Salida.
37. Como guía, quiero errores por campo si un Checkpoint es inválido, para corregirlo.

### Inventario

38. Como encargado de equipo, quiero listar elementos activos, dados de baja o asignables, para ver qué hay disponible.
39. Como encargado de equipo, quiero ver el detalle de un elemento, para consultar cantidad, Estado del elemento y vigencia.
40. Como encargado de equipo, quiero crear y editar un elemento, para mantener el catálogo.
41. Como encargado de equipo, quiero dar de baja y reactivar un elemento, para retirar equipo sin borrarlo.
42. Como encargado de equipo, quiero que los elementos en reparación, fuera de servicio o extraviados no figuren como asignables, para no asignarlos a una Salida.

### Clientes y fichas médicas

43. Como administrador, quiero listar Clientes activos, dados de baja o todos, para encontrar a quién busco.
44. Como administrador, quiero ver el detalle de un Cliente, incluido si es menor, para aplicar las reglas de menores.
45. Como administrador, quiero crear y editar un Cliente con validación por campo, para mantener datos correctos.
46. Como administrador, quiero validar los datos de un Cliente sin guardarlos, para dar retroalimentación mientras escribe.
47. Como administrador, quiero dar de baja y reactivar un Cliente, para excluirlo sin borrar su registro.
48. Como administrador, quiero adjuntar o reemplazar la ficha médica de un Cliente subiendo un archivo, para tenerla disponible.
49. Como administrador, quiero descargar la ficha médica con su nombre original, para revisarla.
50. Como administrador, quiero quitar la ficha médica de un Cliente, para eliminarla cuando ya no corresponda.
51. Como administrador, quiero un límite de tamaño configurable para la ficha médica, 10 MB por defecto, para evitar archivos desmedidos.
52. Como administrador, quiero que descargar una ficha médica inexistente responda como no encontrada, para saber que el Cliente no tiene una.

### Salidas

53. Como guía, quiero listar las Salidas con su resumen operativo y si están vencidas, para ver el estado general.
54. Como guía, quiero ver el detalle de una Salida, para consultar su estado y datos.
55. Como guía, quiero consultar la Copia operativa de una Salida, para ver exactamente qué se planificó.
56. Como guía, quiero confirmar una Salida con fecha, Ruta, equipamiento, elementos nuevos, Clientes existentes y Clientes nuevos, para crearla en una sola operación.
57. Como guía, quiero que confirmar la Salida sea atómico, para que un fallo en stock no deje datos a medias.
58. Como guía, quiero que los Clientes nuevos de la confirmación puedan traer su ficha médica como nombre y contenido en base64, para guardarla en la misma transacción.
59. Como guía, quiero que no se pueda planificar una Salida con una Ruta borrador, para respetar la regla del dominio.
60. Como guía, quiero editar una Salida planificada reemplazando su Copia operativa vigente, para corregirla sin crear historial.
61. Como guía, quiero consultar qué elementos puedo asignar al editar una Salida, para no ofrecer los que no están disponibles.
62. Como guía, quiero marcar una Salida como realizada, para cerrarla.
63. Como guía, quiero cancelar una Salida, para liberar su reserva de stock.
64. Como guía, quiero que una Salida realizada o cancelada no admita nuevos cambios de estado, para respetar los estados finales.
65. Como guía, quiero descargar la Ficha operativa en PDF con un nombre de archivo reconocible, para imprimirla.
66. Como guía, quiero que la Ficha operativa no incorpore adjuntos médicos, para no divulgar datos sensibles.

### Dashboard

67. Como director, quiero consultar los indicadores generales (Rutas activas, Clientes activos, Salidas próximas, equipo no disponible), para tener una vista rápida.
68. Como director, quiero consultar los indicadores de Salidas, para ver el estado de la planificación.
69. Como director, quiero listar las próximas Salidas con ruta, fecha y horarios, para anticiparme.

## Implementation Decisions

### Estructura

- Se agrega un módulo `trekking-api` al proyecto Maven padre. Depende de `trekking-core` y `trekking-sqlite`, nunca de `trekking-desktop` ni de JavaFX, y ningún otro módulo depende de él.
- `trekking-core` no cambia, salvo que una historia lo requiera de forma explícita; la API traduce y no decide reglas.
- La API tiene su propio ensamblado de casos de uso y repositorios, equivalente a `Servicios` de desktop, expuesto como beans. Se acepta repetir ese cableado (ADR 0006).
- La ubicación de la base se resuelve igual que en `ConexionDB`: propiedad de sistema `trekking.db`, o `trekking.db` en la raíz del repositorio. La API inicializa el esquema al arrancar.
- El reloj del sistema se inyecta a los casos de uso que lo piden, como hoy.

### Configuración

- Puerto `8080` y bind a `127.0.0.1` por defecto, ambos configurables.
- CORS con un único origen configurable (`trekking.api.origen-web`); sin valor, no se permite ningún origen cruzado.
- Tamaño máximo de ficha médica configurable, 10 MB por defecto.
- Sin autenticación en esta etapa (ADR 0006).

### Convenciones del contrato

- Prefijo `/api`; recursos en plural y en español: `rutas`, `clientes`, `inventario`, `salidas`, `dashboard`.
- Cuerpos JSON con campos en español, fechas ISO-8601, horas `HH:mm`, ids enteros.
- DTOs de petición y respuesta propios de la API, separados de los tipos del núcleo.
- Listados completos, sin paginación. Rutas, Inventario y Clientes aceptan `estado=activas|bajas|todas` con `activas` por defecto. Inventario también ofrece el listado de asignables.
- Altas con `POST` devuelven 201 con el recurso creado; ediciones con `PUT`; bajas y reactivaciones como `POST .../baja` y `POST .../reactivar`. No hay `DELETE` para registros con Baja.
- Contrato OpenAPI generado y una página de documentación interactiva.

### Mapeo de errores

Cuerpo JSON uniforme con un código de error, un mensaje y, cuando corresponde, la lista de errores por campo.

| Excepción del núcleo | HTTP |
|---|---|
| `ValidacionException` | 422, con la lista de `ErrorCampo` |
| `EntidadNoEncontradaException` | 404 |
| `TransicionInvalidaException` | 409 |
| `SalidaNoEditableException` | 409 |
| `StockInsuficienteException` | 409, con elemento y disponible |
| Cuerpo mal formado o tipos incorrectos | 400 |
| Cualquier otra | 500, mensaje genérico sin detalles internos |

### Recursos y operaciones

- **Rutas:** listar (con filtro), obtener, crear, editar, baja, reactivar.
- **Itinerario:** obtener y guardar el Itinerario completo de una Ruta; guardarlo devuelve la Ruta resultante.
- **Inventario:** listar (activos, bajas, asignables), obtener, crear, editar, baja, reactivar.
- **Clientes:** listar (con filtro), obtener, crear, editar, baja, reactivar, validar sin guardar.
- **Ficha médica:** subir/reemplazar con multipart, descargar con el nombre original, quitar. Descargar una inexistente da 404.
- **Salidas:** listar con resumen y condición de vencida, obtener, ver Copia operativa, listar elementos para editar, confirmar, editar, marcar realizada, cancelar, descargar Ficha operativa en PDF.
- **Confirmar Salida:** recibe fecha, Ruta, líneas de equipamiento, elementos nuevos, ids de Clientes existentes y Clientes nuevos. Cada Cliente nuevo puede traer una ficha como nombre de archivo y contenido en base64. Los Checkpoints no se envían: la Salida copia el Itinerario vigente.
- **Dashboard:** indicadores, indicadores de Salidas y próximas Salidas.

### Arquitectura

- Los controladores solo traducen entre HTTP y los casos de uso; no tienen reglas de negocio ni SQL.
- El manejo de errores está centralizado en un único punto.
- La transacción de confirmar y editar Salidas sigue bajo control del núcleo (ADR 0005), de modo que la API no abre ni cierra transacciones.
- La Ficha operativa se genera en el núcleo como bytes; la API solo la entrega como descarga.
- Sin cambios de esquema: la API usa el esquema vigente.

### Orden de entrega

1. Esqueleto del módulo, ensamblado, configuración, mapeo de errores, OpenAPI y Rutas con Itinerario.
2. Inventario.
3. Clientes y fichas médicas.
4. Dashboard.
5. Salidas (confirmar, editar, cerrar, Copia operativa, Ficha operativa), al final porque orquestan todo lo anterior.

## Testing Decisions

- **Seam único:** la frontera HTTP de la API, probada con `MockMvc` contra el ensamblado real (casos de uso reales y SQLite en un `@TempDir`; nunca el `trekking.db` del repositorio). Es el punto más alto disponible y cubre contrato, traducción y mapeo de errores de punta a punta.
- Un buen test observa solo comportamiento externo: estado HTTP, cuerpo JSON y efectos visibles en consultas posteriores. No verifica clases internas, DTOs ni llamadas a casos de uso.
- Por recurso: el camino feliz y un test por cada error mapeado que ese recurso puede producir (400, 404, 409, 422).
- Pruebas específicas: filtro `estado`, baja y reactivación, descarga y subida de ficha médica (incluido el límite de tamaño), confirmación atómica de una Salida con falta de stock sin dejar datos parciales, cierre de una Salida ya final, y Ficha operativa en PDF que no incluye adjuntos médicos.
- Las reglas de negocio se siguen probando en `trekking-core`; la API no las repite.
- Prior art: los tests de `trekking-sqlite` con `@TempDir` para el aislamiento de la base, y los de `trekking-core` para el estilo de casos por comportamiento público.
- La concurrencia con el escritorio no se automatiza; se valida a mano en la primera entrega que escriba datos.

## Out of Scope

- Autenticación, autorización, usuarios y roles (ADR 0006); queda para una etapa posterior con su spec y ADR.
- Exponer la API fuera de `127.0.0.1` o despliegue en un servidor.
- La interfaz web en sí.
- Paginación, búsqueda de texto y ordenamiento configurable.
- Cambios de esquema, cambio de motor de base de datos o migración desde SQLite.
- Retirar o modificar la app de escritorio.
- Versionado de la API, límites de uso, caché y métricas.
- Notificaciones, tiempo real y subida de archivos en partes.
- Nuevas reglas de negocio o cambios al glosario.

## Further Notes

- Si durante la implementación aparece una regla que hoy solo vive en un controlador de desktop, se mueve primero al núcleo con su test y luego se expone; no se reimplementa en la API.
- `GLOSSARY.md` no cambia: "API" e "interfaz web" son términos técnicos.
- Riesgo asumido y conocido: la API expone fichas médicas sin autenticación; por eso solo escucha en local hasta que exista la etapa de autenticación.
- Al compartir base con el escritorio, ambos procesos deben respetar la escritura serializada (ADR 0003).
