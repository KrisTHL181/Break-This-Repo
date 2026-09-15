use cgmath::{InnerSpace, SquareMatrix, Vector3};
use winit::event::{ElementState, KeyboardInput, VirtualKeyCode, WindowEvent};

use crate::scene::State;

pub struct Camera {
    pub eye: cgmath::Point3<f32>,
    pub target: cgmath::Point3<f32>,
    up: cgmath::Vector3<f32>,
    pub aspect: f32,
    /// y-axis fov in degrees
    pub fovy: f32,
}

impl Camera {
    pub fn quick_camera(aspect: f32) -> Self {
        Camera {
            // position the camera one unit up and 2 units back
            // +z is out of the screen
            eye: (0.5, 0.5, -500.5).into(),
            // have it look at the origin
            target: (0.5, 0.5, -498.5).into(),
            // which way is "up"
            up: cgmath::Vector3::unit_y(),
            aspect,
            // In degrees
            fovy: 45.0,
        }
    }

    pub fn build_view_projection_matrix(&self) -> cgmath::Matrix4<f32> {
        let view = cgmath::Matrix4::look_at_rh(self.eye, self.target, self.up);

        return view;
    }

    pub fn get_ray_dir(
        &self,
        _point @ [x, y]: [f32; 2],
        _resoltion @ [width, _height]: [f32; 2],
    ) -> Vector3<f32> {
        let view_proj = self.build_view_projection_matrix();
        let camera_to_world = match view_proj.invert() {
            Some(c) => c,
            None => panic!("Could not invert camera matrix"),
        };
        // @TODO: Thorough check this !!! No chance I got it right the first time

        let height = width / self.aspect;
        let u = camera_to_world.x;
        let v = camera_to_world.y;
        let w = camera_to_world.z;
        let wp = (-width / 2.0) * u + (height / 2.0) * v
            - w * (height / 2.0) / (self.fovy.to_radians() * 0.5).tan();
        let mv = -v;

        (x * u + y * mv + wp).truncate().normalize()
    }
}

pub struct CameraController {
    speed: f32,
    is_forward_pressed: bool,
    is_backward_pressed: bool,
    is_left_pressed: bool,
    is_right_pressed: bool,
    is_up_pressed: bool,
    is_down_pressed: bool,
}

const CAMERA_MIN_Y_ANGLE: f32 = 20.0;
const CAMERA_MOUSE_SENSITIVITY: f32 = 2.3 * 60.;
const CAMERA_SPEED: f32 = 0.1 * 60. * 20.;

impl CameraController {
    pub fn new() -> Self {
        Self {
            speed: CAMERA_SPEED,
            is_forward_pressed: false,
            is_backward_pressed: false,
            is_left_pressed: false,
            is_right_pressed: false,
            is_up_pressed: false,
            is_down_pressed: false,
        }
    }

    pub fn process_events(&mut self, event: &WindowEvent) -> bool {
        match event {
            WindowEvent::KeyboardInput {
                input:
                    KeyboardInput {
                        state,
                        virtual_keycode: Some(keycode),
                        ..
                    },
                ..
            } => {
                let is_pressed = *state == ElementState::Pressed;
                match keycode {
                    VirtualKeyCode::W | VirtualKeyCode::Up => {
                        self.is_forward_pressed = is_pressed;
                        true
                    }
                    VirtualKeyCode::A | VirtualKeyCode::Left => {
                        self.is_left_pressed = is_pressed;
                        true
                    }
                    VirtualKeyCode::S | VirtualKeyCode::Down => {
                        self.is_backward_pressed = is_pressed;
                        true
                    }
                    VirtualKeyCode::D | VirtualKeyCode::Right => {
                        self.is_right_pressed = is_pressed;
                        true
                    }
                    VirtualKeyCode::LShift => {
                        self.is_down_pressed = is_pressed;
                        true
                    }
                    VirtualKeyCode::Space => {
                        self.is_up_pressed = is_pressed;
                        true
                    }
                    _ => false,
                }
            }
            _ => false,
        }
    }

    pub fn update_camera(&mut self, camera: &mut Camera, state: &State) {
        //        dbg!(camera.target);
        let forward = camera.target - camera.eye;
        let forward_norm = forward.normalize();

        // MOVEMENT

        if self.is_forward_pressed {
            camera.eye += forward_norm * self.speed * state.dt;
            camera.target += forward_norm * self.speed * state.dt;
        }
        if self.is_backward_pressed {
            camera.eye -= forward_norm * self.speed * state.dt;
            camera.target -= forward_norm * self.speed * state.dt;
        }

        let right = forward_norm.cross(camera.up).normalize();

        if self.is_right_pressed {
            camera.eye += right * self.speed * state.dt;
            camera.target += right * self.speed * state.dt;
        }
        if self.is_left_pressed {
            camera.eye -= right * self.speed * state.dt;
            camera.target -= right * self.speed * state.dt;
        }

        let up = camera.up.normalize();
        if self.is_up_pressed {
            camera.eye += up * self.speed * state.dt;
            camera.target += up * self.speed * state.dt;
        }
        if self.is_down_pressed {
            camera.eye -= up * self.speed * state.dt;
            camera.target -= up * self.speed * state.dt;
        }

        // ROTATION

        if state.prev_cursor == state.curr_cursor {
            return;
        }
        let ray_prev = camera.get_ray_dir(state.prev_cursor, state.resolution);
        let ray_curr = camera.get_ray_dir(state.curr_cursor, state.resolution);
        let diff = ray_curr - ray_prev;
        // @TODO: to make the camera smoother maybe interpolate between the diff's in a set of frames
        //        and compute the rotation direction that way
        let d = diff * CAMERA_MOUSE_SENSITIVITY * state.dt;

        let (target, eye) = (camera.target, camera.eye);
        camera.target = camera.eye
            + (camera.target + d - camera.eye).normalize()
                * (camera.target - camera.eye).magnitude();

        let y = Vector3::new(0.0, 1.0, 0.0);
        let v = camera.target - camera.eye;
        let alpha = y.angle(v).0.to_degrees();
        let beta = (-y).angle(v).0.to_degrees();
        if alpha < CAMERA_MIN_Y_ANGLE || beta < CAMERA_MIN_Y_ANGLE {
            // @TODO: allow to move to be just on the edge of max rotation
            (camera.target, camera.eye) = (target, eye);
        }
    }
}
