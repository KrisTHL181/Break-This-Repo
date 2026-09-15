# HelixDB documentation

This directory is the canonical source for the public HelixDB documentation.
Mintlify reads `docs.json`; `llms.txt` and `llms-full.txt` are generated artifacts.

## Page contract

Every route in `docs.json` must have exactly one custom `pageType`:

- `Tutorial`
- `Guide`
- `Concept`
- `Reference`
- `Troubleshooting`

Render the same value as the first badge after frontmatter. Optional maturity
`status` values are `Preview`, `Beta`, or `Deprecated` and require a second matching
badge. Do not use Mintlify's native `tag` field because it adds the type to the
sidebar.

Database page paths mirror the sidebar hierarchy under
`database/helix-db/<group>/` and `database/helix-cloud/<group>/`. Keep redirects
when moving an existing public route.

## Local checks

```bash
npm install
npm run generate-llms
npm run check
npx mint broken-links --check-anchors --check-redirects --check-snippets
npx mint dev --no-open
```

`npm run check-docs` validates navigation, page metadata, badges, redirects, legacy
AST/API markers, JSON examples, and Rust/TypeScript/Go/Python/JSON code groups.
Client-construction groups may omit JSON when immediately marked with
`{/* client-setup: no JSON representation */}`.
Package-install groups use Bash snippets for each SDK and
`{/* package-install: no JSON representation */}`.

The shared SDK parity suite verifies that Rust, TypeScript, Go, and Python serialize
the same operation-tree requests:

```bash
cd ../sdks/typescript
npm run parity:generate
npm run parity:compare-json
```

## Generated files

Run `npm run generate-llms` after changing navigation or page content. The generators
write page type and maturity as plain text so metadata remains available after JSX is
removed.
