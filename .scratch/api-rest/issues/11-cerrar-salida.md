# 11: Cerrar Salida

**What to build:** La interfaz web puede marcar una Salida como realizada o cancelarla; ambos son estados finales y cancelar libera su reserva de stock.

**Blocked by:** 09

**Status:** resolved

- [x] `POST /api/salidas/{id}/realizada` y `POST /api/salidas/{id}/cancelar` cierran una Salida planificada.
- [x] Cerrar una Salida que ya está realizada o cancelada responde 409.
- [x] Cancelar una Salida libera su reserva de stock: la cantidad vuelve a estar disponible para otra Salida.
- [x] Una Salida inexistente responde 404.
- [x] El estado resultante se refleja en el listado y el detalle de Salidas, y una Salida vencida deja de serlo al cerrarse.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: ambos cierres, 409 en estado final, liberación de stock y 404.
