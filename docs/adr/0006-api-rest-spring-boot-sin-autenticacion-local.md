# La API REST usa Spring Boot y, por ahora, no autentica y solo escucha en local

La futura interfaz web consume una API REST en un módulo `trekking-api` (el previsto en el ADR 0005), construida con Spring Boot 3; las anotaciones de framework quedan confinadas a ese módulo y el núcleo sigue en Java plano. Se eligió Spring Boot frente a Javalin o el `HttpServer` del JDK por contar con un estándar para validación, CORS, seguridad y contrato OpenAPI, que se necesitarán cuando la API salga de la máquina local.

En esta etapa la API no tiene autenticación: escucha solo en `127.0.0.1` y acepta CORS únicamente desde un origen configurado. Es un riesgo asumido y explícito, porque la API expone datos sensibles (fichas médicas de Clientes); por eso no debe exponerse a una red hasta que exista una etapa propia de autenticación y autorización, con su spec y su ADR.

## Consequences

- La API y la app de escritorio comparten el mismo archivo SQLite mientras dure la migración; la serialización de escrituras (ADR 0003 y 0005) lo permite.
- Spring Security no se incorpora ahora; cambiar de "sin autenticación" a "con autenticación" no obliga a rehacer los controladores, pero sí a revisar CORS y el bind.
- `trekking-api` repite el ensamblado de casos de uso que hoy hace `Servicios` en desktop, en lugar de extraer un módulo compartido.
