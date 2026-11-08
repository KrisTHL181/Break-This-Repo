# Broken Link Ledger

This page is a deliberately small Markdown rendering experiment. It keeps a
link that is guaranteed not to resolve on the public internet while leaving a
breadcrumb for anyone auditing the repository.

## The experiment

[Follow the link to the repository's imaginary maintenance tunnel](https://break-this-repo.invalid/maintenance-tunnel)

The `.invalid` top-level domain is reserved for examples and documentation, so
the target above is intentionally unreachable. A link checker should report it
without needing to contact a real service. The rest of this page explains why
the failure is expected instead of silently leaving a broken reference behind.

## What to observe

1. GitHub renders the link as a normal Markdown link.
2. Opening it produces a network/DNS failure rather than a page from this
   repository.
3. A documentation audit can distinguish this expected failure from an
   accidental typo by matching the `.invalid` suffix and this explanation.

Please keep the experiment harmless: do not replace the reserved domain with a
real service or add credentials, tracking, or executable content.
