# 12: Realizar y cancelar Salida

**What to build:** El guía marca una Salida como realizada o la cancela desde su fila, con confirmación. Si el estado cambió desde otro cliente, la web lo informa y recarga sin reintentar.

**Blocked by:** 10

**Status:** ready-for-agent

- [ ] Realizar y cancelar son acciones por fila con confirmación; cancelar libera la reserva de stock (efecto visible al consultar de nuevo).
- [ ] Una Salida realizada o cancelada no ofrece nuevas acciones.
- [ ] Un 409 muestra el mensaje de la API, recarga el recurso y no reintenta solo.
- [ ] Un 404 muestra aviso y regresa al listado.
- [ ] Tests del seam único: realizar, cancelar, 409 con recarga sin reintento y 404.
