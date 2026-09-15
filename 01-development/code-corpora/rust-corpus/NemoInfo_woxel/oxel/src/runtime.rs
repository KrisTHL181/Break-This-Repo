use log::warn;
use winit::{
    dpi::PhysicalPosition,
    event::{ElementState, Event, KeyboardInput, MouseButton, VirtualKeyCode, WindowEvent},
    event_loop::{ControlFlow, EventLoopWindowTarget},
    window::Window,
};

use crate::{render::WgpuContext, scene::Scene};

pub struct Runtime {
    context: WgpuContext,
    window: Window,
    scene: Scene,
}

impl Runtime {
    pub fn new(context: WgpuContext, window: Window, scene: Scene) -> Self {
        Runtime {
            context,
            window,
            scene,
        }
    }

    pub fn main_loop<T>(
        &mut self,
        event: Event<()>,
        _target: &EventLoopWindowTarget<T>,
        control_flow: &mut ControlFlow,
    ) {
        if !self.scene.state.cursor_grabbed {
            // The ui should only capture events if we haven't grabbed the cursor, i.e. we are notin FPS mode.
            self.context.egui_dev.platform.handle_event(&event);
            if self.context.egui_dev.platform.captures_event(&event) {
                // This check if the event shouldn't be propagated onto the window
                return;
            }
        }

        match event {
            Event::WindowEvent {
                ref event,
                window_id,
            } if window_id == self.window.id() => {
                self.scene.input(&event);

                match event {
                    WindowEvent::CloseRequested
                    | WindowEvent::KeyboardInput {
                        input:
                            KeyboardInput {
                                state: ElementState::Pressed,
                                virtual_keycode: Some(VirtualKeyCode::Escape),
                                ..
                            },
                        ..
                    } => {
                        *control_flow = ControlFlow::Exit;
                        match self.context.frame_recorder.take() {
                            Some(fr) => fr
                                .lock()
                                .expect("Could not aquire frame recorder")
                                .end_encoder(),
                            _ => {}
                        }
                    }
                    WindowEvent::Resized(physical_size) => {
                        self.context.resize(*physical_size);
                        self.scene.state.resolution = (*physical_size).into();
                    }
                    WindowEvent::ScaleFactorChanged { new_inner_size, .. } => {
                        self.context.resize(**new_inner_size);
                        self.scene.state.resolution = self.context.size.into();
                    }
                    WindowEvent::Focused(focus) => self.handle_cursor_focus(*focus),
                    WindowEvent::MouseInput {
                        state: ElementState::Pressed,
                        button: MouseButton::Left,
                        ..
                    } => self.handle_cursor_pressed(),
                    WindowEvent::CursorMoved { position, .. } => self.handle_cursor_move(*position),
                    WindowEvent::KeyboardInput {
                        input:
                            KeyboardInput {
                                state: ElementState::Pressed,
                                virtual_keycode: Some(VirtualKeyCode::H),
                                ..
                            },
                        ..
                    } => {
                        self.context.egui_dev.show = !self.context.egui_dev.show;
                    }
                    _ => {}
                }
            }
            Event::RedrawRequested(window_id) if window_id == self.window.id() => {
                self.scene.update();

                match self.context.render(&self.scene, &self.window) {
                    Ok(_) => {}
                    // Reconfigure the surface if lost
                    Err(wgpu::SurfaceError::Lost) => self.context.resize(self.context.size),
                    // The system is out of memory, we should probably quit
                    Err(wgpu::SurfaceError::OutOfMemory) => *control_flow = ControlFlow::Exit,
                    // All other errors (Outdated, Timeout) should be resolved by the next frame
                    Err(e) => eprintln!("{:?}", e),
                }
            }
            Event::MainEventsCleared => {
                self.window.request_redraw();
            }
            _ => {}
        }
    }

    const FOCUS_2_GRAB_MODE: [winit::window::CursorGrabMode; 2] = [
        winit::window::CursorGrabMode::None,
        winit::window::CursorGrabMode::Confined,
    ];
    fn handle_cursor_focus(&mut self, focus: bool) {
        match self
            .window
            .set_cursor_grab(Self::FOCUS_2_GRAB_MODE[focus as usize])
        {
            Ok(_) => {
                self.scene.state.cursor_grabbed = focus;
                self.window.set_cursor_visible(!focus);
            }
            Err(e) => {
                warn!("handle_cursor_focus({focus}) failed with: {e}");
                self.scene.state.cursor_grabbed = !focus;
                self.window.set_cursor_visible(focus);
            }
        }
    }

    fn handle_cursor_pressed(&mut self) {
        if self.scene.state.cursor_grabbed {
            if let Ok(_) = self
                .window
                .set_cursor_grab(winit::window::CursorGrabMode::None)
            {
                self.scene.state.cursor_grabbed = false;
                self.window.set_cursor_visible(true);
            }

            return;
        }
        match self
            .window
            .set_cursor_grab(winit::window::CursorGrabMode::Confined)
        {
            Err(e) => {
                warn!("{e}");
                self.scene.state.cursor_grabbed = false;
                self.window.set_cursor_visible(true);
            }
            Ok(_) => {
                self.scene.state.cursor_grabbed = true;
                self.window.set_cursor_visible(false);
                self.scene.state.prev_cursor = self.scene.state.curr_cursor;
            }
        }

        let [w, h]: [u32; 2] = self.window.inner_size().into();
        let pos = [w / 2, h / 2];
        self.window
            .set_cursor_position(Into::<PhysicalPosition<u32>>::into(pos))
            .unwrap_or_else(|_| warn!("Failed to position cursor"));
        self.scene.state.cursor_jumped = Some([pos[0] as f32, pos[1] as f32]);
    }

    fn handle_cursor_move(&mut self, position: PhysicalPosition<f64>) {
        if !self.scene.state.cursor_grabbed {
            return;
        }
        let [x, y]: [f64; 2] = position.into();
        let [w, h]: [u32; 2] = self.window.inner_size().into();

        if x as u32 == w - 1 || x as u32 == 0 {
            let pos = [w / 2, y as u32];
            self.window
                .set_cursor_position(Into::<PhysicalPosition<u32>>::into(pos))
                .unwrap_or_else(|_| warn!("Failed to position cursor"));
            self.scene.state.cursor_jumped = Some([pos[0] as f32, pos[1] as f32]);
        }
        if y as u32 == h - 1 || y as u32 == 0 {
            let pos = [x as u32, h / 2];
            self.window
                .set_cursor_position(Into::<PhysicalPosition<u32>>::into(pos))
                .unwrap_or_else(|_| warn!("Failed to position cursor"));
            self.scene.state.cursor_jumped = Some([pos[0] as f32, pos[1] as f32]);
        }
    }
}
