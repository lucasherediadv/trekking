# 11: Dashboard en core

**What to build:** Los indicadores del Dashboard y las próximas salidas se calculan mediante core. Las próximas salidas usan el reloj inyectado con la misma ventana de días que hoy. El conteo de equipo no disponible usa la regla de estados no asignables del enum de Estado del elemento, igual que la asignación a Salidas.

**Blocked by:** 02, 07

**Status:** ready-for-human

- [x] El Dashboard no accede a la persistencia: solo usa casos de uso de core.
- [x] La lista de estados no asignables no aparece escrita en ninguna consulta.
- [x] Tests de casos de uso con reloj fijo: próximas salidas dentro y fuera de la ventana, Salidas no planificadas excluidas y conteo de equipo no disponible.
- [ ] Validación manual: el Dashboard muestra los mismos valores que antes del cambio sobre la misma base.

## Comments

- Implementado en `CasosDeUsoDashboard` (core). El tablero de la pantalla Salidas usa `indicadoresDeSalidas()` (equipo crítico por cantidad disponible, como antes); se eliminaron `DashboardService`, `SalidaDAO` y `EquipamientoDAO`.
- Pendiente: validación manual del usuario (Dashboard y tablero de Salidas con los mismos valores que antes).
