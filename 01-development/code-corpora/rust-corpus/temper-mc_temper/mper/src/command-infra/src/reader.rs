use crate::ParseError;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Checkpoint {
    cursor: usize,
}

#[derive(Clone, Copy, Debug)]
pub enum StringSpan<'a> {
    Bare(&'a str),
    Quoted(&'a str),
}

pub struct CommandReader<'a> {
    input: &'a str,
    cursor: usize,
}

impl<'a> CommandReader<'a> {
    pub fn new(input: &'a str) -> Self {
        Self { input, cursor: 0 }
    }

    pub fn input(&self) -> &'a str {
        self.input
    }

    pub fn cursor(&self) -> usize {
        self.cursor
    }

    pub fn remaining(&self) -> &'a str {
        &self.input[self.cursor..]
    }

    pub fn checkpoint(&self) -> Checkpoint {
        Checkpoint {
            cursor: self.cursor,
        }
    }

    pub fn rewind(&mut self, checkpoint: Checkpoint) {
        self.cursor = checkpoint.cursor;
    }

    pub fn has_remaining(&self) -> bool {
        self.cursor < self.input.len()
    }

    pub fn peek(&self) -> Option<char> {
        self.remaining().chars().next()
    }

    pub fn read_char(&mut self) -> Option<char> {
        let c = self.peek()?;
        self.cursor += c.len_utf8();
        Some(c)
    }

    pub fn skip_whitespace(&mut self) {
        while self.peek().is_some_and(char::is_whitespace) {
            self.read_char();
        }
    }

    pub fn read_word_span(&mut self) -> Result<&'a str, ParseError> {
        self.skip_whitespace();
        let start = self.cursor;

        while self.peek().is_some_and(|c| !c.is_whitespace()) {
            self.read_char();
        }

        if start == self.cursor {
            return Err(ParseError::expected(start, "word"));
        }

        Ok(&self.input[start..self.cursor])
    }

    pub fn read_quoted_string_span(&mut self) -> Result<&'a str, ParseError> {
        self.skip_whitespace();
        let quote_cursor = self.cursor;

        if self.read_char() != Some('"') {
            return Err(ParseError::expected(quote_cursor, "quoted string"));
        }

        let start = self.cursor;
        let mut escaped = false;

        while let Some(c) = self.read_char() {
            if escaped {
                escaped = false;
                continue;
            }

            match c {
                '\\' => escaped = true,
                '"' => {
                    let end = self.cursor - c.len_utf8();
                    return Ok(&self.input[start..end]);
                }
                _ => {}
            }
        }

        Err(ParseError::new(
            quote_cursor,
            "closing quote",
            "unterminated quoted string",
        ))
    }

    pub fn read_string_span(&mut self) -> Result<StringSpan<'a>, ParseError> {
        self.skip_whitespace();
        if self.peek() == Some('"') {
            self.read_quoted_string_span().map(StringSpan::Quoted)
        } else {
            self.read_word_span().map(StringSpan::Bare)
        }
    }

    pub fn read_remaining_span(&mut self) -> Result<&'a str, ParseError> {
        self.skip_whitespace();
        let start = self.cursor;
        self.cursor = self.input.len();

        if start == self.cursor {
            return Err(ParseError::expected(start, "remaining input"));
        }

        Ok(&self.input[start..])
    }

    pub fn expect_end(&mut self) -> Result<(), ParseError> {
        self.skip_whitespace();
        if self.has_remaining() {
            Err(ParseError::new(
                self.cursor,
                "end of command",
                "unexpected trailing input",
            ))
        } else {
            Ok(())
        }
    }
}
