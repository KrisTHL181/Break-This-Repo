use mux::pane::Pane;
use mux::renderable::RenderableDimensions;
use std::time::Instant;
use wezterm_term::StableRowIndex;

/// Per-window scrollbar UI state. Bundled here so `TermWindow` keeps one
/// scrollbar field instead of several related timers and snapshots.
#[derive(Default)]
pub struct ScrollbarState {
    /// The render dimensions snapshot used to detect when the scrollbar
    /// needs a repaint.
    pub last_scroll_info: RenderableDimensions,
    /// True while the cursor is inside the scrollbar hover slop region.
    pub hovering: bool,
    /// When set, the scrollbar stays painted until this deadline (auto-hide
    /// fade-out timer driven by recent scroll activity).
    pub visible_until: Option<Instant>,
}

const MAX_SCROLLBAR_THUMB_WIDTH: usize = 11;
const MIN_SCROLLBAR_THUMB_WIDTH: usize = 8;
const MIN_SCROLLBAR_TRACK_INSET: usize = 8;
const MAX_SCROLLBAR_TRACK_INSET: usize = 12;
const MAX_SCROLLBAR_THUMB_RATIO: f32 = 0.16;
const SCROLLBAR_EDGE_INSET: usize = 3;
const SCROLLBAR_HOVER_SLOP: isize = 12;

pub struct ScrollHit {
    /// Offset from the top of the window in pixels
    pub top: usize,
    /// Height of the thumb, in pixels.
    pub height: usize,
}

pub struct ScrollbarTrack {
    pub x: usize,
    pub width: usize,
    pub top: usize,
    pub height: usize,
    pub thumb_x: usize,
    pub thumb_width: usize,
}

impl ScrollHit {
    /// Compute the y-coordinate for the top of the scrollbar thumb
    /// and the height of the thumb and return them.
    pub fn thumb(
        pane: &dyn Pane,
        viewport: Option<StableRowIndex>,
        max_thumb_height: usize,
        min_thumb_size: usize,
    ) -> Self {
        if max_thumb_height == 0 {
            return Self { top: 0, height: 0 };
        }

        let render_dims = pane.get_dimensions();

        let scroll_top = render_dims
            .physical_top
            .saturating_sub(viewport.unwrap_or(render_dims.physical_top))
            as f32;

        let scroll_size = render_dims.scrollback_rows.max(render_dims.viewport_rows) as f32;

        let thumb_size = (render_dims.viewport_rows as f32 / scroll_size) * max_thumb_height as f32;

        let min_thumb_size = min_thumb_size as f32;
        let thumb_size = (if thumb_size < min_thumb_size {
            min_thumb_size
        } else {
            thumb_size
        })
        .ceil() as usize;
        let max_visual_thumb_size =
            ((max_thumb_height as f32) * MAX_SCROLLBAR_THUMB_RATIO).ceil() as usize;
        let thumb_size = thumb_size.min(
            max_visual_thumb_size
                .max(min_thumb_size as usize)
                .min(max_thumb_height),
        );

        let scroll_extent = render_dims
            .physical_top
            .saturating_sub(render_dims.scrollback_top) as f32;
        if scroll_extent <= 0.0 || thumb_size >= max_thumb_height {
            return Self {
                top: 0,
                height: thumb_size,
            };
        }

        let scroll_percent = 1.0 - (scroll_top / scroll_extent).clamp(0.0, 1.0);
        let thumb_top =
            (scroll_percent * (max_thumb_height.saturating_sub(thumb_size)) as f32).ceil() as usize;

        Self {
            top: thumb_top,
            height: thumb_size,
        }
    }

    /// Given a new thumb top coordinate (produced by dragging the thumb),
    /// compute the equivalent viewport offset.
    pub fn thumb_top_to_scroll_top(
        thumb_top: usize,
        pane: &dyn Pane,
        viewport: Option<StableRowIndex>,
        max_thumb_height: usize,
        min_thumb_size: usize,
    ) -> StableRowIndex {
        let thumb = Self::thumb(pane, viewport, max_thumb_height, min_thumb_size);
        let available_height = max_thumb_height.saturating_sub(thumb.height);
        let render_dims = pane.get_dimensions();
        let scroll_extent = render_dims
            .physical_top
            .saturating_sub(render_dims.scrollback_top);

        if available_height == 0 || scroll_extent == 0 {
            return render_dims.scrollback_top;
        }

        let scroll_percent = thumb_top.min(available_height) as f32 / available_height as f32;

        render_dims
            .scrollback_top
            .saturating_add((scroll_extent as f32 * scroll_percent) as StableRowIndex)
    }
}

pub fn scrollbar_thumb_width(track_width: usize) -> usize {
    track_width
        .saturating_sub(5)
        .clamp(MIN_SCROLLBAR_THUMB_WIDTH, MAX_SCROLLBAR_THUMB_WIDTH)
        .min(track_width)
}

pub fn scrollbar_thumb_x(track_x: usize, track_width: usize, thumb_width: usize) -> usize {
    track_x + track_width.saturating_sub(thumb_width.saturating_add(SCROLLBAR_EDGE_INSET))
}

pub fn scrollbar_hover_hit(
    track_x: usize,
    track_top: usize,
    track_width: usize,
    track_height: usize,
    x: isize,
    y: isize,
) -> bool {
    x >= track_x as isize - SCROLLBAR_HOVER_SLOP
        && x <= (track_x + track_width) as isize + SCROLLBAR_HOVER_SLOP
        && y >= track_top as isize - SCROLLBAR_HOVER_SLOP
        && y <= (track_top + track_height) as isize + SCROLLBAR_HOVER_SLOP
}

pub fn scrollbar_track(
    window_width: usize,
    window_height: usize,
    track_top: usize,
    bottom_reserved: usize,
    cell_height: usize,
    track_width: usize,
    right_reserved: usize,
) -> ScrollbarTrack {
    let inset = (cell_height / 5).clamp(MIN_SCROLLBAR_TRACK_INSET, MAX_SCROLLBAR_TRACK_INSET);
    let top = track_top.saturating_add(inset);
    let height = window_height.saturating_sub(top + bottom_reserved + inset);
    let width = track_width;
    let x = window_width.saturating_sub(width.saturating_add(right_reserved));
    let thumb_width = scrollbar_thumb_width(width);
    let thumb_x = scrollbar_thumb_x(x, width, thumb_width);
    ScrollbarTrack {
        x,
        width,
        top,
        height,
        thumb_x,
        thumb_width,
    }
}

#[cfg(test)]
mod tests {
    use super::{
        scrollbar_hover_hit, scrollbar_thumb_width, scrollbar_thumb_x, scrollbar_track, ScrollHit,
    };

    #[test]
    fn thumb_width_is_capped_for_wide_tracks() {
        assert_eq!(scrollbar_thumb_width(12), 8);
        assert_eq!(scrollbar_thumb_width(16), 11);
        assert_eq!(scrollbar_thumb_width(20), 11);
    }

    #[test]
    fn track_adds_visual_inset() {
        let track = scrollbar_track(300, 400, 20, 10, 20, 16, 6);
        assert_eq!(track.x, 278);
        assert_eq!(track.width, 16);
        assert_eq!(track.top, 28);
        assert_eq!(track.height, 354);
        assert_eq!(track.thumb_width, 11);
        assert_eq!(track.thumb_x, 280);
    }

    #[test]
    fn thumb_sits_inside_right_edge_gap() {
        assert_eq!(scrollbar_thumb_x(100, 16, 11), 102);
    }

    #[test]
    fn hover_hit_allows_nearby_pointer() {
        assert!(scrollbar_hover_hit(100, 40, 16, 100, 96, 36));
        assert!(!scrollbar_hover_hit(100, 40, 16, 100, 70, 36));
    }

    #[test]
    fn zero_height_thumb_is_stable() {
        let thumb = ScrollHit { top: 0, height: 0 };
        assert_eq!(thumb.top, 0);
        assert_eq!(thumb.height, 0);
    }
}
