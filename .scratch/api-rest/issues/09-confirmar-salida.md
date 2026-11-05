# 09: Confirmar Salida

**What to build:** La interfaz web puede crear una Salida en una sola operación atómica a partir de lo que reúne el Asistente de Salida: fecha, Ruta, equipamiento, elementos nuevos, Clientes existentes y Clientes nuevos con su ficha médica opcional. La Salida conserva su Copia operativa (ADR 0001) y reserva stock de forma atómica (ADR 0003).

**Blocked by:** 03, 04, 06, 08

**Status:** ready-for-agent

- [x] `POST /api/salidas` recibe fecha, `rutaId`, líneas de equipamiento (elemento y cantidad), elementos nuevos, ids de Clientes existentes y Clientes nuevos; los Checkpoints no se envían, la Salida copia el Itinerario vigente.
- [x] Cada Cliente nuevo puede traer su ficha como nombre de archivo y contenido en base64, que se guarda en la misma transacción.
- [x] Responde 201 con la Salida creada, y su Copia operativa y la ficha de cada Cliente nuevo son consultables después.
- [x] Si falta stock responde 409 con el elemento y la cantidad disponible, y no queda ningún dato a medias (ni Salida, ni elementos nuevos, ni Clientes nuevos).
- [x] Una Ruta borrador o dada de baja se rechaza; datos inválidos responden 422 con errores por campo; una Ruta o Cliente inexistente responde 404.
- [x] Cambios posteriores en la Ruta o los Clientes de origen no alteran la Copia operativa de la Salida creada.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: camino feliz completo, atomicidad ante falta de stock, ficha en base64, Ruta borrador y copia aislada.
