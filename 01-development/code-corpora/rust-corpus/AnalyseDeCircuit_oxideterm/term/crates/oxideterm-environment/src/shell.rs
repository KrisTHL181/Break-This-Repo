// Copyright (C) 2026 OxideTerm contributors.
// SPDX-License-Identifier: GPL-3.0-only

/// Quote one value for use as a single POSIX shell argument.
pub fn shell_quote(value: &str) -> String {
    if value.is_empty() {
        return "''".to_string();
    }
    format!("'{}'", value.replace('\'', "'\\''"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn posix_shell_quote_preserves_empty_space_and_apostrophe_values() {
        assert_eq!(shell_quote(""), "''");
        assert_eq!(shell_quote("Oxide Term"), "'Oxide Term'");
        assert_eq!(shell_quote("it's-ready"), "'it'\\''s-ready'");
    }
}
