# Trekking Project

## Architecture
- Multi-module Maven project (Java 17) with parent POM `trekking-app/pom.xml`. Allowed dependencies: `trekking-api` → `trekking-core` + `trekking-sqlite`; `trekking-sqlite` → `trekking-core`; `trekking-core` depends on no project module and never on a presentation framework.
  - `trekking-core` (`com.example.core`): all business rules (validations, state transitions, stock and date calculations) and use cases (`CasosDeUso*`) per area: `cliente`, `inventario`, `ruta`, `itinerario`, `salida`, `dashboard`. It declares the repository/port interfaces (`Repositorio*`, `Transaccion`) that sqlite implements.
  - `trekking-sqlite` (`com.example.sqlite`): SQLite implementations of those ports (`Repositorio*Sqlite`, `AlmacenFichasMedicasSqlite`, `TransaccionSqlite`) and the connection/schema (`BaseDatosSqlite`, which takes the DB file location by constructor). All SQL lives here.
  - `trekking-api` (`com.example.api`): Spring Boot REST API (ADR 0006) over the core use cases. No business rules and no SQL; controllers only translate HTTP to core use cases. Config in `application.properties`, overridable with `-D`: `trekking.db` (DB file; if unset, `trekking.db` at the repository root), `server.port` (8080), `server.address` (127.0.0.1), `trekking.api.origen-web` (CORS origin; empty = no cross-origin), `trekking.api.max-ficha-medica` (upload limit, 10MB).
  - `trekking-web` (`trekking-app/trekking-web/`, ADR 0007): React SPA that talks to `trekking-api` only over HTTP. It is an npm project outside the Maven reactor and the module dependency rules. It contains no business rules (only format validation for instant feedback; errors come from the API) and stores nothing in `localStorage`, `sessionStorage` or IndexedDB. Commands under Commands.
- Main feature areas include clients/medical records, routes/checkpoints (Itinerario), equipment (inventario), and scheduled walks (`Salida`).

## Conventions
- Keep Java sources in their module under `src/main/java/com/example/<core|sqlite|api>/`, grouped by the existing packages.
- SQL access only in `trekking-sqlite` repositories, using JDBC prepared statements and try-with-resources. Schema initialization is centralized in `BaseDatosSqlite.inicializarEsquema()` (called by the API at startup).
- Preserve the existing Spanish domain names and UI vocabulary. Before domain changes, follow `docs/agents/domain.md`; check applicable glossary/ADR guidance there.
- Tickets live in `.scratch/<feature>/issues/NN-<slug>.md`; specs live in `docs/specs/<feature>/spec.md`. Use `Status:` with `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, or `wontfix`; append discussion under `## Comments`.
- Treat spec, ticket breakdown, implementation, and review as separate stages; get explicit user approval between stages. Wayfinding map/ticket rules are in `docs/agents/issue-tracker.md`.
- Domain docs use a single-context layout. Follow `docs/agents/domain.md` for glossary vocabulary and ADR conflict handling.

## Commands
- Run from repository root; the parent Maven project file is `trekking-app/pom.xml`.
- Compile/typecheck: `mvn -f trekking-app/pom.xml compile`.
- Test lifecycle: `mvn -f trekking-app/pom.xml test` (JUnit 5 in `trekking-core`, `trekking-sqlite` and `trekking-api`).
- Build/package: `mvn -f trekking-app/pom.xml clean package`.
- Run the API: `mvn -f trekking-app/pom.xml -pl trekking-api -am spring-boot:run`. Properties reach the app only via `"-Dspring-boot.run.jvmArguments=-Dtrekking.db=<file> -Dtrekking.api.origen-web=<url>"`; a plain `-D` is ignored and the API opens the repository `trekking.db`.
- Web (`trekking-app/trekking-web/`, Vite + React + TypeScript, ADR 0007; npm only, outside the Maven reactor). Run from that folder:
  - Install: `npm install`. Dev server: `npm run dev` (needs the API on `127.0.0.1:8080`; Vite proxies `/api`; env vars `VITE_API_URL` base URL, `VITE_API_PROXY` proxy target, see `.env.example`).
  - Test: `npm test` (Vitest + Testing Library + MSW; single seam = the whole app rendered via `renderApp(ruta)` in `src/test/renderApp.tsx`, API mocked with `server.use(...)` from `src/test/servidor.ts`). Build/typecheck: `npm run build`.
  - Regenerate API types (API must be running): `npm run gen:api` → `src/api/schema.d.ts` (committed, never edit by hand). Pin `typescript@5`: `openapi-typescript` breaks on TypeScript 7.
  - Errors/loading: API calls go through `llamar(...)` (`src/api/errores.ts`, the single interpreter of the uniform error body); show failures with `AvisoError` (load, with retry) or `useToast().error(e)` (actions); load with `useCarga`/`Cargando`. Never read HTTP error bodies in screens.
  - Forms: `useFormulario` + `Formulario` + `CampoTexto`/`CampoSeleccion` (`src/ui/`, example `rutas/FormularioRuta.tsx`) give per-field 422 errors, general notice, 404 handling and unsaved-changes confirmation (`ConfirmarSalida`, `useBlocker`); do not reimplement them. Pass `guardar` to also offer "Guardar" (see `rutas/ItinerarioRuta.tsx`).
  - Client form kit (`src/clientes/CamposCliente.tsx`): `CamposCliente` (fields + live `POST /api/clientes/validar` + minor rule from the API), `VACIO_CLIENTE`, `desdeCliente`, `aDatosCliente`; reuse in other client forms.
  - Files: `src/ui/archivo.ts` (`errorDeArchivo` 10 MB check, `leerBase64`) and `CampoArchivo` (file picker that rejects oversize files before any API call); `clientes/FichaMedica.tsx` is the medical-file section of the client edit page. Tests with multipart uploads: jsdom `File` cannot be serialized by Node fetch, see the `FormData.prototype.append` spy in `fichaMedica.test.tsx`.
  - Clientes: `ClienteDto` has no "has ficha médica" flag; the list probes `HEAD /api/clientes/{id}/ficha-medica` per row (200/404), never downloading the file.
- No lint or formatting command/plugin is configured in the POM.

## Testing
- Tests live in `trekking-core/src/test/java`, `trekking-sqlite/src/test/java` and `trekking-api/src/test/java` (API tests use MockMvc on a temporary DB). Core tests exercise use cases through the public interface with in-memory fakes of the repositories. Sqlite tests exercise the repositories against a temporary database file (`@TempDir`) and never the repository's `trekking.db`.
- For changes, at minimum run the compile command; use `test` or `clean package` when the change warrants the Maven lifecycle. Web UI behavior is covered by Vitest (`npm test`).

## Extending the Project
- Put domain types, rules and use cases in `trekking-core`, with a test; add the repository port there and its SQL implementation (plus schema changes in `BaseDatosSqlite`) in `trekking-sqlite`, with a test. Register new use cases in `ConfiguracionCasosDeUso` (`trekking-api`) and expose them through a controller.

## Git
- Commits follow Conventional Commits: `<type>(<optional scope>): <description>`. Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- Subject line: imperative mood, lowercase, no trailing period, in Spanish. Aim for 50 characters; 72 is the hard limit.
- Scope names the module or feature area (`core`, `sqlite`, `api`, `web`, `salidas`, `pdf`); omit it when the change spans several.
- Terse: the subject alone says what changed, and a one-line commit is the default. Add a body only when the why is not obvious from the diff: at most 3 lines wrapped at 72, stating motive, never a list of files or changes.
- Mark breaking changes with `!` after the type/scope or a `BREAKING CHANGE:` footer.
- Examples: `feat(salidas): mostrar participantes en la tabla`, `fix(pdf): corregir error al exportar la ficha`, `refactor(sqlite): extraer esquema a BaseDatosSqlite`.
- One commit per logical change; do not mix unrelated changes.
- Never add co-authorship or attribution to Claude or Anthropic in commits (no `Co-Authored-By: Claude ...` trailer or similar).
- Branch names follow `<type>/<short-description-in-kebab-case>`, using the same types as commits (e.g. `feat/separacion-nucleo`, `fix/error-exportar-pdf`). Use only lowercase letters, digits, and hyphens.
- Do not work directly on `main`; create a branch first.

## Files and Directories to Avoid
Do not read or modify these paths unless explicitly required:
- `/target/`
- `trekking-app/mvnw`, `trekking-app/mvnw.cmd`, `trekking-app/.mvn/`
- `trekking.db`
- `trekking-app/scripts/`
- `trekking-app/trekking-core/src/main/resources/com/example/fonts/`
