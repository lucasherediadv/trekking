# 16: Asistente de Salida: Ruta nueva

**What to build:** En el paso Ruta, además de elegir una Ruta existente, el guía puede crear una Ruta nueva sin salir del asistente, con la regla de completitud del glosario.

**Blocked by:** 03, 13

**Status:** resolved

- [x] El paso Ruta alterna entre Ruta existente y Ruta nueva.
- [x] La Ruta nueva se considera completa con nombre, altitud máxima válida (número mayor o igual a cero), tipo de terreno, dificultad técnica y dificultad física.
- [x] Una Ruta nueva queda como Ruta borrador y no permite confirmar la Salida; el asistente lo explica y bloquea el avance.
- [x] Los errores 422 se muestran por campo.
- [x] Tests del seam único: Ruta nueva completa, incompleta (bloquea pasos posteriores), cambio entre modos y 422 por campo.
