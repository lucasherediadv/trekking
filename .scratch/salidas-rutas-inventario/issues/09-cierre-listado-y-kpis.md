# 09: Cerrar Salidas y consultar el resumen operativo

**What to build:** El guía puede cerrar manualmente Salidas planificadas, consultar el estado de las reservas y reconocer rápidamente las salidas que requieren atención.

**Blocked by:** 04: Crear Salidas desde el nuevo flujo; 05: Asignar Equipamiento y reservar stock; 06: Asociar Participantes a una Salida

**Status:** closed

- [ ] Una Salida planificada se puede marcar realizada o cancelada; ambas acciones liberan reservas.
- [ ] Las transiciones son manuales y los estados realizada/cancelada no se revierten en el flujo normal; editar conserva el estado.
- [ ] El listado filtra por estado, busca por Ruta o Cliente y ordena por fecha descendente.
- [ ] Las Salidas planificadas con fecha anterior a hoy se resaltan como pendientes de cierre.
- [ ] El KPI Salidas próximas cuenta planificadas desde hoy local hasta hoy + 7 días, inclusive.
- [ ] Los KPIs Rutas activas y Clientes activos reflejan sus catálogos vigentes.
- [ ] Inventario crítico cuenta Elementos activos con disponible <= 2 o estado En Reparación, Fuera de Servicio o Extraviado.
- [ ] La eliminación física no aparece como acción habitual; cancelar conserva la Salida y libera su reserva.
