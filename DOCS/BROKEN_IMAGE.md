# Broken image fixture

The image below deliberately points at a path that does not exist:

![A placeholder cat that was never uploaded](./assets/cat-that-was-never-uploaded.png)

Browsers should display the alt text (and often a broken-image indicator) while
offline documentation tools may report a missing asset. The target is local to
this repository, so the fixture never contacts a real service or sends data
anywhere.
