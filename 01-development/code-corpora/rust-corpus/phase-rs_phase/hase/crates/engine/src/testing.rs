//! Test utilities for engine types.
//!
//! Provides [`assert_engine_eq!`] which uses JSON serialization for readable
//! assertion failure output, avoiding `Debug`-based stack overflows on deeply
//! nested types like `Effect` and `AbilityDefinition`.

/// Asserts that two values are equal, using `serde_json` pretty-printing for
/// the failure message instead of `Debug`. This avoids stack overflows on
/// mutually recursive engine types and produces more readable diffs.
///
/// Both values must implement `PartialEq` and `serde::Serialize`.
///
/// # Examples
///
/// ```ignore
/// use engine::assert_engine_eq;
///
/// let expected = Effect::Draw { count: QuantityExpr::Fixed(2) };
/// let actual = parse_effect("draw two cards");
/// assert_engine_eq!(actual, expected);
/// assert_engine_eq!(actual, expected, "parsing 'draw two cards'");
/// ```
#[macro_export]
macro_rules! assert_engine_eq {
    ($left:expr, $right:expr $(,)?) => {
        $crate::assert_engine_eq!($left, $right, "")
    };
    ($left:expr, $right:expr, $($arg:tt)+) => {
        match (&$left, &$right) {
            (left_val, right_val) => {
                if *left_val != *right_val {
                    let left_json = serde_json::to_string_pretty(left_val)
                        .unwrap_or_else(|e| format!("<serialization error: {e}>"));
                    let right_json = serde_json::to_string_pretty(right_val)
                        .unwrap_or_else(|e| format!("<serialization error: {e}>"));
                    panic!(
                        "assertion `left == right` failed: {}\n\nleft:\n{}\n\nright:\n{}",
                        format_args!($($arg)+),
                        left_json,
                        right_json,
                    );
                }
            }
        }
    };
}
