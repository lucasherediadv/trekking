# 11: Detalle de Salida, Copia operativa y Ficha operativa

**What to build:** El guía abre una Salida y consulta su estado y su Copia operativa vigente, y descarga la Ficha operativa en PDF con una acción explícita.

**Blocked by:** 10

**Status:** resolved

- [x] El detalle tiene URL propia y breadcrumbs, y muestra datos, estado, Ruta, Checkpoints, equipamiento y Participantes de la Copia operativa.
- [x] La Ficha operativa se descarga solo con una acción explícita, con un nombre de archivo reconocible, sin previsualización.
- [x] El detalle no muestra ni descarga adjuntos médicos de los Participantes.
- [x] Un 404 muestra aviso y regresa al listado.
- [x] Tests del seam único: camino feliz, descarga del PDF, 404 y que no hay enlace a fichas médicas.
