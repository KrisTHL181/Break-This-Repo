# Bidirectional-text fixture

These elements exercise direction handling with visible text only:

- isolated text: <bdi>猫</bdi> next to `ABC`
- forced right-to-left order: <bdo dir="rtl">ABC 123</bdo>

The `bdi` and `bdo` tags may change ordering or isolation in a browser, while
plain-text viewers leave the characters as written. No hidden directional
control characters, scripts, or external resources are used here.
