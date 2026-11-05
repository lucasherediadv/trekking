# 03: Gestionar el catálogo de Inventario

**What to build:** El administrador puede mantener el catálogo de Elementos de inventario, consultar cantidades y estados, y dar de baja elementos sin borrar sus datos.

**Blocked by:** None (can start immediately)

**Status:** closed

- [ ] Se pueden crear, listar, buscar, editar, dar de baja y reactivar Elementos de inventario.
- [ ] El formulario exige nombre, categoría cerrada, cantidad total entera >= 0 y estado cerrado.
- [ ] El listado muestra cantidad total, reservada y disponible; mientras no existan asignaciones, reservada es cero y disponible es igual al total.
- [ ] No se puede reducir la cantidad total por debajo de lo reservado.
- [ ] Los Elementos dados de baja dejan de ofrecerse para nuevas asignaciones y siguen disponibles para mostrar copias operativas existentes.
- [ ] Dar de baja un Elemento con reserva advierte que las Salidas planificadas mantienen esa reserva.
- [ ] El estado manual no se modifica automáticamente por la reserva de una Salida.
