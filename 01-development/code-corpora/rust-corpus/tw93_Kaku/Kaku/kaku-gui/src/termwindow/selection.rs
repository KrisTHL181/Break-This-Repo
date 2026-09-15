use crate::selection::{Selection, SelectionCoordinate, SelectionMode, SelectionRange, SelectionX};
use ::window::WindowOps;
use mux::pane::{Pane, PaneId};
use std::cell::RefMut;
use std::sync::Arc;
use termwiz::surface::Line;
use wezterm_term::StableRowIndex;

fn clear_selection_after_successful_terminal_input(
    selection: &mut Selection,
    result: anyhow::Result<()>,
) -> anyhow::Result<bool> {
    result?;
    Ok(selection.clear())
}

impl super::TermWindow {
    pub fn selection(&self, pane_id: PaneId) -> RefMut<'_, Selection> {
        RefMut::map(self.pane_state(pane_id), |state| &mut state.selection)
    }

    /// Returns the selection region as a series of Line
    pub fn selection_lines(&self, pane: &Arc<dyn Pane>) -> Vec<Line> {
        let mut result = vec![];

        let rectangular = self.selection(pane.pane_id()).rectangular;
        if let Some(sel) = self
            .selection(pane.pane_id())
            .range
            .as_ref()
            .map(|r| r.normalize())
        {
            let mut last_was_wrapped = false;
            let first_row = sel.rows().start;
            let last_row = sel.rows().end;
            let term_cols = pane.get_dimensions().cols;
            let unwrap_tui = self.config.copy_unwrap_tui_lines;

            for line in pane.get_logical_lines(sel.rows()) {
                if result.is_empty() || !last_was_wrapped {
                    result.push(Line::with_width(0, line.physical_lines[0].current_seqno()));
                }
                let last_idx = line.physical_lines.len().saturating_sub(1);
                for (idx, phys) in line.physical_lines.iter().enumerate() {
                    let this_row = line.first_row + idx as StableRowIndex;
                    if this_row >= first_row && this_row < last_row {
                        let cols = sel.cols_for_row(this_row, rectangular);
                        let mut col_span = phys.columns_as_line(cols);
                        let seqno = col_span.current_seqno();
                        // Only trim trailing whitespace if we are the last line
                        // in a wrapped sequence
                        if idx == last_idx {
                            col_span.prune_trailing_blanks(seqno);
                        }

                        result
                            .last_mut()
                            .map(|line| line.append_line(col_span, seqno));

                        // Layer A: use the per-line wrap attribute so partial-width
                        // selections on a genuinely soft-wrapped row still suppress \n.
                        last_was_wrapped = phys.last_cell_was_wrapped();

                        // Layer B: heuristic for TUI self-wrapped full-width rows.
                        // Ratatui-style renderers (codex, cursor CLI) repaint via
                        // absolute cursor positioning and never set the DECAWM wrap
                        // attribute. phys.len() is also unreliable (lazy storage).
                        // Instead, find the rightmost non-space cell and check whether
                        // its right edge reaches the terminal width.
                        if !last_was_wrapped && unwrap_tui {
                            let last_content_right = phys
                                .visible_cells()
                                .filter(|c| c.str() != " ")
                                .last()
                                .map(|c| c.cell_index() + c.width())
                                .unwrap_or(0);
                            if last_content_right >= term_cols {
                                last_was_wrapped = true;
                            }
                        }
                    }
                }
            }
        }

        result
    }

    /// Returns the selection text only
    pub fn selection_text(&self, pane: &Arc<dyn Pane>) -> String {
        let mut s = String::new();
        let rectangular = self.selection(pane.pane_id()).rectangular;
        if let Some(sel) = self
            .selection(pane.pane_id())
            .range
            .as_ref()
            .map(|r| r.normalize())
        {
            let mut last_was_wrapped = false;
            let first_row = sel.rows().start;
            let last_row = sel.rows().end;
            let unwrap_tui = self.config.copy_unwrap_tui_lines;

            for line in pane.get_logical_lines(sel.rows()) {
                if !s.is_empty() && !last_was_wrapped {
                    s.push('\n');
                }
                let last_idx = line.physical_lines.len().saturating_sub(1);
                for (idx, phys) in line.physical_lines.iter().enumerate() {
                    let this_row = line.first_row + idx as StableRowIndex;
                    if this_row >= first_row && this_row < last_row {
                        let cols = sel.cols_for_row(this_row, rectangular);
                        let col_span = phys.columns_as_str(cols);

                        // When joining to the previous row, also trim trailing
                        // whitespace from the content we're about to add.
                        let joining = last_was_wrapped;
                        let content: &str = if idx == last_idx || joining {
                            col_span.trim_end()
                        } else {
                            &col_span
                        };

                        if joining && !s.is_empty() {
                            // Joining to previous row: normalize whitespace at
                            // the seam. If the continuation has leading spaces
                            // (Ink / CLI word-wrap indent), strip them and ensure
                            // a single-space separator. If no leading spaces
                            // (DECAWM mid-word wrap), append directly so no
                            // space is inserted inside a token.
                            if content.starts_with(' ') {
                                let trimmed = content.trim_start();
                                if !trimmed.is_empty() {
                                    if !s.ends_with(' ') {
                                        s.push(' ');
                                    }
                                    s.push_str(trimmed);
                                }
                            } else {
                                s.push_str(content);
                            }
                        } else {
                            s.push_str(content);
                        }

                        // Layer A: use the per-line wrap attribute so partial-width
                        // selections on a genuinely soft-wrapped row still suppress \n.
                        last_was_wrapped = phys.last_cell_was_wrapped();

                        // Layer B: when copy_unwrap_tui_lines is on, always treat
                        // this row as wrapped-to-next so the entire selection joins
                        // into one line. Covers Ratatui full-width boxes, Ink
                        // word-wrapped CLI output, and DECAWM-wrapped terminal text.
                        if !last_was_wrapped && unwrap_tui {
                            last_was_wrapped = true;
                        }
                    }
                }
            }
        }

        s
    }

    pub fn clear_selection(&self, pane: &Arc<dyn Pane>) {
        let mut selection = self.selection(pane.pane_id());
        let cleared = selection.clear();
        drop(selection);
        if cleared {
            if let Some(window) = self.window.as_ref() {
                window.invalidate();
            }
        }
    }

    pub(super) fn finish_terminal_input<E>(
        &self,
        pane: &Arc<dyn Pane>,
        result: Result<(), E>,
    ) -> anyhow::Result<()>
    where
        E: Into<anyhow::Error>,
    {
        let result = result.map_err(Into::into);

        // Per-pane overlays own their input and may share the underlying pane id.
        // Preserve the terminal selection while an overlay handles the event.
        if self.pane_state(pane.pane_id()).overlay.is_some() {
            return result;
        }

        let cleared = {
            let mut selection = self.selection(pane.pane_id());
            clear_selection_after_successful_terminal_input(&mut selection, result)?
        };
        if cleared {
            if let Some(window) = self.window.as_ref() {
                window.invalidate();
            }
        }
        Ok(())
    }

    pub fn extend_selection_at_mouse_cursor(&mut self, mode: SelectionMode, pane: &Arc<dyn Pane>) {
        let (position, y) = match self.pane_state(pane.pane_id()).mouse_terminal_coords {
            Some(coords) => coords,
            None => return,
        };
        let x = position.column;
        match mode {
            SelectionMode::Cell | SelectionMode::Block => {
                // Origin is the cell in which the selection action started. E.g. the cell
                // that had the mouse over it when the left mouse button was pressed
                let origin = self
                    .selection(pane.pane_id())
                    .origin
                    .unwrap_or(SelectionCoordinate::x_y(x, y));
                self.selection(pane.pane_id()).origin = Some(origin);
                self.selection(pane.pane_id()).rectangular = mode == SelectionMode::Block;

                // Compute the start and end horizontall cell of the selection.
                // The selection extent depends on the mouse cursor position in relation
                // to the origin.
                let (start_x, end_x) = if mode == SelectionMode::Block {
                    if x >= origin.x {
                        // If the selection is extending forwards from the origin,
                        // it includes the origin
                        (origin.x, SelectionX::Cell(x).saturating_sub(1))
                    } else {
                        // If the selection is extending backwards from the origin,
                        // it doesn't include the origin
                        (origin.x.saturating_sub(1), SelectionX::Cell(x))
                    }
                } else {
                    if (x >= origin.x && y == origin.y) || y > origin.y {
                        // If the selection is extending forwards from the origin, it includes the
                        // origin and doesn't include the cell under the cursor. Note that the
                        // reported cell here is offset by -50% from the real cell you see on the
                        // screen, so this causes a visual cell on the screen to be selected when
                        // the mouse moves over 50% of its width, which effectively means the next
                        // cell is being reported here, hence it's excluded
                        (origin.x, SelectionX::Cell(x).saturating_sub(1))
                    } else {
                        // If the selection is extending backwards from the origin, it doesn't
                        // include the origin and includes the cell under the cursor, which has
                        // the same effect as described above when going backwards
                        (origin.x.saturating_sub(1), SelectionX::Cell(x))
                    }
                };

                self.selection(pane.pane_id()).range =
                    if mode == SelectionMode::Block && origin.x == x {
                        // Ignore rectangle selections with a width of zero
                        None
                    } else if origin.x != x || origin.y != y {
                        // Only considers a selection if the cursor moved from the origin point
                        Some(
                            SelectionRange::start(SelectionCoordinate {
                                x: start_x,
                                y: origin.y,
                            })
                            .extend(SelectionCoordinate { x: end_x, y }),
                        )
                    } else {
                        None
                    };
            }
            SelectionMode::Word => {
                let end_word = SelectionRange::word_around(SelectionCoordinate::x_y(x, y), &**pane);

                let start_coord = self
                    .selection(pane.pane_id())
                    .origin
                    .clone()
                    .unwrap_or(end_word.start);
                let start_word = SelectionRange::word_around(start_coord, &**pane);

                let selection_range = start_word.extend_with(end_word);
                self.selection(pane.pane_id()).range = Some(selection_range);
                self.selection(pane.pane_id()).rectangular = false;
            }
            SelectionMode::Line => {
                let end_line = SelectionRange::line_around(SelectionCoordinate::x_y(x, y), &**pane);

                let start_coord = self
                    .selection(pane.pane_id())
                    .origin
                    .clone()
                    .unwrap_or(end_line.start);
                let start_line = SelectionRange::line_around(start_coord, &**pane);

                let selection_range = start_line.extend_with(end_line);
                self.selection(pane.pane_id()).range = Some(selection_range);
                self.selection(pane.pane_id()).rectangular = false;
            }
            SelectionMode::SemanticZone => {
                let end_word = SelectionRange::zone_around(SelectionCoordinate::x_y(x, y), &**pane);

                let start_coord = self
                    .selection(pane.pane_id())
                    .origin
                    .clone()
                    .unwrap_or(end_word.start);
                let start_word = SelectionRange::zone_around(start_coord, &**pane);

                let selection_range = start_word.extend_with(end_word);
                self.selection(pane.pane_id()).range = Some(selection_range);
                self.selection(pane.pane_id()).rectangular = false;
            }
        }

        let dims = pane.get_dimensions();

        // Scroll viewport when mouse mouves out of its vertical bounds
        if position.row == 0 && position.y_pixel_offset < 0 {
            self.set_viewport(pane.pane_id(), Some(y.saturating_sub(1)), dims);
        } else if position.row >= dims.viewport_rows as i64 {
            let top = self.effective_viewport(pane).unwrap_or(dims.physical_top);
            self.set_viewport(pane.pane_id(), Some(top + 1), dims);
        }

        self.window.as_ref().unwrap().invalidate();
    }

    pub fn select_text_at_mouse_cursor(&mut self, mode: SelectionMode, pane: &Arc<dyn Pane>) {
        let (x, y) = match self.pane_state(pane.pane_id()).mouse_terminal_coords {
            Some(coords) => (coords.0.column, coords.1),
            None => return,
        };
        match mode {
            SelectionMode::Line => {
                let start = SelectionCoordinate::x_y(x, y);
                let selection_range = SelectionRange::line_around(start, &**pane);

                self.selection(pane.pane_id()).origin = Some(start);
                self.selection(pane.pane_id()).range = Some(selection_range);
                self.selection(pane.pane_id()).rectangular = false;
            }
            SelectionMode::Word => {
                let selection_range =
                    SelectionRange::word_around(SelectionCoordinate::x_y(x, y), &**pane);

                self.selection(pane.pane_id()).origin = Some(selection_range.start);
                self.selection(pane.pane_id()).range = Some(selection_range);
                self.selection(pane.pane_id()).rectangular = false;
            }
            SelectionMode::SemanticZone => {
                let selection_range =
                    SelectionRange::zone_around(SelectionCoordinate::x_y(x, y), &**pane);

                self.selection(pane.pane_id()).origin = Some(selection_range.start);
                self.selection(pane.pane_id()).range = Some(selection_range);
                self.selection(pane.pane_id()).rectangular = false;
            }
            SelectionMode::Cell | SelectionMode::Block => {
                self.selection(pane.pane_id())
                    .begin(SelectionCoordinate::x_y(x, y));
                self.selection(pane.pane_id()).rectangular = mode == SelectionMode::Block;
            }
        }

        self.window.as_ref().unwrap().invalidate();
    }
}

#[cfg(test)]
mod tests {
    use super::clear_selection_after_successful_terminal_input;
    use crate::selection::{Selection, SelectionCoordinate, SelectionRange};

    fn active_selection() -> Selection {
        let start = SelectionCoordinate::x_y(2, 3);
        Selection {
            origin: Some(start),
            range: Some(SelectionRange::start(start)),
            rectangular: false,
        }
    }

    #[test]
    fn successful_terminal_input_clears_selection() {
        let mut selection = active_selection();

        let cleared = clear_selection_after_successful_terminal_input(&mut selection, Ok(()))
            .expect("successful input should clear the selection");

        assert!(cleared);
        assert!(selection.is_empty());
        assert!(selection.origin.is_none());
    }

    #[test]
    fn failed_terminal_input_preserves_selection() {
        let mut selection = active_selection();

        assert!(clear_selection_after_successful_terminal_input(
            &mut selection,
            Err(anyhow::anyhow!("input failed")),
        )
        .is_err());

        assert!(!selection.is_empty());
        assert!(selection.origin.is_some());
    }
}
