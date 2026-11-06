# 18: Verificación final y documentación

**What to build:** La web queda verificada de punta a punta y documentada. Se confirma que cumple con la spec, que no deja datos sensibles en el navegador y que convive con la API y el escritorio sobre la misma base.

**Blocked by:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12, 13, 14, 15, 16, 17

**Status:** ready-for-agent

- [ ] La compilación y las pruebas de la web pasan, y `mvn -f trekking-app/pom.xml clean package` sigue pasando.
- [ ] Se comprueba por revisión del código que la web no usa `localStorage`, `sessionStorage` ni IndexedDB.
- [ ] `AGENTS.md` refleja la web: ubicación, comandos y la regla de que no contiene reglas de negocio.
- [ ] Validación manual documentada en un comentario de este ticket: con la API, la web y el escritorio sobre la misma base, una escritura de cada lado se ve del otro, y la descarga de ficha médica y de Ficha operativa funcionan.
- [ ] Si falta la validación manual (requiere al usuario), el ticket lo deja anotado como pendiente y no se cierra la spec.
- [ ] Cuando todo está verificado, el `Status` de `docs/specs/interfaz-web/spec.md` pasa a `closed`.
