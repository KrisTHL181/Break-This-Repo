# Escaped URL fixture

The two links below try to reach the same path, but only one percent-encodes
the space:

- [encoded](./SPACE%20NAME.md)
- [literal](./SPACE NAME.md)

The first form is URL-safe; the second tests whether a renderer encodes it on
the reader's behalf. Both destinations are local and contain only explanatory
text, so this page has no external side effects.
