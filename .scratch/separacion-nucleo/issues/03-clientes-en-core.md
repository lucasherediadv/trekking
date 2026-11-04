# 03: Clientes en core

**What to build:** Alta, edición, baja, reactivación y listado de Clientes pasan a hacerse mediante casos de uso de core, siguiendo el patrón del piloto de Inventario. Las validaciones de Cliente se mueven a core (DNI, fecha de nacimiento, contacto de emergencia, datos de tutor para menores). La condición de menor de edad se calcula con un reloj inyectado, que se introduce en este ticket como dependencia de core. Los formularios de alta y edición validan en vivo con los validadores de core. La ficha médica sigue funcionando como hoy; su migración es el ticket 04.

**Blocked by:** 02

**Status:** closed

- [x] Las pantallas de Clientes no acceden a la persistencia: solo usan casos de uso de core.
- [x] Ninguna regla de validación de Cliente queda en la presentación.
- [x] Core no lee el reloj del sistema directamente: recibe un reloj por constructor; desktop le pasa el reloj del sistema.
- [x] Los formularios muestran errores por campo en vivo y al guardar.
- [x] Tests de casos de uso con reloj fijo: cliente válido, DNI duplicado o inválido, fecha de nacimiento futura, menor de edad sin tutor rechazado, menor con tutor aceptado y cumpleaños de 18 justo en la fecha del reloj.
- [x] Tests de casos de uso: baja y reactivación.
- [x] Test de integración en sqlite: repositorio de clientes, incluido el contacto de emergencia.
- [x] Validación manual de Clientes: alta, edición, baja, reactivación, listado y búsqueda.

## Comments

- Implementado en el commit `feat(clientes): mover Clientes a casos de uso de core`. El reloj se inyecta como `java.time.Clock`; desktop pasa el del sistema.
- La ficha médica sigue en `FichaMedicaDAO` (desktop) y se guarda en un paso aparte del Cliente; el ticket 04 la reemplaza por el almacén de core.
- El alta inline de Cliente en el Asistente de Salida sigue usando `ClienteDAO.insertarEnTransaccion` (ticket 08); ya valida con core.
- Dar de baja o reactivar un Cliente inexistente ahora lanza `EntidadNoEncontradaException`.
- Validación manual aprobada por el usuario el 2026-11-04; issue cerrado.
