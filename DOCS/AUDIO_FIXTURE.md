# Audio fallback fixture

This element has controls but no source, so a browser has nothing to fetch or
play:

<audio controls>
  This browser does not render the empty audio fixture.
</audio>

There is no `src`, script, or remote resource. The page only compares empty
media and fallback-text rendering.
