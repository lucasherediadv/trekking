# 12: Cierre de la separación

**What to build:** Se termina la separación: desktop ya no contiene acceso a datos ni lógica de negocio, y la documentación de agentes describe la nueva estructura. Se hace una validación manual completa de la aplicación.

**Blocked by:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11

**Status:** ready-for-human

- [x] Desktop no contiene DAOs, SQL ni acceso directo a la conexión de base.
- [x] Desktop no contiene reglas de negocio: validaciones, transiciones de estado ni cálculos de stock o fechas.
- [x] La documentación de agentes describe los tres módulos, sus dependencias permitidas, los comandos, dónde viven los tests y la estrategia de tests, y usa Salida en lugar de Recorrido.
- [x] La fase de tests de Maven pasa en todos los módulos.
- [ ] Validación manual completa: Dashboard, Clientes con ficha médica, Inventario, Rutas con Itinerario, Asistente de Salida, edición y cierre de Salidas, y exportación de la Ficha operativa.
