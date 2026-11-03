# 06: Checkpoints e Itinerario en core

**What to build:** Alta, edición, borrado y ordenamiento de los Checkpoints del Itinerario de una Ruta pasan a hacerse mediante casos de uso de core. Checkpoint pasa a ser un objeto plano sin JavaFX; si alguna pantalla necesita propiedades observables, desktop arma su propio modelo de vista. El Itinerario se ordena por hora y, ante horas iguales, conserva la posición explícita (ADR 0002). La altitud de cada Checkpoint respeta la altitud máxima de la Ruta. Se respeta la regla de que la plantilla de Ruta admite cero o al menos dos Checkpoints (ADR 0001).

**Blocked by:** 05

**Status:** ready-for-agent

- [ ] Checkpoint en core no depende de JavaFX.
- [ ] Las pantallas de Checkpoints e Itinerario no acceden a la persistencia: solo usan casos de uso de core.
- [ ] El formulario de Checkpoint valida en vivo con el validador de core.
- [ ] Tests de casos de uso: orden por hora, desempate por posición explícita, altitud mayor a la máxima rechazada, guardar un único Checkpoint rechazado y pasar de borrador a ruta usable.
- [ ] Test de integración en sqlite: guardado del Itinerario con su orden.
- [ ] Validación manual: crear, editar, reordenar y borrar Checkpoints de una Ruta.
