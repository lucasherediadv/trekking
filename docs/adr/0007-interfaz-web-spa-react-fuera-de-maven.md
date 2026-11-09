# La interfaz web es una SPA React + TypeScript fuera del reactor Maven

La interfaz web consume la API REST (ADR 0006) como una SPA con Vite, React y TypeScript, en la carpeta hermana `trekking-app/trekking-web/`, gestionada solo con npm y fuera del reactor Maven: el `pom.xml` padre y la regla de dependencias entre módulos Java no cambian. Se eligió una SPA sobre servir HTML desde Spring porque la API ya es el único punto de entrada al núcleo y mantiene a la presentación sin reglas de negocio, como pide el ADR 0005.

Los tipos TypeScript se generan desde el contrato OpenAPI de la API, para que el contrato no se desincronice. En desarrollo la web llama a `/api` mediante el proxy de Vite hacia `127.0.0.1:8080`, de modo que no hace falta CORS (`trekking.api.origen-web` queda vacío); la URL base es configurable por variable de entorno.

## Consequences

- Hay dos procesos en desarrollo (`spring-boot:run` y `npm run dev`); no se empaqueta ni se sirve la web desde Spring en esta etapa, y el despliegue queda para una etapa posterior junto con la autenticación.
- La web no almacena datos de Clientes ni fichas médicas en el navegador (ni `localStorage` ni caché entre sesiones); las fichas médicas y la Ficha operativa solo se descargan de forma explícita, sin vista previa.
- Los errores 409 (estado cambiado, stock insuficiente) se muestran, se recarga el recurso y no se reintenta sola.
- El comportamiento de UX de referencia es el de las specs de UI y el ADR 0004; las diferencias se anotan en la spec de la web.
- Las pruebas son Vitest + Testing Library con la API simulada (MSW); los E2E con navegador real quedan fuera de alcance.
