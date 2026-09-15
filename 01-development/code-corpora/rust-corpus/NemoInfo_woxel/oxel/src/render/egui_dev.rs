use std::{fs, time::Instant};

use crate::scene::Scene;
use cgmath::Point3;
use egui::{ClippedPrimitive, Color32, ComboBox, FontId, RichText, Slider, TexturesDelta, Vec2};
use egui_plot::{Bar, BarChart, Plot};
use egui_wgpu_backend::ScreenDescriptor;
use instant::Duration;
use winit::window::Window;

#[derive(PartialEq, Clone, Copy)]
pub enum RenderMode {
    Gray,
    Rgb,
    Ray,
    Diffuse,
    Glossy,
}

impl RenderMode {
    fn text(&self) -> String {
        String::from(match &self {
            Self::Gray => "Gray",
            Self::Rgb => "Rgb",
            Self::Ray => "Ray",
            Self::Diffuse => "Diffuse",
            Self::Glossy => "Glossy",
        })
    }

    fn rich_text(&self) -> RichText {
        RichText::new(self.text()).font(FontId::proportional(15.0))
    }
}

const FPS_UPDATE_INTERVAL: Duration = Duration::from_secs(1);
pub struct EguiDev {
    pub platform: egui_winit_platform::Platform,
    pub selected_model: usize,
    pub render_mode: RenderMode,
    pub show_grid: [bool; 3],
    pub models: Vec<VdbFile>,
    pub sun_settings: SunSettings,
    pub recording: bool,
    pub recording_file: String,
    pub show: bool,
    last_fps_update: Instant,
    time_last_frame: Instant,
    past_fps: Vec<f32>,
    current_fps: f32,
}

impl EguiDev {
    pub fn new(platform: egui_winit_platform::Platform) -> Self {
        Self {
            platform,
            selected_model: 0,
            models: get_available_vdbs(),
            render_mode: RenderMode::Diffuse,
            show_grid: [false; 3],
            sun_settings: SunSettings::default(),
            last_fps_update: Instant::now(),
            time_last_frame: Instant::now(),
            current_fps: 0.,
            past_fps: vec![0.0; 200],
            recording: false,
            recording_file: "output.mp4".to_string(),
            show: true,
        }
    }

    pub fn get_frame(
        &mut self,
        scene: &Scene,
        window: &Window,
    ) -> (
        TexturesDelta,
        Vec<ClippedPrimitive>,
        ScreenDescriptor,
        bool,
        bool,
        bool,
    ) {
        self.update_fps();

        let screen_descriptor = ScreenDescriptor {
            physical_width: window.inner_size().width,
            physical_height: window.inner_size().height,
            scale_factor: window.scale_factor() as f32,
        };

        self.platform.begin_frame();
        self.platform
            .context()
            .set_cursor_icon(match scene.state.cursor_grabbed {
                false => egui::CursorIcon::Default,
                true => egui::CursorIcon::None,
            });

        let mut model_changed = false;
        let mut reload_shaders = false;
        let mut recording_changed = false;
        egui::Window::new("Developer tools")
            .title_bar(false)
            .resizable(true)
            .show(&self.platform.context(), |ui| {
                ui.label(
                    RichText::new(format!("FPS: {:.0}", (self.current_fps + 20.0).min(165.0)))
                        .color(Color32::from_rgb(7, 173, 51))
                        .font(FontId::proportional(20.0)),
                );
                ui.horizontal(|ui| {
                    ComboBox::from_label(RichText::new("Model").font(FontId::proportional(15.0)))
                        .selected_text(
                            RichText::new(self.models[self.selected_model].name.clone())
                                .font(FontId::proportional(15.0)),
                        )
                        .width(150.0)
                        .show_ui(ui, |ui| {
                            for (id, model) in self.models.iter().enumerate() {
                                model_changed |= ui
                                    .selectable_value(
                                        &mut self.selected_model,
                                        id,
                                        RichText::new(&model.name).font(FontId::proportional(15.0)),
                                    )
                                    .clicked();
                            }
                        });
                });

                ui.label(
                    RichText::new(format!(
                        "Camera xyz: {:.2} {:.2} {:.2}",
                        scene.camera.eye.x, scene.camera.eye.y, scene.camera.eye.z
                    ))
                    .font(FontId::proportional(15.0)),
                );

                ui.label(
                    RichText::new(format!(
                        "Facing: {}",
                        self.facing(scene.camera.eye, scene.camera.target)
                    ))
                    .font(FontId::proportional(15.0))
                    .color(Color32::WHITE),
                );

                let chart = BarChart::new(
                    self.past_fps
                        .iter()
                        .enumerate()
                        .map(|(index, &time)| Bar::new(index as f64, time as f64 * 1000.))
                        .collect(),
                )
                .color(Color32::LIGHT_GREEN)
                .vertical();

                ui.label(RichText::new("dt histogram (μs)").font(FontId::proportional(15.0)));
                ui.vertical(|ui| {
                    ui.set_height(10.0);
                    Plot::new("dt (ms)")
                        .clamp_grid(true)
                        .y_axis_width(2)
                        .allow_zoom(false)
                        .allow_drag(false)
                        .include_y(20.0)
                        .show(ui, |plot_ui| plot_ui.bar_chart(chart));
                });

                ui.horizontal(|ui| {
                    ui.set_visible(false);
                    ui.label(
                        RichText::new("Camera xyz: -1000.00 -1000.00 -1000.00")
                            .font(FontId::proportional(15.0)),
                    );
                });

                ui.horizontal(|ui| {
                    ui.label(RichText::new("Render: ").font(FontId::proportional(15.0)));
                    ui.selectable_value(
                        &mut self.render_mode,
                        RenderMode::Gray,
                        RenderMode::Gray.rich_text(),
                    );
                    ui.selectable_value(
                        &mut self.render_mode,
                        RenderMode::Rgb,
                        RenderMode::Rgb.rich_text(),
                    );
                    ui.selectable_value(
                        &mut self.render_mode,
                        RenderMode::Ray,
                        RenderMode::Ray.rich_text(),
                    );
                    ui.selectable_value(
                        &mut self.render_mode,
                        RenderMode::Diffuse,
                        RenderMode::Diffuse.rich_text(),
                    );
                    ui.selectable_value(
                        &mut self.render_mode,
                        RenderMode::Glossy,
                        RenderMode::Glossy.rich_text(),
                    );
                });

                if self.render_mode != RenderMode::Diffuse {
                    ui.horizontal(|ui| {
                        ui.label(RichText::new("Show grid: ").font(FontId::proportional(15.0)));

                        ui.toggle_value(
                            &mut self.show_grid[0],
                            RichText::new("Node 3").font(FontId::proportional(15.0)),
                        );

                        ui.toggle_value(
                            &mut self.show_grid[1],
                            RichText::new("Node 4").font(FontId::proportional(15.0)),
                        );

                        ui.toggle_value(
                            &mut self.show_grid[2],
                            RichText::new("Node 5").font(FontId::proportional(15.0)),
                        );
                    });
                }

                reload_shaders = ui
                    .button(RichText::new("Reload shaders").font(FontId::proportional(15.0)))
                    .clicked();

                if self.render_mode == RenderMode::Diffuse {
                    self.sun_settings.get_frame(ui);
                }

                ui.collapsing(
                    RichText::new("Recording Menu").font(FontId::proportional(15.0)),
                    |ui| {
                        if !self.recording {
                            let response = ui.button(
                                RichText::new("Start Recording").font(FontId::proportional(15.0)),
                            );
                            recording_changed = response.clicked();
                            self.recording = response.clicked();
                        } else {
                            let response = ui.button(
                                RichText::new("End Recording").font(FontId::proportional(15.0)),
                            );
                            self.recording = !response.clicked();
                            recording_changed = response.clicked();
                        }
                        ui.text_edit_singleline(&mut self.recording_file);
                    },
                )
            });

        let full_output = self.platform.end_frame(Some(&window));
        let paint_jobs = self.platform.context().tessellate(full_output.shapes);
        let tdelta = full_output.textures_delta;

        (
            tdelta,
            paint_jobs,
            screen_descriptor,
            model_changed,
            reload_shaders,
            recording_changed,
        )
    }

    fn facing(&self, eye: Point3<f32>, target: Point3<f32>) -> String {
        let dir = target - eye;
        let adir = dir.map(|c| c.abs());
        let yzx = (adir.y, adir.z, adir.x).into();
        let zxy = (adir.z, adir.x, adir.y).into();
        let b1 = adir.zip(yzx, |u, v| u >= v);
        let b2 = adir.zip(zxy, |u, v| u >= v);
        let bl = b1.zip(b2, |u, v| u && v);

        let neg = ["-", " "];
        match bl.into() {
            (true, _, _) => format!("{}X", neg[(dir.x > 0.) as usize]),
            (_, true, _) => format!("{}Y", neg[(dir.y > 0.) as usize]),
            (_, _, true) => format!("{}Z", neg[(dir.z > 0.) as usize]),
            _ => unreachable!(),
        }
    }

    fn update_fps(&mut self) {
        let dt = self.time_last_frame.elapsed().as_secs_f32();
        self.past_fps.push(dt);
        self.past_fps.remove(0);
        let now = Instant::now();
        if now.duration_since(self.last_fps_update) > FPS_UPDATE_INTERVAL {
            self.last_fps_update = now;
            let avg_dt = self.past_fps.iter().sum::<f32>() / self.past_fps.len() as f32;
            self.current_fps = 1.0 / avg_dt;
        }

        self.time_last_frame = now;
    }
}

#[derive(Debug, Clone)]
pub struct VdbFile {
    pub name: String,
    pub path: String,
    pub grid: String,
}

fn get_available_vdbs() -> Vec<VdbFile> {
    let mut files = vec![];
    let path = "assets";

    for entry in fs::read_dir(path).unwrap() {
        let Ok(entry) = entry else { continue };
        let path = entry.path();

        if path.is_file() {
            let Some(extention) = path.extension() else {
                continue;
            };
            if extention != "vdb" {
                continue;
            }

            let Some(name) = path.file_stem() else {
                continue;
            };
            let Some(name) = name.to_str() else {
                continue;
            };

            files.push(VdbFile {
                name: name.to_string(),
                path: path.display().to_string(),
                grid: "ls_".to_string() + &name.to_string(),
            });
        }
    }

    files.sort_by(|x, y| x.name.cmp(&y.grid));

    files
}

#[derive(Debug)]
pub struct SunSettings {
    pub dir3: glam::Vec3,
    pub color: [f32; 3],
    pub intensity: f32,
}

impl Default for SunSettings {
    fn default() -> Self {
        Self {
            dir3: glam::Vec3 {
                x: 1.0,
                y: -1.0,
                z: 0.5,
            }
            .normalize(),
            color: [255. / 255., 210. / 255., 160. / 255.],
            intensity: 1.0,
        }
    }
}

impl SunSettings {
    fn get_frame(&mut self, ui: &mut egui::Ui) {
        ui.collapsing(
            RichText::new("Sunlight settings").font(FontId::proportional(15.0)),
            |ui| {
                ui.horizontal(|ui| {
                    ui.label(
                        RichText::new(format!("X: {:.2}", self.dir3.x))
                            .font(FontId::proportional(15.0))
                            .color(Color32::RED),
                    );
                    ui.label(
                        RichText::new(format!("Y: {:.2}", self.dir3.y))
                            .font(FontId::proportional(15.0))
                            .color(Color32::from_rgb(0, 136, 255)),
                    );
                    ui.label(
                        RichText::new(format!("Z: {:.2}", self.dir3.z))
                            .font(FontId::proportional(15.0))
                            .color(Color32::GREEN),
                    );
                });
                ui.horizontal(|ui| {
                    ui.vertical(|ui| {
                        ui.set_max_width(150.);
                        ui.set_min_height(150.);
                        ui.centered_and_justified(|ui| {
                            self.get_circle(ui);
                        });
                    });
                    ui.vertical_centered_justified(|ui| {
                        let prev_fg_color = ui.style().visuals.widgets.inactive.fg_stroke.color;
                        let prev_ac_color = ui.style().visuals.widgets.active.fg_stroke.color;
                        let prev_hv_color = ui.style().visuals.widgets.hovered.fg_stroke.color;

                        ui.style_mut().visuals.widgets.inactive.fg_stroke.color =
                            Color32::from_rgb(0, 136, 255);
                        ui.style_mut().visuals.widgets.active.fg_stroke.color =
                            Color32::from_rgb(20, 160, 255);
                        ui.style_mut().visuals.widgets.hovered.fg_stroke.color =
                            Color32::from_rgb(20, 160, 255);
                        let mut slider_val = -self.dir3.y;
                        ui.add_space(25.);
                        ui.add(
                            Slider::new(&mut slider_val, -1.0..=1.0)
                                .vertical()
                                .show_value(false),
                        )
                        .rect;
                        ui.style_mut().visuals.widgets.inactive.fg_stroke.color = prev_fg_color;
                        ui.style_mut().visuals.widgets.active.fg_stroke.color = prev_ac_color;
                        ui.style_mut().visuals.widgets.hovered.fg_stroke.color = prev_hv_color;
                        self.dir3.y = -slider_val;
                        self.dir3 = self.dir3.normalize()
                    });
                });

                ui.horizontal(|ui| {
                    ui.label(RichText::new("Intensity").font(FontId::proportional(15.0)));
                    ui.add(Slider::new(&mut self.intensity, 0.0..=50.0));
                });

                ui.horizontal(|ui| {
                    ui.label(RichText::new("Light color").font(FontId::proportional(15.0)));
                    ui.color_edit_button_rgb(&mut self.color);
                });
            },
        );
    }

    fn get_circle(&mut self, ui: &mut egui::Ui) {
        let size = 100.;
        let (response, painter) =
            ui.allocate_painter(Vec2::new(size, size), egui::Sense::click_and_drag());
        let rect = response.rect;
        let center = rect.center();
        let radius = rect.width().min(rect.height()) * 0.4;

        // Draw the sphere representation
        painter.circle(center, radius, egui::Color32::DARK_GRAY, egui::Stroke::NONE);

        if let Some(pointer_pos) = response.interact_pointer_pos() {
            let direction = pointer_pos - center;
            // Calculate the direction vector, but restrict it to the sphere's surface
            let dir2 = if direction.length() > radius {
                direction.normalized()
            } else {
                direction / radius
            };

            self.dir3.z = dir2.x;
            self.dir3.x = -dir2.y;
            self.dir3.y = self.dir3.y.signum()
                * (1.0 - self.dir3.x * self.dir3.x - self.dir3.z * self.dir3.z)
                    .abs()
                    .sqrt();

            assert!(self.dir3.is_normalized());
        }

        let dir2 = Vec2 {
            x: self.dir3.z * radius,
            y: -self.dir3.x * radius,
        };

        painter.arrow(
            center,
            dir2,
            egui::Stroke::new(2.0, egui::Color32::from_rgb(255, 200, 0)),
        );

        painter.arrow(center, Vec2::UP * 60., egui::Stroke::new(1.0, Color32::RED));
        painter.arrow(
            center,
            Vec2::RIGHT * 60.,
            egui::Stroke::new(1.0, Color32::GREEN),
        );
    }
}
