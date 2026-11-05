# Rediseño integral de Salidas, Rutas e Inventario

Status: closed

## Objetivo

Implementar el rediseño funcional descrito en [la especificación de Salidas, Rutas e Inventario](../../../docs/specs/salidas-rutas-inventario/spec.md): cuatro módulos, wizard transaccional, copias operativas independientes, stock reservado por estado, gestión de Clientes y generación de ficha PDF.

## Alcance

El trabajo cubre todos los criterios de la especificación enlazada. No incluye migrar la base existente, por lo que la validación debe partir de una base nueva. La implementación y la revisión son etapas posteriores a la aprobación de la etapa de especificación.

## Comments

- La seam de aceptación acordada es una validación manual end-to-end en JavaFX con SQLite, porque el repositorio no tiene framework de pruebas automatizadas.
