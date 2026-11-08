# Fieldset fixture

This disabled group has a legend and a control, but no form around it:

<fieldset disabled>
  <legend>Optional cat preferences</legend>
  <label><input type="checkbox"> extra whiskers</label>
</fieldset>

Browsers may gray the whole group and assistive tools may announce the legend.
Nothing can be submitted from this document; the markup only tests grouping and
disabled-state rendering.
