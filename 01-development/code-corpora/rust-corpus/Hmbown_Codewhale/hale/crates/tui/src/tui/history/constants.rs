//! Shared constants for history transcript rendering.
//!
//! ## How the live tool-card budgets were chosen
//!
//! The caps below were measured, not guessed. The sample is 53 real saved
//! sessions from `~/.codewhale/sessions` — 5,470 tool results, 4,001 of them
//! `Bash` (the "run" cards) and 3,777 `Bash` commands.
//!
//! Observed `Bash` result length, in source lines:
//! `p25=3  p50=9  p75=25  p90=60  p95=113  max=1161`.
//!
//! Observed `Bash` command length: `p50=251 chars`, `p90=1404` — i.e. the
//! median command is multi-line once wrapped, not a one-liner.
//!
//! Each cap sits at the knee of its own coverage curve: the point past which
//! more rows buy very little more content. Going further chases a long tail
//! that a single card should never try to hold — that is what the details
//! pager is for.

/// Wrapped rows of the *command* echoed inside a live tool card.
///
/// Coverage of real `Bash` commands shown whole, at an 80-column terminal:
/// `3 → 45%`, `4 → 58%`, **`6 → 70%`**, `8 → 75%`, `10 → 77%`.
/// Six is the knee: +25 points over the old cap of 3, where 8 adds only 4
/// more and 10 only 2. At 3 the *median* command was clipped, which is the
/// "run cards never show enough" complaint at its source.
pub(super) const TOOL_COMMAND_LINE_LIMIT: usize = 6;

/// Wrapped rows of tool *output* shown in a live card before the details
/// affordance takes over.
///
/// Fraction of real `Bash` results shown whole: `8 → 50%`, `12 → 60%`,
/// `16 → 68%`, **`20 → 72%`**, `24 → 75%`, `32 → 80%`.
/// Twenty covers three quarters of real results while still leaving half of
/// a 40-row terminal for everything else; 24 buys under three points for
/// four more rows.
pub(super) const TOOL_OUTPUT_LINE_LIMIT: usize = 20;

/// Rows of output a *successful* live `run` card shows before the details
/// affordance takes over.
///
/// This used to be zero: success collapsed to the bare header, so a card told
/// you a command finished but nothing at all about what it produced. Against
/// the sampled corpus (3,465 `Bash` results with no error marker,
/// `p25=3 p50=8 p75=26`), a six-row preview shows ~45% of successful runs in
/// their entirety and the opening of the rest. Eight rows would reach ~51%,
/// but it spends two more rows on *every* successful card, and the transcript
/// now also spends a separator row between blocks. Failures are unaffected —
/// they keep the full `TOOL_OUTPUT_LINE_LIMIT` budget, because an error you
/// cannot read is the expensive one.
pub(super) const TOOL_SUCCESS_OUTPUT_PREVIEW_LINES: usize = 6;

pub(super) const TOOL_TEXT_LIMIT: usize = 300;

/// Characters of the summary shown after `·` in a tool-card header. Real
/// commands run far longer than any header (p50 = 251 chars), so this is a
/// glance budget, not a fit budget — the header line is width-clipped
/// downstream regardless. 72 keeps the header inside an 80-column terminal
/// while showing meaningfully more of the command on a wide one.
pub(super) const TOOL_HEADER_SUMMARY_LIMIT: usize = 72;

/// Contiguous rows taken from the start of a truncated output.
///
/// `p50` of a real `Bash` result is 9 source lines, so a 10-row head shows
/// the whole opening of a median result rather than a fragment of it.
pub(super) const TOOL_OUTPUT_HEAD_LINES: usize = 10;

/// Contiguous rows taken from the end of a truncated output — where exit
/// status, totals, and error summaries land. Head + tail = 16 of the 20-row
/// budget, leaving 4 rows for importance-ranked lines from the middle.
pub(super) const TOOL_OUTPUT_TAIL_LINES: usize = 6;
#[cfg(test)]
pub(super) const TOOL_RUNNING_SYMBOLS: [&str; 8] = crate::tui::spinner::BRAILLE_SPINNER_FRAMES;
#[cfg(test)]
pub(super) const TOOL_STATUS_SYMBOL_MS: u64 = crate::tui::spinner::BRAILLE_SPINNER_FRAME_MS;
/// Visual marker for the user role at the start of their message line. Solid
/// vertical bar — no animation; user input is a finished thing.
pub(super) const USER_GLYPH: &str = crate::tui::glyphs::USER;
/// Visual marker for the assistant role. Solid bullet that pulses at 2s
/// cycle while the response is streaming, holds full brightness when idle.
pub(super) const ASSISTANT_GLYPH: &str = crate::tui::glyphs::CURRENT;
/// Transcript body left rail. Solid 1/8 block (`▏`) followed by a space —
/// used as a visual left-margin anchor for continuation lines, tool-card
/// detail rows, and affordance lines. Dimmed so it guides the eye without
/// competing with content.
pub(super) const TRANSCRIPT_RAIL: &str = crate::tui::glyphs::TRANSCRIPT_RAIL;
/// Total rendered rows a non-failed tool card keeps when `show_tool_details`
/// is off — the shipped default, so this is the cap almost every user
/// actually sees.
///
/// It was an unnamed literal `2`: header plus a single row, then an "expand"
/// affordance. Three rows spent to learn that *something* ran. Every other
/// budget in this file was invisible underneath it. Six rows is a header, up
/// to four rows of real content, and the affordance — enough to answer "what
/// did that do?" without opening anything, and still a card rather than a
/// wall. Failures are excluded from this path entirely and keep their full
/// budget.
pub(super) const TOOL_SUMMARY_CARD_LINES: usize = 6;

/// Total rendered rows a non-failed tool card keeps in calm mode — also on by
/// default, and applied *after* the `show_tool_details` summary cap above.
///
/// It was 4, i.e. stricter than the summary cap, which inverted the two: a
/// user who turned tool details *on* while leaving calm mode alone saw fewer
/// rows than one who left both at their defaults. Calm mode is about quiet,
/// not about hiding, so it bounds the card at the header plus the full
/// successful-run preview plus the expand affordance.
pub(super) const TOOL_CARD_SUMMARY_LINES: usize = TOOL_SUCCESS_OUTPUT_PREVIEW_LINES + 2;
pub(super) const TOOL_DONE_SYMBOL: &str = crate::tui::glyphs::DONE;
pub(super) const TOOL_FAILED_SYMBOL: &str = crate::tui::glyphs::FAILED;
/// Compact Ctrl+B affordance for foreground shell waits in the live transcript.
pub(super) const FOREGROUND_SHELL_WAIT_HINT: &str = "Ctrl+B → /jobs";
