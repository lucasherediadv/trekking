# 04: Crear Salidas desde el nuevo flujo

**What to build:** El guía puede iniciar la aplicación en Salidas, navegar los cuatro módulos y confirmar una Salida con fecha y copia operativa de una Ruta, aunque no agregue Equipamiento ni Participantes.

**Blocked by:** 02: Gestionar Rutas e Itinerarios; 03: Gestionar el catálogo de Inventario

**Status:** closed

- [ ] La aplicación abre en Salidas y la navegación muestra Salidas, Rutas, Clientes e Inventario; Checkpoints, Inicio/dashboard y Recorridos dejan de ser módulos de navegación.
- [ ] La pantalla Salidas permite iniciar el wizard y consultar las Salidas confirmadas en una tabla ordenada por fecha descendente.
- [ ] El wizard expone sus cinco pasos y permite avanzar con cero Equipamiento y cero Participantes.
- [ ] Se puede elegir una Ruta activa o crear una Ruta nueva; la Salida conserva los datos técnicos y los Checkpoints como copia operativa.
- [ ] Una Ruta borrador sin Checkpoints permite confirmar la Salida; la copia operativa de una Salida puede tener cero, uno o más Checkpoints.
- [ ] La fecha es obligatoria y acepta fechas pasadas o futuras; la Salida inicia en estado `planificada` y no solicita hora de inicio/fin.
- [ ] Se puede confirmar una Salida sin Checkpoints, Equipamiento ni Participantes; la tabla muestra la fecha, Ruta y estado.
- [ ] La Ruta nueva y la Salida se guardan juntas al confirmar; cancelar el wizard no persiste la Ruta ni ningún cambio del flujo.
- [ ] Si falla el guardado, no queda una Salida o Ruta parcialmente guardada.
- [ ] Iniciar “Nueva Salida” reemplaza el contenido central dentro de la ventana actual; la barra lateral permanece visible con Salidas activa y no se abre una ventana independiente.
- [ ] El wizard muestra el título “Nueva Salida” y un indicador compacto de los cinco pasos, resaltando el paso actual y diferenciando los pasos completados.
- [ ] Equipamiento y Participantes limitan la altura de sus listas; el contenido puede desplazarse y las acciones Cancelar, Anterior y Siguiente/Confirmar permanecen accesibles.
- [ ] Crear un Cliente o un Elemento de inventario nuevo abre un diálogo auxiliar y devuelve al mismo paso. El borrador conserva los datos ya ingresados y el registro nuevo solo se persiste al confirmar la Salida.
- [ ] Cancelar con datos ingresados o cambiar de sección desde la barra lateral solicita confirmación. Permanecer conserva el borrador; descartar no persiste cambios parciales. Sin cambios que perder, no se solicita confirmación.
- [ ] Confirmar con éxito vuelve a la tabla de Salidas actualizada; descartar el wizard vuelve a la tabla sin guardar.
- [ ] Validar manualmente el flujo completo en JavaFX: abrir el wizard, recorrer pasos, abrir/cerrar diálogos auxiliares, comprobar desplazamiento, cancelar y cambiar de sección con y sin borrador, y confirmar una Salida.
