# Picture fallback fixture

This `<picture>` has a source candidate but intentionally omits its fallback
`img` element:

<picture>
  <source media="(min-width: 1px)" srcset="./missing-picture.png">
  No fallback image was supplied.
</picture>

Browsers may show nothing, expose the fallback sentence, or report a missing
source. The path is local and nonexistent; no remote asset, script, or data is
involved.
