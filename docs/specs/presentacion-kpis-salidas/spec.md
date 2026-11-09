# Especificación: presentación de indicadores en Salidas

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

**Estado:** Borrador para aprobación  
**Fecha:** 25/10/2026  
**Alcance:** Mejora visual y contextual de las cuatro tarjetas de indicadores de la pantalla Salidas.

## Problem Statement

En la pantalla Salidas, las cuatro tarjetas de indicadores tienen el mismo peso visual y muestran solo una etiqueta y una cifra. No explican el alcance de cada cifra ni usan iconos para reconocer rápidamente qué área representa cada indicador.

## Solution

Mejorar la presentación de las cuatro tarjetas existentes sin cambiar su distribución horizontal ni los indicadores que calculan. Cada tarjeta mostrará un icono contextual, una cifra con jerarquía visual, un texto breve que aclare su alcance y un tratamiento de interacción consistente. Las tarjetas llevarán a la sección correspondiente al activarse.

## User Stories

1. Como guía, quiero reconocer de un vistazo las Salidas próximas, para anticipar la actividad inmediata.
2. Como guía, quiero saber que el indicador de Salidas incluye las Salidas planificadas desde hoy hasta hoy más siete días inclusive, para interpretar correctamente su cifra.
3. Como guía, quiero identificar las Rutas activas con un icono y una descripción claros, para distinguir su total del de las Salidas.
4. Como guía, quiero identificar los Clientes activos con un icono y una descripción claros, para entender que la cifra cuenta Clientes activos y no todos los registros.
5. Como guía, quiero entender qué hace crítico al Inventario, para interpretar la cifra sin confundirla con el stock total.
6. Como guía, quiero distinguir visualmente los cuatro indicadores sin que uno pierda legibilidad, para consultar el resumen con rapidez.
7. Como guía, quiero poder activar una tarjeta para llegar a la sección relacionada, para continuar desde el resumen hacia la gestión correspondiente.
8. Como guía, quiero conservar a la vista la tabla y los controles actuales de Salidas, para seguir usando la pantalla sin reaprender su distribución.

## Implementation Decisions

- La pantalla objetivo es Salidas, que contiene la fila de cuatro tarjetas sobre los filtros y la tabla. No se modifica la pantalla separada de inicio/dashboard.
- Se conservan el orden y la distribución horizontal actual: Salidas próximas, Rutas activas, Clientes activos e Inventario crítico.
- Cada tarjeta suma un icono contextual, un título, la cifra y un texto auxiliar. La cifra mantiene mayor jerarquía tipográfica que el texto auxiliar.
- Los textos auxiliares describen los criterios actuales: “Planificadas; hoy y próximos 7 días (inclusive)”, “En estado activo”, “En estado activo” y “Disponible ≤ 2 o en reparación, fuera de servicio o extraviado”. El indicador de Inventario cuenta Elementos activos cuya cantidad disponible después de las reservas es menor o igual a 2, o cuyo estado es En Reparación, Fuera de Servicio o Extraviado.
- Se reutilizan los iconos existentes: calendario para Salidas, trazado para Rutas, personas para Clientes y alerta para Inventario crítico.
- Las tarjetas son activables y llevan a Salidas, Rutas, Clientes e Inventario, respectivamente. Activar la tarjeta de Salidas mantiene al usuario en esa sección y permite volver al listado.
- La interacción debe comunicar que las tarjetas son activables y conservar un tratamiento de foco visible para navegación por teclado. Los iconos son decorativos cuando el título adyacente ya identifica el contenido.
- Se mantienen los valores y reglas de cálculo existentes. No hay cambios de base de datos, consultas, límites temporales, reservas ni estados.
- La implementación se limita a la presentación de Salidas y a la navegación asociada a las tarjetas; no rediseña el resto de la pantalla.

## Testing Decisions

- **Punto de verificación acordado:** una validación manual de extremo a extremo en la pantalla Salidas de la aplicación JavaFX.
- Un buen escenario comprueba el comportamiento visible: las cuatro tarjetas conservan su fila y orden; títulos, cifras, textos e iconos se leen correctamente; la activación de cada tarjeta abre la sección acordada; y la tabla, los filtros y “Nueva Salida” siguen accesibles.
- La validación confirma que los indicadores conservan sus valores y criterios: intervalo inclusivo de hoy a hoy más siete días para Salidas planificadas; Rutas y Clientes activos; e Inventario activo con disponible menor o igual a dos o con alguno de los estados críticos definidos.
- El repositorio no tiene infraestructura de pruebas automatizadas para esta interfaz. No se propone crear una nueva seam automatizada para una mejora visual localizada. La compilación del proyecto y la inspección manual de JavaFX son las comprobaciones previstas durante implementación.

## Out of Scope

- Cambiar los cálculos, criterios o valores de los indicadores.
- Cambiar el orden, la cantidad o la distribución horizontal de las tarjetas.
- Rediseñar la tabla de Salidas, sus filtros, el formulario de alta o el resto de módulos.
- Cambiar el modelo de dominio, persistencia, esquema de SQLite o navegación global.
- Modificar las tarjetas de la pantalla separada de inicio/dashboard.

## Further Notes

- La terminología de la interfaz conserva “Salidas”, “Rutas”, “Clientes” e “Inventario”, conforme al glosario y a la especificación funcional existente.
- Los estilos deben integrarse con la paleta y los recursos visuales ya usados por la aplicación.
- Esta especificación no crea ni aprueba un ticket de implementación. El desglose en tickets es la siguiente etapa y requiere aprobación explícita de esta especificación según las convenciones del repositorio.
