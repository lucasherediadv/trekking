# 04: Fichas médicas en un almacén

**What to build:** Adjuntar, reemplazar y consultar la ficha médica de un Cliente se hace mediante un contrato de almacenamiento definido en core. La implementación inicial, en sqlite, la guarda en la base, que pasa a ser la única fuente de verdad. Se elimina la copia de la ficha en una carpeta local, junto con sus avisos de error asociados.

**Blocked by:** 03

**Status:** ready-for-human

- [x] Core no conoce el sistema de archivos para las fichas médicas.
- [x] Al adjuntar o reemplazar una ficha ya no se escribe ninguna copia en una carpeta local.
- [x] Las fichas ya guardadas en la base se siguen pudiendo consultar.
- [x] Tests de casos de uso con almacén falso: adjuntar, reemplazar y consultar.
- [x] Test de integración en sqlite del almacén de fichas.
- [ ] Validación manual: adjuntar una ficha en el alta, reemplazarla en la edición y abrirla.
