# Diff-fence fixture

The block below resembles a patch, but it is only a fenced example. It should
be highlighted as a diff without changing any file when this document is
rendered.

```diff
--- a/imaginary.txt
+++ b/imaginary.txt
@@ -1 +1 @@
-old meow
+new meow
```

The path is fictional and the hunk is not applied anywhere. This fixture is
safe to copy into a Markdown renderer while testing its syntax highlighting.
