# helix-db-embedded

Native embedded runtime for the `helix-db` Python SDK.

Install both packages:

```sh
python -m pip install helix-db helix-db-embedded
```

Applications continue to use the public SDK:

```python
from helixdb import Client, Disk

client = Client.embedded(Disk("./data", "app"))
```

The SDK imports `helixdb_uniffi` internally. Native wheels are published for
macOS universal2, manylinux x86_64/aarch64, and Windows x86_64.
