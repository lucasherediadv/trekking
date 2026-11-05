# Header global y breadcrumbs reutilizables

**Status:** closed  
**Spec:** [docs/specs/breadcrumbs-navegacion/spec.md](../../../docs/specs/breadcrumbs-navegacion/spec.md)

## Objetivo

Implementar el header global de Trekrisk y un componente reutilizable de breadcrumbs integrado con la navegación existente del shell, de acuerdo con la especificación funcional.

## Criterios de aceptación

- El header global muestra “Trekrisk” y “Gestión operativa de salidas de montaña” y se ubica por encima del sidebar y del contenido principal.
- El sidebar conserva sus secciones y pie, sin repetir la marca ni su subtítulo.
- Los breadcrumbs se muestran debajo del header global y encima del título de pantalla; se alinean con el contenido y se adaptan al redimensionamiento.
- Las rutas se definen centralmente y cambian al cambiar la pantalla activa, sin duplicarse.
- Los elementos anteriores navegan por el mecanismo existente y respetan los guards; el elemento actual no es accionable.
- Se ocultan los breadcrumbs de pantallas no configuradas, diálogos y ventanas auxiliares, sin dejar espacio vacío.
- Salidas es el inicio de navegación; los accesos antiguos al Dashboard vuelven a Salidas mediante el shell.
- Se mantienen las funcionalidades existentes y no se modifica persistencia ni se agregan dependencias.
- La aplicación compila con Maven y se completa la validación manual descrita en la spec.

## Comments
