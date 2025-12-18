# Repository Guidelines

## Project Structure & Module Organization

- Monorepo layout (repo root): `backend-java-spring/`, `frontend-angular-ts/`, `DOCS/`, `tools/`.
- Frontend SPA (this directory):
  - App source: `src/`
  - Static assets: `public/` (copied via `angular.json`)
  - Unit tests: `src/**/*.spec.ts` (keep specs alongside the code they cover)
  - Dev proxy: `proxy.conf.json` (proxies `/api` to a local backend)

## Build, Test, and Development Commands

From `frontend-angular-ts/`:

- `npm install` — install dependencies.
- `npm start` — run the Angular dev server (uses `proxy.conf.json`).
- `npm test` — run unit tests (`ng test`).
- `npm run build` — production build.

From repo root (backend, if needed):

- `cd backend-java-spring; .\\mvnw.cmd clean install` — build all services.
- `cd backend-java-spring; .\\mvnw.cmd test` — run all backend tests.
- `cd backend-java-spring\\auth-service; ..\\mvnw.cmd spring-boot:run` — run one service.

## Coding Style & Naming Conventions
- C:\Users\Randall\Documents\kaban\cursor-codex-rules
- Indentation: 2 spaces; insert final newline; trim trailing whitespace (see `.editorconfig`).
- Quotes: single quotes for TypeScript (see `.editorconfig` and `package.json` Prettier config).
- Angular naming: `*.component.ts/html/css`, `*.service.ts`, `*.spec.ts`; selector prefix is `app` (see `angular.json`).

## Testing Guidelines

- Frontend: keep tests in `src/**/*.spec.ts`; prefer testing public behavior via Angular testing utilities.
- Backend: JUnit/Spring Boot tests live under `backend-java-spring/*/src/test/java`.

## Commit & Pull Request Guidelines

- Commit messages: Conventional Commits (e.g., `feat(frontend): add login form`, `fix(auth): handle expired token`).
- PRs: include a clear description, steps to test locally, linked issue (if any), and screenshots for UI changes.

## Security & Configuration Tips

- Do not commit secrets; use environment variables or untracked local config.
- Do not commit build output (`**/target/`) or dependencies (`frontend-angular-ts/node_modules/`).
