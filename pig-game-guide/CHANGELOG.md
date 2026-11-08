# Changelog

## 0.1.0 — 2026-09-15

- Publish the pig-board recognizer, simulator, visualization and guarded automatic clicking in a standalone directory.
- Add an isolated local installer, launcher, installation diagnostics, pinned dependencies and a phone-free demo.
- Use a standard per-user runtime directory and validate arguments before starting native UI or importing optional dependencies.
- Retain long-pig, elephant and numeric-lock behavior, including confirmed-exit and collision checks.
- Include regression fixtures with non-board pixels and image metadata removed; omit private runtime logs, developer environment paths and generated historical plans.

Validation: a new isolated Python 3.12 environment installed the pinned dependencies successfully; `pip check` passed; all 74 tests passed; the standalone demo produced a 120-step solution with an empty final simulated board. No real-device clicks were performed for this publication. An initial native OCR timeout was observed and the unchanged 4-second guard rejected it; subsequent demo and full-regression runs passed.
