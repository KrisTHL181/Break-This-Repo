# Object fallback fixture

The object below points at a missing local resource and includes fallback text:

<object data="./missing-object.bin" type="application/octet-stream">
  No object was found.
</object>

Browsers may hide the fallback, show it, or report a failed object load. The
target does not exist and no remote resource, script, or executable payload is
involved.
