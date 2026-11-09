# Header compacto con marca TrekRisk

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

## Problem Statement

El header global tiene fondo blanco y ocupa más altura de la deseada. No comparte el color de la sidebar y muestra el nombre actual sin el icono de montaña requerido. Esto hace que la identidad de la aplicación se vea menos consistente.

## Solution

Actualizar el header para usar el mismo fondo azul oscuro de la sidebar, reducir su altura y mostrar el icono de montaña a la izquierda del nombre exacto **TrekRisk**. El icono debe mantener el mismo tamaño visual y alineación que los iconos de navegación. Se conserva el subtítulo actual y se ajustan los colores del contenido para que sea legible sobre el fondo oscuro.

## User Stories

1. Como usuario de TrekRisk, quiero que el header use el mismo color que la sidebar, para que la interfaz tenga una identidad visual coherente.
2. Como usuario de TrekRisk, quiero que el header sea más bajo, para disponer de más espacio vertical para el contenido de cada sección.
3. Como usuario de TrekRisk, quiero ver el nombre escrito **TrekRisk**, para reconocer la marca con su capitalización correcta.
4. Como usuario de TrekRisk, quiero ver el icono de montaña antes del nombre, para identificar visualmente la aplicación.
5. Como usuario de TrekRisk, quiero que el icono de montaña tenga el mismo tamaño visual y alineación que los iconos de navegación, para que la marca se integre con la interfaz.
6. Como usuario de TrekRisk, quiero conservar el subtítulo “Gestión operativa de salidas de montaña”, para mantener el contexto del propósito de la aplicación.
7. Como usuario de TrekRisk, quiero que el nombre, el icono y el subtítulo tengan contraste suficiente sobre el nuevo fondo, para poder leerlos con claridad.
8. Como usuario de TrekRisk, quiero que el header siga ocupando todo el ancho de la ventana, para conservar la estructura general de la shell.
9. Como usuario de TrekRisk, quiero que el header se vea correctamente tanto con la sidebar expandida como contraída, para mantener una cabecera estable al navegar.

## Implementation Decisions

- El cambio se limita al header global de la shell JavaFX.
- El color de fondo del header será `#172b4d`, igual al color actual de la sidebar.
- Se reducirá el padding vertical del header manteniendo su ancho completo.
- Se reutilizará el recurso de icono de montaña `LANDSCAPE.svg` disponible en el proyecto.
- El icono del encabezado tendrá un contenedor visual de 20 × 20 px, igual al tamaño empleado por los iconos de navegación, y quedará centrado verticalmente junto al nombre.
- El nombre de marca será `TrekRisk`; se conservará el subtítulo “Gestión operativa de salidas de montaña”.
- El texto y el icono usarán colores claros legibles sobre el fondo oscuro.
- No se requieren cambios de dominio, persistencia, navegación ni esquema de base de datos.

## Testing Decisions

- La aceptación se verificará mediante revisión manual de la aplicación JavaFX, en el punto más alto disponible: la shell completa.
- La revisión comprobará el color coincidente del header y la sidebar, la reducción de altura, el nombre y subtítulo, el contraste y la alineación/tamaño del icono en los estados expandido y contraído de la sidebar.
- No hay pruebas automatizadas de interfaz existentes en el proyecto que sirvan como prior art para validar este cambio visual.
- No se propone añadir una nueva seam automatizada para un ajuste exclusivamente visual.

## Out of Scope

- Cambios a los colores, dimensiones o comportamiento de la sidebar.
- Cambios a las páginas internas, breadcrumbs, navegación o controles.
- Cambios al icono de montaña original o creación de nuevos recursos gráficos.
- Cambios a la ventana de aplicación o a su icono del sistema.

## Further Notes

- El icono existente usa un SVG de 24 px; su representación en el header debe seguir la escala visual de 20 × 20 px usada por los iconos de navegación.
- La revisión visual debe considerar la sidebar expandida y contraída, ya que el header permanece visible en ambos estados.
