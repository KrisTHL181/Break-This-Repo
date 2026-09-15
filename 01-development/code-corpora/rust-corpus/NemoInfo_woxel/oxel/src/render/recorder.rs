use std::{
    path::PathBuf,
    sync::{
        mpsc::{self, Receiver, Sender},
        Arc, Mutex,
    },
    thread,
};

use itertools::Itertools;
use ndarray::Array3;
use video_rs::{Encoder, EncoderSettings, Locator, Time};

pub struct Frame {
    pub data: Vec<u8>,
    pub size: [u32; 2],
    pub time: std::time::Instant,
}

impl Frame {
    fn ndarray_frame(&self) -> Array3<u8> {
        let rgb_data = self
            .data
            .iter()
            .enumerate()
            .filter_map(|(i, &b)| {
                if i % 4 != 3 {
                    Some(linear_to_srgb(b))
                } else {
                    None
                }
            })
            .collect_vec();

        Array3::from_shape_vec((self.size[1] as usize, self.size[0] as usize, 3), rgb_data).unwrap()
    }
}

pub struct FrameRecorder {
    sender: Option<Sender<Frame>>,
    receiver: Arc<Mutex<Receiver<Frame>>>,
    encoding_thread: Option<thread::JoinHandle<()>>,
    encoder: Arc<Mutex<Encoder>>,
    pub prev_frame_time: std::time::Instant,
}

impl FrameRecorder {
    pub fn new(output_path: PathBuf) -> Self {
        let (sender, reciever) = mpsc::channel();
        let receiver = Arc::new(Mutex::new(reciever));

        let destination: Locator = output_path.into();
        let settings = EncoderSettings::for_h264_yuv420p(1600, 900, false);

        let encoder = Encoder::new(&destination, settings).expect("failed to create encoder");
        let encoder = Arc::new(Mutex::new(encoder));

        Self {
            sender: Some(sender),
            receiver,
            encoding_thread: None,
            encoder,
            prev_frame_time: std::time::Instant::now(),
        }
    }

    pub fn start_encoding_thread(&mut self) {
        let receiver = Arc::clone(&self.receiver);

        let mut i = 0;

        let encoder_ptr = Arc::clone(&self.encoder);
        self.encoding_thread = Some(thread::spawn(move || {
            let receiver = receiver.lock().unwrap();

            let mut prev_time = std::time::Instant::now();
            let mut position = Time::zero();
            while let Ok(frame) = receiver.recv() {
                println!("Encoding {i}");
                i += 1;

                encoder_ptr
                    .lock()
                    .expect("fuck")
                    .encode(&frame.ndarray_frame(), &position)
                    .expect("failed to encode frame");

                let elapsed = Time::from_secs_f64(prev_time.elapsed().as_secs_f64());
                position = position.aligned_with(&elapsed).add();
                prev_time = std::time::Instant::now();
            }
        }))
    }

    pub fn send_frame(&mut self, frame: Frame) {
        match &self.sender {
            Some(s) => {
                s.send(frame).expect("Failed to send frame");
                self.prev_frame_time = std::time::Instant::now();
            }
            _ => {
                println!("No sender to send frame")
            }
        }
    }

    pub fn end_encoder(&mut self) {
        let s = self.sender.take();
        drop(s);
        println!("trying to join the thread");
        if let Some(thread) = self.encoding_thread.take() {
            thread.join().unwrap();
        }
        println!("joined the thread");
        self.encoder
            .lock()
            .expect("Could not get encoder to finnish")
            .finish()
            .expect("Could not finish encoder");
    }
}

fn _srgb_to_linear(value: u8) -> u8 {
    let c = value as f32 / 255.0; // Normalize to 0..1
    let linear = if c <= 0.04045 {
        c / 12.92
    } else {
        ((c + 0.055) / 1.055).powf(2.4)
    };
    (linear * 255.0) as u8 // Convert back to 0..255 range
}
fn linear_to_srgb(value: u8) -> u8 {
    let c = value as f32 / 255.0; // Normalize to 0..1
    let srgb = if c <= 0.0031308 {
        12.92 * c
    } else {
        1.055 * c.powf(1.0 / 2.4) - 0.055
    };
    (srgb * 255.0).round() as u8
}
