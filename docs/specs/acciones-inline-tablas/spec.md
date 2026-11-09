# Especificación funcional: acciones por fila y edición inline

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Revisión de refinamiento visual
**Fecha:** 25/10/2026
**Alcance:** Acciones contextuales en las tablas de Clientes, Inventario, Rutas y Salidas; edición inline acotada para registros activos de Clientes, Inventario y Rutas.

## Problem Statement

Las acciones de edición y eliminación aparecen fuera de las tablas y dependen de seleccionar una fila. Esto obliga a alternar la atención entre la fila y la barra superior, y hace que cambios pequeños —como corregir el nombre de una Ruta o el total de un Elemento de inventario— abran formularios completos. Las capturas también muestran que las acciones disponibles y el tratamiento de las bajas no son uniformes entre módulos.

## Solution

Cada tabla tendrá una columna **Acciones** con las operaciones de su registro. Las acciones principales serán visibles y las restantes podrán ubicarse en un menú de fila cuando sean numerosas. Crear seguirá siendo una acción general de la pantalla.

Clientes, Inventario y Rutas permitirán editar campos sencillos directamente en celdas de registros activos, conservando los formularios completos para altas y cambios amplios. La edición se confirma con Enter o al salir de la celda; Escape descarta el cambio. Una validación o persistencia fallida conserva el valor almacenado y muestra el error junto a la celda.

Las bajas serán lógicas y confirmadas; los registros inactivos podrán reactivarse, pero no editarse inline. Salidas tendrá sus acciones en la fila según el estado, mientras que su edición seguirá usando el formulario completo debido a sus datos relacionados y su copia operativa reemplazable.

En las cuatro tablas, la columna de acciones no mostrará un título. Los botones de cada fila se agruparán y centrarán horizontalmente dentro de la celda, incluso cuando las acciones varíen entre filas. El ancho de la columna se ajustará para que el grupo quepa sin recortes. En Salidas, la acción de generar la ficha operativa usará un icono de descarga en lugar del icono PDF; conservará su ayuda accesible y el flujo actual de exportación, incluido el selector de destino.

## User Stories

1. Como guía, quiero ver las acciones disponibles en la fila de cada registro, para actuar sobre ese registro sin depender de una selección separada.
2. Como guía, quiero reconocer las operaciones de cada fila por sus botones centrados y sus ayudas accesibles, para encontrar y usar las acciones sin un encabezado textual.
3. Como guía, quiero seguir creando Clientes, Elementos de inventario, Rutas y Salidas desde la acción general de cada pantalla, para iniciar altas sin asociarlas accidentalmente a otra fila.
4. Como administrador, quiero editar un Cliente activo desde su celda, para corregir un dato puntual sin abrir el formulario completo.
5. Como administrador, quiero editar nombre, apellido, email, teléfono, fecha de nacimiento y sexo de un Cliente desde la tabla, para mantener sus datos simples con menos pasos.
6. Como administrador, quiero editar nombre, categoría y cantidad total de un Elemento de inventario desde la tabla, para mantener el catálogo sin abrir el formulario completo.
7. Como administrador, quiero editar nombre, altitud máxima, tipo de terreno y dificultades técnica y física de una Ruta desde la tabla, para corregir datos técnicos puntuales rápidamente.
8. Como administrador, quiero conservar los identificadores de Cliente, Elemento de inventario y Ruta como no editables, para no alterar claves usadas por sus relaciones.
9. Como administrador, quiero que las cantidades reservada y disponible del Inventario sigan siendo de solo lectura, para distinguirlas del stock total editable y evitar inconsistencias con las Salidas.
10. Como administrador, quiero que una edición de cantidad total no pueda dejar el total por debajo de lo reservado, para conservar las asignaciones existentes.
11. Como guía, quiero activar la edición de una celda con una interacción directa de tabla y confirmar con Enter o al salir de ella, para hacer cambios pequeños con un flujo similar a una hoja de cálculo.
12. Como guía, quiero descartar una edición en curso con Escape, para salir sin guardar un cambio accidental.
13. Como guía, quiero ver junto a la celda el error de validación o guardado, para corregir el dato sin perder de vista el registro que estoy editando.
14. Como guía, quiero que un cambio inválido o fallido no sustituya el valor persistido, para no dejar el catálogo en un estado incierto.
15. Como administrador, quiero conservar los formularios completos de alta y edición, para realizar cambios amplios o relacionados en un solo flujo.
16. Como administrador, quiero dar de baja un Cliente, Elemento de inventario o Ruta desde su fila con una confirmación que identifique el registro y explique el efecto, para excluirlo del uso activo de manera deliberada.
17. Como administrador, quiero reactivar un registro dado de baja desde su fila con confirmación, para devolverlo al uso activo cuando corresponda.
18. Como administrador, quiero consultar registros inactivos usando los filtros o vistas existentes, para encontrar la fila que debo reactivar.
19. Como administrador, quiero que una fila inactiva no permita edición inline, para reactivarla antes de hacer cambios rápidos; si necesito corregir otros datos, puedo usar el formulario completo.
20. Como administrador, quiero que el borrado físico no aparezca como acción normal de fila, para evitar perder registros relacionados o copias operativas por error.
21. Como guía, quiero abrir la ficha médica desde la fila del Cliente, para acceder a su información sin cambiar de registro.
22. Como guía, quiero que las acciones de una Ruta se apliquen a esa fila y mantengan su selección, para seguir consultando sus Checkpoints y el equipo mínimo requerido en los paneles asociados.
23. Como guía, quiero que los paneles de Checkpoints y equipo mínimo mantengan sus propias acciones, para que la gestión de elementos relacionados conserve su contexto.
24. Como guía, quiero crear una Salida desde la acción general de la pantalla, para iniciar una planificación sin depender de una fila existente.
25. Como guía, quiero editar una Salida desde su fila mediante el formulario completo, para revisar en conjunto sus relaciones y datos operativos.
26. Como guía, quiero ver en cada Salida solo las acciones válidas para su estado, para evitar ofrecer transiciones que el flujo normal no permite.
27. Como guía, quiero acceder desde la fila de una Salida a editarla, marcarla realizada, cancelarla o generar su ficha PDF cuando corresponda, para completar sus operaciones desde un único contexto.
28. Como guía, quiero que las Salidas realizada y cancelada mantengan su estado final del flujo normal y no ofrezcan acciones de cierre incompatibles, para evitar transiciones inválidas.
29. Como guía, quiero que las acciones principales estén visibles y que las adicionales puedan agruparse en un menú de fila, para conservar la facilidad de descubrimiento sin sobrecargar tablas con muchas acciones.

## Implementation Decisions

- Las tablas afectadas son las de Clientes, Inventario, Rutas y Salidas. La creación permanece como acción general de cada pantalla.
- La columna **Acciones** actúa sobre la entidad de su propia fila. No se requiere seleccionar una fila para editarla, darla de baja, reactivarla o ejecutar otra acción contextual.
- Los campos inline editables son: Cliente (nombre, apellido, email, teléfono, fecha de nacimiento y sexo); Elemento de inventario (nombre, categoría y cantidad total); Ruta (nombre, altitud máxima, tipo de terreno, dificultad técnica y dificultad física).
- DNI e ID no son editables inline. Cantidad reservada, cantidad disponible y estado activo tampoco son celdas editables; las bajas y reactivaciones son acciones explícitas.
- La activación de una celda editable se realiza con doble clic. Para campos de texto o numéricos se usa un editor adecuado; los valores con opciones cerradas usan un selector y la fecha de nacimiento un selector de fecha. Los valores se confirman con Enter o al perder el foco y se descartan con Escape.
- El guardado inline reutiliza las reglas de validación existentes para cada entidad. Una validación fallida o un error de persistencia mantiene el dato anterior y muestra el error cerca de la celda.
- La cantidad total del Inventario debe ser un entero no negativo y no puede quedar por debajo de la cantidad reservada. Cantidad reservada y disponible se recalculan o refrescan tras guardar el total.
- Los formularios completos actuales permanecen disponibles para alta y edición amplia. Los registros dados de baja pueden abrirse en el formulario completo, pero sus celdas no se editan inline hasta reactivarlos.
- Dar de baja o reactivar requiere confirmación, identificando el registro y comunicando que la baja lo excluye del uso activo. La reactivación se ofrece en el contexto que muestra los registros inactivos.
- El borrado físico no forma parte de las acciones normales por fila ni de este cambio de interacción; queda reservado a una operación administrativa excepcional.
- La selección de Ruta continúa alimentando los paneles inferiores de Checkpoints y equipo mínimo. Una acción contextual de Ruta no debe perder ni cambiar esa selección.
- En Salidas no existe edición inline. La acción **Editar** abre el formulario completo. La acción **Marcar realizada** y **Cancelar Salida** solo están habilitadas para Salidas planificadas; la edición completa conserva el comportamiento actual para cualquier estado y la ficha PDF permanece disponible según la capacidad actual.
- Las acciones principales se muestran como botones visibles. Si una fila tiene más acciones de las que caben razonablemente, las adicionales se agrupan en un menú contextual de esa fila.
- Las tablas de Clientes, Inventario, Rutas y Salidas no muestran texto en el encabezado de la columna de acciones. Los botones disponibles en cada fila se centran como grupo dentro de la celda; el ancho de columna basta para mostrarlos completos.
- En Salidas, el botón para generar la ficha operativa usa un icono de descarga y una ayuda accesible que identifica la descarga de la ficha PDF. Mantiene el selector de destino, el nombre sugerido y la exportación existentes.
- No se modifican el esquema de persistencia, las reglas de reserva de stock ni las relaciones de dominio.

## Testing Decisions

- Las pruebas deben comprobar comportamiento observable: acción aplicada al registro correcto, valor persistido, validación visible y estado/listado actualizado. No deben depender de nombres de métodos, campos internos del controlador ni estructura de FXML.
- Seam de prueba acordado: validación manual de extremo a extremo desde la interfaz JavaFX existente y comprobación de persistencia mediante los datos que vuelve a mostrar la aplicación.
- Módulos a validar: tablas de Clientes, Inventario y Rutas; paneles dependientes de Rutas; tabla de Salidas; y persistencia de cambios simples y estados de baja/reactivación.
- Casos de aceptación: iniciar y cancelar una edición inline; confirmar con Enter y al perder foco; probar Escape; corregir valores inválidos; verificar que los identificadores y valores calculados no se editan; evitar cantidad total inferior a la reservada; dar de baja y reactivar con confirmación; verificar que registros inactivos no permiten edición inline; comprobar que la selección de Ruta mantiene sus paneles; y comprobar las acciones visibles por estado de Salida.
- Comprobar que las cuatro tablas no muestran el título «Acciones», que los grupos de botones quedan centrados y no se recortan cuando las filas tienen distintas cantidades de acciones, y que la ficha de Salida se exporta desde el botón con icono de descarga conservando el selector de destino.
- El repositorio no tiene árbol de pruebas ni framework de pruebas configurado, por lo que no hay prior art automatizado para estas pantallas. La validación manual se ejecuta con la aplicación y una base de datos de desarrollo.

## Out of Scope

- Cambiar los formularios completos de alta y edición o sus campos.
- Habilitar edición inline en Salidas, Checkpoints, asignaciones de participantes o equipamiento de una Salida, o en el contenido de los paneles de Ruta.
- Cambiar transiciones de estado, validaciones de negocio, reservas de stock, persistencia o esquema de base de datos.
- Diseñar una nueva herramienta administrativa para borrado físico.
- Añadir un framework de pruebas automatizadas o nuevos seams de prueba.
- Rediseñar filtros, ordenamientos, búsquedas o navegación general de los módulos.

## Further Notes

- El vocabulario sigue el glosario del proyecto: **Ruta**, **Salida**, **Inventario**, **Baja** y **Copia operativa**.
- La frontera entre edición inline simple de catálogos y edición completa de Salidas está registrada en el ADR de acciones por fila y edición inline.
- La spec no desglosa tickets de implementación; ese desglose corresponde a una etapa posterior que requiere aprobación explícita.
