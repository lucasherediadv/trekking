# Especificación funcional: rediseño de la Ficha operativa

**Status:** ready-for-agent  
**Fecha:** 28/10/2026  
**Alcance:** Formato y presentación del PDF que se exporta desde la tabla de Salidas.

## Problem Statement

La **Ficha operativa** que exporta la app es solo texto plano. Las tablas de checkpoints, equipamiento y participantes se imprimen como líneas con `|` como separador, sin columnas alineadas, sin encabezados destacados y sin separación visual entre filas. No tiene cabecera de marca, ni numeración de páginas, ni identificación en las hojas sueltas. Los datos de la Salida y de la Ruta aparecen como líneas sueltas y los campos vacíos dicen «No especificado» incluso dentro de las tablas. Es un documento pensado para imprimirse y llevarse a la salida, pero no se lee con comodidad ni transmite la identidad de la aplicación (TrekRisk).

## Solution

Rediseñar la Ficha operativa como un documento A4 vertical, cuidado y legible impreso en color o en blanco y negro. Lleva una cabecera con el logo de TrekRisk en vector, dos tarjetas con los datos de la Salida y de la Ruta, tablas reales con encabezado destacado y filas alternas, una sección de observaciones para escribir a mano y un pie de página con numeración e identificación de la Salida. Usa la paleta de la aplicación.

El contenido sigue siendo la **Copia operativa** vigente de la Salida (ADR 0001): la ficha no consulta catálogos vivos ni incorpora adjuntos médicos. No cambian los datos exportados, el flujo del botón ni el guardado del archivo.

## User Stories

1. Como guía, quiero que la ficha tenga una cabecera con el logo de TrekRisk, para reconocer que el documento viene de la aplicación.
2. Como guía, quiero que el logo reproduzca el de la aplicación (ícono de montaña y la palabra «TrekRisk» en negrita), para que la marca sea consistente.
3. Como guía, quiero que la cabecera muestre el título «Ficha operativa» y la fecha de la Salida, para identificar el documento de un vistazo.
4. Como guía, quiero que la cabecera use la franja oscura de la paleta de la aplicación, para que el documento tenga la misma identidad visual.
5. Como guía, quiero ver los datos de la Salida en una tarjeta con pares etiqueta/valor, para encontrar fecha y estado sin leer líneas sueltas.
6. Como guía, quiero ver los datos técnicos de la Ruta en una tarjeta al lado de la de la Salida, para tener el contexto completo en la parte superior de la primera página.
7. Como guía, quiero que el estado de la Salida se muestre como una etiqueta de color según su estado, para distinguir de un vistazo si está planificada, realizada o cancelada.
8. Como guía, quiero que la tarjeta de la Ruta muestre nombre, altitud máxima, tipo de terreno, dificultad técnica y dificultad física, para evaluar la exigencia de la salida.
9. Como guía, quiero que los campos de la Salida o la Ruta sin dato digan «No especificado», para saber que el dato falta y no que se omitió.
10. Como guía, quiero que los checkpoints se muestren en una tabla con columnas de orden, nombre, horario, coordenadas, altitud y descripción, para seguir el itinerario con claridad.
11. Como guía, quiero que la altitud y las coordenadas estén alineadas a la derecha y que las coordenadas usen una fuente más chica, para comparar valores numéricos con facilidad.
12. Como guía, quiero que la descripción de cada checkpoint se muestre completa y la fila crezca en alto, para no perder información operativa.
13. Como guía, quiero que el equipamiento se muestre en una tabla con elemento, categoría y cantidad, para verificar lo que debo llevar.
14. Como guía, quiero que los participantes se muestren en una tabla con DNI, nombre y apellido, teléfono y contacto de emergencia, para tener a mano los datos de cada persona.
15. Como guía, quiero que el contacto de emergencia ocupe una sola columna con el nombre y teléfono en dos líneas y la relación entre paréntesis, para que la tabla entre cómoda en A4 vertical.
16. Como guía, quiero una columna «Presente» con un cuadrito vacío en la tabla de participantes, para pasar lista a mano el día de la salida.
17. Como guía, quiero que el encabezado de cada tabla tenga un fondo oscuro con texto blanco, para distinguirlo de las filas de datos.
18. Como guía, quiero que las filas alternen un fondo claro, para seguir una fila a lo largo de todo su ancho.
19. Como guía, quiero que el encabezado de una tabla se repita en cada página donde continúa, para entender las columnas sin volver a la primera hoja.
20. Como guía, quiero que una fila nunca se corte entre dos páginas, para no leer a medias los datos de un participante o checkpoint.
21. Como guía, quiero que las celdas de tabla sin dato muestren un guion «—», para no llenar la tabla de texto repetido.
22. Como guía, quiero que una sección sin registros muestre una fila gris única («Sin checkpoints registrados», «Sin equipamiento asignado», «Sin participantes»), para saber que está vacía a propósito.
23. Como guía, quiero que cada sección tenga un título con una línea fina inferior, para ubicar rápido cada grupo de información.
24. Como guía, quiero una sección «Observaciones» con renglones vacíos al final, para anotar a mano novedades de la salida.
25. Como guía, quiero que la sección de observaciones tenga al menos 5 renglones, para tener espacio útil aunque la última página esté casi llena.
26. Como guía, quiero una nota discreta sobre los adjuntos médicos, para recordar que esa información no está en la ficha.
27. Como guía, quiero un pie de página con «Página X de Y», para saber si tengo el documento completo.
28. Como guía, quiero que el pie incluya la fecha y hora de generación, para saber qué tan actualizada es la ficha impresa.
29. Como guía, quiero que el pie incluya el nombre de la Ruta y la fecha de la Salida, para identificar una hoja suelta.
30. Como guía, quiero que la ficha se lea bien impresa en blanco y negro, para no depender de una impresora a color.
31. Como guía, quiero que los acentos y caracteres especiales de nombres y descripciones se vean bien, para no leer símbolos rotos.
32. Como guía, quiero que el nombre del archivo propuesto al guardar siga siendo el mismo, para no cambiar mis hábitos de archivo.
33. Como guía, quiero que el archivo se guarde de forma atómica, para no quedarme con un PDF a medias si algo falla.

## Implementation Decisions

- **Servicio de exportación**: se modifica el servicio existente que genera la Ficha operativa. Su interfaz pública (calcular el nombre del archivo y exportar una Salida por identificador a un destino) no cambia, por lo que el controlador de Salidas no se toca.
- **Carga de datos**: los datos siguen leyéndose de la Copia operativa persistida de la Salida. Se conserva el orden actual: checkpoints por posición del Itinerario, equipamiento por nombre, participantes por apellido y nombre. La carga deja de armar cadenas con `|`; entrega valores separados por columna para que el renderizado los ubique en cada celda.
- **Librería**: se mantiene PDFBox con las fuentes Open Sans regular y negrita ya incluidas. No se agregan dependencias ni imágenes.
- **Formato de página**: A4 vertical con márgenes uniformes.
- **Paleta** (la de la aplicación): franja de cabecera `#32321f`; encabezado de tabla `#454425`; acentos y títulos de sección `#6e6928`; bordes y líneas finas `#cfcab7`; filas alternas `#f2f1ec`; texto secundario `#625f50`. Etiquetas de estado con los colores de estado de la aplicación (éxito, advertencia, peligro, información).
- **Logo**: se dibuja como vector con los mismos dos trazos del ícono de montaña de la aplicación, en blanco sobre la franja oscura, a la izquierda de la palabra «TrekRisk» en negrita. A la derecha de la franja van el título «Ficha operativa» y la fecha de la Salida. La cabecera completa aparece solo en la primera página.
- **Orden de secciones**: tarjetas de Salida y Ruta lado a lado; luego Checkpoints, Equipamiento, Participantes y Observaciones; nota sobre adjuntos médicos al final.
- **Tarjetas**: pares etiqueta (gris) y valor (negrita). El estado se muestra como etiqueta de color. Los campos sin dato muestran «No especificado».
- **Tablas**: columnas de ancho fijo calculado sobre el ancho útil de la página; padding de 6 pt en las celdas; texto con salto de línea dentro de la celda y fila de alto variable. Si una fila no entra en el espacio restante, pasa completa a la página siguiente. El encabezado se repite en cada página donde la tabla continúa. El título de una sección no puede quedar solo al final de una página sin al menos una fila de su tabla.
- **Alineación**: altitud y coordenadas alineadas a la derecha; coordenadas con fuente más chica.
- **Participantes**: columnas DNI, nombre y apellido (formato «Apellido, Nombre»), teléfono, contacto de emergencia (nombre y teléfono en dos líneas, relación entre paréntesis) y Presente (cuadrito vacío).
- **Valores vacíos**: «—» en las celdas de tabla; «No especificado» en las tarjetas. Una sección sin registros muestra una única fila gris.
- **Observaciones**: los renglones ocupan el espacio restante de la última página, con un mínimo de 5. Si no entran, se agrega una página.
- **Pie de página**: en todas las páginas, con «Página X de Y», fecha y hora de generación, nombre de la Ruta y fecha de la Salida. La numeración total se resuelve una vez conocida la cantidad de páginas, al terminar el contenido.
- **Información médica**: deja de ser una sección propia y pasa a ser una nota breve, en gris y con fuente más chica, sobre los adjuntos médicos.
- **Sin cambios**: el nombre propuesto del archivo, la exportación atómica con archivo temporal, el flujo del botón en la tabla de Salidas y el mensaje de éxito o error.
- **Vocabulario**: el término canónico es **Ficha operativa** (agregado a `GLOSSARY.md`). No se requiere un ADR: la decisión es fácil de revertir.

## Testing Decisions

- No se agregan tests automáticos. El proyecto no tiene árbol de tests ni framework de pruebas, y se decidió mantener la validación manual. No se debe afirmar cobertura de comportamiento por una fase `test` de Maven exitosa.
- **Chequeo mínimo**: compilar con `mvn -f backend/pom.xml compile`.
- **Validación manual** exportando fichas desde la tabla de Salidas y abriéndolas en un visor de PDF:
  1. Salida con pocos datos (una sola página): cabecera, tarjetas, tablas, observaciones y pie.
  2. Salida con muchos participantes y checkpoints: varias páginas, encabezado de tabla repetido, filas sin cortar y «Página X de Y» correcto en todas.
  3. Salida sin checkpoints, sin equipamiento y sin participantes: fila gris de sección vacía.
  4. Descripción de checkpoint muy larga: la fila crece y no se recorta.
  5. Campos vacíos: «No especificado» en tarjetas y «—» en tablas.
  6. Nombres con acentos, eñes y caracteres especiales.
  7. Un estado de cada tipo de Salida: etiqueta de color correcta.
  8. Impresión en blanco y negro: lectura correcta de encabezados y filas alternas.
  9. Guardar sobre un archivo existente y sobre un destino sin permisos: sin temporales residuales y con mensaje de error.
- **Antecedente**: la validación visual de los rediseños anteriores (formularios, tablas de Salidas) también se hizo manualmente.

## Out of Scope

- Otros PDFs o reportes (clientes, rutas, inventario).
- Incorporar o interpretar adjuntos médicos.
- Cambios en los datos exportados, en la Copia operativa o en el esquema de la base.
- Opciones de personalización de la ficha por parte del usuario (orientación, secciones a incluir, logo alternativo).
- Pruebas automáticas y cambios en `ConexionDB`.
- Cambios en el botón, el selector de archivo o los avisos de la tabla de Salidas.

## Further Notes

- El logo de la imagen de referencia es negro sobre fondo claro; en el PDF va en blanco sobre la franja oscura, igual que en la barra lateral de la aplicación.
- Al ser un documento para llevar a la salida, la prioridad es la legibilidad impresa por encima del detalle decorativo.
