# 07: Dashboard

**What to build:** La interfaz web puede mostrar la vista general: indicadores, indicadores de Salidas y próximas Salidas.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] Un endpoint devuelve los indicadores generales (Rutas activas, Clientes activos, Salidas próximas y equipo no disponible).
- [ ] Un endpoint devuelve los indicadores de Salidas.
- [ ] Un endpoint lista las próximas Salidas con nombre de Ruta, fecha ISO-8601 y horarios `HH:mm`.
- [ ] Con la base vacía los indicadores devuelven cero y la lista viene vacía, sin error.
- [ ] Tests `MockMvc` con SQLite en `@TempDir`: base vacía y base con datos sembrados mediante los casos de uso.
