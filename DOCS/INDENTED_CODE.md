# Indented-code fixture

The four-space block below should be parsed as code, while the final sentence
is ordinary prose.

    meow = "plain text inside an indented block"
    print(meow)

No tool executes these lines. The fixture only compares legacy indented-code
parsing with fenced-code parsing.
