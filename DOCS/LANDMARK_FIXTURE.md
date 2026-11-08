# Landmark fixture

These landmarks intentionally contain only short text:

<nav aria-label="Imaginary navigation">
  <a href="#meow">Meow</a>
</nav>

<main id="meow">
  The destination is an empty local anchor.
</main>

Screen readers may announce the navigation and main landmarks; plain-text
viewers should show the tags inline. The page has no script or external target.
