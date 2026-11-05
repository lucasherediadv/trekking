# 08: Editar Equipamiento y Participantes de una Salida

**What to build:** El guía puede cambiar los Equipamientos y Participantes de una Salida existente, conservar o reemplazar las copias correspondientes y confirmar el conjunto de cambios sin estados parciales.

**Blocked by:** 05: Asignar Equipamiento y reservar stock; 06: Asociar Participantes a una Salida; 07: Editar Ruta y Checkpoints de una Salida

**Status:** closed

- [ ] El wizard permite agregar o quitar asignaciones de Equipamiento y Participantes sin duplicados.
- [ ] Para refrescar una copia de Cliente o Elemento de inventario se lo quita y vuelve a agregar; los datos copiados no se actualizan automáticamente.
- [ ] Al editar una Salida planificada se recalculan reservas excluyendo su reserva previa; la operación rechaza cantidades superiores a la nueva disponibilidad.
- [ ] Editar Equipamiento de una Salida realizada o cancelada no cambia la disponibilidad.
- [ ] Elementos de inventario o Clientes inactivos ya copiados se conservan hasta que el guía los quite; no se ofrecen para nuevas asociaciones.
- [ ] Nuevos Elementos de inventario o Clientes agregados y luego quitados antes de confirmar no se persisten.
- [ ] Fecha, Ruta, Checkpoints, Equipamiento y Participantes se guardan en una transacción; si falla una parte, no persiste ningún cambio.
- [ ] El escenario combinado permite crear Ruta, Elemento de inventario y Cliente en el wizard, asociarlos a la Salida y confirmar todo junto; cancelar descarta todo.
