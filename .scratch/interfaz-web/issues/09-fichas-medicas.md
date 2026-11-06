# 09: Fichas médicas

**What to build:** El administrador adjunta o reemplaza la ficha médica de un Cliente, la descarga con su nombre original y la quita, todo mediante acciones explícitas. Nunca se previsualiza ni se guarda en el navegador.

**Blocked by:** 08

**Status:** ready-for-agent

- [ ] Se puede adjuntar o reemplazar la ficha desde el Cliente; un archivo de más de 10 MB se rechaza en el navegador antes de enviar, sin llamar a la API.
- [ ] La descarga se dispara solo con una acción explícita y conserva el nombre original; no hay vista previa ni incrustación.
- [ ] Quitar la ficha pide confirmación.
- [ ] Si el Cliente no tiene ficha, se muestra el aviso en lugar de intentar descargar (el 404 de la API se trata igual).
- [ ] Los errores de la API (413, 422, 404) se muestran con mensaje claro.
- [ ] No se escribe nada en `localStorage`, `sessionStorage` ni IndexedDB.
- [ ] Tests del seam único: subir, reemplazar, descargar, quitar, rechazo de más de 10 MB sin petición, 404 y ausencia de escritura en almacenamiento.
