# 02: Gestionar Rutas e Itinerarios

**What to build:** El guía puede administrar Rutas reutilizables y sus Checkpoints desde Rutas, con validaciones que impiden guardar itinerarios incompletos o inconsistentes.

**Blocked by:** None (can start immediately)

**Status:** closed

- [ ] Se puede crear, listar, buscar, editar y dar de baja o reactivar una Ruta.
- [ ] Nombre obligatorio de hasta 100 caracteres, normalizado recortando y colapsando espacios, con letras Unicode/números/espacios y único sin distinguir mayúsculas entre Rutas activas e inactivas.
- [ ] Se puede guardar una Ruta con cero Checkpoints como borrador; se rechaza una Ruta con exactamente uno y se acepta con dos o más.
- [ ] Se pueden agregar, editar y quitar Checkpoints propios de la Ruta; no se pueden compartir ni reasignar a otra Ruta.
- [ ] Los Checkpoints exigen nombre, hora `HH:mm`, latitud y longitud; coordenadas respetan -90..90 y -180..180; altitud en metros es >= 0 y descripción es opcional.
- [ ] Terreno y dificultades solo aceptan las opciones cerradas definidas en la especificación.
- [ ] Altitud máxima de Ruta es >= 0 y >= la altitud de cada Checkpoint.
- [ ] El Itinerario se ordena por hora ascendente y mantiene posición explícita para desempatar; sus horas corresponden a un mismo día operativo.
- [ ] Dar de baja una Ruta conserva sus Checkpoints y no permite reutilizar el nombre mediante una Ruta duplicada.
