# 11: Dashboard en core

**What to build:** Los indicadores del Dashboard y las próximas salidas se calculan mediante core. Las próximas salidas usan el reloj inyectado con la misma ventana de días que hoy. El conteo de equipo no disponible usa la regla de estados no asignables del enum de Estado del elemento, igual que la asignación a Salidas.

**Blocked by:** 02, 07

**Status:** ready-for-agent

- [ ] El Dashboard no accede a la persistencia: solo usa casos de uso de core.
- [ ] La lista de estados no asignables no aparece escrita en ninguna consulta.
- [ ] Tests de casos de uso con reloj fijo: próximas salidas dentro y fuera de la ventana, Salidas no planificadas excluidas y conteo de equipo no disponible.
- [ ] Validación manual: el Dashboard muestra los mismos valores que antes del cambio sobre la misma base.
