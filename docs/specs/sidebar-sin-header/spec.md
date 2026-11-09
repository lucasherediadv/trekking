# Especificación: sidebar expandible sin header global

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Status:** closed  
**Fecha:** 26/10/2026  
**Alcance:** Estructura visual de navegación compartida por las pantallas cargadas en el shell.

## Problem Statement

La interfaz reserva una franja horizontal global para la marca y el texto “Gestión operativa de salidas de montaña”. Esa franja reduce el espacio vertical disponible y separa la marca de la navegación lateral. La sidebar ya puede contraerse, pero necesita presentar la identidad y los accesos de forma coherente en sus estados abierto y compacto.

## Solution

Eliminar el header global y trasladar la identidad TrekRisk a la parte superior de la sidebar. La sidebar comienza expandida, con el logo de montaña y el nombre “TrekRisk”, y permite contraerse a una barra de iconos. En el estado contraído se conserva el logo y se oculta el nombre. Se mantiene la paleta actual de la aplicación, así como los títulos y breadcrumbs de las pantallas.

## User Stories

1. Como guía, quiero que la interfaz use el alto disponible sin una franja global de marca, para disponer de más espacio para el contenido.
2. Como guía, quiero reconocer TrekRisk en la parte superior de la sidebar expandida, para identificar la aplicación junto a su navegación.
3. Como guía, quiero ver el logo de montaña junto al nombre TrekRisk, para asociar la marca con la identidad visual de la aplicación.
4. Como guía, quiero que la marca use el recurso visual MOUNTAIN-SNOW existente, para mantener el logo previsto para la interfaz.
5. Como guía, quiero que desaparezca el texto “Gestión operativa de salidas de montaña”, para quitar la descripción de la franja global.
6. Como guía, quiero que la sidebar esté expandida al abrir la aplicación, para ver los nombres de las secciones disponibles.
7. Como guía, quiero contraer la sidebar a una barra de iconos, para ampliar el espacio horizontal del contenido cuando lo necesite.
8. Como guía, quiero que el logo permanezca visible al contraer la sidebar y que el texto TrekRisk se oculte, para conservar una referencia visual compacta.
9. Como guía, quiero que los iconos de navegación sigan identificando Salidas, Rutas, Inventario y Clientes en ambos estados, para mantener accesibles las secciones principales.
10. Como guía, quiero ver el nombre de una sección al pasar sobre su icono con la sidebar contraída, para identificar cada destino.
11. Como guía que navega con tecnologías de asistencia, quiero que los iconos y el control de expansión tengan nombres accesibles, para conocer su función sin depender de la presentación visual.
12. Como guía, quiero encontrar el control para expandir o contraer al pie de la sidebar, para cambiar su estado desde el mismo lugar.
13. Como guía, quiero distinguir visualmente la sección activa en ambos estados de la sidebar, para saber dónde estoy.
14. Como guía, quiero conservar la paleta y el tema actuales, para que el cambio de estructura no altere la identidad visual existente.
15. Como guía, quiero conservar los títulos y breadcrumbs de cada pantalla, para seguir entendiendo mi ubicación dentro de la aplicación.
16. Como guía, quiero que la navegación lateral permanezca disponible en las pantallas principales y secundarias del shell, incluidos formularios y fichas, para moverme entre secciones durante esos flujos.

## Implementation Decisions

- El cambio afecta la estructura visual compartida del shell y no altera la navegación ni el contenido funcional de cada pantalla.
- Se elimina la franja horizontal global que actualmente contiene la marca y el subtítulo. El área principal ocupa el espacio liberado.
- La parte superior de la sidebar expandida presenta el logo MOUNTAIN-SNOW y el nombre exacto “TrekRisk”. No muestra el subtítulo “Gestión operativa de salidas de montaña”.
- La sidebar expandida muestra los nombres de las secciones. La sidebar contraída muestra el logo y los iconos de navegación, sin texto de marca ni nombres de sección visibles.
- Los nombres de sección aparecen como tooltips en el estado contraído. Se conservan nombres accesibles para las secciones y para la acción del control de expandir/contraer.
- La sidebar inicia expandida cada vez que se abre la aplicación. El cambio entre estados se mantiene durante la sesión actual; no se persiste al reiniciar.
- El control para expandir o contraer permanece al pie de la sidebar.
- Se conserva el tema claro y la paleta azul actual, junto con los estilos de sección activa.
- Se conservan los títulos, breadcrumbs, destinos de navegación y guardas de cambios sin guardar existentes.
- La decisión de quitar el header global sustituye las decisiones visuales incompatibles de las especificaciones anteriores `header-marca-compacta` y `breadcrumbs-navegacion`. La jerarquía y el comportamiento de breadcrumbs siguen vigentes; su ubicación deja de estar condicionada por un header global.
- No hay cambios de vocabulario del dominio, modelo, persistencia, esquema SQLite ni dependencias.

## Testing Decisions

- **Seam acordado:** compilación Maven y revisión manual de la aplicación JavaFX con la sidebar abierta y contraída.
- Una buena validación observa la interfaz completa: comprueba que no se muestre la franja global ni el subtítulo, que el logo y TrekRisk aparezcan en el estado expandido, y que el estado contraído conserve el logo y los iconos mientras oculta los textos.
- La revisión comprueba los tooltips y nombres accesibles, el control al pie, el indicador de sección activa y el cambio de ancho disponible para el contenido.
- Se recorren las pantallas principales y secundarias del shell, incluidos formularios y fichas, y se confirma que los títulos y breadcrumbs permanecen visibles y funcionales.
- El repositorio no tiene un framework de pruebas automatizadas de interfaz. La validación visual y de navegación es manual; la compilación detecta errores de integración y carga de recursos.

## Out of Scope

- Cambiar la paleta, el tema o los componentes internos de las pantallas.
- Cambiar el contenido o comportamiento de títulos, breadcrumbs, navegación, guardas o formularios.
- Cambiar iconos de navegación distintos del logo de marca.
- Añadir persistencia para recordar el estado de la sidebar entre ejecuciones.
- Cambiar el modelo de dominio, SQLite, consultas, reglas de negocio o dependencias.
- Implementar este cambio o crear tickets de implementación como parte de esta especificación. Esas etapas requieren aprobación explícita de la especificación.

## Further Notes

- La especificación recoge el diseño confirmado en la conversación: las referencias visuales definen la estructura de la sidebar en sus dos estados; la paleta se mantiene según la aplicación actual.
- La terminología de secciones sigue el glosario: Salidas, Rutas, Inventario y Clientes.

## Comments

- Implementación completada, compilación Maven correcta y revisión visual aceptada por el usuario el 2026-10-26. Cambios registrados en `9d3cde9`, `c4bfd56` y `7c822e9`.
