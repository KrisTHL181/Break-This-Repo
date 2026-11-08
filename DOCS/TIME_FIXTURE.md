# Time-element fixture

The `datetime` attribute below uses a date that does not exist in the
Gregorian calendar (2099 is not a leap year):

<time datetime="2099-02-29">the impossible day after February 28</time>

Browsers and accessibility tools may preserve the visible text while rejecting
or ignoring the machine-readable value. This is a text-only rendering fixture;
there is no script, scheduling action, or external service.
