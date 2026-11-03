# 08: Confirmar Salida desde el Asistente

**What to build:** Confirmar una Salida desde el Asistente de Salida se hace mediante un único caso de uso de core, que recibe todos los datos (fecha, Ruta, Checkpoints, equipamiento y participantes) y valida todo. Core define un contrato de transacción: dentro de ella valida la Reserva de stock (incluidos los elementos no asignables por estado) y guarda la Copia operativa. Sqlite implementa la transacción tomando el bloqueo de escritura al comenzar, para que dos instancias no confirmen a la vez más stock del existente (ADR 0003). El Asistente mantiene su estado y navegación en la presentación, sin cambios visibles y sin borradores guardados.

**Blocked by:** 06, 07

**Status:** ready-for-agent

- [ ] El Asistente no accede a la persistencia: confirma mediante el caso de uso de core.
- [ ] La validación de la Reserva de stock vive en core y no en consultas de sqlite.
- [ ] Un stock insuficiente produce un error que identifica el elemento, y desktop lo muestra.
- [ ] No es posible confirmar con una Ruta borrador o dada de baja.
- [ ] Tests de casos de uso: confirmación válida, stock insuficiente, elemento no asignable por estado, Ruta borrador rechazada y Copia operativa aislada de cambios posteriores en los catálogos.
- [ ] Test de integración en sqlite: dos transacciones concurrentes no pueden reservar en conjunto más que el stock total.
- [ ] Validación manual: recorrer el Asistente completo, incluidos volver a pasos completados y omitir los pasos opcionales.
