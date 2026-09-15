use winit::event::WindowEvent;

use crate::render::{Camera, CameraController, WgpuContext};

use super::State;

pub struct Scene {
    pub state: State,
    pub camera: Camera,
    pub camera_controller: CameraController,
    // pub voxels: VDB
}

impl Scene {
    pub fn new(context: &WgpuContext) -> Self {
        let resolution = [context.config.width as f32, context.config.height as f32];
        let aspect = resolution[0] / resolution[1];
        let camera_controller = CameraController::new();
        Self {
            state: State::new(resolution),
            camera: Camera::quick_camera(aspect),
            camera_controller,
        }
    }

    pub fn update(&mut self) {
        self.camera_controller
            .update_camera(&mut self.camera, &self.state);
        self.state.update();
    }

    pub fn input(&mut self, event: &WindowEvent) {
        self.state.process_events(event);
        self.camera_controller.process_events(event);
    }
}
