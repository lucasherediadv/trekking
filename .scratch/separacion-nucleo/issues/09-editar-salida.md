# 09: Editar Salida

**What to build:** Editar una Salida planificada se hace mediante core. Guardar reemplaza la Copia operativa vigente sin mantener versiones anteriores (ADR 0001), revalida la Reserva de stock dentro de la transacción y permite guardar también en la Ruta activa un Checkpoint nuevo; los demás cambios de Checkpoints quedan solo en la Salida (ADR 0002). La navegación, la confirmación al salir con cambios sin guardar y el regreso al listado siguen igual (ADR 0004).

**Blocked by:** 08

**Status:** closed

- [x] La edición de Salida no accede a la persistencia: solo usa casos de uso de core.
- [x] Tests de casos de uso: el reemplazo de la Copia operativa no deja versiones anteriores, aumentar una cantidad por encima del stock es rechazado, un Checkpoint nuevo puede guardarse en la Ruta activa, un cambio local no modifica la Ruta, los Checkpoints de la copia respetan la altitud máxima copiada y una Salida cerrada no es editable por el flujo normal.
- [x] Test de integración en sqlite: reemplazo de la Copia operativa.
- [x] Validación manual: editar datos, Checkpoints, equipamiento y participantes; cancelar con cambios pendientes; guardar y volver al listado actualizado.

## Comments

- Implementado en 6d43209: `CasosDeUsoEditarSalida` en core y `buscarCopia`/`reemplazarCopia` en `RepositorioSalidas` (sqlite). `SalidaDAO` quedó solo con los indicadores del tablero.
- Pendientes menores de la revisión: un Checkpoint solo en `checkpointsNuevosParaRuta` no se agrega a la Salida (el formulario siempre los envía en ambos); `elementosParaEditar` falla si un elemento de la copia ya no existe; quedaron sin uso `EquipamientoDAO.obtenerTodosParaEdicion`/`obtenerDadosDeBaja` y `RutaDAO.obtenerTodasIncluyendoInactivas`/`estaActiva`.
