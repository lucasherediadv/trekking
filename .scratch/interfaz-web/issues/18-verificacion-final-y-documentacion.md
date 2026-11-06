# 18: Verificación final y documentación

**What to build:** La web queda verificada de punta a punta y documentada. Se confirma que cumple con la spec, que no deja datos sensibles en el navegador y que convive con la API y el escritorio sobre la misma base.

**Blocked by:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12, 13, 14, 15, 16, 17

**Status:** ready-for-human

- [x] La compilación y las pruebas de la web pasan, y `mvn -f trekking-app/pom.xml clean package` sigue pasando.
- [x] Se comprueba por revisión del código que la web no usa `localStorage`, `sessionStorage` ni IndexedDB.
- [x] `AGENTS.md` refleja la web: ubicación, comandos y la regla de que no contiene reglas de negocio.
- [ ] Validación manual documentada en un comentario de este ticket: con la API, la web y el escritorio sobre la misma base, una escritura de cada lado se ve del otro, y la descarga de ficha médica y de Ficha operativa funcionan.
- [x] Si falta la validación manual (requiere al usuario), el ticket lo deja anotado como pendiente y no se cierra la spec.
- [ ] Cuando todo está verificado, el `Status` de `docs/specs/interfaz-web/spec.md` pasa a `closed`.

## Comments

2026-11-06 (verificacion automatica):
- `npm test`: 17 archivos, 138 tests OK. `npm run build` (tsc + vite build) OK. `mvn -f trekking-app/pom.xml clean package` OK (jars de api y desktop generados).
- `grep localStorage|sessionStorage|indexedDB` sobre `src/`: solo aparece en tests que verifican que NO se usan (listadoClientes, fichaMedica, participantesAsistente).
- Humo con API sobre DB temporal (`/tmp/trek18/t.db`, `-Dserver.port=18080`) y Vite con `VITE_API_PROXY`: `GET /api/rutas` via el proxy de Vite devuelve 200 `[]`; `POST /api/rutas` via el proxy llega a la API y devuelve su 422 por campo.
- `AGENTS.md` ahora describe la web (ubicacion, fuera de Maven, sin reglas de negocio, sin almacenamiento del navegador).
- PENDIENTE (requiere al usuario): validacion manual con API + web + escritorio sobre la misma base (escritura de cada lado visible del otro; descarga de ficha medica y de Ficha operativa en el navegador). Por eso este ticket no se cierra y la spec `interfaz-web` sigue sin pasar a `closed`.
