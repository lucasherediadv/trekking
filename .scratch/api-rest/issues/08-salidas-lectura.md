# 08: Salidas de solo lectura

**What to build:** La interfaz web puede listar las Salidas con su resumen operativo, ver cuáles están vencidas y consultar el detalle y la Copia operativa de cada una.

**Blocked by:** 01

**Status:** resolved

- [x] `GET /api/salidas` lista las Salidas con su resumen operativo y la condición calculada de Salida vencida.
- [x] `GET /api/salidas/{id}` devuelve el detalle, incluido el estado.
- [x] Un endpoint devuelve la Copia operativa de una Salida (fecha, Ruta, Checkpoints, equipamiento y participantes).
- [x] Una Salida inexistente responde 404.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: lista vacía, 404 y, con datos sembrados directamente en la base de prueba, listado, detalle, Copia operativa y condición de vencida (con el reloj fijado).
