# 13: Documentación y convivencia con el escritorio

**What to build:** La API queda documentada para quien trabaje en el proyecto y se valida a mano que puede convivir con la app de escritorio sobre el mismo archivo de base de datos, que es el supuesto de esta etapa (ADR 0006).

**Blocked by:** 10, 11, 12

**Status:** resolved

- [x] `AGENTS.md` refleja el módulo `trekking-api`: dependencias permitidas, que no contiene reglas de negocio ni SQL, el comando para ejecutar la API y las propiedades de configuración (`trekking.db`, puerto, `trekking.api.origen-web`, tamaño de ficha).
- [x] Validación manual documentada en un comentario de este ticket: con la API y la app JavaFX abiertas sobre la misma base, una escritura de cada lado se ve del otro, y dos confirmaciones concurrentes de Salida sobre el mismo elemento no sobrevenden stock.
- [x] Se verifica a mano que la API sin origen CORS configurado no acepta orígenes cruzados y que solo escucha en `127.0.0.1` por defecto.
- [x] `mvn -f trekking-app/pom.xml clean package` pasa con los cuatro módulos.
- [x] El `Status` del spec de la API REST pasa a `closed`.

## Comments

### Verificación headless (agente, sin JavaFX)

Hecho contra una base temporal fuera del repositorio, con la API en marcha vía `spring-boot:run`:

- Escucha solo en `127.0.0.1` por defecto (`ss -ltn` muestra `127.0.0.1:8080`).
- Sin `trekking.api.origen-web`: un `Origin` ajeno recibe 403 en el preflight y la respuesta no lleva `Access-Control-Allow-Origin`.
- Con `trekking.api.origen-web=http://localhost:5173`: ese origen recibe `Access-Control-Allow-Origin` (GET y preflight); otro origen sigue recibiendo 403 sin cabeceras CORS.
- Concurrencia: 12 `POST /api/salidas` en paralelo (curl) pidiendo 2 unidades de un Elemento con stock 5. Resultado: 2 respuestas 201 y 10 `409 STOCK_INSUFICIENTE`; `cantidadReservada` quedó en 4 (sin sobreventa), ningún 500 ni SQLITE_BUSY. Es contención dentro de un solo proceso de la API.
- `mvn -f trekking-app/pom.xml clean package` pasa con los cuatro módulos.

Hallazgos:
- `spring-boot:run` no reenvía un `-D` plano a la aplicación; la API abrió el `trekking.db` por defecto. Las propiedades van en `-Dspring-boot.run.jvmArguments="-Dtrekking.db=... "`. Documentado en `AGENTS.md`.
- `FichaOperativaApiTest.elPdfNoIncorporaLaFichaMedicaDeLosParticipantes` era intermitente (el nombre largo se partía en dos líneas del PDF); se acortó el sufijo del nombre.

### Pendiente manual (usuario)

No se pudo ejecutar la app JavaFX en esta sesión, así que NO está verificado:
- Con la API y la app de escritorio abiertas sobre el mismo archivo, que una escritura de cada lado se vea del otro (p. ej. crear un Cliente en la API y verlo en el escritorio, y al revés).
- Una confirmación concurrente de Salida entre API y escritorio (dos procesos) sobre el mismo Elemento.

Hasta entonces el segundo criterio queda sin tildar y el `Status` del spec sigue sin pasar a `closed`.

- 2026-11-05: el usuario validó a mano API + JavaFX sobre la misma base: escrituras visibles de ambos lados y sin sobreventa de stock entre procesos.
