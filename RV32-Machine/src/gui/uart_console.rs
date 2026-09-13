use std::io::{self, Write, Read};
use std::sync::Arc;
use std::thread;
use std::time::Duration;

pub struct UartConsole {
    pub enabled: bool,
    input_buffer: Arc<parking_lot::Mutex<Vec<u8>>>,
}

impl UartConsole {
    pub fn new() -> Self {
        let input_buffer = Arc::new(parking_lot::Mutex::new(Vec::new()));

        let buf = input_buffer.clone();
        thread::spawn(move || {
            let mut stdin = io::stdin();
            let mut byte_buf = [0u8; 1];
            loop {
                match stdin.read(&mut byte_buf) {
                    Ok(1) => {
                        buf.lock().push(byte_buf[0]);
                    }
                    Ok(_) => {}
                    Err(_) => {
                        thread::sleep(Duration::from_millis(10));
                    }
                }
            }
        });

        UartConsole {
            enabled: true,
            input_buffer,
        }
    }

    pub fn write_bytes(&self, data: &[u8]) {
        if !self.enabled {
            return;
        }
        let mut stdout = io::stdout();
        let _ = stdout.write_all(data);
        let _ = stdout.flush();
    }

    pub fn read_byte(&self) -> Option<u8> {
        self.input_buffer.lock().pop()
    }
}