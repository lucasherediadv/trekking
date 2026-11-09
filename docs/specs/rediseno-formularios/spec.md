# Especificación funcional: rediseño de formularios

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Cerrada — implementación completada  
**Fecha:** 26/10/2026  
**Alcance:** Formularios de alta y edición de Ruta, Elemento de Inventario y Cliente.

## Problem Statement

Los formularios actuales de Ruta, Inventario y Cliente usan distribuciones diferentes. Algunos colocan los labels a la izquierda, los anchos de controles varían y el formulario extenso de Cliente no agrupa sus datos. Esto dificulta recorrer las pantallas y encontrar campos relacionados, especialmente en ventanas estrechas.

## Solution

Unificar los formularios de alta y edición bajo una presentación coherente con la identidad visual de TrekRisk. Cada pantalla tendrá el título, la descripción y una tarjeta de formulario alineados sobre el mismo eje. Los campos se agruparán por tema, con labels sobre controles de altura uniforme, ayudas pertinentes y errores de validación junto al campo.

La reorganización conservará todos los campos, opciones, reglas de validación y comportamientos actuales. La navegación, las operaciones de guardado y la cancelación seguirán conduciendo a sus destinos actuales.

## User Stories

1. Como guía, quiero que los formularios de Ruta, Inventario y Cliente compartan una presentación, para reconocer el mismo patrón al cambiar de módulo.
2. Como guía, quiero conservar la sidebar, la navegación y los colores de TrekRisk, para mantener el contexto de la aplicación mientras completo un formulario.
3. Como guía, quiero ver breadcrumbs desde el módulo correspondiente, para entender dónde estoy sin presentar Salidas como raíz de todos los módulos.
4. Como guía, quiero ver el título y la descripción alineados con la tarjeta, para identificar con claridad el propósito del formulario.
5. Como guía, quiero que los campos aparezcan dentro de una tarjeta blanca con borde sutil y esquinas redondeadas, para distinguir el formulario del fondo de la pantalla.
6. Como guía, quiero que los formularios tengan un ancho máximo de 760 px y permanezcan centrados en el área de contenido, para leerlos cómodamente en pantallas amplias.
7. Como guía, quiero ver labels encima de sus controles, para asociar cada campo con su nombre sin recorrer la fila visualmente.
8. Como guía, quiero controles de altura y estilo uniformes, para completar los formularios con una apariencia consistente.
9. Como guía, quiero que los campos relacionados aparezcan en dos columnas en escritorio y en una columna en móvil, para completar los datos sin desplazamiento horizontal.
10. Como guía, quiero que la tarjeta conserve un padding suficiente y que los campos tengan separación clara, para distinguir grupos y evitar una presentación apretada.
11. Como guía, quiero que solo se mantengan ayudas útiles, para recibir instrucciones de formato o reglas relevantes sin indicaciones redundantes.
12. Como guía, quiero identificar los campos obligatorios mediante un asterisco, para saber qué datos se requieren antes de guardar.
13. Como guía, quiero ver errores de validación junto al campo correspondiente al guardar, para localizar y corregir el dato que impide continuar.
14. Como guía, quiero que los errores de campos ya interactuados se actualicen mientras corrijo sus valores, para saber cuándo el dato cumple la regla.
15. Como guía, quiero que los errores generales de guardado sigan mostrándose con alertas, para distinguir problemas que no corresponden a un campo concreto.
16. Como guía, quiero que las opciones de los selectores se conserven, para no alterar los valores válidos que ofrece cada formulario.
17. Como guía, quiero que el formulario de Ruta agrupe la información en una sección y muestre el nombre a ancho completo, para priorizar la identificación de la ruta.
18. Como guía, quiero que altitud máxima y tipo de terreno compartan una fila, para revisar juntos dos atributos principales de la Ruta.
19. Como guía, quiero que dificultad técnica y física compartan una fila, para comparar ambas dimensiones sin recorrer toda la tarjeta.
20. Como guía, quiero saber que el Itinerario y sus Checkpoints se configuran después de crear la Ruta, para entender el siguiente paso del flujo.
21. Como guía, quiero que el formulario de Inventario agrupe los datos del equipamiento y muestre el nombre a ancho completo, para identificar el elemento antes de completar sus propiedades.
22. Como guía, quiero que categoría y cantidad compartan una fila, para completar juntos el tipo de elemento y su disponibilidad total.
23. Como guía, quiero que estado ocupe el ancho completo, para distinguir claramente su valor de los datos de cantidad y categoría.
24. Como guía, quiero que el formulario de Cliente separe los datos personales, el contacto de emergencia y la documentación/autorización, para ubicar cada grupo de información con facilidad.
25. Como guía, quiero ver DNI y fecha de nacimiento en una fila, para completar juntos los identificadores personales requeridos.
26. Como guía, quiero ver nombre y apellido en una fila, para ingresar juntos los componentes del nombre del Cliente.
27. Como guía, quiero ver email y teléfono en una fila y sexo a ancho completo, para conservar una distribución clara de los datos de contacto y personales.
28. Como guía, quiero que la sección de contacto de emergencia indique que es opcional y que, al completar un dato, deben completarse nombre, teléfono y relación, para evitar registros parciales.
29. Como guía, quiero que el nombre del contacto de emergencia ocupe el ancho completo y teléfono y relación compartan una fila, para distinguir el contacto de sus datos asociados.
30. Como guía, quiero conservar la ficha médica y los controles de autorización de menores, para seguir gestionando la documentación y los datos requeridos para Clientes menores.
31. Como guía, quiero que los datos del tutor conserven sus condiciones actuales de visibilidad y validación, para que la reorganización visual no cambie el flujo de autorización.
32. Como administrador, quiero conservar el control de estado activo en la edición de Cliente, para mantener disponible la operación actual desde el mismo formulario.
33. Como guía, quiero conservar las acciones y el destino actual de Cancelar, para salir del formulario sin guardar y regresar al contexto esperado.
34. Como guía, quiero que las altas muestren una acción principal específica para crear la entidad y las ediciones indiquen Guardar cambios, para reconocer el resultado de la acción antes de confirmarla.
35. Como guía, quiero tener Cancelar como acción secundaria y la acción principal en verde, para distinguir la salida del guardado.
36. Como guía, quiero que los controles y acciones se puedan recorrer y activar con teclado, para completar los formularios sin depender del mouse.
37. Como guía, quiero que las seis pantallas de alta y edición funcionen y se adapten a anchos estrechos, para usar los formularios sin perder controles ni provocar desbordamiento horizontal.

## Implementation Decisions

- Se actualizarán las presentaciones de alta y edición de Ruta, Inventario y Cliente, compartiendo reglas de estructura y estilo mediante los mecanismos existentes de JavaFX.
- La tarjeta tendrá ancho máximo de 760 px, padding de 24–32 px, borde sutil y radio de 8 px. El título visual usará 24 px; los labels, 14 px; y los controles, 40 px de alto. La separación entre campos será de 20–24 px.
- El contenido se centrará horizontalmente dentro del área de contenido. Los títulos, descripciones y tarjetas compartirán alineación izquierda.
- Se conservarán sidebar, navegación y colores actuales. Los breadcrumbs serán `Rutas > Nueva/Editar ruta`, `Inventario > Nuevo/Editar elemento` y `Clientes > Nuevo/Editar cliente`; no incluirán Salidas como raíz común.
- Los formularios usarán labels sobre los controles. Los grids tendrán dos columnas en escritorio cuando lo indique la distribución y una sola columna en móvil. No deberá aparecer desbordamiento horizontal.
- Ruta se organizará en una sección de información: nombre a ancho completo; altitud máxima y tipo de terreno en dos columnas; dificultades técnica y física en dos columnas. Al final se mostrará: «Podrás configurar el itinerario y sus checkpoints después de crear la ruta».
- Inventario se organizará en una sección de datos del equipamiento: nombre a ancho completo; categoría y cantidad en dos columnas; estado a ancho completo.
- Cliente conservará los campos de alta y edición actuales. La tarjeta tendrá las secciones **Datos personales**, **Contacto de emergencia** y **Documentación y autorización**. En Datos personales se organizarán DNI con fecha de nacimiento, nombre con apellido y email con teléfono en dos columnas; sexo ocupará el ancho completo.
- Contacto de emergencia indicará que es opcional y que debe completarse en conjunto. El nombre ocupará el ancho completo; teléfono y relación compartirán dos columnas.
- Documentación y autorización conservará ficha médica, autorización de menores y datos del tutor. Se mantienen las reglas actuales de visibilidad y validación del tutor. La edición de Cliente conserva el control de estado activo, además de los campos compartidos con el alta.
- Se mantienen campos, opciones y reglas de negocio existentes. No se modifican modelos, APIs, persistencia ni valores de los selectores.
- Se eliminan instrucciones redundantes como «Seleccione una opción de la lista». Se conservan instrucciones relevantes como el formato del DNI y las reglas para email y contacto de emergencia.
- Los errores de validación aparecen junto al campo correspondiente al intentar guardar y se actualizan mientras se corrigen los campos con los que el usuario ya interactuó. No se muestran errores antes de la interacción. Los errores generales de guardado continúan usando alertas.
- En altas, la acción principal será `Crear ruta`, `Crear elemento` o `Crear cliente`. En ediciones será `Guardar cambios`. `Cancelar` será la acción secundaria. Ambas acciones conservarán el comportamiento y la navegación actuales.
- En escritorio, las acciones se alinean a la derecha y se separan del formulario mediante una línea sutil. En móvil, se apilan y ocupan el ancho disponible para facilitar su activación.
- No se incorporan dependencias ni componentes externos. Los estilos y estructuras compartidos seguirán las convenciones JavaFX existentes.

## Testing Decisions

- Las verificaciones deben cubrir comportamiento observable de pantalla, sin depender de nombres de métodos, estructura interna de controladores ni detalles de implementación.
- Seam acordado: revisión manual desde la interfaz JavaFX, en el nivel de pantalla, de las seis combinaciones de alta y edición de Ruta, Inventario y Cliente. Complementar con compilación Maven.
- Revisar en cada pantalla la distribución visual, recorrido por teclado, guardado y cancelación. Comprobar el comportamiento de los errores de validación y que un error general siga apareciendo como alerta.
- Comprobar para Cliente la ficha médica, la autorización, la visibilidad y validación de los datos del tutor, el estado activo en edición y la regla conjunta del contacto de emergencia.
- Revisar la presentación a un ancho de escritorio y a un ancho estrecho, confirmando una columna en móvil, botones accesibles y ausencia de desbordamiento horizontal.
- No hay árbol de pruebas ni framework de pruebas automatizadas configurado en el proyecto. La compilación no se considera cobertura de comportamiento; la interacción visual requiere validación manual.

## Out of Scope

- Cambiar campos, opciones, reglas de validación, modelos, APIs, persistencia o reglas de negocio.
- Cambiar el flujo funcional de guardado, navegación o cancelación.
- Cambiar la sidebar, la navegación general o la identidad visual de TrekRisk.
- Convertir Cliente en un flujo multipaso.
- Añadir dependencias o infraestructura de pruebas automatizadas.
- Modificar pantallas fuera de los formularios de alta y edición de Ruta, Inventario y Cliente, y los estilos compartidos necesarios para ellos.

## Further Notes

- El vocabulario sigue el glosario del proyecto: **Ruta**, **Inventario**, **Cliente**, **Itinerario** y **Checkpoint**.
- La spec no cambia el modelo de dominio ni requiere actualizar el glosario o registrar una decisión arquitectónica.
- El desglose de implementación es una etapa separada y requiere aprobación explícita después de revisar esta spec.

## Implementación y cierre

- Se actualizaron las seis pantallas de alta y edición de Ruta, Inventario y Cliente, con secciones, distribución adaptable, validación visible junto a los campos, acciones diferenciadas y breadcrumbs por módulo.
- Se aplicaron controles y botones planos con la paleta de TrekRisk.
- Verificación: `mvn -f trekking-app/pom.xml compile` y `git diff --check` completados correctamente. La revisión visual se realizó sobre las pantallas compartidas por el usuario.
- Commits: `e498b0b`, `6195fc4` y `f16e020`.
- Cierre aprobado por el usuario el 2026-10-26.
