# 03: Crear y editar Ruta

**What to build:** El guía crea y edita una Ruta en un formulario con URL propia (ADR 0004). Los errores de validación aparecen en cada campo, y si intenta salir con cambios sin guardar se le pide confirmación. Este ticket fija el patrón de formulario y de confirmación de salida que reusan los demás.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Hay un formulario de alta y otro de edición, cada uno con URL propia, breadcrumbs, Guardar y Cancelar; ambos vuelven al listado, que se actualiza.
- [ ] El 422 se asigna a los campos por nombre; los errores sin campo se muestran como aviso general del formulario.
- [ ] Un 404 al editar muestra un aviso y lleva al listado.
- [ ] Con cambios sin guardar, salir por menú, breadcrumb o historial del navegador pide confirmación; sin cambios no la pide.
- [ ] Una Ruta nueva queda como Ruta borrador hasta tener dos o más Checkpoints.
- [ ] Tests del seam único: alta feliz, edición feliz, 422 por campo, 404 y la confirmación al salir.
