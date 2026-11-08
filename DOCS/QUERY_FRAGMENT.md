# Query-and-fragment fixture

This link points at an existing local file but adds a query string and a
fragment that the file does not define:

[Open the file with imaginary parameters](./NO_EXTENSION?mode=cat&noise=1#missing-section)

Browsers may ignore the query, scroll nowhere for the fragment, or hand both
to a static host. The destination remains inside the repository; this page has
no real network or executable behavior.
