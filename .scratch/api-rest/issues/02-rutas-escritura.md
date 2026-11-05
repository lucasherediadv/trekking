# 02: Rutas: crear, editar, baja y reactivar

**What to build:** La interfaz web puede registrar una Ruta nueva, corregir sus datos y sacarla del uso con una Baja (y reactivarla), con errores de validación por campo.

**Blocked by:** 01

**Status:** ready-for-agent

- [x] `POST /api/rutas` crea una Ruta y responde 201 con el recurso creado.
- [x] `PUT /api/rutas/{id}` edita una Ruta.
- [x] `POST /api/rutas/{id}/baja` y `POST /api/rutas/{id}/reactivar` cambian la Baja; no existe `DELETE`.
- [x] Datos inválidos responden 422 con la lista de errores por campo.
- [x] Operar sobre una Ruta inexistente responde 404.
- [x] Una Ruta dada de baja aparece en `estado=bajas` y deja de aparecer en `activas`; al reactivarla vuelve.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: camino feliz de cada operación, 422 y 404.
