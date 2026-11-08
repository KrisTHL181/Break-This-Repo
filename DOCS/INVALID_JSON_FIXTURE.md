# Invalid JSON fixture

This fenced block is labeled `json` but intentionally contains a trailing
comma. A strict parser should reject it; a syntax highlighter may still color
it as if it were valid JSON.

```json
{
  "repository": "break-this-repo",
  "mood": "chaotic",
}
```

The snippet is documentation only. It is not read by a build, workflow, or
application, and it contains no secrets or instructions to execute it.
