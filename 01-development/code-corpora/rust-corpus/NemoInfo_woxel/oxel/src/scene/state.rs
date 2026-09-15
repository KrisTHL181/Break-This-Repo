use winit::event::WindowEvent;

pub struct State {
    pub resolution: [f32; 2],
    pub prev_cursor: [f32; 2],
    pub curr_cursor: [f32; 2],
    pub cursor_jumped: Option<[f32; 2]>,
    pub cursor_grabbed: bool,
    pub time_last_frame: instant::Instant,
    pub initial_time: instant::Instant,
    pub total_time_elapsed: u64,
    pub fps: f32,
    pub dt: f32,
}

impl State {
    pub fn new(resolution: [f32; 2]) -> Self {
        State {
            resolution,
            prev_cursor: [0.0; 2],
            curr_cursor: [0.0; 2],
            cursor_jumped: None,
            cursor_grabbed: false,
            time_last_frame: instant::Instant::now(),
            initial_time: instant::Instant::now(),
            total_time_elapsed: 0,
            fps: 0.,
            dt: 0.,
        }
    }

    pub fn process_events(&mut self, event: &WindowEvent) -> bool {
        match event {
            WindowEvent::CursorMoved { position, .. } => {
                if !self.cursor_grabbed {
                    return false;
                }
                self.check_jumped();
                self.prev_cursor = self.curr_cursor;
                self.curr_cursor = [position.x as f32, position.y as f32];
                true
            }
            _ => false,
        }
    }

    pub fn update(&mut self) {
        // @TODO: Add web compatibility
        self.dt = self.time_last_frame.elapsed().as_secs_f32();
        self.fps = 1. / self.dt;

        self.prev_cursor = self.curr_cursor;
        self.check_jumped();
        self.time_last_frame = instant::Instant::now();
        self.total_time_elapsed = self.initial_time.elapsed().as_secs();
    }

    pub fn check_jumped(&mut self) {
        if let Some(position) = self.cursor_jumped {
            self.curr_cursor = position;
        }
        self.cursor_jumped = None;
    }
}
