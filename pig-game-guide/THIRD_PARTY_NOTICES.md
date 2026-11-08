# Reference data and dependencies

The MIT license in this directory covers original program code, tests and documentation. It does not grant rights in third-party game artwork, game names or trademarks.

- `templates.npz`, `long_templates.npz`, and `elephant_templates.npz` contain small appearance-reference arrays extracted from the WeChat pig puzzle game shown by the contributor. These are reference pixels used by the recognizer, not independently created character artwork.
- PNG files under `examples/` derive from contributor-provided game views. Regions outside the game board have been replaced with a plain background and image metadata has been removed. They are used for reproducible recognition and motion regressions. The original game's title and rights holder have not been independently identified; this project does not claim to own or relicense that artwork or represent the game developer.
- JSON fixtures contain measured or simulated board geometry and movement samples. Personal session logs, desktop window positions and timestamps are excluded from the public fixture set.
- NumPy, Pillow, OpenCV and PyObjC are installed from their separately distributed Python packages and remain subject to their own licenses. Apple frameworks are supplied by macOS, not bundled here.

A new skin or a different game will require its own reference data and verified movement rules. Keep image provenance and privacy information with any new fixture contribution.
