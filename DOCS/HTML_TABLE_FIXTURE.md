# HTML table fixture

GitHub permits a small amount of HTML in Markdown. This table deliberately
uses `rowspan` and `colspan` so that HTML-aware and Markdown-only renderers
can be compared.

<table>
  <tr>
    <th>path</th>
    <th>signal</th>
  </tr>
  <tr>
    <td rowspan="2">DOCS/</td>
    <td>text</td>
  </tr>
  <tr>
    <td colspan="2">one cell spans two columns</td>
  </tr>
</table>

There are no scripts, forms, or remote resources in this fixture. Any odd
layout is limited to this document's rendering.
