# 10: Generar la ficha operativa PDF

**What to build:** El guía puede generar una ficha operativa desde la copia vigente de una Salida y compartir una representación fiel de la planificación guardada.

**Blocked by:** 07: Editar Ruta y Checkpoints de una Salida; 08: Editar Equipamiento y Participantes de una Salida; 09: Cerrar Salidas y consultar el resumen operativo

**Status:** closed

- [ ] El PDF se genera desde las copias operativas vigentes, no desde los datos vivos de los catálogos.
- [ ] Incluye título, identificador, fecha, estado, datos técnicos de Ruta, Checkpoints, Equipamiento y Participantes.
- [ ] Sin Checkpoints, Equipamiento o Participantes muestra respectivamente “Sin checkpoints registrados”, “Sin equipamiento asignado” o “Sin participantes”.
- [ ] Incluye una nota indicando que adjuntos médicos no se incorporan ni interpretan; no exporta esos adjuntos.
- [ ] El nombre del archivo incluye nombre de Ruta, fecha e identificador de Salida, evitando colisiones en la misma Ruta y fecha.
- [ ] Se puede generar para Salidas planificadas, realizadas y canceladas, reflejando su estado actual.
