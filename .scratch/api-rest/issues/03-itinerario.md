# 03: Itinerario de una Ruta

**What to build:** La interfaz web puede ver y guardar el Itinerario completo de una Ruta con el orden explícito de sus Checkpoints; una Ruta borrador deja de serlo al guardar dos o más Checkpoints (ADR 0002).

**Blocked by:** 02

**Status:** resolved

- [x] `GET` del Itinerario de una Ruta devuelve sus Checkpoints en orden, con horas `HH:mm`.
- [x] `PUT` del Itinerario guarda la secuencia completa con el orden explícito y devuelve la Ruta resultante.
- [x] Una Ruta borrador deja de serlo al guardar dos o más Checkpoints, y el detalle de la Ruta lo refleja.
- [x] Un Checkpoint inválido responde 422 con errores por campo.
- [x] Un Itinerario de una Ruta inexistente responde 404.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: camino feliz, reordenamiento, transición de borrador, 422 y 404.
