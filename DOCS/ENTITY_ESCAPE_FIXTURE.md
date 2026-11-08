# Entity and escape fixture

The same characters appear in several contexts below:

- `&copy;` stays literal inside code
- &copy; becomes an HTML entity in normal text
- `\*stars\*` keeps its asterisks
- *stars* uses the asterisks as Markdown emphasis

The distinctions are intentionally tiny. They let a renderer or sanitizer
show which parsing layer handled each line. This page is plain text and does
not embed executable HTML or scripts.
