# 17: Editar Salida planificada

**What to build:** El guía edita una Salida planificada en un formulario completo que reemplaza su Copia operativa vigente, sin crear historial, ofreciendo solo los elementos que puede asignar.

**Blocked by:** 14, 15

**Status:** resolved

- [x] El formulario tiene URL propia y breadcrumbs, con Guardar y Cancelar que vuelven al listado actualizado.
- [x] Permite cambiar fecha, Ruta, equipamiento y Participantes, ofreciendo solo los elementos asignables que devuelve la API.
- [x] Guardar reemplaza la Copia operativa vigente en una sola petición.
- [x] Editar una Salida que ya no está planificada responde con el 409 de la API: se muestra el mensaje, se recarga y no se reintenta.
- [x] Un 409 de stock indica el elemento y la cantidad disponible.
- [x] Salir con cambios sin guardar pide confirmación.
- [x] Tests del seam único: edición feliz, 422 por campo, 409 por estado final, 409 de stock y confirmación al salir.
