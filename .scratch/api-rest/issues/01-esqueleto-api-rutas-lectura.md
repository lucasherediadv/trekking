# 01: Esqueleto de la API con Rutas de solo lectura

**What to build:** El servicio HTTP de la API arranca con un solo comando Maven y responde consultas de Rutas. Es el tracer bullet que deja listos el módulo, el ensamblado de casos de uso, la configuración, el mapeo central de errores y el contrato OpenAPI, para que los demás tickets solo agreguen recursos. Spec: `docs/specs/api-rest/spec.md`; decisiones en los ADR 0005 y 0006.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Existe el módulo `trekking-api` en el proyecto Maven padre; depende solo de `trekking-core` y `trekking-sqlite`, y nada depende de él.
- [ ] La API arranca con un comando Maven desde la raíz y escucha por defecto en `127.0.0.1:8080`; puerto y bind son configurables.
- [ ] La base se resuelve con la propiedad `trekking.db` o, si falta, el `trekking.db` de la raíz del repositorio, y el esquema se inicializa al arrancar.
- [ ] CORS permite un único origen configurable (`trekking.api.origen-web`); sin valor, no se permite ningún origen cruzado.
- [ ] El tamaño máximo de ficha médica queda como propiedad configurable (10 MB por defecto), lista para usar en el ticket 06.
- [ ] El ensamblado de casos de uso se declara como beans del módulo, equivalente al de desktop.
- [ ] Un manejador central de errores devuelve un cuerpo JSON uniforme (código, mensaje y errores por campo si corresponde): `ValidacionException` → 422, `EntidadNoEncontradaException` → 404, `TransicionInvalidaException`, `SalidaNoEditableException` y `StockInsuficienteException` → 409 (con elemento y disponible), cuerpo mal formado → 400, cualquier otra → 500 con mensaje genérico sin detalles internos.
- [ ] Contrato OpenAPI generado y página de documentación disponibles.
- [ ] `GET /api/rutas` lista con `estado=activas|bajas|todas` (por defecto `activas`) y `GET /api/rutas/{id}` devuelve el detalle, con DTOs propios, nombres en español, ids enteros y fechas ISO-8601.
- [ ] Tests `MockMvc` contra el ensamblado real con SQLite en `@TempDir` (nunca el `trekking.db` del repositorio): camino feliz del listado con sus tres filtros y 404 en una Ruta inexistente.
- [ ] `mvn -f trekking-app/pom.xml test` pasa.
