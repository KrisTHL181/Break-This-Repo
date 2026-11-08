# Canvas fallback fixture

This canvas has dimensions but no drawing script:

<canvas width="240" height="60">
  A browser without canvas support should show this sentence.
</canvas>

Modern browsers may reserve a blank rectangle, while other viewers expose the
fallback text. The page intentionally includes no script, event handler, or
external resource.
