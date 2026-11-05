# 05: Asignar Equipamiento y reservar stock

**What to build:** El guía puede asignar Elementos de inventario a una Salida y saber que la cantidad planificada queda reservada sin descontar físicamente el stock.

**Blocked by:** 03: Gestionar el catálogo de Inventario; 04: Crear Salidas desde el nuevo flujo

**Status:** closed

- [ ] El paso Equipamiento lista solo Elementos activos y muestra su cantidad disponible.
- [ ] Una asignación exige cantidad entera positiva no mayor que disponible; el error muestra cuánto hay disponible.
- [ ] Reagregar el mismo Elemento suma la cantidad en una sola asignación.
- [ ] Se puede crear un Elemento de inventario desde el wizard; solo se persiste si queda asociado a la Salida confirmada.
- [ ] Una Salida planificada reserva la cantidad asignada; cancelar el wizard o confirmar con el Elemento quitado no crea asignaciones ni reserva stock.
- [ ] La disponibilidad se calcula como total menos asignaciones de Salidas planificadas; estados manuales no alteran el cálculo ni bloquean por sí solos una asignación.
- [ ] Actualizar inventario por debajo de las cantidades reservadas se rechaza; dar de baja un elemento reservado conserva la reserva y muestra la advertencia.
- [ ] Validación y guardado de reserva son serializados dentro de la transacción: dos confirmaciones concurrentes no pueden superar el stock total.
- [ ] Un fallo al guardar revierte la Salida, la nueva entidad y su asignación como una sola operación.
