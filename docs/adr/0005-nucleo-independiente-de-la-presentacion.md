# El núcleo de la aplicación es independiente de la presentación

Para migrar la aplicación de escritorio a web (y eventualmente a otros clientes), la lógica se separa en un proyecto Maven multi-módulo: `trekking-core` contiene el dominio, los casos de uso y las interfaces de repositorio y almacenamiento, en Java plano sin JavaFX ni anotaciones de framework; `trekking-sqlite` implementa esas interfaces; `trekking-desktop` (y más adelante `trekking-api`) solo presenta y traduce. Se elige separar en módulos y no solo en paquetes porque la separación por paquetes ya existía y JavaFX igual se filtró al modelo y a los DAOs; con módulos, el compilador impide esa dependencia.

## Consequences

- Las reglas de negocio que hoy viven en controllers (transiciones de estado, validaciones, Ruta borrador, reserva de stock) pasan a casos de uso de core, que exponen objetos simples y excepciones propias no chequeadas; nunca `SQLException`, `ObservableList` ni DAOs.
- Core controla el límite de la transacción mediante un puerto; la implementación SQLite lo serializa (`BEGIN IMMEDIATE`) para conservar lo decidido en el ADR 0003.
- Core no conoce el sistema de archivos: las fichas médicas se guardan mediante una interfaz de almacenamiento y la Ficha operativa se genera como bytes que cada presentación guarda o descarga.
- SQLite se mantiene durante la separación; cambiar de motor es una decisión posterior que solo afecta a la implementación de los repositorios.
