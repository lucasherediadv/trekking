# 09: Editar Salida

**What to build:** Editar una Salida planificada se hace mediante core. Guardar reemplaza la Copia operativa vigente sin mantener versiones anteriores (ADR 0001), revalida la Reserva de stock dentro de la transacción y permite guardar también en la Ruta activa un Checkpoint nuevo; los demás cambios de Checkpoints quedan solo en la Salida (ADR 0002). La navegación, la confirmación al salir con cambios sin guardar y el regreso al listado siguen igual (ADR 0004).

**Blocked by:** 08

**Status:** ready-for-agent

- [ ] La edición de Salida no accede a la persistencia: solo usa casos de uso de core.
- [ ] Tests de casos de uso: el reemplazo de la Copia operativa no deja versiones anteriores, aumentar una cantidad por encima del stock es rechazado, un Checkpoint nuevo puede guardarse en la Ruta activa, un cambio local no modifica la Ruta, los Checkpoints de la copia respetan la altitud máxima copiada y una Salida cerrada no es editable por el flujo normal.
- [ ] Test de integración en sqlite: reemplazo de la Copia operativa.
- [ ] Validación manual: editar datos, Checkpoints, equipamiento y participantes; cancelar con cambios pendientes; guardar y volver al listado actualizado.
