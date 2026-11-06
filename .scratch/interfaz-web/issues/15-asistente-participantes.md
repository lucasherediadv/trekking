# 15: Asistente de Salida: Participantes

**What to build:** En el paso Participantes, el guía suma Clientes existentes y Clientes nuevos, estos últimos con ficha médica opcional que el navegador lee y envía en base64 junto con la confirmación.

**Blocked by:** 08, 09, 13

**Status:** ready-for-agent

- [ ] Se pueden elegir Clientes activos existentes y crear Clientes nuevos con validación por campo.
- [ ] La ficha médica de un Cliente nuevo es opcional; un archivo de más de 10 MB se rechaza antes de enviar.
- [ ] Revisión incluye los Participantes y la confirmación los envía, con las fichas, en la misma petición atómica.
- [ ] Los datos de Clientes y las fichas solo viven en memoria; no se escriben en el almacenamiento del navegador.
- [ ] Tests del seam único: existente, nuevo sin ficha, nuevo con ficha, rechazo de más de 10 MB sin petición, 422 por campo y ausencia de escritura en almacenamiento.
