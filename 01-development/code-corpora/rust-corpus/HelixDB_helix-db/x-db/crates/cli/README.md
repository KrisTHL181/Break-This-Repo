# Helix CLI

The v3 CLI manages local Helix instances and WorkOS-session-authenticated Helix Cloud resources.

- Local: `init`, `add`, `start`, `stop`, `restart`, `status`, `logs`, `query`, `shell`, `prune`.
- Cloud discovery/resources: `workspace`, `project`, `cluster`, `database`, `service-credential`, `api`.
- Cloud queries: `query` and `shell` execute through the backend query broker.
- Authentication: `auth login|status|logout` stores only a rotating WorkOS session.

The Cloud CLI accepts no API-key login, service-credential login, direct gateway path, or custom
query authorization. Tenant creation returns a default read-write application key once for direct
gateway clients; the CLI displays but never stores or uses it. Additional application keys remain
explicitly managed secrets. `push` and `sync` are removed.

See [the CLI docs](../../docs/cli/command-reference.mdx).
