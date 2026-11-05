# 05: Clientes

**What to build:** La interfaz web puede administrar Clientes con validación por campo, saber si un Cliente es menor y validar los datos sin guardarlos mientras se escribe.

**Blocked by:** 01

**Status:** resolved

- [x] `GET` lista Clientes con `estado=activas|bajas|todas` (por defecto activas) y devuelve el detalle de uno, incluida su condición de menor.
- [x] `POST` crea un Cliente (201) y `PUT` lo edita; datos inválidos responden 422 con errores por campo.
- [x] Un endpoint valida los datos de un Cliente sin guardarlos y devuelve la lista de errores por campo.
- [x] La baja y la reactivación son acciones `POST`; no existe `DELETE`.
- [x] Un Cliente inexistente responde 404.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: camino feliz de cada operación, filtros, menor de edad, validación sin guardar, 422 y 404.
