# Especificación: carga de datos demo para SQLite

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Borrador para revisión  
**Fecha:** 25/10/2026

## Problem Statement

Para mostrar el funcionamiento de Trekking App hace falta preparar manualmente muchos registros relacionados. Una base vacía no permite demostrar con rapidez las pantallas de Rutas, Clientes, Inventario y Salidas, ni los indicadores, filtros, reservas de stock y fichas operativas.

La carga debe representar lugares identificables de la provincia de Córdoba y ofrecer datos variados sin exponer información personal real. También debe poder repetirse para mantener vigentes las fechas relativas sin duplicar los registros demo ni alterar los datos ajenos.

## Solution

Proveer un comando local que prepare el esquema SQLite si todavía no existe y cargue un conjunto coherente de datos demo. El comando se ejecutará desde la raíz del repositorio, usará por defecto la misma base `trekking.db` que abre la aplicación desde ese directorio, y permitirá indicar otra ubicación SQLite.

El conjunto inicial tendrá aproximadamente 6 Rutas, 15 Clientes, 10 Elementos de inventario y 9 Salidas. Incluirá Itinerarios, Participantes, Equipamiento de salida y copias operativas. Los registros se basarán en lugares reconocibles de Córdoba, con coordenadas aproximadas de demostración, sin prometer precisión de navegación. Los datos personales serán ficticios y no incluirán fichas médicas.

La carga será aditiva y segura de repetir. Identificará los registros que administra para refrescar las fechas y estados de sus propias Salidas demo en cada ejecución. Si encuentra una colisión con un registro que no administra, lo omitirá, reportará el conflicto y continuará con los datos independientes que sí pueda cargar.

## User Stories

1. Como persona que presenta la aplicación, quiero poblar la base con un comando, para preparar una demo rápidamente.
2. Como persona que presenta la aplicación, quiero cargar Rutas, Clientes, Inventario y Salidas relacionados, para recorrer los módulos principales con datos consistentes.
3. Como persona que presenta la aplicación, quiero ver aproximadamente seis Rutas con Itinerarios variados, para mostrar la diferencia entre plantillas de Ruta y Salidas fechadas.
4. Como persona que presenta la aplicación, quiero contar con una Ruta borrador sin Checkpoints y otra Ruta inactiva, para mostrar estados distintos del catálogo.
5. Como persona que presenta la aplicación, quiero ver unos quince Clientes ficticios, para mostrar listados, búsquedas y Participantes sin usar datos personales reales.
6. Como persona que presenta la aplicación, quiero que los Clientes demo no tengan fichas médicas, para evitar incluir documentación sensible o archivos adjuntos.
7. Como persona que presenta la aplicación, quiero ver unos diez Elementos de inventario con cantidades y estados distintos, para mostrar disponibilidad y alertas de inventario crítico.
8. Como persona que presenta la aplicación, quiero que las Salidas planificadas reserven cantidades válidas de inventario, para que lo disponible sea coherente con las asignaciones.
9. Como persona que presenta la aplicación, quiero tener unas nueve Salidas con estados planificada, realizada y cancelada, para mostrar filtros y acciones de cada estado.
10. Como persona que presenta la aplicación, quiero que algunas Salidas planificadas tengan fecha próxima y otra tenga fecha pasada, para mostrar los indicadores de próximas y pendientes de cierre.
11. Como persona que presenta la aplicación, quiero que las Salidas tengan cantidades variadas de Participantes, Checkpoints y Equipamiento de salida, incluidos algunos casos vacíos, para demostrar listas completas y estados vacíos.
12. Como persona que presenta la aplicación, quiero que los datos geográficos aludan a lugares reconocibles de Córdoba, para que la demo tenga un contexto local.
13. Como persona que presenta la aplicación, quiero que las coordenadas se entiendan como aproximadas y de demostración, para no confundir la app con una herramienta de navegación de campo.
14. Como persona que presenta la aplicación, quiero ejecutar el comando más de una vez sin duplicar la demo, para poder preparar la base cuando lo necesite.
15. Como persona que presenta la aplicación, quiero que las fechas de las Salidas demo se actualicen respecto del día de ejecución, para que siempre existan Salidas recientes y próximas.
16. Como persona que presenta la aplicación, quiero que las ejecuciones posteriores solo refresquen registros identificados como propios del conjunto demo, para conservar intactos los datos ajenos.
17. Como persona que presenta la aplicación, quiero que una colisión de nombre o DNI se informe y omita, para no modificar ni borrar un registro que ya existía.
18. Como persona que presenta la aplicación, quiero que el comando siga cargando datos que no dependan de una colisión, para obtener una demo útil incluso en una base parcialmente poblada.
19. Como persona que presenta la aplicación, quiero que el comando cree el esquema requerido sin abrir JavaFX, para preparar una base nueva de manera directa.
20. Como persona que presenta la aplicación, quiero elegir otra ruta de archivo SQLite cuando lo necesite, para aislar una demo de la base habitual.
21. Como persona que presenta la aplicación, quiero recibir un resumen de los registros insertados, actualizados, omitidos y los conflictos, para saber qué resultado produjo la carga.

## Implementation Decisions

- El comando será un script local de Python 3, sin requerir servicios externos. Se documentará su invocación desde la raíz del repositorio y aceptará una ruta SQLite alternativa; por defecto usará `trekking.db` desde la raíz, coincidiendo con el directorio de trabajo indicado para ejecutar la aplicación.
- El script preparará el esquema a través de la inicialización existente de la aplicación mediante una entrada Java de línea de comandos, sin iniciar la ventana JavaFX. La ruta de base seleccionada deberá llegar de forma consistente a esa inicialización y a la conexión SQLite usada para la carga.
- La carga de registros se realizará con SQLite, en transacciones, y respetará las relaciones y restricciones del esquema existente. Se respetarán el orden de Itinerario, las copias operativas de Salida, los estados válidos, las categorías cerradas de Inventario y la disponibilidad reservada por Salidas planificadas.
- Se mantendrá un registro interno de propiedad de los registros demo mediante claves estables. En ejecuciones posteriores solo se actualizarán filas que dicho registro identifique como propias. No se borrarán datos del usuario ni se actualizarán registros ajenos.
- Los datos de demo tendrán nombres claramente identificables y DNI ficticios. Si una clave natural reservada ya pertenece a un registro ajeno, se omitirá ese registro y las relaciones que dependan de él, se informará el conflicto y se continuará con los elementos independientes.
- Las fechas se calcularán con relación al día de ejecución. El conjunto incluirá Salidas planificadas próximas, una planificada pasada, realizadas recientes y anteriores, y canceladas. Se mantendrá variedad de cantidades, incluyendo Salidas sin Participantes o sin Equipamiento.
- La selección de lugares incluirá sitios como Quebrada del Condorito, Villa Alpina y Cerro Champaquí, Los Gigantes y Cerro Uritorco. Los nombres de checkpoints y sus coordenadas serán representativos para demo; no describirán un track ni una navegación validada.
- Los Clientes demo serán ficticios, con datos de contacto plausibles de demostración y sin adjuntos ni ficha médica. El conjunto evitará fichas médicas y cualquier dato personal real.
- Esta funcionalidad no cambia las pantallas ni la lógica de negocio de la aplicación. Su resultado se consume en los módulos existentes.

## Testing Decisions

- Un buen escenario verificará comportamiento externo: salida del comando, contenidos y relaciones guardados en SQLite, resultado visible en los módulos, indicadores y copias operativas. No dependerá de nombres de clases ni de la estructura interna del script.
- El seam automatizado propuesto es ejecutar el comando contra una base SQLite temporal y comprobar la creación del esquema, cantidades y relaciones cargadas, coherencia de reservas, fechas relativas, repetición sin duplicados, actualización de fechas demo, conservación de registros ajenos y reporte de colisiones.
- El seam visual de mayor nivel disponible es una comprobación manual de la aplicación JavaFX sobre la base preparada: Salidas y KPIs, Rutas, Clientes e Inventario, incluyendo estados vacíos y reservas.
- El repositorio no cuenta actualmente con infraestructura automatizada de pruebas. La implementación deberá incorporar la verificación automatizada del comando en el seam acordado; la comprobación JavaFX seguirá siendo manual.

## Out of Scope

- Fichas médicas, PDF médicos u otros datos de salud.
- Coordenadas o tracks validados para navegación real, guía de campo o seguridad de excursiones.
- Cambios en formularios, navegación, reportes o reglas de negocio de la aplicación.
- Borrado, reinicialización o reemplazo de la base existente.
- Servicios externos, descarga de datos en línea o dependencia de una conexión de red para cargar la demo.
- Población del modelo legado `Recorrido`; el conjunto se orienta al flujo actual de Salidas.

## Further Notes

- Los lugares propuestos corresponden a referencias turísticas de la provincia de Córdoba. El Cerro Champaquí y otros sitios serranos pueden requerir condiciones y prestadores específicos para visitas reales; los registros de este conjunto son demostrativos y no deben interpretarse como recomendaciones de excursión.
- El vocabulario de dominio sigue el glosario existente: Ruta, Itinerario, Checkpoint, Salida, Participante, Inventario, Equipamiento de salida y copia operativa.
- No se propone un ADR: la estrategia de carga e identificación es reversible y específica de esta herramienta de demo.
