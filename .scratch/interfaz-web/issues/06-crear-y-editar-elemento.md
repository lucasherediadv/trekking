# 06: Crear y editar elemento

**What to build:** El encargado de equipo crea y edita un elemento del Inventario en un formulario con URL propia, con errores por campo y confirmación al salir con cambios.

**Blocked by:** 03, 05

**Status:** resolved

- [x] Hay formularios de alta y edición con URL propia, breadcrumbs, Guardar y Cancelar, que vuelven al listado actualizado.
- [x] Los campos incluyen cantidad total, Estado del elemento y vigencia.
- [x] El 422 se muestra por campo y el 404 con aviso y regreso al listado.
- [x] Salir con cambios sin guardar pide confirmación.
- [x] Tests del seam único: alta feliz, edición feliz, 422 por campo, 404 y confirmación al salir.
