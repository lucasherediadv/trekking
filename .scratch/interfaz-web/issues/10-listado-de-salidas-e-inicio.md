# 10: Listado de Salidas e inicio

**What to build:** Salidas pasa a ser la pantalla de inicio. El guía ve los indicadores de Salidas, las próximas Salidas con ruta, fecha y horarios, y la tabla de Salidas con su resumen operativo, la condición de Salida vencida y las acciones válidas para el estado de cada fila.

**Blocked by:** 02

**Status:** resolved

- [x] La raíz de la aplicación redirige a Salidas.
- [x] Se muestran los indicadores de Salidas y la lista de próximas Salidas con ruta, fecha y horarios.
- [x] La tabla muestra el resumen operativo y marca la Salida vencida como condición, no como estado.
- [x] Las filas ofrecen solo las acciones válidas para su estado; las Salidas realizadas o canceladas no ofrecen cambios.
- [x] Hay carga, estado vacío y reintento, con el manejo común de errores del ticket 02.
- [x] Los colores de estado siguen los del escritorio.
- [x] Tests del seam único: camino feliz, redirección de la raíz, Salida vencida, acciones por estado y un error de carga.
