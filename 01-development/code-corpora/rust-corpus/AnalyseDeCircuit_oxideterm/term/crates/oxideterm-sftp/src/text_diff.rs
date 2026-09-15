// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

//! Line-oriented text comparison used by SFTP previews.

const MAX_LCS_CELLS: usize = 4_000_000;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum TextDiffLineKind {
    Unchanged,
    Added,
    Removed,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct TextDiffLine {
    pub kind: TextDiffLineKind,
    pub content: String,
    pub left_line_num: Option<usize>,
    pub right_line_num: Option<usize>,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct TextDiffStats {
    pub added: usize,
    pub removed: usize,
    pub unchanged: usize,
}

/// Computes a stable line diff using the same LCS tie-breaking as the legacy UI implementation.
pub fn compute_text_diff(left: &str, right: &str) -> Vec<TextDiffLine> {
    let left_lines = left.split('\n').collect::<Vec<_>>();
    let right_lines = right.split('\n').collect::<Vec<_>>();
    let left_len = left_lines.len();
    let right_len = right_lines.len();
    let leading_equal = left_lines
        .iter()
        .zip(&right_lines)
        .take_while(|(left_line, right_line)| left_line == right_line)
        .count();
    let trailing_equal = left_lines[leading_equal..]
        .iter()
        .rev()
        .zip(right_lines[leading_equal..].iter().rev())
        .take_while(|(left_line, right_line)| left_line == right_line)
        .count();
    let left_middle = &left_lines[leading_equal..left_len - trailing_equal];
    let right_middle = &right_lines[leading_equal..right_len - trailing_equal];
    let row_width = right_middle.len() + 1;
    let cell_count = (left_middle.len() + 1).saturating_mul(row_width);

    let mut lines = Vec::with_capacity(left_len.max(right_len));
    lines.extend((0..leading_equal).map(|index| TextDiffLine {
        kind: TextDiffLineKind::Unchanged,
        content: left_lines[index].to_string(),
        left_line_num: Some(index + 1),
        right_line_num: Some(index + 1),
    }));

    if cell_count > MAX_LCS_CELLS {
        // A coarse middle keeps preview memory bounded when unrelated files have huge line counts.
        lines.extend(
            left_middle
                .iter()
                .enumerate()
                .map(|(index, content)| TextDiffLine {
                    kind: TextDiffLineKind::Removed,
                    content: (*content).to_string(),
                    left_line_num: Some(leading_equal + index + 1),
                    right_line_num: None,
                }),
        );
        lines.extend(
            right_middle
                .iter()
                .enumerate()
                .map(|(index, content)| TextDiffLine {
                    kind: TextDiffLineKind::Added,
                    content: (*content).to_string(),
                    left_line_num: None,
                    right_line_num: Some(leading_equal + index + 1),
                }),
        );
    } else {
        let mut lcs_lengths = vec![0u32; cell_count];
        for left_index in 1..=left_middle.len() {
            for right_index in 1..=right_middle.len() {
                let cell = left_index * row_width + right_index;
                lcs_lengths[cell] = if left_middle[left_index - 1] == right_middle[right_index - 1]
                {
                    lcs_lengths[(left_index - 1) * row_width + right_index - 1] + 1
                } else {
                    lcs_lengths[(left_index - 1) * row_width + right_index]
                        .max(lcs_lengths[left_index * row_width + right_index - 1])
                };
            }
        }

        let mut left_index = left_middle.len();
        let mut right_index = right_middle.len();
        let middle_start = lines.len();
        while left_index > 0 || right_index > 0 {
            if left_index > 0
                && right_index > 0
                && left_middle[left_index - 1] == right_middle[right_index - 1]
            {
                lines.push(TextDiffLine {
                    kind: TextDiffLineKind::Unchanged,
                    content: left_middle[left_index - 1].to_string(),
                    left_line_num: Some(leading_equal + left_index),
                    right_line_num: Some(leading_equal + right_index),
                });
                left_index -= 1;
                right_index -= 1;
            } else if right_index > 0
                && (left_index == 0
                    || lcs_lengths[left_index * row_width + right_index - 1]
                        >= lcs_lengths[(left_index - 1) * row_width + right_index])
            {
                lines.push(TextDiffLine {
                    kind: TextDiffLineKind::Added,
                    content: right_middle[right_index - 1].to_string(),
                    left_line_num: None,
                    right_line_num: Some(leading_equal + right_index),
                });
                right_index -= 1;
            } else {
                lines.push(TextDiffLine {
                    kind: TextDiffLineKind::Removed,
                    content: left_middle[left_index - 1].to_string(),
                    left_line_num: Some(leading_equal + left_index),
                    right_line_num: None,
                });
                left_index -= 1;
            }
        }
        lines[middle_start..].reverse();
    }

    for offset in (0..trailing_equal).rev() {
        let left_index = left_len - offset;
        let right_index = right_len - offset;
        lines.push(TextDiffLine {
            kind: TextDiffLineKind::Unchanged,
            content: left_lines[left_index - 1].to_string(),
            left_line_num: Some(left_index),
            right_line_num: Some(right_index),
        });
    }
    lines
}

pub fn text_diff_stats(lines: &[TextDiffLine]) -> TextDiffStats {
    let mut stats = TextDiffStats::default();
    for line in lines {
        match line.kind {
            TextDiffLineKind::Unchanged => stats.unchanged += 1,
            TextDiffLineKind::Added => stats.added += 1,
            TextDiffLineKind::Removed => stats.removed += 1,
        }
    }
    stats
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn computes_stable_line_numbers_and_summary() {
        let lines = compute_text_diff("alpha\nbeta", "alpha\ngamma");

        assert_eq!(
            lines,
            vec![
                TextDiffLine {
                    kind: TextDiffLineKind::Unchanged,
                    content: "alpha".to_string(),
                    left_line_num: Some(1),
                    right_line_num: Some(1),
                },
                TextDiffLine {
                    kind: TextDiffLineKind::Removed,
                    content: "beta".to_string(),
                    left_line_num: Some(2),
                    right_line_num: None,
                },
                TextDiffLine {
                    kind: TextDiffLineKind::Added,
                    content: "gamma".to_string(),
                    left_line_num: None,
                    right_line_num: Some(2),
                },
            ]
        );
        assert_eq!(
            text_diff_stats(&lines),
            TextDiffStats {
                added: 1,
                removed: 1,
                unchanged: 1,
            }
        );
    }

    #[test]
    fn large_unrelated_inputs_use_bounded_coarse_diff() {
        let left = (0..2_001)
            .map(|index| format!("left-{index}"))
            .collect::<Vec<_>>()
            .join("\n");
        let right = (0..2_001)
            .map(|index| format!("right-{index}"))
            .collect::<Vec<_>>()
            .join("\n");

        let lines = compute_text_diff(&left, &right);

        assert_eq!(lines.len(), 4_002);
        assert!(
            lines[..2_001]
                .iter()
                .all(|line| line.kind == TextDiffLineKind::Removed)
        );
        assert!(
            lines[2_001..]
                .iter()
                .all(|line| line.kind == TextDiffLineKind::Added)
        );
    }
}
