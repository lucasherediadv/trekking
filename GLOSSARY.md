# Salidas y planificación de trekking

Este contexto organiza rutas técnicas reutilizables y las salidas fechadas que se planifican a partir de ellas, junto con sus participantes y equipamiento.

## Lenguaje

**Ruta**:
Plantilla técnica reutilizable con datos del recorrido y sus checkpoints.
_Avoid_: Recorrido

**Ruta borrador**:
Ruta cuyo Itinerario todavía no tiene Checkpoints; no puede usarse para planificar una Salida. Al completar y guardar dos o más Checkpoints, deja de ser borrador.
_Avoid_: Ruta incompleta (como estado persistido independiente)

**Checkpoint**:
Punto de control que pertenece a una única ruta y ocupa una posición en su itinerario; una salida conserva su propia copia operativa de esos puntos.
_Avoid_: Punto global

**Itinerario**:
Secuencia ordenada de checkpoints de una ruta, cuyos horarios pertenecen a un mismo día operativo.
_Avoid_: Ruta (para referirse solo a la secuencia de puntos)

**Salida**:
Instancia fechada de una ruta, con participantes, equipamiento y checkpoints operativos propios.
_Avoid_: Recorrido

**Salida vencida**:
Salida planificada cuya fecha ya pasó y que espera ser marcada como realizada o cancelada. Es una condición calculada, no un estado propio.
_Avoid_: Salida atrasada, Salida pendiente de cierre

**Copia operativa**:
Datos de ruta, checkpoints, participantes o equipamiento conservados en una salida y aislados de cambios posteriores en sus catálogos de origen. La edición de la salida reemplaza su copia vigente; no constituye un historial de versiones.
_Avoid_: Datos vivos, versión histórica

**Asistente de Salida**:
Recorrido de cinco pasos (Datos, Ruta, Equipamiento, Participantes y Revisión) para crear una Salida. Datos y Ruta son obligatorios; Equipamiento y Participantes son opcionales y cuentan como completados una vez visitados. Se puede volver a cualquier paso completado, y avanzar a cualquier paso cuando los obligatorios previos estén completos.
_Avoid_: Wizard

**Ficha operativa**:
Documento PDF imprimible que exporta la copia operativa vigente de una salida, sin incorporar adjuntos médicos.
_Avoid_: Reporte, Exportable

**Participante**:
Cliente asociado a una salida.
_Avoid_: Cliente de salida

**Inventario**:
Catálogo de elementos con cantidad total, estado y vigencia para asignaciones.
_Avoid_: Equipamiento (como nombre del módulo)

**Estado del elemento**:
Condición física de un elemento de inventario: disponible, en reparación, en inspección, fuera de servicio o extraviado. En reparación, fuera de servicio y extraviado impiden asignarlo a una salida; estar asignado no es un estado, se deduce de la Reserva de stock.
_Avoid_: Asignado (como estado)

**Equipamiento de salida**:
Elemento de inventario asignado a una salida en una cantidad determinada.
_Avoid_: Equipamiento requerido por ruta

**Reserva de stock**:
Cantidad de un elemento de inventario comprometida por las salidas planificadas.
_Avoid_: Descuento físico de stock

**Baja**:
Estado de un cliente, una ruta o un elemento de inventario que lo excluye del uso activo sin borrar el registro; cuando la sección lo permite, puede reactivarse.
_Avoid_: Eliminación (para la baja lógica)
