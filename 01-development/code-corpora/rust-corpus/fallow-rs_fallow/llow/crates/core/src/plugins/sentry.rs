//! Sentry error tracking plugin.
//!
//! Detects Sentry projects and marks client/server/edge config files as always used.

use super::Plugin;

const ENABLERS: &[&str] = &[
    "@sentry/nextjs",
    "@sentry/react",
    "@sentry/node",
    "@sentry/browser",
];

const ALWAYS_USED: &[&str] = &[
    "sentry.client.config.{ts,js,mjs}",
    "sentry.server.config.{ts,js,mjs}",
    "sentry.edge.config.{ts,js,mjs}",
];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "@sentry/nextjs",
    "@sentry/react",
    "@sentry/node",
    "@sentry/browser",
    "@sentry/cli",
    "@sentry/webpack-plugin",
    "@sentry/vite-plugin",
];

define_plugin! {
    struct SentryPlugin => "sentry",
    enablers: ENABLERS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}
