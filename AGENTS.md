# Trekking Project

## Architecture
- One Maven application module: `trekking-app/` (Java 17, JavaFX/FXML, SQLite); entry point `com.example.App`.
- UI: `controller/` classes paired with `src/main/resources/com/example/fxml/` views. `model/` holds domain data, `dao/` performs persistence, `database/ConexionDB.java` opens SQLite and initializes schema, `service/` contains query/report use cases, and `util/` has shared UI helpers.
- Main feature areas include clients/medical records, routes/checkpoints, equipment, and scheduled walks (`Recorrido`). PDF output is in `service/FichaOperativaPDFService.java`.

## Conventions
- Keep Java sources under `trekking-app/src/main/java/com/example/`, grouped by the existing packages; FXML and shared CSS live under `src/main/resources/com/example/fxml/`.
- Controllers use JavaFX `@FXML` bindings and load FXML from absolute classpath paths such as `/com/example/fxml/dashboard.fxml`; keep `fx:controller`, `fx:id`, and handler names aligned.
- Use the existing DAO pattern for SQL access via `ConexionDB.conectar()`, JDBC prepared statements, and try-with-resources. Schema initialization is centralized in `ConexionDB.crearTabla()`.
- Preserve the existing Spanish domain names and UI vocabulary. Before domain changes, follow `docs/agents/domain.md`; check applicable glossary/ADR guidance there.
- Tickets live in `.scratch/<feature>/issues/NN-<slug>.md`; specs live in `docs/specs/<feature>/spec.md`. Use `Status:` with `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, or `wontfix`; append discussion under `## Comments`.
- Treat spec, ticket breakdown, implementation, and review as separate stages; get explicit user approval between stages. Wayfinding map/ticket rules are in `docs/agents/issue-tracker.md`.
- Domain docs use a single-context layout. Follow `docs/agents/domain.md` for glossary vocabulary and ADR conflict handling.

## Commands
- Run from repository root; Maven project file is `trekking-app/pom.xml`.
- Compile/typecheck: `mvn -f trekking-app/pom.xml compile`.
- Test lifecycle: `mvn -f trekking-app/pom.xml test` (no test source tree or test framework dependency is currently present).
- Build/package: `mvn -f trekking-app/pom.xml clean package`.
- Run the JavaFX app: `mvn -f trekking-app/pom.xml javafx:run`.
- No lint or formatting command/plugin is configured in the POM.

## Testing
- No `src/test` files or test dependencies are present. Do not claim behavioral test coverage from a successful Maven test phase.
- For changes, at minimum run the compile command; use `test` or `clean package` when the change warrants the Maven lifecycle. UI behavior requires manual JavaFX validation; no automated UI test setup is configured.

## Extending the Project
- Add/adjust a domain type in `model/`, its SQL operations in a focused `dao/` class, and schema setup in `database/ConexionDB.java` when persistence changes.
- Add UI screens by pairing a controller in `controller/` with FXML under `src/main/resources/com/example/fxml/`; wire navigation through existing `NavigationShell` patterns.
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
- `trekking-app/trekking.db`
- `trekking-app/scripts/`
- `trekking-app/src/main/resources/com/example/fonts/`
