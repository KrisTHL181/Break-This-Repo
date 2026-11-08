# Nested fence fixture

Four backticks fence this example so that the inner three-backtick fence stays
visible as text:

````markdown
```text
the inner fence is not the outer fence
```
````

Renderers that count fence length correctly should show the inner fence inside
the code block. The fixture contains no executable snippet; it only tests
delimiter parsing.
