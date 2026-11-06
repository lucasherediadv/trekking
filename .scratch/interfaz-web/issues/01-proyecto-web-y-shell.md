# 01: Proyecto web y shell navegable

**What to build:** El proyecto web (Vite, React y TypeScript, ADR 0007) corre junto a la API y muestra el shell de la aplicación: menú lateral con marca TrekRisk, breadcrumbs y una página para URLs inexistentes. La raíz redirige a Rutas mientras no exista Salidas. Queda listo el arnés de pruebas del seam único (la app completa con la API simulada) con un primer test.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] El proyecto web vive fuera del reactor Maven; el `pom.xml` padre y las dependencias entre módulos Java no cambian.
- [ ] Un comando de npm levanta la web en desarrollo; el proxy envía `/api` a `127.0.0.1:8080` y la URL base se configura por variable de entorno.
- [ ] Los tipos de petición y respuesta se generan desde el contrato OpenAPI de la API con un comando de npm.
- [ ] El menú lateral muestra Salidas, Rutas, Inventario y Clientes con la marca y el logo, se contrae a iconos con tooltips y marca la sección activa.
- [ ] Las pantallas secundarias muestran breadcrumbs; una URL inexistente muestra un aviso con enlace de regreso.
- [ ] La raíz redirige a Rutas.
- [ ] Todo el texto está en español con los términos del glosario.
- [ ] Hay un test del seam único (app renderizada con enrutador y MSW) que verifica la navegación del shell.
- [ ] `AGENTS.md` documenta los comandos de instalación, desarrollo, prueba y compilación de la web, y la ubicación del proyecto.
