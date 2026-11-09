# Especificación funcional: sistema de color para estados en tablas

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Cerrada — implementación completada  
**Fecha:** 27/10/2026  
**Alcance:** Presentación visual de estados y dificultades en las tablas de Salidas, Rutas, Inventario y Clientes.

## Problem Statement

Las tablas muestran estados y dificultades como texto plano, indistinguibles del resto de las columnas. En Clientes, la columna "Activo" muestra el valor técnico `true`. Para encontrar una salida cancelada, un elemento de inventario fuera de servicio o una ruta de dificultad alta, el guía debe leer cada fila.

## Solution

Mostrar los estados y las dificultades como badges (píldoras) con color semántico: fondo suave y texto oscuro del mismo tono. Un único conjunto de estilos compartidos define los tonos, y cada tabla elige el tono según el valor de la celda. Los tonos funcionales (éxito, advertencia, peligro, información, neutro) conviven con la paleta base de TrekRisk, sin reemplazarla.

## User Stories

1. Como guía, quiero ver el estado de cada Salida como un badge de color, para distinguir planificadas, realizadas y canceladas de un vistazo.
2. Como guía, quiero ver el estado de cada elemento de Inventario como un badge de color, para detectar rápido qué elementos están disponibles y cuáles no.
3. Como guía, quiero ver la dificultad técnica y la física de cada Ruta con una escala de color, para comparar la exigencia de las rutas.
4. Como guía, quiero ver "Activo" o "Inactivo" en vez de `true` o `false` en Clientes, con color, para reconocer su estado sin interpretar un valor técnico.
5. Como guía, quiero que el color nunca sea el único indicador, para no depender de él: el texto del estado siempre permanece visible.
6. Como guía, quiero que los mismos estados tengan los mismos colores en todas las tablas, para aprender un único código visual.
7. Como guía, quiero que la selección y el hover de fila sigan funcionando con los badges, para no perder la retroalimentación de la paleta base.
8. Como guía, quiero que filtros, búsqueda y orden se comporten igual que antes, para conservar mis flujos de trabajo.

## Implementation Decisions

- **Presentación:** badge con esquinas redondeadas, fondo suave y texto oscuro del mismo tono. El texto del estado siempre se muestra.
- **Tonos compartidos:** éxito, advertencia, peligro, información y neutro. Se definen una sola vez en el estilo compartido (`styles.css`) como clases reutilizables, usando los tokens funcionales existentes (`-color-success`, `-color-warning`, `-color-danger`) y agregando un token de información acorde a la paleta.
- **Celda reutilizable:** un helper en `util/` crea la celda de tabla con el badge a partir de una función valor → tono, para que las cuatro tablas compartan el mismo código. Sigue el patrón de `RowActionCell` y `TableEmptyState`.
- **Mapeo de Salidas (columna Estado):**
  - `planificada` → información
  - `realizada` → éxito
  - `cancelada` → peligro
- **Mapeo de Inventario (columna Estado):**
  - `Disponible` → éxito
  - `Asignado` → información
  - `En Inspección` y `En Reparación` → advertencia
  - `Fuera de Servicio` y `Extraviado` → peligro
- **Mapeo de Rutas (columnas Dificultad técnica y Dificultad física, solo en la tabla de Rutas):**
  - `Baja` → éxito
  - `Media` → advertencia
  - `Alta` → peligro
  - La columna Itinerario (Borrador/Completa) queda sin cambios.
- **Mapeo de Clientes (columna Activo):**
  - Se muestra "Activo" (éxito) o "Inactivo" (neutro) en lugar de `true` o `false`.
  - "Activo" se mantiene como nombre de columna.
- Valores desconocidos o vacíos se muestran como texto sin badge, sin romper la tabla.
- Se mantiene el resaltado existente de filas de Salidas planificadas con fecha pasada. Es un indicador de fila independiente del badge de estado.
- No hay cambios en persistencia, esquema, DAOs ni valores almacenados. Es un cambio de presentación.
- Filtros, búsqueda y orden siguen operando sobre los valores originales, no sobre el texto del badge.
- Vocabulario: "Baja" como nivel de dificultad no es la baja lógica del glosario. El badge de dificultad usa la escala éxito/advertencia/peligro, no el tono neutro de una baja lógica, para no confundirlos.

## Testing Decisions

- La validación comprueba resultados observables en la interfaz JavaFX; no se prueban nombres de clases CSS.
- Verificación manual en las cuatro tablas, con los datos demo, de que cada valor muestra el tono y el texto esperados.
- Se comprueba que el texto del badge permanezca legible sobre fila normal, hover y seleccionada.
- Se comprueba que los valores desconocidos o vacíos se muestren sin badge y sin errores.
- Se verifica que filtros, búsqueda, orden y acciones por fila funcionan igual que antes.
- Comando mínimo de código: `mvn -f trekking-app/pom.xml compile`. El proyecto no tiene pruebas automatizadas de UI, por lo que la ejecución de `test` no cuenta como cobertura de comportamiento.

## Out of Scope

- Colorear estados en otras vistas: dashboard, wizard de Salidas, listas de checkpoints y el PDF operativo.
- Colorear el estado del itinerario (Borrador/Completa) o el filtro desplegable de estado.
- Cambiar la paleta base, la distribución de las pantallas o el vocabulario del dominio.
- Cambiar el esquema, los valores almacenados o las reglas de negocio.
- Cambiar los textos de las demás columnas.

## Further Notes

- Los colores de la paleta base están definidos en `trekking-app/src/main/resources/com/example/fxml/styles.css`; su integración está en `docs/specs/paleta-colores-base/spec.md`.
- El glosario no requiere cambios: la spec no agrega ni modifica conceptos del dominio.
- Ningún ADR existente entra en conflicto.
