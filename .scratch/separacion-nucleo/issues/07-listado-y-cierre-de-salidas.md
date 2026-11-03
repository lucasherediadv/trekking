# 07: Listado y cierre de Salidas

**What to build:** El listado de Salidas, con su resumen de participantes y equipamiento, se obtiene mediante core. El estado de Salida pasa a ser un enum. Marcar una Salida como realizada o cancelada se hace mediante un caso de uso que valida la transición: solo una Salida planificada puede cerrarse, ambos estados son finales (ADR 0001) y el cierre libera su Reserva de stock. Core calcula la Salida vencida con el reloj inyectado, y el listado la sigue resaltando. El filtrado y la búsqueda siguen en la presentación.

**Blocked by:** 02, 03

**Status:** ready-for-agent

- [ ] El listado y el cierre de Salidas no acceden a la persistencia: solo usan casos de uso de core.
- [ ] Intentar cerrar una Salida que no está planificada produce un error de transición inválida, que desktop muestra como alerta.
- [ ] La base sigue guardando los mismos textos de estado.
- [ ] Tests de casos de uso con reloj fijo: planificada a realizada, planificada a cancelada, cerrar una realizada o cancelada rechazado, Reserva de stock liberada al cerrar, Salida vencida con fecha pasada y Salida no vencida en la fecha de hoy.
- [ ] Test de integración en sqlite: listado con resúmenes y cambio de estado.
- [ ] Validación manual: listado, filtros por estado, búsqueda, resaltado de vencidas y cierre de Salidas.
