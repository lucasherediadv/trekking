# 01: Proyecto web y shell navegable

**What to build:** El proyecto web (Vite, React y TypeScript, ADR 0007) corre junto a la API y muestra el shell de la aplicación: menú lateral con marca TrekRisk, breadcrumbs y una página para URLs inexistentes. La raíz redirige a Rutas mientras no exista Salidas. Queda listo el arnés de pruebas del seam único (la app completa con la API simulada) con un primer test.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] El proyecto web vive fuera del reactor Maven; el `pom.xml` padre y las dependencias entre módulos Java no cambian.
- [x] Un comando de npm levanta la web en desarrollo; el proxy envía `/api` a `127.0.0.1:8080` y la URL base se configura por variable de entorno.
- [x] Los tipos de petición y respuesta se generan desde el contrato OpenAPI de la API con un comando de npm.
- [x] El menú lateral muestra Salidas, Rutas, Inventario y Clientes con la marca y el logo, se contrae a iconos con tooltips y marca la sección activa.
- [x] Las pantallas secundarias muestran breadcrumbs; una URL inexistente muestra un aviso con enlace de regreso.
- [x] La raíz redirige a Rutas.
- [x] Todo el texto está en español con los términos del glosario.
- [x] Hay un test del seam único (app renderizada con enrutador y MSW) que verifica la navegación del shell.
- [x] `AGENTS.md` documenta los comandos de instalación, desarrollo, prueba y compilación de la web, y la ubicación del proyecto.
