# Trekking Project

## Architecture
- Multi-module Maven project (Java 17) with parent POM `trekking-app/pom.xml`. Allowed dependencies: `trekking-desktop` → `trekking-core` + `trekking-sqlite`; `trekking-sqlite` → `trekking-core`; `trekking-core` depends on no project module and never on JavaFX.
  - `trekking-core` (`com.example.core`): plain-Java domain rules.
  - `trekking-sqlite` (`com.example.sqlite`): SQLite connection and schema (`BaseDatosSqlite`, which takes the DB file location by constructor).
  - `trekking-desktop` (`com.example.desktop`): the JavaFX app; entry point `com.example.desktop.App`. `database/ConexionDB.java` resolves the DB file (system property `trekking.db`, else `trekking.db` at the repository root) and delegates to `BaseDatosSqlite`.
- Desktop UI: `controller/` classes paired with `trekking-desktop/src/main/resources/com/example/fxml/` views. `model/` holds domain data, `dao/` performs persistence, `service/` contains query/report use cases, and `util/` has shared UI helpers.
- Main feature areas include clients/medical records, routes/checkpoints, equipment, and scheduled walks (`Salida`). PDF output is in `service/FichaOperativaPDFService.java`.

## Conventions
- Keep Java sources in their module under `src/main/java/com/example/<core|sqlite|desktop>/`, grouped by the existing packages; FXML and shared CSS live under `trekking-desktop/src/main/resources/com/example/fxml/`.
- Controllers use JavaFX `@FXML` bindings and load FXML from absolute classpath paths such as `/com/example/fxml/dashboard.fxml`; keep `fx:controller`, `fx:id`, and handler names aligned.
- Use the existing DAO pattern for SQL access via `ConexionDB.conectar()`, JDBC prepared statements, and try-with-resources. Schema initialization is centralized in `BaseDatosSqlite.inicializarEsquema()` (reached from desktop via `ConexionDB.crearTabla()`).
- Preserve the existing Spanish domain names and UI vocabulary. Before domain changes, follow `docs/agents/domain.md`; check applicable glossary/ADR guidance there.
- Tickets live in `.scratch/<feature>/issues/NN-<slug>.md`; specs live in `docs/specs/<feature>/spec.md`. Use `Status:` with `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, or `wontfix`; append discussion under `## Comments`.
- Treat spec, ticket breakdown, implementation, and review as separate stages; get explicit user approval between stages. Wayfinding map/ticket rules are in `docs/agents/issue-tracker.md`.
- Domain docs use a single-context layout. Follow `docs/agents/domain.md` for glossary vocabulary and ADR conflict handling.

## Commands
- Run from repository root; the parent Maven project file is `trekking-app/pom.xml`.
- Compile/typecheck: `mvn -f trekking-app/pom.xml compile`.
- Test lifecycle: `mvn -f trekking-app/pom.xml test` (JUnit 5 in `trekking-core` and `trekking-sqlite`).
- Build/package: `mvn -f trekking-app/pom.xml clean package`.
- Run the JavaFX app: `mvn -f trekking-app/pom.xml javafx:run` (runs only in `trekking-desktop`; other modules skip it).
- No lint or formatting command/plugin is configured in the POM.

## Testing
- Tests live in `trekking-core/src/test/java` and `trekking-sqlite/src/test/java`. Sqlite tests use a temporary database file (`@TempDir`) and never the repository's `trekking.db`. Desktop has no automated tests.
- For changes, at minimum run the compile command; use `test` or `clean package` when the change warrants the Maven lifecycle. UI behavior requires manual JavaFX validation; no automated UI test setup is configured.

## Extending the Project
- Add/adjust a domain type in `model/`, its SQL operations in a focused `dao/` class, and schema setup in `trekking-sqlite`'s `BaseDatosSqlite` when persistence changes.
- Add UI screens by pairing a controller in `controller/` with FXML under `trekking-desktop/src/main/resources/com/example/fxml/`; wire navigation through existing `NavigationShell` patterns.
- Put shared calculations/query workflows in `service/`; reuse helpers in `util/` for validation, alerts, and navigation.

## Git
- Commits follow Conventional Commits: `<type>(<optional scope>): <description>`. Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- Subject line: imperative mood, lowercase, no trailing period, at most 72 characters. Keep it in Spanish, consistent with the project's domain vocabulary.
- Add a body (after a blank line) only to explain the why. Mark breaking changes with `!` after the type/scope or a `BREAKING CHANGE:` footer.
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
- `trekking-app/trekking-desktop/src/main/resources/com/example/fonts/`
