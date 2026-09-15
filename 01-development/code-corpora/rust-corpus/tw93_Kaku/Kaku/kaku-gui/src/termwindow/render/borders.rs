use crate::quad::TripleLayerQuadAllocator;
use crate::utilsprites::RenderMetrics;
use ::window::ULength;
use config::{Config, ConfigHandle, Dimension, DimensionContext, TabBarColors};
use window::color::LinearRgba;

const INTEGRATED_BUTTONS_TOP_TAB_INSET_POINTS: f32 = 3.0;
const INTEGRATED_BUTTONS_TOP_CLEARANCE_POINTS: f32 = 16.0;

fn integrated_buttons_top_inset_pixels(dpi: f32, top_tab_bar_visible: bool) -> usize {
    let points = if top_tab_bar_visible {
        INTEGRATED_BUTTONS_TOP_TAB_INSET_POINTS
    } else {
        INTEGRATED_BUTTONS_TOP_CLEARANCE_POINTS
    };
    Dimension::Points(points).evaluate_as_pixels(DimensionContext {
        dpi,
        pixel_max: 0.0,
        pixel_cell: 0.0,
    }) as usize
}

pub(crate) fn integrated_buttons_top_inset(
    config: &Config,
    is_fullscreen: bool,
    top_tab_bar_visible: bool,
    dpi: f32,
) -> usize {
    if !is_fullscreen
        && config
            .window_decorations
            .contains(::window::WindowDecorations::INTEGRATED_BUTTONS)
    {
        integrated_buttons_top_inset_pixels(dpi, top_tab_bar_visible)
    } else {
        0
    }
}

fn integrated_buttons_top_background(
    config: &ConfigHandle,
    non_fancy_top_tab_bar_visible: bool,
    pane_background: LinearRgba,
) -> LinearRgba {
    if non_fancy_top_tab_bar_visible && config.window_background_opacity == 1.0 {
        config
            .resolved_palette
            .tab_bar
            .as_ref()
            .cloned()
            .unwrap_or_else(TabBarColors::default)
            .background()
            .to_linear()
    } else {
        pane_background.mul_alpha(config.window_background_opacity)
    }
}

impl crate::TermWindow {
    pub fn paint_window_borders(
        &mut self,
        layers: &mut TripleLayerQuadAllocator,
    ) -> anyhow::Result<()> {
        let is_fullscreen = self.layout_is_effective_fullscreen();
        // Keep border geometry consistent with pane layout.
        // In fullscreen we still need user window_frame border widths;
        // OS border (eg: notch safe-area) is merged by get_os_border().
        let border_dimensions = self.get_os_border();
        let fullscreen_border_color = border_dimensions.color;

        if border_dimensions.top.get() > 0
            || border_dimensions.bottom.get() > 0
            || border_dimensions.left.get() > 0
            || border_dimensions.right.get() > 0
        {
            let height = self.dimensions.pixel_height as f32;
            let width = self.dimensions.pixel_width as f32;
            let top_tab_bar_visible = self.show_tab_bar && !self.config.tab_bar_at_bottom;
            let integrated_top_inset = integrated_buttons_top_inset(
                &self.config,
                is_fullscreen,
                top_tab_bar_visible,
                self.dimensions.dpi as f32,
            )
            .min(border_dimensions.top.get()) as f32;

            // In fullscreen, use palette background color for all borders.
            // In windowed mode, use configured border colors if available.
            let border_color = |default: LinearRgba| -> LinearRgba {
                if is_fullscreen {
                    fullscreen_border_color
                } else {
                    default
                }
            };

            let border_top = border_dimensions.top.get() as f32;
            if border_top > 0.0 {
                if integrated_top_inset > 0.0 {
                    let pane_background = self
                        .get_active_pane_or_overlay()
                        .map(|pane| pane.palette().background)
                        .unwrap_or_else(|| self.palette().background)
                        .to_linear();
                    let background = integrated_buttons_top_background(
                        &self.config,
                        top_tab_bar_visible && !self.config.use_fancy_tab_bar,
                        pane_background,
                    );
                    self.filled_rectangle(
                        layers,
                        1,
                        euclid::rect(0.0, 0.0, width, border_top),
                        background,
                    )?;
                } else {
                    let color = border_color(
                        self.config
                            .window_frame
                            .border_top_color
                            .map(|c| c.to_linear())
                            .unwrap_or(border_dimensions.color),
                    );
                    self.filled_rectangle(
                        layers,
                        1,
                        euclid::rect(0.0, 0.0, width, border_top),
                        color,
                    )?;
                }
            }

            let border_left = border_dimensions.left.get() as f32;
            if border_left > 0.0 {
                let color = border_color(
                    self.config
                        .window_frame
                        .border_left_color
                        .map(|c| c.to_linear())
                        .unwrap_or(border_dimensions.color),
                );
                self.filled_rectangle(
                    layers,
                    1,
                    euclid::rect(0.0, 0.0, border_left, height),
                    color,
                )?;
            }

            let border_bottom = border_dimensions.bottom.get() as f32;
            if border_bottom > 0.0 {
                let color = border_color(
                    self.config
                        .window_frame
                        .border_bottom_color
                        .map(|c| c.to_linear())
                        .unwrap_or(border_dimensions.color),
                );
                self.filled_rectangle(
                    layers,
                    1,
                    euclid::rect(0.0, height - border_bottom, width, height),
                    color,
                )?;
            }

            let border_right = border_dimensions.right.get() as f32;
            if border_right > 0.0 {
                let color = border_color(
                    self.config
                        .window_frame
                        .border_right_color
                        .map(|c| c.to_linear())
                        .unwrap_or(border_dimensions.color),
                );
                self.filled_rectangle(
                    layers,
                    1,
                    euclid::rect(width - border_right, 0.0, border_right, height),
                    color,
                )?;
            }
        }

        // macOS simple fullscreen can occasionally show a 1px seam at the
        // window edge due to compositor rounding. Cover edges explicitly.
        let is_simple_fullscreen_with_notch_padding = is_fullscreen
            && self
                .os_parameters
                .as_ref()
                .and_then(|p| p.border_dimensions.as_ref())
                .map(|b| {
                    b.top.get() > 0 || b.left.get() > 0 || b.right.get() > 0 || b.bottom.get() > 0
                })
                .unwrap_or(false);

        if is_simple_fullscreen_with_notch_padding {
            let height = self.dimensions.pixel_height as f32;
            let width = self.dimensions.pixel_width as f32;
            let edge = 1.0f32;

            if width > 0.0 && height > 0.0 {
                self.filled_rectangle(
                    layers,
                    1,
                    euclid::rect(0.0, 0.0, width, edge),
                    fullscreen_border_color,
                )?;
                self.filled_rectangle(
                    layers,
                    1,
                    euclid::rect(0.0, (height - edge).max(0.0), width, edge),
                    fullscreen_border_color,
                )?;
                self.filled_rectangle(
                    layers,
                    1,
                    euclid::rect(0.0, 0.0, edge, height),
                    fullscreen_border_color,
                )?;
                self.filled_rectangle(
                    layers,
                    1,
                    euclid::rect((width - edge).max(0.0), 0.0, edge, height),
                    fullscreen_border_color,
                )?;
            }
        }

        Ok(())
    }

    pub fn get_os_border_impl(
        os_parameters: &Option<window::parameters::Parameters>,
        config: &ConfigHandle,
        dimensions: &crate::Dimensions,
        render_metrics: &RenderMetrics,
    ) -> window::parameters::Border {
        let mut border = os_parameters
            .as_ref()
            .and_then(|p| p.border_dimensions.clone())
            .unwrap_or_default();

        border.left += ULength::new(
            config
                .window_frame
                .border_left_width
                .evaluate_as_pixels(DimensionContext {
                    dpi: dimensions.dpi as f32,
                    pixel_max: dimensions.pixel_width as f32,
                    pixel_cell: render_metrics.cell_size.width as f32,
                })
                .ceil() as usize,
        );
        border.right += ULength::new(
            config
                .window_frame
                .border_right_width
                .evaluate_as_pixels(DimensionContext {
                    dpi: dimensions.dpi as f32,
                    pixel_max: dimensions.pixel_width as f32,
                    pixel_cell: render_metrics.cell_size.width as f32,
                })
                .ceil() as usize,
        );
        border.top += ULength::new(
            config
                .window_frame
                .border_top_height
                .evaluate_as_pixels(DimensionContext {
                    dpi: dimensions.dpi as f32,
                    pixel_max: dimensions.pixel_height as f32,
                    pixel_cell: render_metrics.cell_size.height as f32,
                })
                .ceil() as usize,
        );
        border.bottom += ULength::new(
            config
                .window_frame
                .border_bottom_height
                .evaluate_as_pixels(DimensionContext {
                    dpi: dimensions.dpi as f32,
                    pixel_max: dimensions.pixel_height as f32,
                    pixel_cell: render_metrics.cell_size.height as f32,
                })
                .ceil() as usize,
        );

        border
    }

    pub fn get_os_border(&self) -> window::parameters::Border {
        let mut border = Self::get_os_border_impl(
            &self.os_parameters,
            &self.config,
            &self.dimensions,
            &self.render_metrics,
        );

        let is_fullscreen = self.layout_is_effective_fullscreen();
        let extra_top = integrated_buttons_top_inset(
            &self.config,
            is_fullscreen,
            self.show_tab_bar && !self.config.tab_bar_at_bottom,
            self.dimensions.dpi as f32,
        );
        if extra_top > 0 {
            border.top += ULength::new(extra_top);
        }

        border
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn integrated_buttons_top_inset_is_state_sensitive() {
        let mut config = Config::default_config();
        config.window_decorations =
            ::window::WindowDecorations::INTEGRATED_BUTTONS | ::window::WindowDecorations::RESIZE;

        assert_eq!(integrated_buttons_top_inset(&config, false, true, 144.0), 6);
        assert_eq!(
            integrated_buttons_top_inset(&config, false, false, 144.0),
            32
        );
        assert_eq!(integrated_buttons_top_inset(&config, true, true, 144.0), 0);

        config.window_decorations =
            ::window::WindowDecorations::TITLE | ::window::WindowDecorations::RESIZE;
        assert_eq!(integrated_buttons_top_inset(&config, false, true, 144.0), 0);
    }

    #[test]
    fn integrated_buttons_top_inset_scales_with_dpi() {
        assert_eq!(integrated_buttons_top_inset_pixels(72.0, true), 3);
        assert_eq!(integrated_buttons_top_inset_pixels(144.0, true), 6);
        assert_eq!(integrated_buttons_top_inset_pixels(72.0, false), 16);
        assert_eq!(integrated_buttons_top_inset_pixels(110.0, false), 24);
        assert_eq!(integrated_buttons_top_inset_pixels(144.0, false), 32);
    }

    /// The bundled padding is expressed in device pixels on purpose. Switching
    /// it to points doubles the gutter on a 2x display, which is the display
    /// the spacing was tuned on.
    #[test]
    fn bundled_padding_stays_in_device_pixels() {
        let bundled_config =
            include_str!("../../../../assets/macos/Kaku.app/Contents/Resources/kaku.lua");

        assert!(bundled_config
            .contains("return { left = '26px', right = '26px', top = '26px', bottom = '0px' }"));
        assert!(bundled_config
            .contains("return { left = '40px', right = '40px', top = '40px', bottom = '0px' }"));
    }

    #[test]
    fn opaque_non_fancy_top_tab_inset_matches_tab_bar_background() {
        let config = ConfigHandle::default_config();
        let pane_background = LinearRgba::with_components(0.1, 0.2, 0.3, 1.0);
        let expected = config
            .resolved_palette
            .tab_bar
            .as_ref()
            .cloned()
            .unwrap_or_else(TabBarColors::default)
            .background()
            .to_linear();

        assert_eq!(
            integrated_buttons_top_background(&config, true, pane_background),
            expected
        );
        assert_eq!(
            integrated_buttons_top_background(&config, false, pane_background),
            pane_background
        );
    }
}
