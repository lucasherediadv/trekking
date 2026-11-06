# 13: Asistente de Salida: núcleo

**What to build:** El guía crea una Salida básica con el Asistente de Salida: cinco pasos con URL propia (Datos, Ruta, Equipamiento, Participantes y Revisión), indicador de pasos clicable con las reglas de navegación del glosario, y confirmación en una única petición. En este ticket los pasos Equipamiento y Participantes existen y se pueden visitar, pero vacíos; Ruta solo ofrece Rutas existentes.

**Blocked by:** 02, 10

**Status:** ready-for-agent

- [ ] Cada paso tiene URL propia; el estado del asistente vive en memoria y una recarga lo reinicia.
- [ ] Datos y Ruta son obligatorios; Equipamiento y Participantes son opcionales y cuentan como completados al visitarlos, estén vacíos o no.
- [ ] Hacia atrás siempre se puede volver, sin validar y conservando lo ingresado; hacia adelante solo si Datos y Ruta están completos; los pasos bloqueados aparecen deshabilitados y sin diálogos de error.
- [ ] Invalidar un dato obligatorio ya completado vuelve a bloquear los pasos posteriores.
- [ ] El paso Ruta ofrece solo Rutas existentes no borrador.
- [ ] Revisión resume los datos con accesos de edición a cada paso y confirma con una sola petición; el éxito lleva al listado con un toast.
- [ ] Un 422 o un 409 al confirmar se muestra sin perder lo ingresado.
- [ ] Con datos ingresados, abandonar el asistente pide confirmación.
- [ ] Tests del seam único: creación feliz, reglas de navegación del indicador, bloqueo de pasos, Ruta borrador no ofrecida, reinicio tras recarga y confirmación al abandonar.
