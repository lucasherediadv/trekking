# 06: Fichas médicas

**What to build:** La interfaz web puede adjuntar, descargar, reemplazar y quitar la ficha médica de un Cliente, con un límite de tamaño configurable.

**Blocked by:** 05

**Status:** ready-for-agent

- [x] `PUT` multipart sobre la ficha médica de un Cliente la adjunta o la reemplaza.
- [x] `GET` descarga el archivo con su nombre original; si el Cliente no tiene ficha responde 404.
- [x] `DELETE` quita la ficha; quitarla cuando no hay ninguna no es un error.
- [x] Un archivo que supera el tamaño máximo configurado (10 MB por defecto) se rechaza con un error claro.
- [x] Un Cliente inexistente responde 404.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: subir, reemplazar, descargar, quitar, 404 y límite de tamaño.
