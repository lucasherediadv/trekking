# 02: Inventario en core (piloto)

**What to build:** Alta, edición, baja, reactivación y listado de elementos de Inventario pasan a hacerse mediante casos de uso de core, implementados en sqlite. Este ticket fija el patrón que siguen las demás áreas: excepciones de negocio no chequeadas (validación con errores por campo, entidad no encontrada), validador por entidad sin efectos que el formulario usa para validar en vivo, repositorio falso en memoria para tests de casos de uso y test de integración del repositorio en sqlite. El Estado del elemento pasa a ser un enum que indica qué estados impiden asignar el elemento a una Salida (en reparación, fuera de servicio, extraviado). "Asignado" deja de ser elegible y los elementos existentes con ese estado se migran a "Disponible". La Reserva de stock se sigue mostrando como cantidad reservada.

**Blocked by:** 01

**Status:** closed

- [x] La pantalla de Inventario no accede a la persistencia: solo usa casos de uso de core.
- [x] Los casos de uso no exponen colecciones observables, errores de SQL ni booleanos de éxito; los errores llegan como excepciones de negocio.
- [x] El formulario muestra errores por campo mientras se escribe, usando el validador de core, y el caso de uso vuelve a validar al guardar.
- [x] El selector de estado ya no ofrece "Asignado"; al iniciar, los elementos con ese estado quedan en "Disponible".
- [x] La regla de estados no asignables está definida una sola vez en core, y las consultas de sqlite la reciben como parámetro.
- [x] En inspección sigue siendo asignable.
- [x] Tests de casos de uso (con repositorio falso): alta válida, alta inválida con errores por campo, edición, baja y reactivación, y estados asignables y no asignables.
- [x] Test de integración en sqlite: repositorio de inventario con cantidad reservada, y migración de "Asignado" a "Disponible".
- [x] Validación manual de Inventario: alta, edición, baja, reactivación, listado y filtros.

## Comments

- Implementado en 54a5402. Salidas sigue usando lecturas de `EquipamientoDAO` y consultas SQL de desktop que reciben los estados no asignables desde core; los tickets 07 a 11 las mueven a core y sqlite.
- Las categorías pasaron a core y el validador rechaza las que no estén en la lista.
- Pendiente para esos tickets: el texto guardado de cada estado vive en sqlite (`texto()`) y el SQL de desktop usa `etiqueta()`; se unifica al migrar esas consultas.
- Validación manual aprobada por el usuario el 2026-11-04; issue cerrado.
