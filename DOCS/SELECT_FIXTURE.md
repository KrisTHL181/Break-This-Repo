# Select-control fixture

This disabled control intentionally marks two options as selected:

<select disabled>
  <option selected>first cat</option>
  <option selected>second cat</option>
</select>

HTML parsers may keep the first selection, the last selection, or normalize the
markup. Because the control is disabled and not inside a form, it cannot submit
anything; the page only compares fallback behavior.
