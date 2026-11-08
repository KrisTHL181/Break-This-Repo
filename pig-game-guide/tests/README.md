# Tests

Run from the `pig-game-guide` directory after installation:

```sh
.venv/bin/python -m unittest discover -s tests -p 'test_*.py' -q
```

The regression fixtures live in `../examples`. Their non-board image regions and metadata were removed for publication, while the normalized board pixels used by the recognizers were preserved. Direction/geometry, full simulated clearing, passive bird escape, elephant width, numeric locks and concurrent movement guards are checked independently.

Deployment tests additionally exercise dependency-free help, invalid CLI arguments, a phone-free demo launched from another directory, folder names containing spaces, a missing virtual environment and portable per-user state paths. Test callbacks never call `native_click`; the test suite does not connect to or capture iPhone Mirroring.
