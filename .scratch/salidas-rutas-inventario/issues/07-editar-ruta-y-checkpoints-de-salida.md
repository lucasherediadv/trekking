# 07: Editar Ruta y Checkpoints de una Salida

**What to build:** El guía puede editar los datos y el itinerario operativo de una Salida sin modificar accidentalmente sus Equipamientos o Participantes ni las copias de otras Salidas.

**Blocked by:** 04: Crear Salidas desde el nuevo flujo; 05: Asignar Equipamiento y reservar stock; 06: Asociar Participantes a una Salida

**Status:** closed

- [x] El wizard de edición abre con la copia operativa guardada y permite modificar fecha, Ruta y Checkpoints.
- [x] Cambiar la Ruta reemplaza la copia de Ruta y sus Checkpoints, conservando Equipamiento y Participantes.
- [x] Una Ruta inactiva ya asociada puede conservarse y editarse; no se puede cambiar a una Ruta inactiva distinta.
- [x] “Actualizar desde ruta” reemplaza los datos copiados de Ruta y todos sus Checkpoints, advierte que se perderán cambios locales y no modifica Equipamiento ni Participantes.
- [x] Editar o quitar un Checkpoint afecta solo a esa Salida; la copia puede quedar con cero, uno o más Checkpoints.
- [x] Cada Checkpoint operativo respeta la altitud máxima copiada de la Ruta.
- [x] Al agregar un Checkpoint se ofrece guardarlo también en la Ruta únicamente si está activa y la cantidad resultante de la plantilla es cero o al menos dos.
- [x] Guardar la opción también modifica la Ruta en la misma transacción que la Salida; las demás modificaciones locales siguen siendo propias de la Salida.
- [x] Editar una Salida conserva su estado y no modifica copias de otras Salidas.
