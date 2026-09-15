use thiserror::Error;

#[derive(Clone, Debug, Error, Eq, PartialEq)]
#[error("{message} at cursor {cursor}")]
pub struct ParseError {
    pub cursor: usize,
    pub expected: &'static str,
    pub message: String,
}

impl ParseError {
    pub fn new(cursor: usize, expected: &'static str, message: impl Into<String>) -> Self {
        Self {
            cursor,
            expected,
            message: message.into(),
        }
    }

    pub fn expected(cursor: usize, expected: &'static str) -> Self {
        Self::new(cursor, expected, format!("expected {expected}"))
    }

    pub fn farthest(self, other: Self) -> Self {
        if other.cursor > self.cursor {
            other
        } else {
            self
        }
    }
}
