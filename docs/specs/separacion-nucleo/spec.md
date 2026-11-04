# Especificación técnica: separación del núcleo de la presentación

**Status:** closed  
**Fecha:** 03/11/2026  
**Alcance:** Primera fase de la migración a web. Separar la lógica de la aplicación de su presentación JavaFX, manteniendo la app de escritorio funcionando sin cambios visibles para el usuario.

## Problem Statement

Quiero llevar la aplicación de escritorio a la web y, más adelante, quizás a otros clientes. Hoy no puedo hacerlo sin reescribir buena parte del sistema, porque la lógica está atada a JavaFX. Las pantallas consultan y guardan directamente en la base. Las reglas del negocio viven en las pantallas: cuándo una Salida puede cerrarse, qué datos de un Cliente son válidos, cuándo una Ruta sigue siendo borrador. Algunas reglas están repetidas dentro de consultas SQL, como los estados de inventario que impiden asignar un elemento. Incluso el modelo de datos y la persistencia usan tipos de JavaFX. Las reglas que dependen de la fecha leen el reloj del sistema directamente, y las fichas médicas se copian a una carpeta local de la máquina.

Además, no hay tests. Cualquier cambio estructural solo se puede validar a mano, pantalla por pantalla, y no tengo forma de comprobar que la web se comporte igual que el escritorio.

## Solution

Separar la aplicación en un núcleo independiente de cualquier presentación y en presentaciones que solo muestran y traducen. Al terminar esta fase:

- El núcleo concentra todas las reglas de negocio y las expone como casos de uso. No sabe si lo usa una ventana de escritorio, una API web o un test.
- La persistencia en SQLite es un detalle reemplazable que implementa contratos definidos por el núcleo.
- La app de escritorio funciona igual que antes para el usuario, pero ya no contiene reglas de negocio ni accede a la base.
- Las reglas tienen tests automáticos sobre el mismo contrato que usará la futura API web.

La migración se hace por áreas: **Inventario** (piloto) → **Clientes** → **Rutas, Checkpoints e Itinerario** → **Salidas**. Cada área termina con la app de escritorio funcionando y sus tests en verde. La API web es la fase 2 y queda fuera de esta spec.

## User Stories

### Arquitectura y desarrollo

1. Como desarrollador, quiero que el núcleo no compile si depende de JavaFX, para que la separación no se pueda romper sin querer.
2. Como desarrollador, quiero que el núcleo no dependa de ningún framework web ni de inyección, para elegir el framework de la fase 2 sin tocar las reglas.
3. Como desarrollador, quiero que la presentación de escritorio no acceda a la persistencia, para que sea reemplazable.
4. Como desarrollador de la futura API web, quiero invocar los mismos casos de uso que el escritorio, para no duplicar reglas de negocio.
5. Como desarrollador, quiero que los casos de uso reciban y devuelvan objetos simples, para traducirlos sin esfuerzo a pantallas o a JSON.
6. Como desarrollador, quiero que los errores de negocio lleguen como excepciones propias y no como booleanos ni errores de SQL, para que cada presentación decida cómo mostrarlos.
7. Como desarrollador, quiero que los errores de validación indiquen el campo afectado, para marcarlo en un formulario de escritorio o en una respuesta web.
8. Como desarrollador, quiero que el núcleo reciba el reloj como dependencia, para testear reglas de fecha con un "hoy" fijo.
9. Como desarrollador, quiero poder cambiar el motor de base de datos tocando solo la implementación de persistencia, para preparar la web con varios usuarios.
10. Como desarrollador, quiero que la ruta del archivo de base se configure desde afuera, para que los tests no toquen la base real.
11. Como desarrollador, quiero que los comandos Maven actuales sigan funcionando desde la raíz del repositorio, para no cambiar mi flujo de trabajo.
12. Como desarrollador, quiero migrar un área por vez y dejar la app funcionando al final de cada una, para poder pausar el refactor en cualquier punto sin dejar el proyecto roto.
13. Como desarrollador, quiero eliminar el código heredado de "Recorrido", que no se usa, para no migrar conceptos que el dominio ya descartó.
14. Como desarrollador, quiero que la documentación de agentes describa la nueva estructura de módulos y el vocabulario vigente, para que futuras tareas usen el patrón correcto.

### Inventario

15. Como encargado de inventario, quiero dar de alta, editar, dar de baja y reactivar elementos igual que hoy, para que el refactor no cambie mi trabajo.
16. Como encargado de inventario, quiero que el estado de un elemento describa solo su condición física, para que no contradiga la Reserva de stock calculada.
17. Como encargado de inventario, quiero que "Asignado" deje de ser un estado elegible y que los elementos que lo tenían pasen a "Disponible", para que haya una sola fuente de verdad sobre lo asignado.
18. Como encargado de inventario, quiero seguir viendo cuánto de cada elemento está reservado por Salidas planificadas, para saber qué queda disponible.
19. Como encargado de inventario, quiero que un elemento en reparación, fuera de servicio o extraviado no se pueda asignar a una Salida, para no planificar con equipo inutilizable.
20. Como encargado de inventario, quiero que un elemento en inspección se pueda seguir asignando, como hoy.
21. Como encargado de inventario, quiero que el formulario me avise de errores mientras escribo, para corregirlos antes de guardar.

### Clientes

22. Como administrativo, quiero dar de alta, editar, dar de baja y reactivar Clientes igual que hoy.
23. Como administrativo, quiero que las validaciones de un Cliente (DNI, fecha de nacimiento, contacto de emergencia, datos de tutor para menores) sean las mismas en escritorio y en web, para no tener datos válidos en un cliente e inválidos en otro.
24. Como administrativo, quiero que la condición de menor de edad se calcule siempre respecto de la fecha actual, para pedir tutor cuando corresponde.
25. Como administrativo, quiero adjuntar, reemplazar y consultar la ficha médica de un Cliente igual que hoy.
26. Como administrativo, quiero que la ficha médica se guarde en un único lugar, para no tener copias desincronizadas entre la base y una carpeta.
27. Como administrativo, quiero que los errores aparezcan junto al campo correspondiente mientras completo el formulario.

### Rutas, Checkpoints e Itinerario

28. Como guía, quiero crear, editar, dar de baja y reactivar Rutas igual que hoy.
29. Como guía, quiero que una Ruta sin Checkpoints siga siendo una Ruta borrador y no se pueda usar para planificar una Salida.
30. Como guía, quiero que una Ruta deje de ser borrador al guardar dos o más Checkpoints.
31. Como guía, quiero que el Itinerario conserve el orden explícito que elegí cuando dos Checkpoints comparten hora (ADR 0002).
32. Como guía, quiero que la altitud de los Checkpoints respete la altitud máxima de la Ruta.
33. Como guía, quiero que el formulario de Checkpoint me avise de errores mientras escribo.

### Salidas

34. Como guía, quiero crear una Salida con el Asistente de Salida igual que hoy, con los mismos pasos y reglas de navegación.
35. Como guía, quiero que confirmar una Salida valide todos los datos en un único paso, para que la regla sea la misma desde cualquier presentación.
36. Como guía, quiero que dos personas no puedan reservar a la vez más stock del que existe, para no planificar con equipo inexistente (ADR 0003).
37. Como guía, quiero que la confirmación me diga qué elemento no tiene stock suficiente, para corregir la cantidad.
38. Como guía, quiero que una Salida conserve su Copia operativa y que editarla la reemplace (ADR 0001).
39. Como guía, quiero marcar una Salida planificada como realizada o cancelada, y que esos estados sean finales.
40. Como guía, quiero que intentar cerrar una Salida que no está planificada dé un error claro.
41. Como guía, quiero que cerrar una Salida libere su Reserva de stock.
42. Como guía, quiero que las Salidas vencidas sigan resaltadas en el listado, para acordarme de cerrarlas.
43. Como guía, quiero que el listado de Salidas siga mostrando el resumen de participantes y equipamiento de cada una.
44. Como guía, quiero seguir filtrando Salidas por estado y buscando por Ruta o Cliente igual que hoy.
45. Como guía, quiero exportar la Ficha operativa igual que hoy, con el mismo contenido, nombre de archivo y guardado atómico.
46. Como guía, quiero que la fecha de emisión de la Ficha operativa sea la del momento de generarla.

### Dashboard

47. Como responsable de la operación, quiero ver los mismos indicadores del Dashboard que hoy.
48. Como responsable de la operación, quiero que las próximas salidas se calculen desde la fecha actual con la misma ventana de días.
49. Como responsable de la operación, quiero que el conteo de equipo no disponible use la misma regla de estados que la asignación a Salidas.

## Implementation Decisions

### Módulos

- El proyecto Maven pasa a ser multi-módulo, con un POM padre en la misma ubicación que el proyecto actual, para que los comandos documentados sigan siendo válidos:
  - **trekking-core**: dominio, casos de uso y contratos (repositorios, almacenamiento de fichas médicas, transacción). Java plano: sin JavaFX y sin anotaciones de framework.
  - **trekking-sqlite**: implementa los contratos de core sobre SQLite. Es dueño del esquema, de su inicialización y de la conexión.
  - **trekking-desktop**: la aplicación JavaFX. Contiene pantallas, controllers, utilidades visuales y el punto de entrada.
- Dependencias permitidas: desktop → core y sqlite; sqlite → core. Core no depende de ningún otro módulo. JavaFX solo aparece en desktop. La librería de PDF pasa a core.
- La raíz de paquetes sigue siendo `com.example`, con un subpaquete por módulo (core, sqlite, desktop).
- Ejecutar la app de escritorio pasa a apuntar al módulo desktop.
- Decisión registrada en el ADR 0005.

### Contrato del núcleo

- **Un conjunto de casos de uso por área**: Inventario, Clientes, Rutas (con Checkpoints e Itinerario), Salidas, Dashboard y Ficha operativa. Son la única puerta de entrada de cualquier presentación.
- Los casos de uso reciben y devuelven records u objetos de dominio simples. Nunca exponen conexiones, errores de SQL, colecciones observables ni otros tipos de JavaFX.
- **Dependencias por constructor**: repositorios, almacenamiento de fichas médicas, transacción y reloj.
- **Excepciones de negocio no chequeadas**:
  - Validación, con la lista de errores por campo.
  - Stock insuficiente, indicando el elemento.
  - Transición de estado inválida.
  - Entidad no encontrada.

  Las operaciones dejan de devolver booleanos de éxito o fracaso. Desktop traduce cada excepción a una alerta o a errores en el formulario. La API web las traducirá a códigos HTTP.
- **Validadores por entidad** expuestos sin efectos, que devuelven errores por campo. Desktop los usa para validar en vivo y los casos de uso los vuelven a aplicar al guardar. Las reglas de validación que hoy están en la presentación se mueven a core.

### Reglas de dominio que se formalizan

- **Estado de Salida**: enum con planificada, realizada y cancelada. Solo una Salida planificada puede pasar a realizada o cancelada, y ambos son estados finales (ADR 0001). Cerrarla libera su Reserva de stock.
- **Estado del elemento** (inventario): enum con disponible, en reparación, en inspección, fuera de servicio y extraviado. El propio enum indica cuáles impiden asignar el elemento a una Salida: en reparación, fuera de servicio y extraviado. Esta regla se define una sola vez, y el Dashboard y la asignación la consultan desde ahí.
- **Baja** de Cliente, Ruta y elemento como condición explícita del dominio.
- **Salida vencida**: condición calculada por core, sin persistir. Es una Salida planificada con fecha anterior a hoy.
- **Ruta borrador**: la regla pasa a core.
- **Reloj**: toda regla que dependa de "hoy" usa el reloj inyectado: menor de edad, Salida vencida, próximas salidas y emisión de la Ficha operativa.
- La base sigue guardando los mismos textos de estado que hoy. La conversión entre enum y texto es responsabilidad de sqlite.

### Transacciones y concurrencia

- Core define un contrato de transacción: ejecuta un bloque de trabajo de forma atómica y serializada.
- El caso de uso de confirmar o editar una Salida valida la Reserva de stock y guarda la Copia operativa dentro de una misma transacción. Así la regla vive en core.
- Sqlite implementa la transacción tomando el bloqueo de escritura al comenzar, para conservar la garantía del ADR 0003 entre instancias.

### Asistente de Salida

- No se persisten borradores. La presentación mantiene el estado del asistente y la navegación entre pasos.
- Core ofrece un único caso de uso de confirmación que recibe todos los datos (fecha, Ruta, Checkpoints, equipamiento y participantes) y valida todo.

### Listados

- Core devuelve listados completos con sus resúmenes, por ejemplo participantes y equipamiento por Salida. La presentación filtra y busca. No hay paginación en esta fase.

### Archivos

- **Fichas médicas**: core las guarda y lee mediante un contrato de almacenamiento. La implementación inicial usa la base de datos, que pasa a ser la única fuente de verdad. Se elimina la copia en una carpeta local.
- **Ficha operativa**: core la genera como bytes, sin conocer el destino. Desktop elige dónde guardarla y la escribe de forma atómica, como hoy.

### Persistencia

- La ubicación del archivo de base se recibe por constructor. Desktop la resuelve con la lógica actual: propiedad de sistema y, si no está, búsqueda del proyecto hacia arriba.
- **Cambios de esquema** (fuera de estos, el esquema queda congelado durante la fase):
  1. Se dejan de crear las tablas del concepto heredado "Recorrido". No se borran de bases existentes.
  2. Los elementos de inventario con estado "Asignado" se migran a "Disponible".
- El script de datos de demostración no genera el estado "Asignado" y no requiere cambios.

### Presentación de escritorio

- Los controllers solo dependen de casos de uso de core y de utilidades visuales.
- Los Checkpoints pasan a ser objetos planos en core. Si una pantalla necesita propiedades observables, desktop arma su propio modelo de vista.
- Cambios visibles: se quita "Asignado" del selector de estado de inventario y desaparece la copia de fichas médicas a la carpeta local. El resto de la interfaz no cambia.

### Orden de trabajo

1. Estructura multi-módulo y contratos transversales: excepciones, transacción, reloj y configuración de base.
2. Inventario (piloto que fija el patrón).
3. Clientes, con fichas médicas.
4. Rutas, Checkpoints e Itinerario.
5. Salidas, con Asistente, Reserva de stock, Ficha operativa y Dashboard.
6. Limpieza del código heredado y actualización de la documentación de agentes.

## Testing Decisions

- **Qué es un buen test**: verifica comportamiento observable a través de un contrato público. Dados ciertos datos, un caso de uso devuelve un resultado, persiste un cambio o lanza una excepción de negocio determinada. No verifica cómo está implementado por dentro: métodos privados, llamadas internas o estructura de clases. Así un refactor interno no rompe tests.
- **Punto principal: los casos de uso públicos de core**, con repositorios falsos en memoria y reloj fijo. Es el mismo contrato que usarán escritorio y web, así que ahí se prueban todas las reglas. Como mínimo:
  - Transiciones de estado de Salida y liberación de la Reserva de stock al cerrar.
  - Stock insuficiente al confirmar, y elementos no asignables por su estado.
  - Salida vencida.
  - Validaciones de Cliente, incluida la regla de menor de edad.
  - Ruta borrador y altitud de Checkpoints.
  - Orden del Itinerario (ADR 0002).
  - Reemplazo de la Copia operativa al editar (ADR 0001).
- **Punto secundario: los contratos de repositorio y de transacción implementados por sqlite**, contra una base temporal por test. Solo para lo que los repositorios falsos no pueden probar: consultas SQL, serialización de la reserva de stock (ADR 0003) y la migración de "Asignado" a "Disponible".
- **No se testean** controllers, validadores sueltos ni entidades por separado. Sus reglas se prueban a través de los casos de uso.
- **Herramienta**: JUnit 5 en core y sqlite. La fase de tests de Maven corre ambos módulos.
- Cada área agrega sus tests antes de darse por migrada.
- **Validación manual del escritorio** en cada área: alta, edición, baja y reactivación, listados y filtros. Al final, el Asistente de Salida completo y la exportación de la Ficha operativa.
- **Antecedentes**: el proyecto no tiene tests previos. Inventario, como piloto, fija el patrón de tests de casos de uso y de integración que siguen las demás áreas.

## Out of Scope

- API HTTP, framework web y cliente web (fase 2).
- Cambio de motor de base de datos.
- Usuarios, autenticación, roles y permisos.
- Paginación y filtrado del lado del servidor.
- Borradores persistidos del Asistente de Salida.
- Renombrar la raíz de paquetes.
- Herramienta de migraciones de esquema.
- Cambios visibles en la interfaz de escritorio, salvo los dos indicados en Implementation Decisions.
- Tests automáticos de la interfaz de escritorio.

## Further Notes

- Decisión de arquitectura registrada en el ADR 0005. Se respetan los ADR 0001 (Copia operativa), 0002 (orden del Itinerario), 0003 (Reserva de stock atómica) y 0004 (acciones por fila y formularios).
- Términos nuevos en el glossary: **Salida vencida** y **Estado del elemento**.
- El cambio de motor de base probablemente se vuelva necesario en la fase web, por la concurrencia de varios usuarios sobre la Reserva de stock. Esta separación lo deja acotado a la implementación de persistencia.
