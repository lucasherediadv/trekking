# 07: Listado de Clientes

**What to build:** El administrador ve los Clientes en una tabla, filtra por estado y da de baja o reactiva desde cada fila.

**Blocked by:** 02

**Status:** resolved

- [x] La tabla lista los Clientes activos por defecto; el selector `estado` ofrece activas, bajas y todas.
- [x] Se indica en cada fila si el Cliente es menor y si tiene ficha médica, sin mostrar su contenido.
- [x] Baja y reactivación son acciones por fila, con solo la acción válida para el estado.
- [x] Hay carga, estado vacío y reintento, con el manejo común de errores del ticket 02.
- [x] La web no guarda datos de Clientes en el almacenamiento del navegador.
- [x] Tests del seam único: camino feliz, filtro `estado`, baja y reactivación, un error de carga y que no se escribe en `localStorage`.
