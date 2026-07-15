# AGENTS.md

## Cursor Cloud specific instructions

This repository is a **design/prototype-only** project for an IoT Gateway (物联网网关). It
contains a design document (`docs/iot-gateway-design.md`) and a **static HTML/CSS/JS
prototype** under `prototype/`. There is intentionally **no backend, build system, package
manager, tests, or dependencies** — everything the design doc describes (APIs, databases,
message brokers, plugins) is design intent, not implemented code.

### Running the prototype
- The only runnable service is a static file server for `prototype/`. From the repo root:
  `python3 -m http.server 8000 -d prototype` then open `http://localhost:8000/index.html`.
- Python 3 is pre-installed; no dependency install step is needed.
- Pages: `index.html` (overview), `devices.html`, `plugins.html`, `messages.html`,
  `settings.html`. Interactivity (tab switching, modals, drawers) is handled by
  `prototype/assets/app.js`; there are no network/API calls, so all data is hardcoded.

### Lint / test / build
- There is no lint, test, or build tooling in this repo. Do not expect `npm`, `make`, etc.
- Verification = serving the static files and confirming pages render and the UI interactions
  (open/close modals, tab switching) work in a browser.

### Notes
- Docs and UI text are primarily in Chinese; the README is at `README.md`.
