# Especificación funcional: Salidas, rutas, checkpoints e inventario

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Borrador implementable para revisión  
**Fecha:** 25/10/2026  
**Alcance:** Rediseño de rutas, checkpoints y salidas; reorganización de módulos; inventario y equipamiento por salida.

## Problem Statement

La aplicación administra checkpoints de forma global, usa recorridos como instancias de rutas y hereda el equipamiento requerido desde la ruta. Esto dificulta que un guía planifique una salida completa, administre el equipamiento realmente asignado y conserve una representación confiable de lo planificado frente a cambios posteriores en los catálogos.

En la gestión de Rutas, los Checkpoints se muestran en un panel fijo del listado y solo pueden editarse desde el formulario general de alta o edición de la Ruta. Esto mezcla dos tareas y no ofrece una pantalla enfocada en consultar y administrar el Itinerario de una Ruta.

## Solution

Organizar la aplicación alrededor de cuatro módulos: Salidas, Rutas, Clientes e Inventario. Una Ruta será una plantilla técnica con su propio itinerario; una Salida será una instancia fechada que mantendrá una copia operativa reemplazable de la ruta, sus checkpoints, equipamiento y participantes. Un wizard permitirá crear o editar la planificación en una sola operación transaccional, validando la disponibilidad de stock. La pantalla principal de Salidas incluirá los indicadores operativos y permitirá consultar o generar una ficha PDF desde la copia guardada.

En Rutas, el listado ofrecerá una acción por fila para abrir la gestión del Itinerario. El alta y edición de los datos técnicos de Ruta quedarán separados de la gestión de Checkpoints, que se guardará como un conjunto completo y validado.

## User Stories

1. Como guía, quiero iniciar la aplicación en Salidas, para acceder directamente a la planificación operativa.
2. Como guía, quiero navegar solo entre Salidas, Rutas, Clientes e Inventario, para encontrar cada tarea en un módulo claro.
3. Como guía, quiero crear una Ruta con datos técnicos, para reutilizar una plantilla en futuras Salidas.
4. Como guía, quiero guardar una Ruta sin Checkpoints y verla como borrador, para completar su itinerario más adelante.
5. Como guía, quiero agregar, editar, ordenar y quitar Checkpoints de una Ruta, para mantener su Itinerario actualizado.
6. Como guía, quiero que cada Checkpoint pertenezca a una sola Ruta, para evitar compartir puntos accidentalmente entre plantillas.
7. Como guía, quiero ver el estado borrador/completa y la cantidad de Checkpoints de cada Ruta, para reconocer si su Itinerario está listo.
8. Como guía, quiero dar de baja y reactivar Rutas sin perder sus Checkpoints ni las Salidas existentes, para conservar el historial.
9. Como guía, quiero crear una Salida en cinco pasos, para completar su fecha, Ruta, Equipamiento, Participantes y revisión en orden.
10. Como guía, quiero elegir una Ruta activa existente o crear una nueva dentro del wizard, para planificar sin abandonar el flujo.
11. Como guía, quiero editar la copia operativa de los Checkpoints de una Salida, para reflejar condiciones particulares de esa planificación.
12. Como guía, quiero refrescar la copia de Ruta y Checkpoints con “Actualizar desde ruta”, para reemplazarla por los datos actuales de la plantilla.
13. Como guía, quiero guardar un Checkpoint nuevo de una Salida también en su Ruta activa, para reutilizar ese punto en futuras Salidas.
14. Como guía, quiero asignar Elementos de inventario y cantidades a una Salida, para registrar el Equipamiento que se llevará realmente.
15. Como guía, quiero ver la cantidad disponible al asignar Equipamiento, para evitar comprometer más stock del existente.
16. Como guía, quiero crear un Elemento de inventario desde el wizard, para completar una planificación sin interrumpirla.
17. Como guía, quiero asociar Clientes activos existentes a una Salida, para registrar sus Participantes.
18. Como guía, quiero crear un Cliente desde el wizard, para completar una planificación sin salir del flujo.
19. Como guía, quiero reutilizar un Cliente existente si su DNI ya está registrado, para no duplicar personas.
20. Como guía, quiero revisar toda la planificación antes de confirmarla, para detectar errores antes de reservar stock.
21. Como guía, quiero cancelar el wizard y descartar sus entidades nuevas, para evitar registros parciales que no pertenezcan a una Salida confirmada.
22. Como guía, quiero que el alta o edición de una Salida se guarde atómicamente, para que un fallo no deje datos parciales.
23. Como guía, quiero editar una Salida en cualquier estado conservando su estado, para corregir datos operativos sin revertir una transición.
24. Como guía, quiero marcar una Salida planificada como realizada o cancelada, para cerrar manualmente su planificación y liberar su reserva.
25. Como guía, quiero que una Salida planificada reserve stock y que una Salida realizada o cancelada lo libere, para consultar disponibilidad real.
26. Como guía, quiero que el sistema valide de forma serializada la disponibilidad al guardar, para evitar sobreasignaciones desde instancias concurrentes de la aplicación.
27. Como guía, quiero que al editar una Salida planificada se excluya su propia reserva anterior del cálculo, para poder cambiar sus cantidades sin falsos errores.
28. Como guía, quiero consultar salidas próximas, Rutas activas, Clientes activos e inventario crítico, para entender rápidamente la situación operativa.
29. Como guía, quiero buscar Salidas por Ruta o Cliente y filtrarlas por estado, para localizar una planificación.
30. Como guía, quiero distinguir las Salidas planificadas con fecha pasada, para identificar las pendientes de cierre.
31. Como guía, quiero generar una ficha operativa PDF desde la copia de la Salida, para compartir los datos que se confirmaron.
32. Como administrador, quiero gestionar Clientes con sus datos actuales y baja lógica, para mantener el catálogo sin perder relaciones históricas.
33. Como administrador, quiero gestionar el Inventario, su stock, estado y vigencia, para mantener actualizados los elementos disponibles.
34. Como administrador, quiero ver cantidades reservadas y disponibles, para decidir si puedo modificar el stock total.
35. Como administrador, quiero que las copias de Salidas sobrevivan a cambios o bajas de Rutas, Clientes y Elementos de inventario, para conservar el registro de lo planificado.
36. Como administrador, quiero corregir una Salida mediante edición y reservar la eliminación física para errores de carga, para evitar perder historial en el uso normal.
37. Como guía, quiero abrir el wizard de creación dentro de la ventana actual, para planificar sin perder el contexto de navegación de la aplicación.
38. Como guía, quiero ver el nombre de la tarea y el progreso de los cinco pasos, para saber dónde estoy y cuánto falta.
39. Como guía, quiero que los pasos largos se desplacen sin esconder sus acciones principales, para completar la planificación con comodidad.
40. Como guía, quiero crear un Cliente o un Elemento de inventario en un diálogo y regresar al paso donde estaba, para completar la Salida sin perder lo ingresado.
41. Como guía, quiero confirmar antes de descartar una Salida sin confirmar, para evitar perder datos por cancelar o cambiar de módulo accidentalmente.
42. Como guía, quiero volver a la tabla actualizada al confirmar o descartar el wizard, para continuar trabajando desde Salidas.
43. Como guía, quiero abrir la gestión del Itinerario desde una acción de la fila de Ruta, para consultar y mantener sus Checkpoints en una pantalla dedicada.
44. Como guía, quiero ver el resumen de la Ruta junto con su lista ordenada de Checkpoints, para entender el Itinerario que estoy administrando.
45. Como guía, quiero agregar y editar cada Checkpoint en su propio formulario, para ingresar y corregir sus datos sin mezclar la edición con los campos generales de Ruta.
46. Como guía, quiero quitar Checkpoints de la lista en edición, para preparar el Itinerario completo antes de guardarlo.
47. Como guía, quiero reordenar Checkpoints que comparten horario y que cambiar un horario actualice su posición cronológica, para conservar un orden coherente con el día operativo.
48. Como guía, quiero guardar todos los cambios del Itinerario juntos, para evitar que una Ruta quede persistida con un solo Checkpoint o con cambios parciales.
49. Como guía, quiero recibir una confirmación al salir con cambios sin guardar y elegir guardar, descartar o seguir editando, para controlar qué ocurre con mi trabajo.
50. Como guía, quiero reconocer las Rutas borrador y saber que debo completar su Itinerario antes de usarlas en una Salida, para evitar planificar con una plantilla incompleta.

## Implementation Decisions

### Módulos y navegación

- La navegación contiene cuatro módulos: Salidas, Rutas, Clientes e Inventario.
- La aplicación abre en Salidas. El dashboard deja de ser un módulo; sus indicadores se integran en Salidas.
- Checkpoints deja de ser un módulo independiente. Recorridos deja de ser vocabulario de interfaz y se reemplaza por Salidas. Equipamiento se usa solo para los Elementos de inventario asignados a una Salida.

### Ruta e Itinerario

- Una Ruta contiene identificador, nombre, altitud máxima, tipo de terreno, dificultad técnica, dificultad física y estado activo.
- El nombre es obligatorio, de hasta 100 caracteres después de normalizar, y admite letras Unicode, números y espacios. Se recortan espacios exteriores y se colapsan secuencias de espacios. La unicidad no distingue mayúsculas e incluye Rutas inactivas; para reutilizar un nombre se reactiva la Ruta existente.
- Las opciones cerradas de terreno son `Rocoso`, `Boscoso`, `Sendero` y `Mixto`. Las opciones cerradas para dificultad técnica y física son `Baja`, `Media` y `Alta`.
- Altitud máxima es obligatoria, numérica, en metros y mayor o igual a cero. Debe ser igual o superior a la altitud de cada Checkpoint de la Ruta.
- Una Ruta con cero Checkpoints es borrador. Una Ruta con dos o más es completa. No se permite persistir una Ruta con exactamente un Checkpoint.
- El Itinerario pertenece a una única Ruta y sus horarios corresponden a un mismo día operativo; no se modela cruce de medianoche.
- Los Checkpoints se presentan por hora ascendente. Para horas iguales se conserva una posición explícita y persistida en el Itinerario, definida antes de guardar; el desempate no depende del identificador.
- Editar una Ruta no altera copias operativas de Salidas existentes. Darla de baja es lógico y conserva sus Checkpoints.
- Las Rutas inactivas no se ofrecen al crear una Salida ni al cambiar la Ruta de una Salida existente. Una Salida que ya referencia una Ruta luego inactivada puede conservarla y editarse; la acción para guardar un Checkpoint nuevo también en la Ruta queda deshabilitada.

#### Gestión del Itinerario desde Rutas

- El listado de Rutas no presenta un panel permanente de Checkpoints ni carga su contenido al seleccionar una fila. Cada fila ofrece la acción «Ver checkpoints» para abrir la pantalla de gestión de esa Ruta.
- La pantalla de gestión muestra un resumen de la Ruta y la lista completa de Checkpoints de su Itinerario. Desde allí se puede agregar, editar y quitar Checkpoints; cada alta o edición utiliza un formulario de Checkpoint.
- El formulario de alta o edición de Ruta contiene los datos técnicos de la Ruta, no el editor del Itinerario. Después de guardar una Ruta nueva, se abre su pantalla de gestión para completar el Itinerario si corresponde.
- Los cambios de Checkpoints se mantienen como edición pendiente y se guardan juntos al confirmar el Itinerario. El guardado completo es atómico: si falla, la Ruta conserva el Itinerario guardado anteriormente.
- Un Itinerario puede guardarse con cero Checkpoints como Ruta borrador o con dos o más como Ruta completa; no se puede guardar con exactamente uno. «Guardar» con exactamente un Checkpoint informa la validación y conserva la pantalla y los cambios pendientes.
- «Ruta borrador» es un estado derivado de tener cero Checkpoints, no un estado manual separado de activa/inactiva. La lista y el resumen muestran «Borrador» para esas Rutas y el requisito de completar el Itinerario antes de planificar una Salida.
- Solo se pueden reordenar manualmente Checkpoints que tengan la misma hora. La hora determina el orden ascendente principal; cambiarla recoloca el Checkpoint según su nueva hora. Para horas iguales, se persiste la posición elegida por el guía.
- Si se intenta salir de la pantalla con cambios pendientes, el aviso ofrece «Guardar», «Descartar» y «Seguir editando». Si guardar no supera las validaciones, se mantiene la pantalla abierta. La navegación por el shell y las acciones de regreso respetan esa confirmación.
- Las Rutas borrador no se pueden seleccionar ni confirmar como Ruta de una Salida. Una Salida puede conservar cero o más Checkpoints operativos propios cuando su Ruta de origen está completa; esta regla no cambia las validaciones de la copia operativa.

### Checkpoint

- Cada Checkpoint contiene identificador, Ruta propietaria, nombre, hora, latitud, longitud, altitud y descripción.
- Nombre y hora son obligatorios. La hora usa formato de 24 horas `HH:mm`.
- Latitud y longitud son obligatorias, numéricas y decimales; latitud debe estar entre -90 y 90 y longitud entre -180 y 180.
- Altitud se expresa en metros, es obligatoria y mayor o igual a cero. Descripción es opcional.
- Un Checkpoint de Ruta no puede reasignarse a otra Ruta. Los Checkpoints copiados en una Salida son registros operativos propios de esa Salida.

### Salida y copia operativa

- Una Salida contiene identificador, Ruta de origen, fecha, estado y copia operativa de los datos de Ruta. No tiene hora de inicio ni de fin.
- La fecha es obligatoria y puede ser pasada o futura. Una Salida nueva nace `planificada`.
- Los estados son `planificada`, `realizada` y `cancelada`. La transición normal es `planificada` a `realizada` o `cancelada`. Los estados realizada y cancelada son finales en el flujo normal. Editar una Salida en cualquier estado conserva su estado; la eliminación física queda como operación excepcional administrativa por error de carga y no forma parte de las acciones habituales.
- La copia operativa incluye nombre, altitud máxima, terreno y dificultades de la Ruta; Checkpoints; Elementos de inventario con referencia, nombre, categoría y cantidad; y Clientes con referencia, DNI, nombre, apellido, teléfono y datos de contacto de emergencia (nombre, teléfono y relación).
- La copia es independiente de cambios posteriores en catálogos. Editar una Salida reemplaza su copia operativa vigente; no se conserva un historial de versiones de la Salida.
- La salida muestra sus copias, no los datos vivos de Ruta, Cliente o Inventario. Para actualizar la copia de un Cliente o Elemento de inventario se lo quita y se vuelve a agregar. Las bajas lógicas no eliminan datos copiados.
- Una Salida puede confirmarse con cero, uno o más Checkpoints, cero participantes y cero Equipamiento. Los Checkpoints operativos pueden diferir de la Ruta, pero ninguno puede superar la altitud máxima copiada.
- Cambiar la Ruta en la edición reemplaza la copia de Ruta y sus Checkpoints, pero conserva participantes y Equipamiento. Editar un Checkpoint afecta solo a la copia de la Salida.
- “Actualizar desde ruta” reemplaza los campos copiados de Ruta y todos sus Checkpoints con los datos actuales; se advierte que los cambios locales de esos Checkpoints se perderán. No cambia participantes ni Equipamiento.
- Al agregar un Checkpoint a una Salida se ofrece “Guardar también en la ruta” solo si la Ruta está activa y el resultado de la plantilla mantiene cero o al menos dos Checkpoints. Al guardar se incorpora ese Checkpoint a la Ruta junto con la Salida; los demás cambios locales de Checkpoints permanecen solo en la copia operativa.

### Wizard de Salida

1. **Datos de la salida:** seleccionar la fecha, sin hora de salida.
2. **Ruta:** elegir una Ruta activa y completa o crear una nueva. Mostrar datos técnicos e Itinerario; copiar Checkpoints a la Salida y permitir editarlos localmente. Una Ruta borrador no se ofrece ni permite continuar a la planificación de la Salida. Una Ruta nueva creada dentro del wizard debe tener dos o más Checkpoints para confirmar la Salida; una Ruta nueva guardada desde el módulo Rutas puede quedar como borrador con cero Checkpoints.
3. **Equipamiento:** listar Elementos activos, cantidad disponible y permitir una cantidad positiva por elemento. Si se vuelve a agregar el mismo elemento, se suman cantidades. Permitir crear un elemento nuevo; solo persistirá si queda asociado al confirmar.
4. **Participantes:** buscar y seleccionar Clientes activos, sin duplicados. Permitir crear un Cliente nuevo; solo persistirá si queda asociado al confirmar. Si el DNI existe en un Cliente activo, ofrecer usar ese registro. Si existe en un Cliente inactivo, informar que ya está registrado y dirigir a su reactivación desde Clientes; no permitir duplicarlo ni asociarlo hasta que vuelva a estar activo.
5. **Revisión:** mostrar fecha, Ruta y datos copiados, Checkpoints, Equipamiento y Participantes; confirmar la Salida como planificada.

- Al cancelar el wizard no se persiste ningún cambio ni entidad nueva. Una entidad nueva creada y luego quitada antes de confirmar tampoco se persiste.
- El alta y la edición guardan todos sus cambios relacionados en una sola transacción. Cualquier fallo revierte la operación completa.

#### Integración visual del wizard de creación

- Iniciar “Nueva Salida” reemplaza la vista central de Salidas por el wizard dentro de la ventana actual. La barra lateral del shell permanece visible y Salidas sigue siendo la sección activa; no se abre una ventana independiente.
- La vista del wizard tiene el título “Nueva Salida” y un indicador compacto que identifica los cinco pasos, resalta el paso actual y diferencia los pasos completados. Se conserva el orden actual: Datos de la salida, Ruta, Equipamiento, Participantes y Revisión.
- Los campos, validaciones, datos mostrados y reglas de guardado de los cinco pasos se mantienen como están definidos en esta especificación.
- El contenido de cada paso puede desplazarse cuando excede el espacio disponible. Las listas de Equipamiento y Participantes tienen una altura acotada; las acciones Cancelar, Anterior y Siguiente/Confirmar permanecen accesibles al desplazarse.
- Crear un Cliente nuevo o un Elemento de inventario nuevo se realiza en un diálogo auxiliar sobre el wizard. Guardar o cancelar el diálogo regresa al mismo paso; al guardar, el nuevo registro queda asociado al borrador de la Salida y solo se persiste al confirmar la Salida completa.
- Cancelar el wizard o elegir otra sección desde la barra lateral, cuando hay datos ingresados que se perderían, solicita confirmación. Si se elige continuar, se descarta el borrador y sus entidades nuevas no asociadas; si se elige permanecer, el wizard conserva el paso y los datos ingresados. Si no hay datos que descartar, la navegación/cancelación continúa sin confirmación.
- Confirmar con éxito la Salida cierra el wizard, vuelve a la tabla de Salidas y actualiza la lista e indicadores. La cancelación confirmada vuelve a la tabla sin persistir cambios.
- Esta integración afecta solo a la creación de Salidas. La presentación y navegación del flujo de edición quedan fuera de alcance.

### Reserva e Inventario

- Cantidad total es el stock físico del Elemento de inventario. La cantidad reservada es la suma asignada a Salidas planificadas. Disponible = total - reservado. El inventario no se descuenta físicamente al reservar.
- Solo Salidas planificadas reservan stock. Marcar realizada, cancelar o eliminar físicamente una Salida libera su reserva; editar Salidas realizada o cancelada no afecta disponibilidad.
- En la edición de una Salida planificada se excluye su reserva previa al calcular lo disponible para la nueva asignación.
- No se puede asignar cantidad cero, negativa o superior a disponible. El error informa la cantidad disponible. No se permite reducir el stock total por debajo de lo reservado.
- La comprobación de disponibilidad y el guardado de la Salida deben ejecutarse como escritura serializada dentro de la transacción para que instancias concurrentes no puedan exceder el stock.
- Estado del inventario es un dato manual e independiente de reservas. No cambia automáticamente al reservar. Solo la baja lógica y la disponibilidad impiden nuevas asignaciones; el estado manual no las bloquea.
- Categorías cerradas: Campamento, Navegación, Comunicación, Seguridad, Escalada, Vestimenta, Hidratación, Primeros Auxilios, Herramientas y Transporte.
- Estados cerrados: Disponible, Asignado, En Reparación, En Inspección, Fuera de Servicio y Extraviado.
- Nombre, categoría, estado y cantidad total son obligatorios; cantidad total es un entero mayor o igual a cero. Elementos dados de baja no se ofrecen para nuevas asignaciones, pero sus copias permanecen en las Salidas.
- Dar de baja un Elemento con reservas se permite con advertencia; las Salidas planificadas conservan la reserva.

### Clientes

- Se conserva el catálogo y sus campos actuales: DNI, nombre, apellido, fecha de nacimiento, email, teléfono, sexo, contacto de emergencia, autorización de menores, datos del tutor, ficha médica PDF, estado activo y fechas de alta/modificación.
- Se mantienen las validaciones actuales: DNI obligatorio de 7 u 8 dígitos; nombre y apellido obligatorios (hasta 50 caracteres, letras, espacios, guiones o apóstrofes); nacimiento obligatorio y no futuro; email opcional válido de hasta 100 caracteres; teléfono obligatorio con formato actual; sexo obligatorio; contacto de emergencia opcional como grupo, pero si se inicia deben completarse nombre, teléfono y relación; para menores se requiere autorización y datos válidos del tutor.
- El DNI se recorta en los extremos antes de validar y se compara como texto numérico exacto, preservando ceros iniciales. Es único.
- La baja es lógica; se puede reactivar desde edición. Clientes inactivos no se ofrecen en nuevas Salidas. La copia operativa de una Salida conserva al participante aunque su Cliente se edite o inactive después.
- Para actualizar datos copiados de un participante se lo quita y vuelve a agregar. Las copias de participante no incluyen ficha médica ni adjuntos médicos.

### Pantalla principal de Salidas

- Muestra KPIs de Salidas próximas, Rutas activas, Clientes activos e inventario crítico.
- Salidas próximas son las planificadas desde la fecha local de hoy hasta hoy + 7 días, inclusive.
- Inventario crítico cuenta Elementos activos con disponible menor o igual a 2, o cuyo estado sea En Reparación, Fuera de Servicio o Extraviado.
- La tabla muestra fecha, Ruta, estado, participantes, Equipamiento asignado y acciones; ordena por fecha descendente, filtra por estado y busca por Ruta o Cliente.
- Las Salidas planificadas con fecha anterior a hoy se destacan como pendientes de cierre.
- Acciones habituales: ver/editar, generar ficha PDF, marcar realizada y cancelar. El borrado físico es excepcional, reservado a errores de carga y fuera de las acciones habituales.

### Ficha operativa PDF

- Se genera exclusivamente desde la copia operativa guardada de la Salida.
- Incluye título “Ficha operativa de la salida”, identificador, fecha, estado, datos copiados de Ruta, tabla de Checkpoints, tabla de Equipamiento y tabla de Participantes.
- Advierte que los adjuntos médicos no se incorporan ni interpretan.
- Si una sección no tiene filas, muestra respectivamente “Sin checkpoints registrados”, “Sin equipamiento asignado” o “Sin participantes”.
- El nombre del archivo incluye nombre de Ruta, fecha e identificador de Salida para evitar colisiones.

### Persistencia y baja

- Se usa un esquema nuevo creado al iniciar. No se migran datos de la base existente. Antes de ejecutar la nueva versión, se requiere retirar manualmente la base anterior y partir de una base nueva; la aplicación no debe borrar ni reinicializar datos automáticamente.
- Una Ruta tiene una relación uno a muchos con sus Checkpoints; cada Checkpoint conserva una posición explícita. Los Checkpoints de Salida son registros operativos separados, con relación a su Salida y datos propios.
- La Salida conserva referencia a la Ruta de origen y copia sus datos técnicos. Sus relaciones de Equipamiento y Participantes conservan referencia al registro de catálogo más los campos de copia definidos arriba. La relación de Participante es única por Cliente y Salida; una asignación de Equipamiento es única por Elemento y Salida, con cantidad positiva.
- Las cantidades reservadas no se almacenan como stock físico: se calculan desde asignaciones de Salidas planificadas. La edición y el alta de Salida deben validar y escribir esa asignación serializadamente.
- Las altas y ediciones de Salida y las nuevas entidades creadas durante el wizard se confirman en una misma transacción.
- Rutas, Clientes y Elementos de inventario se dan de baja lógicamente. Salidas se cancelan en el flujo normal; la eliminación física se limita a correcciones administrativas por error de carga.

### Validación y mensajes

- Los errores de validación aparecen junto al campo y, cuando corresponda, también como mensaje general.
- Si falla una transacción, se informa el error y no queda persistido ninguno de sus cambios.
- Un intento de asignación superior a disponible informa la cantidad disponible.
- Una Ruta con exactamente un Checkpoint explica que el Itinerario necesita inicio y fin. Si una altitud excede el máximo, el error indica la mayor altitud de los Checkpoints.
- Si el DNI ya existe, se ofrece el Cliente activo existente; si está inactivo, se indica que debe reactivarse antes de asociarlo.
- Al dar de baja un Elemento de inventario con reservas, se advierte que las Salidas planificadas mantienen esas reservas.

## Acceptance Criteria

1. La aplicación inicia en Salidas y presenta únicamente los módulos Salidas, Rutas, Clientes e Inventario.
2. Checkpoints no tiene módulo independiente, Recorridos no se usa en la interfaz y el módulo de stock se llama Inventario.
3. Una Ruta sin Checkpoints puede guardarse como borrador; una Ruta con uno se rechaza y una con dos o más se guarda como completa.
4. Checkpoints de una Ruta se muestran por hora y conservan el orden explícito para horas iguales; no se comparten con otra Ruta.
5. Se rechazan nombres de Ruta inválidos o repetidos tras normalizar, incluso cuando el registro con ese nombre está inactivo.
6. Se rechazan coordenadas vacías o fuera de rango y cualquier altitud de Checkpoint mayor que la altitud máxima de Ruta.
7. Una Ruta borrador no se puede usar para confirmar una Salida. Una Salida con Ruta completa puede tener cero, uno o más Checkpoints operativos propios.
8. El wizard tiene cinco pasos y permite asociar Ruta, Equipamiento y Participantes existentes o crear registros nuevos en el contexto de la Salida.
9. Si se cancela el wizard, o una entidad nueva se quita antes de confirmar, no queda persistida ninguna entidad ni cambio del wizard.
10. Al confirmar, la Salida queda planificada y conserva copia de Ruta, Checkpoints, Equipamiento y Participantes; no requiere participantes ni equipamiento.
11. Editar una Ruta, Cliente o Elemento de inventario no altera copias operativas ya guardadas.
12. “Actualizar desde ruta” reemplaza la copia de Ruta y Checkpoints; cambiar de Ruta conserva Participantes y Equipamiento.
13. “Guardar también en la ruta” persiste el Checkpoint nuevo en la Ruta activa solo al guardar la Salida y únicamente si la plantilla resultante tiene cero o al menos dos Checkpoints.
14. Las Salidas planificadas reservan stock; realizadas y canceladas no. Una transición a realizada o cancelada libera la reserva.
15. La asignación supera disponible o la modificación de stock deja el total por debajo de lo reservado: ambas se rechazan con un mensaje útil.
16. Editar una Salida planificada calcula su nueva asignación excluyendo su reserva anterior; editar una realizada o cancelada no cambia disponible.
17. Dos guardados concurrentes no pueden confirmar asignaciones que superen el stock total; un fallo revierte la operación completa.
18. Dar de baja una Ruta, Cliente o Elemento de inventario no borra ni invalida sus copias en Salidas existentes.
19. Una Ruta inactiva no se ofrece en altas ni para cambiar la Ruta de una Salida, pero una Salida existente puede conservar su referencia y editarse.
20. Clientes inactivos y Elementos de inventario dados de baja no se ofrecen para nuevas asociaciones; un DNI existente no permite crear un duplicado.
21. Las opciones de categoría y estado de Inventario son las listas cerradas especificadas; los estados manuales no modifican las reservas.
22. Los KPIs usan el intervalo inclusivo desde hoy hasta hoy + 7, y el cálculo de inventario crítico solo cuenta Elementos activos.
23. Las Salidas se ordenan por fecha descendente, se pueden filtrar por estado y buscar por Ruta o Cliente; las planificadas pasadas se destacan.
24. Las transiciones de estado son manuales; realizar o cancelar es irreversible en el flujo normal y editar conserva el estado actual.
25. El PDF refleja la copia operativa, incluye los mensajes para secciones vacías, no incluye ni interpreta adjuntos médicos y lleva Ruta, fecha e identificador en el nombre del archivo.
26. La aplicación compila y el flujo principal satisface los escenarios de validación manual definidos en Testing Decisions sobre una base nueva.
27. Al iniciar “Nueva Salida”, el wizard ocupa el contenido central de la ventana actual, mantiene visible la barra lateral con Salidas activa y no crea una ventana de aplicación adicional.
28. El wizard muestra su título, los cinco pasos en orden y el paso actual/completado; Anterior y Siguiente mantienen los datos ya ingresados.
29. En Equipamiento y Participantes, las listas no consumen toda la altura disponible; el paso se puede desplazar y sus acciones de navegación siguen accesibles.
30. Crear un Cliente o Elemento de inventario desde el wizard abre un diálogo auxiliar y, al cerrarlo, el mismo paso y sus datos permanecen disponibles. El registro nuevo solo se persiste al confirmar la Salida completa.
31. Cancelar con cambios ingresados o cambiar de sección desde la barra lateral solicita confirmación. Elegir permanecer conserva el borrador; elegir descartar lo elimina y no persiste cambios parciales. Sin cambios ingresados no se solicita confirmación.
32. Confirmar una Salida exitosa regresa a la tabla actualizada; descartar el wizard regresa a la tabla sin guardar.
33. El listado de Rutas no muestra Checkpoints en un panel permanente; «Ver checkpoints» abre la gestión de la Ruta elegida.
34. La pantalla de gestión muestra el resumen de Ruta y sus Checkpoints, y permite agregar, editar y quitar puntos mediante formularios.
35. Guardar los cambios del Itinerario persiste el conjunto completo de forma atómica; cero Checkpoints se guarda como borrador, dos o más como completa y exactamente uno se rechaza sin abandonar la pantalla.
36. Un Checkpoint solo puede cambiar de posición manualmente entre puntos de su misma hora; cambiar su hora actualiza su posición cronológica y la posición para horas iguales queda guardada.
37. Una Ruta borrador se identifica en el listado como borrador y no se puede seleccionar ni confirmar para planificar una Salida.
38. Si se sale con cambios pendientes, «Guardar», «Descartar» y «Seguir editando» respetan la validación y conservan los cambios cuando corresponde.

## Testing Decisions

- **Seam aprobado:** validación manual de extremo a extremo en la aplicación JavaFX sobre una base SQLite nueva, observando el comportamiento visible y los resultados guardados/PDF. Se cubren navegación, formularios y validación, transacciones, reservas, KPIs y generación de ficha.
- Para la gestión del Itinerario, el seam aprobado es un recorrido de interfaz desde la acción de fila «Ver checkpoints»: abrir la Ruta, agregar/editar/quitar y ordenar Checkpoints, guardar el conjunto, probar los casos borrador/cero/uno/dos o más, salir con cambios pendientes y comprobar que una Ruta borrador no se ofrece para una Salida.
- Un buen escenario verifica resultados externos: qué muestra cada pantalla, qué información conserva la Salida, qué disponibilidad informa el sistema, qué ocurre al cancelar o fallar un guardado y qué contiene el PDF. No debe depender de nombres de clases, consultas o estructura interna concreta.
- El repositorio no tiene infraestructura de pruebas automatizadas ni antecedentes de pruebas automatizadas comparables. La verificación de compilación y la validación manual de JavaFX son las comprobaciones previstas para la implementación; esta spec no afirma cobertura automatizada.
- Escenarios de aceptación manuales mínimos:
  - Arrancar en Salidas y navegar por los cuatro módulos.
  - Guardar una Ruta borrador; rechazar una Ruta con un Checkpoint; guardar y ordenar una Ruta con dos o más, incluidos empates de hora.
  - Desde el listado de Rutas, abrir «Ver checkpoints» y comprobar que no hay panel fijo; verificar el resumen y las acciones de gestión en la pantalla de Itinerario.
  - Agregar, editar y quitar Checkpoints en una edición pendiente; guardar y confirmar que el conjunto completo se persistió, y que un fallo conserva el Itinerario anterior.
  - Reordenar puntos con la misma hora, comprobar el orden ascendente al cambiar una hora y verificar que el desempate elegido persiste.
  - Intentar guardar exactamente un Checkpoint y salir con cambios pendientes usando cada opción del aviso; confirmar que la validación conserva la pantalla y que descartar no cambia el Itinerario guardado.
  - Comprobar que las Rutas borrador aparecen identificadas pero no se ofrecen para planificar una Salida; probar que una Ruta completa sí se ofrece y puede producir una Salida con cero Checkpoints operativos.
  - Validar campos de Checkpoint, límites de coordenadas y altitud máxima tanto en Ruta como en copia de Salida.
  - Crear una Salida con Ruta existente y con Ruta nueva, incluyendo cero Checkpoints, y cancelar un wizard con nuevas entidades para comprobar que no quedan registros.
  - Confirmar una salida que incluya un nuevo Elemento de inventario y un nuevo Cliente; editarla y verificar copia independiente, cambio de Ruta, actualización desde Ruta y guardado opcional de Checkpoint en Ruta.
  - Probar duplicados, Cliente inactivo, Ruta inactiva, Elemento de inventario dado de baja, cantidades inválidas y cantidad por debajo de reservas.
  - Reservar stock, editar la reserva excluyendo la anterior, cancelar y marcar realizada; comprobar disponibilidad y que una operación fallida no deje cambios parciales. Repetir guardados concurrentes desde dos instancias para comprobar que no se sobreasigna stock.
  - Comprobar transiciones manuales y que editar Salidas realizada/cancelada no modifica stock ni su estado.
  - Verificar KPIs, búsqueda, filtros, orden y resaltado de planificadas pasadas, con fechas de borde del intervalo inclusivo.
  - Generar el PDF con y sin Checkpoints, Equipamiento y Participantes; comprobar datos copiados, exclusión de adjuntos médicos y nombre sin colisiones para dos Salidas del mismo día y Ruta.
- Para la integración visual del wizard, el seam de verificación es un recorrido manual de extremo a extremo en JavaFX desde Salidas: abrir el wizard, comprobar shell e indicador, recorrer pasos, abrir/cerrar diálogos auxiliares, probar desplazamiento, cancelar y cambiar de sección con y sin borrador, y confirmar una Salida para comprobar el regreso a la tabla actualizada.
- La validación observa comportamiento visible y datos persistidos, no nombres de clases ni detalles internos. No hay infraestructura ni precedentes de pruebas automatizadas de interfaz en el repositorio; la implementación debe compilar y la interfaz debe validarse manualmente. No se afirma cobertura automatizada.

## Out of Scope

- Migración de datos existentes; se requiere una base nueva.
- Roles, permisos y gestión multiusuario.
- Mapa, GPX o visualización geográfica.
- Historial de mantenimiento de equipamiento.
- Notificaciones automáticas.
- Cambios de estado automáticos por fecha.
- Devoluciones parciales de Equipamiento.
- Reserva de stock por rango de fechas.
- Plantillas de Equipamiento por Ruta.
- Reutilización de Checkpoints entre Rutas.
- Sincronización automática entre Ruta y Salida.
- Edición masiva.
- Historial de versiones de una Salida.
- Rediseño del flujo de edición de una Salida.

## Further Notes

- El proyecto actual es una aplicación Java 17 con JavaFX/FXML y SQLite. Hoy inicia en Inicio y contiene módulos separados para Rutas, Checkpoints, Equipamiento, Clientes y Recorridos; el rediseño reemplaza esa navegación.
- La reserva depende de que el guía cierre manualmente las Salidas. La pantalla resalta planificadas pasadas para ayudar a liberar reservas oportunamente.
- “Copia operativa” designa la versión vigente guardada en la Salida, independiente de sus catálogos de origen; editar la Salida reemplaza esa copia, sin crear historial de revisiones.
- Las decisiones de dominio se registraron en el glosario y los ADR asociados a copias operativas, orden del Itinerario y reservas serializadas.
