use super::{DiffKind, InlineSpan, InlineStyle};
use std::sync::OnceLock;
use syntect::easy::HighlightLines;
use syntect::highlighting::{Style, ThemeSet};
use syntect::parsing::SyntaxSet;

struct SyntectState {
    syntax_set: SyntaxSet,
    theme_dark: syntect::highlighting::Theme,
    theme_light: syntect::highlighting::Theme,
}

fn state() -> &'static SyntectState {
    static INSTANCE: OnceLock<SyntectState> = OnceLock::new();
    INSTANCE.get_or_init(|| {
        let syntax_set = SyntaxSet::load_defaults_newlines();
        let mut themes = ThemeSet::load_defaults().themes;
        let theme_dark = themes
            .remove("base16-ocean.dark")
            .expect("built-in dark theme must exist");
        let theme_light = themes
            .remove("InspiredGitHub")
            .expect("built-in light theme must exist");
        SyntectState {
            syntax_set,
            theme_dark,
            theme_light,
        }
    })
}

pub(crate) fn highlight_code_block(
    lines: &[(&str, DiffKind)],
    lang: &str,
    light: bool,
) -> Vec<(Vec<InlineSpan>, DiffKind)> {
    let st = state();
    let syntax = match st.syntax_set.find_syntax_by_token(lang) {
        Some(s) => s,
        None => return fallback(lines),
    };
    let theme = if light {
        &st.theme_light
    } else {
        &st.theme_dark
    };
    let mut h = HighlightLines::new(syntax, theme);
    let mut result = Vec::with_capacity(lines.len());
    for &(text, diff) in lines {
        if diff != DiffKind::None {
            result.push((
                vec![InlineSpan {
                    text: text.to_string(),
                    style: InlineStyle::Code,
                }],
                diff,
            ));
            continue;
        }
        let line_with_nl = format!("{}\n", text);
        let spans = match h.highlight_line(&line_with_nl, &st.syntax_set) {
            Ok(regions) => regions_to_spans(&regions),
            Err(_) => vec![InlineSpan {
                text: text.to_string(),
                style: InlineStyle::Code,
            }],
        };
        result.push((spans, diff));
    }
    result
}

fn regions_to_spans(regions: &[(Style, &str)]) -> Vec<InlineSpan> {
    let mut spans = Vec::with_capacity(regions.len());
    for &(style, text) in regions {
        let trimmed = text.trim_end_matches('\n');
        if trimmed.is_empty() {
            continue;
        }
        spans.push(InlineSpan {
            text: trimmed.to_string(),
            style: InlineStyle::Highlighted(
                style.foreground.r,
                style.foreground.g,
                style.foreground.b,
            ),
        });
    }
    if spans.is_empty() {
        spans.push(InlineSpan {
            text: String::new(),
            style: InlineStyle::Code,
        });
    }
    spans
}

fn fallback(lines: &[(&str, DiffKind)]) -> Vec<(Vec<InlineSpan>, DiffKind)> {
    lines
        .iter()
        .map(|&(text, diff)| {
            (
                vec![InlineSpan {
                    text: text.to_string(),
                    style: InlineStyle::Code,
                }],
                diff,
            )
        })
        .collect()
}
