# Especificación funcional: header global y breadcrumbs de navegación

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Lista para implementación  
**Fecha:** 25/10/2026  
**Alcance:** Identidad global del shell y rutas de navegación visibles en las pantallas principales y secundarias de la aplicación.

## Problem Statement

Quienes usan la aplicación pueden perder de vista en qué sección o pantalla están, especialmente después de abrir formularios, fichas y vistas secundarias. La interfaz no dispone de una jerarquía visible y consistente para volver a pantallas anteriores. Además, el contenido actual mezcla márgenes y anchos preferidos, lo que dificulta mantener alineados los elementos al redimensionar la ventana.

## Solution

La aplicación tendrá un header global con la identidad **Trekrisk** y el texto **Gestión operativa de salidas de montaña**, por encima del sidebar y del área principal. La marca y el subtítulo actuales se trasladarán desde el sidebar; este conservará la navegación y el pie.

El área principal mostrará breadcrumbs debajo del header global y encima del título de cada pantalla. Los breadcrumbs indicarán la ubicación estable de la pantalla, permitirán regresar por sus antecesoras y se ocultarán en pantallas sin ruta configurada. Un único registro de navegación definirá las rutas y sus destinos para mantener el componente visual separado de la navegación y evitar configuración duplicada en controladores.

## User Stories

1. Como guía, quiero ver la identidad de Trekrisk en un header global, para reconocer la aplicación desde cualquier sección.
2. Como guía, quiero que el header muestre “Gestión operativa de salidas de montaña”, para entender el propósito de la aplicación.
3. Como guía, quiero que el header se mantenga separado de los breadcrumbs, para distinguir la identidad global de mi ubicación actual.
4. Como guía, quiero conservar el sidebar debajo del header, para seguir accediendo a las secciones principales.
5. Como guía, quiero ver los breadcrumbs debajo del header y antes del título, para saber en qué pantalla estoy antes de leer su contenido.
6. Como guía, quiero que los breadcrumbs, el título y el contenido compartan márgenes, para que la pantalla se perciba ordenada y alineada.
7. Como guía, quiero que el diseño use el espacio disponible al cambiar el tamaño de la ventana, para continuar usando la aplicación sin desbordamientos innecesarios.
8. Como guía, quiero que “Salidas” sea el inicio de la navegación, para volver a la sección inicial sin una sección “Inicio” adicional.
9. Como guía, quiero ver la sección actual y la pantalla actual en los breadcrumbs, para distinguir una sección de su listado o de una vista secundaria.
10. Como guía, quiero poder activar un elemento anterior de los breadcrumbs, para volver directamente a una ubicación previa de la jerarquía.
11. Como guía, quiero que el elemento actual tenga mayor énfasis y no sea accionable, para reconocer claramente la pantalla abierta.
12. Como guía, quiero que los elementos anteriores se vean como enlaces y muestren estados hover y cursor de mano, para reconocer que permiten navegar.
13. Como guía, quiero que los niveles se separen con chevrons consistentes, para leer la jerarquía con rapidez.
14. Como guía, quiero que las pantallas de listado muestren un nivel “Listado”, para distinguirlas de la sección que las contiene.
15. Como guía, quiero ver rutas como “Salidas > Clientes > Listado”, para reconocer el lugar de las pantallas principales.
16. Como guía, quiero ver una ficha médica bajo “Salidas > Clientes > Listado > Ficha médica”, para entender que pertenece al flujo de clientes.
17. Como guía, quiero ver Checkpoints bajo “Salidas > Rutas > Checkpoints”, para mantenerlos asociados a Rutas sin presentarlos como sección principal.
18. Como guía, quiero que los elementos de Inventario dados de baja aparezcan bajo “Salidas > Inventario > Dados de baja”, para reconocer que son una vista de esa sección.
19. Como guía, quiero distinguir alta y edición en el último breadcrumb, para saber qué operación estoy realizando en los formularios compartidos.
20. Como guía, quiero que un elemento anterior abra su destino mediante la navegación existente, para obtener el mismo comportamiento que al navegar desde el sidebar.
21. Como guía, quiero que los breadcrumbs respeten las confirmaciones por cambios sin guardar, para no perder información al volver a una pantalla anterior.
22. Como guía, quiero que las rutas se actualicen al cambiar la pantalla activa, para que los breadcrumbs nunca indiquen una ubicación obsoleta.
23. Como guía, quiero que al cambiar de pantalla no se acumulen barras de breadcrumbs, para conservar una interfaz limpia.
24. Como guía, quiero que las pantallas sin ruta definida oculten el componente sin reservar espacio, para no crear huecos visuales.
25. Como guía, quiero que los accesos antiguos al Dashboard vuelvan a Salidas a través del shell, para mantener un único punto de inicio y navegación.
26. Como guía, quiero que diálogos y ventanas auxiliares queden fuera de los breadcrumbs, para reservarlos a la navegación del contenido principal.

## Implementation Decisions

- El header global pertenece al shell y abarca el ancho de la aplicación; el sidebar comienza debajo de él junto con el contenido principal.
- El header muestra exactamente la identidad **Trekrisk** y el texto **Gestión operativa de salidas de montaña**. No agrega una acción de perfil.
- La marca y el subtítulo existentes se trasladan del sidebar al header. La navegación y el pie del sidebar se conservan.
- El componente de breadcrumbs es reutilizable y se representa dentro del área principal, debajo del header global y sobre la fila de título de cada pantalla.
- Cada elemento del modelo de breadcrumb contiene una etiqueta y una acción de navegación opcional. Los elementos anteriores tienen acción; el último representa la ubicación actual y no navega.
- El registro central de navegación define los destinos y las rutas jerárquicas. La presentación visual no implementa un sistema de navegación separado ni depende del historial de navegación.
- Las acciones de breadcrumb pasan por el mismo mecanismo de navegación que las acciones del sidebar y respetan el guard de cambios sin guardar.
- La sección inicial es Salidas. No se agrega una sección Inicio. Los accesos existentes al Dashboard se redirigen a Salidas mediante el shell.
- Las listas principales se distinguen como “Listado”; alta y edición se muestran con etiquetas específicas. Las jerarquías acordadas incluyen Clientes, ficha médica, Rutas, Checkpoints, Inventario y elementos dados de baja.
- Checkpoints usa una ruta genérica bajo Rutas, sin incorporar dinámicamente el nombre de una Ruta.
- Las pantallas sin ruta configurada, los diálogos y las ventanas auxiliares no muestran breadcrumbs.
- Los márgenes horizontales se armonizan entre breadcrumbs, título y contenido. Los anchos preferidos innecesarios se ajustan para responder al espacio disponible, manteniendo la distribución del shell y el sidebar.
- Se conservan los estilos y la paleta existentes; no se agregan dependencias externas, cambios de persistencia ni cambios de esquema.

## Testing Decisions

- Las comprobaciones deben validar comportamiento observable de la interfaz: ubicación mostrada, destinos de navegación, guardas, visibilidad del componente, alineación y respuesta al redimensionamiento. No deben depender de nombres internos de métodos, campos de controladores o detalles de implementación del FXML.
- Seam de validación acordado: compilación Maven y recorrido manual de extremo a extremo en la aplicación JavaFX existente.
- La validación manual recorre Salidas, Clientes, Rutas, Inventario, Checkpoints, fichas y formularios; confirma que cada antecesora abre su destino, que la ubicación actual no navega, que los guards se respetan, que no se duplican breadcrumbs y que las pantallas sin ruta no dejan espacio vacío.
- También se comprueba el header global, la alineación horizontal de breadcrumbs, título y contenido, y el redimensionamiento de la ventana en tamaños admitidos por la aplicación.
- El proyecto no tiene árbol de pruebas ni framework de pruebas JavaFX configurado. No hay pruebas automatizadas similares para estas interacciones.
- La compilación requerida se realiza con el comando Maven de compilación documentado por el proyecto. La aceptación visual y de navegación requiere ejecutar la aplicación y comprobarla manualmente.

## Out of Scope

- Añadir autenticación, perfil de usuario o acciones de cuenta al header.
- Agregar una sección “Inicio” o cambiar la sección inicial de Salidas.
- Crear un historial de navegación dependiente del recorrido de la sesión.
- Mostrar breadcrumbs en diálogos o ventanas auxiliares.
- Cambiar SQLite, el modelo de persistencia, reglas de negocio o datos de dominio.
- Añadir dependencias externas o un framework nuevo de pruebas automatizadas.
- Rediseñar la identidad visual, el contenido funcional de las pantallas o la estructura de navegación del sidebar más allá de trasladar la marca al header.

## Further Notes

- Se usa el vocabulario del proyecto para **Ruta**, **Salida**, **Cliente**, **Inventario** y **Baja**. Los breadcrumbs describen la ubicación de interfaz y no introducen conceptos nuevos en el dominio de negocio.
- El seam de validación manual de extremo a extremo fue confirmado antes de redactar esta especificación.
- El desglose de trabajo en múltiples tickets y la implementación son etapas separadas que requieren aprobación explícita.
