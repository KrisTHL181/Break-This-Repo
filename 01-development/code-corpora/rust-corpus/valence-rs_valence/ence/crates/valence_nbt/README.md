# `valence_nbt`

A library for encoding and decoding Minecraft's [Named Binary Tag] (NBT)
format.

[Named Binary Tag]: https://minecraft.wiki/w/NBT_format

# Features

- `binary`: Serialize and deserialize in Java edition's binary format.
- `snbt`: Serialize and deserialize in "stringified" format.
- `preserve_order`: Preserve the order of fields in `Compound`s during insertion and deletion. The iterators on `Compound` then implement `DoubleEndedIterator`.
- `serde`: Adds support for [`serde`](https://docs.rs/serde/latest/serde/).
- `java_string`: Adds support for Java-compatible strings via the `java_string` crate.
- `valence_ident`: Adds compatibility with the `valence_ident` crate.
