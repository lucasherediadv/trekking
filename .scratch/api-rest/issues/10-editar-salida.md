# 10: Editar Salida

**What to build:** La interfaz web puede corregir una Salida planificada reemplazando su Copia operativa vigente (sin historial de versiones), y consultar qué elementos se pueden asignar mientras la edita.

**Blocked by:** 09

**Status:** ready-for-agent

- [ ] `PUT /api/salidas/{id}` edita una Salida planificada y devuelve la Salida resultante.
- [ ] Un endpoint lista los elementos disponibles para editar esa Salida, considerando la reserva de la propia Salida.
- [ ] Editar una Salida realizada o cancelada responde 409.
- [ ] Reservar más stock del disponible responde 409 con el elemento y la cantidad disponible, sin cambios parciales.
- [ ] Datos inválidos responden 422 con errores por campo; una Salida inexistente responde 404.
- [ ] Tests `MockMvc` con SQLite en `@TempDir`: camino feliz, reemplazo de la Copia operativa, 409 por estado final, 409 por stock, 422 y 404.
