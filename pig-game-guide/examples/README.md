# Board fixtures

`demo.png` is a fixed board with 92 pigs and locks requiring 20 and 35 pig exits. Run `sh start.command --demo` from the project directory to produce a new 120-step independently replayed plan.

The remaining PNG and JSON files are regression inputs used by `tests/`. PNG files contain the normalized game board; areas outside rows 178–699 have been replaced by a neutral background, with no image metadata retained. JSON files contain geometry, expected board states or motion observations, not a recording of a user's desktop session.

Generated analysis results and fresh runtime records are intentionally not part of this directory. The reference images and appearance arrays contain third-party artwork; see `../THIRD_PARTY_NOTICES.md` for provenance and license scope.
