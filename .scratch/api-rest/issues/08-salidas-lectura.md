# 08: Salidas de solo lectura

**What to build:** La interfaz web puede listar las Salidas con su resumen operativo, ver cuáles están vencidas y consultar el detalle y la Copia operativa de cada una.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] `GET /api/salidas` lista las Salidas con su resumen operativo y la condición calculada de Salida vencida.
- [ ] `GET /api/salidas/{id}` devuelve el detalle, incluido el estado.
- [ ] Un endpoint devuelve la Copia operativa de una Salida (fecha, Ruta, Checkpoints, equipamiento y participantes).
- [ ] Una Salida inexistente responde 404.
- [ ] Tests `MockMvc` con SQLite en `@TempDir`: lista vacía, 404 y, con datos sembrados directamente en la base de prueba, listado, detalle, Copia operativa y condición de vencida (con el reloj fijado).
