# Octop integration assets

The panel ships one patched file set for Octop's dashboard. The Python side is
the upstream portable archive; only the SPA is rebuilt here.

- `patches/0001-runtime-base-and-token-namespace.patch`
  - reads the mount from `<base>` / `window.__OCTOP_BASE__` at runtime;
  - builds with a relative Vite base;
  - scopes `auth_token`, `auth_remember_pref` and `active-agent` by mount, so
    several instances can share one panel origin;
  - routes the bundled static assets (`octop-mascot-*`, `pwa-192.png`,
    `experts/avatars/*.svg`) through `publicUrl()`, because `<base>` does not
    apply to the upstream's root-absolute paths and the panel answers unknown
    root paths with its SPA shell (broken images otherwise);
  - keeps every navigation and socket inside the mount: redirects go through
    `publicUrl()` (`/login`, `/setup`, `/chat`), in-app links use `<Link to>`,
    and the six hand-built WebSocket URLs (chat, terminal, ADB shell, desktop /
    mobile / browser streams) use `getWsUrl()`.
- `check-mount-escapes.sh <source-dir>` fails the build when the patched
  dashboard grows a root-absolute URL again (hard redirect, `href="/`, `src="/`,
  `location.host}/api`, `fetch("/api`, `window.open("/`). `build-dashboard.sh`
  runs it after applying the patch, before any npm work.
- `build-dashboard.sh <version>` downloads the tag, applies the patch, runs the
  gate, builds with `npm run build` (`tsc -b && vite build` — type errors fail
  here instead of in the user's browser) and writes
  `src/main/resources/octop/octop-dashboard-<version>.zip`.

The jar installer downloads `Octop-portable-<plat>-<version>.zip`, unpacks it,
and replaces `packages/octop/dashboard` with the zip above. Keep the version in
the generated resource name in sync with the version(s) the picker offers.
