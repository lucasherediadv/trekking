# 06: Asociar Participantes a una Salida

**What to build:** El guía puede seleccionar Clientes activos o crear un Cliente nuevo dentro del wizard y conservar en la Salida la copia operativa acordada.

**Blocked by:** 04: Crear Salidas desde el nuevo flujo

**Status:** closed

- [ ] El paso Participantes permite buscar y seleccionar Clientes activos desde la Salida.
- [ ] No se puede asociar dos veces el mismo Cliente.
- [ ] Se puede crear un Cliente desde el wizard aplicando los campos y validaciones vigentes del módulo Clientes.
- [ ] La copia del Participante conserva referencia, DNI, nombre, apellido, teléfono y nombre/teléfono/relación del contacto de emergencia.
- [ ] La ficha médica y sus adjuntos no forman parte de la copia del Participante.
- [ ] Si el DNI ya pertenece a un Cliente activo, se ofrece usar el registro existente y no se crea un duplicado.
- [ ] Si el DNI pertenece a un Cliente inactivo, se informa que debe reactivarse desde Clientes y no se permite asociarlo ni duplicarlo.
- [ ] Clientes nuevos solo se persisten al confirmar la Salida si siguen asociados; cancelar o quitarlos antes de confirmar los descarta.
- [ ] Una Salida puede confirmarse con cero Participantes y cualquier fallo revierte los cambios de esa confirmación.
