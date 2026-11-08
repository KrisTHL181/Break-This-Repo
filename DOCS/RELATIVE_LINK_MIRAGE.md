# Relative-link mirage

The repository already contains `DOCS/README_of_nothing.md`. This link is
deliberately one punctuation mark away from that path:

[Open the almost-right document](./README-of-nothing.md)

The hyphen is not an underscore, so the target should fail on every checkout.
It is a local, read-only link-resolution experiment; no network request is
needed and no existing file is changed.
