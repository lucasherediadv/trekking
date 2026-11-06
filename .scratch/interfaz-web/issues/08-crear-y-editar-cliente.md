# 08: Crear y editar Cliente

**What to build:** El administrador crea y edita un Cliente en un formulario con URL propia. Mientras escribe recibe validación sin guardar, y la regla de menores queda visible.

**Blocked by:** 03, 07

**Status:** ready-for-agent

- [ ] Hay formularios de alta y edición con URL propia, breadcrumbs, Guardar y Cancelar, que vuelven al listado actualizado.
- [ ] La validación sin guardar de la API se usa mientras se escribe y marca cada campo afectado.
- [ ] El 422 al guardar se muestra por campo; el 404 con aviso y regreso al listado.
- [ ] Si el Cliente es menor, el formulario lo indica y aplica las reglas de menores que devuelve la API, sin reimplementarlas.
- [ ] Salir con cambios sin guardar pide confirmación.
- [ ] Tests del seam único: alta feliz, edición feliz, validación sin guardar, 422 por campo, 404 y confirmación al salir.
