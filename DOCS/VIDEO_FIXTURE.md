# Video fallback fixture

This element has controls but no source. Browsers that support HTML video may
show an empty player; other viewers should display the fallback sentence.

<video controls>
  This browser does not render the empty video fixture.
</video>

There is intentionally no `src`, script, or remote resource. The experiment is
limited to how a renderer handles a media element with nothing to load.
