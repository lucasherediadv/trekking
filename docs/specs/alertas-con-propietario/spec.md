# Alertas asociadas a su ventana

> **Nota (08/11/2026):** esta spec nombra la app de escritorio JavaFX, eliminada en el commit `54a0df4`. Se conserva como registro histórico; el comportamiento vigente es el de la web (spec `interfaz-web`).

## Problema

Al cerrar algunas alertas, en particular las confirmaciones de acciones sobre datos, la ventana de la aplicación puede perder el foco y quedar detrás de otra ventana o del escritorio. La aplicación usa una ventana JavaFX principal con navegación entre pantallas, pero varios `Alert` se muestran sin un propietario explícito.

## Comportamiento esperado

- Todas las alertas de la aplicación (confirmación, error, advertencia e información) se muestran asociadas a la ventana que originó la acción.
- Las alertas son modales respecto de esa ventana mientras están abiertas.
- Las alertas se posicionan centradas sobre su ventana propietaria, incluso si la ventana está acomodada en una parte de la pantalla.
- Al aceptar o cancelar una alerta, se cierra solo la alerta y la pantalla que la originó permanece visible y activa.
- Al cerrarse, se devuelve el foco a la ventana propietaria y al control que lo tenía antes de abrir la alerta, si ese control sigue en la escena.
- La navegación no cambia como efecto de cerrar una alerta. Esto también aplica a formularios que se muestran en una ventana propia: al cerrar una alerta del formulario, permanece activo ese formulario.
- El resultado de cada alerta (por ejemplo, aceptar o cancelar una confirmación) conserva su significado y sigue gobernando la acción asociada.

## Alcance

Incluye las alertas creadas en controladores y utilidades de la aplicación, incluidas las confirmaciones, y las alertas de error mostradas durante la navegación. No cambia el contenido, los botones ni las acciones de negocio de cada alerta.

## Criterios de aceptación

1. Al mostrar cualquier alerta desde una pantalla, la ventana de origen queda bloqueada mientras la alerta está abierta.
2. Al cerrar una alerta de confirmación, la ventana y pantalla de origen recuperan el foco; no aparece una ventana en blanco ni se vuelve al dashboard por ese cierre.
3. Confirmar y cancelar conservan el resultado funcional actual de la acción.
4. El mismo comportamiento se mantiene en formularios abiertos en una ventana propia.

## Decisiones acordadas

- El arreglo cubre todas las alertas de la aplicación, no solo las confirmaciones de eliminación.
- Después de cerrar una alerta, el usuario permanece en la pantalla que la abrió.

## Fuera de alcance

- Rediseñar textos, estilos o botones de alertas.
- Cambiar la navegación entre pantallas o el resultado de las operaciones confirmadas.
- Agregar pruebas automatizadas de interfaz; el proyecto no tiene infraestructura de pruebas JavaFX configurada.
