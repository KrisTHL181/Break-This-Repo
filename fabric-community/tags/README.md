# Tags

This directory contains the tags that the bot will use. `.ytag` files contain tag data (to avoid the parser trying
to load unrelated files) - they're basically Markdown files containing a YAML front matter.

**Please note:** This system is currently in development, and may change significantly before release. This document
is correct as of the time of writing, but may need to be changed as development continues.

## Format

All tags are of the following format:

```markdown
# Front matter
type: text  # See below for other types

---

Discord-formatted Markdown content
```

We elected to use this format over something that GitHub is able to render for two reasons:

1. The YAML would look broken in a markdown-formatted file
2. The Markdown used in these files is Discord-formatted, which means:
    * Line breaks always split text onto the next line, even if there's only one
    * Quite a few formatting constructs (eg, tables) are missing or different on Discord

Please bear this in mind when writing tags. You can always create a Discord server and test your Markdown there before
submitting a pull request.

## Type: Alias

Alias tags simply refer to another tag. They exist to provide an easy way to give tags multiple names, without
accidentally creating tags with conflicting names.

Alias tags do not have Markdown content, and thus do not require the separator sequence (`---`).

### Front Matter

```yaml
type: alias

target: String  # The name of the target tag
```

## Type: Embed

For embed tags, Markdown content will be sent as part of a fully-formatted embed. Embeds can be customized
through the front matter.

### Front Matter

```yaml
type: embed

colour: String  # A common colour name, blurple, a hex code, or an integer representing an RGB colour - this overwrites the embed color field
embed:  # This key refers to the EmbedData object that Kord provides - some common fields are provided below.
    title: String  # Embed title
    
    fields:  # Individual headed fields within the embed
        - name: String
          value: String
          inline: Boolean  # Whether this field should allow other fields to be positioned next to it

        - name: String
          value: String
          inline: Boolean

    footer:  # Embed footer
        text: String
        iconUrl: String

    image:  # Large image at the bottom of the embed
        url: String

    thumbnail:  # Smaller image at the upper corner of the embed
        url: String

    author:
        name: String
        url: String
        iconUrl: String  # Very small, at the lower corner of the embed
```

## Type: Text

For text tags, Markdown content will be sent directly as a simple message on Discord, directly by the bot. There are no
front matter options.

### Front Matter

```yaml
type: text
```

## Substitutions

The Markdown body of all tags can contain substitutions. These are simply strings that are provided as part of the tag
invocation on Discord. For example, if we have a tag named `sub.ytag`:

```
type: text

---

First: {{0}}
Second: {{1}}
First again: {{0}}
```

You could make use of this by providing two extra arguments to the tag invocation:

```
!!sub first-argument second-argument
```

This would result in the following:

```md
First: first-argument
Second: second-argument
First again: first-argument
```

**Note:** Tags with substitutions cannot be invoked without providing arguments for all substitutions.
