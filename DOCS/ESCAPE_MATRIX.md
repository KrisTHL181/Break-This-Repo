# Escape matrix

Each item starts with a punctuation character that Markdown normally assigns a
meaning to. The backslash asks the renderer to show it literally.

- \# not a heading
- \* not emphasis
- \[ not a link opener
- \] not a link closer
- \| not a table separator

The matrix is plain text and contains no executable or external content. It
only makes escaping support easy to compare across Markdown implementations.
