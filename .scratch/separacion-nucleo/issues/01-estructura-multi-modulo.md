# 01: Estructura multi-módulo

**What to build:** El proyecto pasa a tener tres módulos Maven bajo un POM padre ubicado donde está hoy el proyecto: trekking-core, trekking-sqlite y trekking-desktop. Todo el código actual se mueve a desktop, salvo la conexión a la base y la inicialización del esquema, que pasan a sqlite y reciben la ubicación del archivo por constructor. Desktop resuelve esa ubicación con la lógica actual (propiedad de sistema y, si no está, búsqueda del proyecto hacia arriba). También se elimina el código heredado de Recorrido y se dejan de crear sus tablas, sin borrarlas de bases existentes. El usuario no nota ningún cambio: la app se ejecuta igual y los comandos documentados siguen funcionando desde la raíz del repositorio.

**Blocked by:** None (can start immediately)

**Status:** closed

- [x] Los comandos de compilación, test y empaquetado documentados funcionan desde la raíz del repositorio.
- [x] La app de escritorio se ejecuta apuntando al módulo desktop y se comporta igual que antes.
- [x] Core no depende de JavaFX ni de ningún otro módulo del proyecto; sqlite depende solo de core; desktop depende de core y sqlite.
- [x] JUnit 5 está configurado en core y sqlite, y la fase de tests de Maven ejecuta al menos un test de integración real en sqlite: inicializa el esquema sobre una base temporal.
- [x] Ningún test toca la base real del proyecto.
- [x] Recorrido no existe más en el código y el esquema ya no crea sus tablas; las bases existentes conservan esas tablas intactas.
- [x] Validación manual: se recorren todas las secciones de la app sin errores.

## Comments

- Implementado en 96a6551. El Dashboard pasó de consultar `recorridos` a contar y listar Salidas planificadas de hoy a hoy + 7 días (con rótulos "Salidas"), porque Recorrido debía salir del código; ticket 11 lo mueve a core.
- La clave del nombre de Ruta pasó a core (`NombreRuta`) para que la migración del esquema en sqlite no dependa de desktop. El script de datos de demostración se adaptó al nuevo classpath.
- Validación manual aprobada por el usuario el 2026-11-03; issue cerrado.
