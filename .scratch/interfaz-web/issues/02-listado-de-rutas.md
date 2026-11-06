# 02: Listado de Rutas

**What to build:** El guía ve las Rutas en una tabla, con carga, estado vacío y reintento si falla, filtra por estado y por terreno y dificultades, y da de baja o reactiva desde cada fila. Este ticket fija el manejo común de errores que usarán las demás pantallas.

**Blocked by:** 01

**Status:** resolved

- [x] La tabla lista las Rutas activas por defecto; el selector `estado` ofrece activas, bajas y todas.
- [x] Los filtros de terreno, dificultad técnica y dificultad física se aplican en el cliente sobre el listado cargado.
- [x] Una Ruta borrador se distingue visiblemente.
- [x] Baja y reactivación son acciones por fila con confirmación breve (toast) y solo se ofrece la acción válida para el estado.
- [x] Hay indicador de carga, estado vacío y aviso con reintento si la carga falla; una falla de red se distingue de un error de negocio.
- [x] Un único punto interpreta el cuerpo de error uniforme de la API: 404 con aviso, 400 y 500 con mensaje genérico, sin detalles internos.
- [x] Los colores de estado siguen los del escritorio.
- [x] Tests del seam único: camino feliz, filtro `estado`, filtros en el cliente, baja y reactivación, y los errores 404, 500 y de red.
