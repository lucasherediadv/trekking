# Especificación funcional: paleta de colores base

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Cerrada — implementación completada
**Fecha:** 27/10/2026  
**Alcance:** Tema visual global de la interfaz JavaFX de TrekRisk.

## Problem Statement

La interfaz usa una combinación de verdes, azules y neutros que no refleja la paleta definida para TrekRisk. Los colores aparecen tanto en tokens compartidos como en reglas puntuales, por lo que algunas vistas y estados pueden quedar visualmente inconsistentes.

## Solution

Integrar la paleta de colores definida en `styles.css` como base visual global de la interfaz. El verde oscuro-medio reemplaza el azul de la sidebar, el verde claro identifica acciones principales y acentos, el verde oscuro sirve para texto y elementos oscuros, y el casi blanco establece el fondo general. Se conservan superficies blancas y colores funcionales para errores, advertencias y éxitos.

Las pantallas conservarán su estructura. Se ajustarán colores de interacción, bordes y contraste para que la paleta se aplique de forma coherente en navegación, controles y estados de hover, foco y selección.

## User Stories

1. Como guía, quiero que todas las pantallas compartan la misma paleta base, para reconocer una identidad visual consistente al cambiar de sección.
2. Como guía, quiero que la sidebar use el verde oscuro-medio en lugar del azul actual, para que la navegación refleje la identidad visual de TrekRisk con un tono menos vibrante.
3. Como guía, quiero leer títulos y contenido sobre superficies claras con texto oscuro, para distinguir la información con claridad.
4. Como guía, quiero identificar las acciones principales por su color verde claro, para reconocer cómo guardar, crear o confirmar cambios.
5. Como guía, quiero que las tarjetas y formularios conserven superficies blancas sobre el fondo general casi blanco, para percibir su separación visual.
6. Como guía, quiero que los estados hover, foco y selección armonicen con la paleta base, para entender qué control está activo o seleccionado.
7. Como guía, quiero que los campos y controles mantengan bordes y estados de foco distinguibles, para poder ubicarlos y operarlos con claridad.
8. Como guía, quiero que errores, advertencias y éxitos mantengan colores funcionales diferenciados, para reconocer el tipo de resultado sin confundirlo con la identidad visual.
9. Como guía, quiero que las notificaciones y diálogos compartan el tema de las pantallas, para que las respuestas del sistema se sientan integradas.
10. Como guía, quiero recorrer las secciones principales sin encontrar controles con colores azules o verdes previos que contradigan el tema, para mantener una experiencia coherente.
11. Como guía, quiero que los cambios de color no alteren la distribución, la navegación ni el comportamiento de los controles, para conservar los flujos de trabajo conocidos.

## Implementation Decisions

- La paleta base global incluye verde oscuro `#202116`, verde oscuro-medio `#32321f`, verde medio `#454425`, verde claro `#6e6928`, verde claro-blanco `#aca680`, `#cfcab7`, `#e1ded5`, tono claro neutro `#eae9e4` y fondo casi blanco `#f4f4f4`.
- El verde oscuro-medio reemplaza el azul de fondo de la sidebar; el verde medio distingue la sección activa y el verde oscuro se usa para texto y elementos oscuros; el verde claro se usa en acciones principales y acentos; el casi blanco se usa como fondo general.
- Los tonos claros `#aca680`, `#cfcab7`, `#e1ded5` y `#eae9e4` permiten diferenciar superficies, selecciones y estados con variaciones sutiles hacia el blanco, evitando contrastes bruscos. Su asignación concreta puede ajustarse según el componente, manteniendo esa progresión visual.
- Las selecciones en tablas, listas y opciones de filtros y menús desplegables usan `#e1ded5`; texto y marcas permanecen oscuros para conservar legibilidad.
- El hover de elementos no seleccionados en tablas, listas y opciones de filtros y menús desplegables usa `#eae9e4`. Un elemento seleccionado conserva `#e1ded5` mientras está bajo el cursor y al retirarlo; el hover de otros elementos se muestra de forma independiente.
- Las tarjetas y formularios pueden conservar superficies blancas como neutro adicional para separarse del fondo.
- Los colores funcionales para errores, advertencias y éxitos se conservan. La paleta define la identidad visual base, no limita la interfaz a cuatro colores.
- Los tonos neutros de borde, texto secundario y controles pueden ajustarse cuando sea necesario para mantener jerarquía visual y contraste.
- El tema se integra en el estilo compartido y cubre pantallas, navegación, formularios, tablas, diálogos y notificaciones; también se revisan colores puntuales que eviten el estilo común.
- Los estados hover, foco y selección deben integrarse con la paleta y seguir siendo distinguibles.
- En tablas, listas y opciones desplegadas de filtros de estado, selección y hover son estados visuales independientes: selección conserva `#e1ded5`, mientras hover aplica `#eae9e4` solo a elementos no seleccionados. Al quitar el cursor, la selección mantiene su color.
- Se conserva la distribución actual de las pantallas y el comportamiento existente de controles y flujos.
- La especificación cubre la interfaz JavaFX. No requiere cambios en persistencia, lógica de negocio ni datos de dominio.

## Testing Decisions

- La validación debe comprobar resultados observables en la interfaz, no nombres de selectores CSS ni detalles internos de implementación.
- El seam de validación acordado es la interfaz JavaFX existente con comprobación manual de extremo a extremo; el estilo compartido es el punto principal de integración.
- Se revisan las secciones principales y sus formularios, tablas, sidebar, diálogos y notificaciones. En cada una se comprueban fondo, texto, superficies, acciones principales, bordes y estados hover, foco y selección.
- Se comprueba que los mensajes de error, advertencia y éxito sigan siendo distinguibles y que no queden colores previos de marca que contradigan la nueva paleta.
- Se verifica visualmente que la navegación, la distribución y el comportamiento de los controles se mantengan.
- El repositorio no cuenta con pruebas automatizadas de interfaz ni pruebas previas para estilos JavaFX; la verificación prevista es manual.

## Out of Scope

- Rediseñar la distribución, navegación o comportamiento de las pantallas.
- Cambiar reglas de negocio, persistencia, esquema de base de datos o vocabulario del dominio.
- Limitar toda la interfaz exclusivamente a los cuatro colores de la paleta.
- Cambiar la tipografía, iconografía o marca más allá de los ajustes de color necesarios para integrar el tema.
- Rediseñar el contenido o el formato del PDF operativo.

## Further Notes

- Los colores de la paleta están definidos en `trekking-app/src/main/resources/com/example/fxml/styles.css`.
- El glosario del proyecto no requiere cambios: esta especificación no agrega ni modifica conceptos del dominio.
- La revisión visual acordada se realiza manualmente en JavaFX; no hay infraestructura automatizada para pruebas de UI.
