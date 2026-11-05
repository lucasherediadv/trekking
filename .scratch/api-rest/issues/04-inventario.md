# 04: Inventario

**What to build:** La interfaz web puede consultar y administrar el catálogo de Inventario, y ofrecer solo los elementos que pueden asignarse a una Salida.

**Blocked by:** 01

**Status:** resolved

- [x] Se pueden listar elementos activos, dados de baja y asignables, y obtener el detalle de uno (cantidad total, Estado del elemento y vigencia).
- [x] `POST` crea un elemento (201) y `PUT` lo edita; datos inválidos responden 422 con errores por campo.
- [x] La baja y la reactivación son acciones `POST`; no existe `DELETE`.
- [x] Los elementos en reparación, fuera de servicio o extraviados no figuran como asignables.
- [x] Un elemento inexistente responde 404.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: camino feliz de cada operación, cada filtro, 422 y 404.
