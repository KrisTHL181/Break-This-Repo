#![allow(incomplete_features)]
#![feature(generic_const_exprs)]
#![feature(raw_ref_op)]
#![feature(trait_alias)]

#[cfg(target_arch = "wasm32")]
use wasm_bindgen::prelude::*;

pub mod vdb;

use winit::dpi::{PhysicalSize, Size};
use winit::event_loop::EventLoop;
use winit::window::WindowBuilder;

mod render;
mod runtime;
mod scene;

use crate::render::WgpuContext;
use crate::runtime::Runtime;
use crate::scene::Scene;

const DEFAULT_SIZE: Size = Size::Physical(PhysicalSize::new(1600, 900));

#[cfg_attr(target_arch = "wasm32", wasm_bindgen(start))]
pub async fn run() {
    cfg_if::cfg_if! {
        if #[cfg(target_arch = "wasm32")] {
            std::panic::set_hook(Box::new(console_error_panic_hook::hook));
            console_log::init_with_level(log::Level::Warn).expect("Couldn't initialize logger");
        } else {
            env_logger::init();
        }
    }

    let event_loop = EventLoop::new();
    let window = WindowBuilder::new()
        .with_title("Woxel")
        .with_inner_size(DEFAULT_SIZE)
        .build(&event_loop)
        .unwrap();

    window.set_cursor_visible(false);

    if let Some(monitor) = window.current_monitor() {
        let screen_size = monitor.size();
        let window_size = window.outer_size();

        window.set_outer_position(winit::dpi::PhysicalPosition {
            x: screen_size.width.saturating_sub(window_size.width) as f64 / 2.
                + monitor.position().x as f64,
            y: screen_size.height.saturating_sub(window_size.height) as f64 / 2.
                + monitor.position().y as f64,
        });
    }

    #[cfg(target_arch = "wasm32")]
    {
        // Winit prevents sizing with CSS, so we have to set
        // the size manually when on web.
        use winit::dpi::PhysicalSize;
        window.set_inner_size(PhysicalSize::new(1600, 900));

        use winit::platform::web::WindowExtWebSys;
        web_sys::window()
            .and_then(|win| win.document())
            .and_then(|doc| {
                let dst = doc.body()?;
                let canvas = web_sys::Element::from(window.canvas());
                dst.append_child(&canvas).ok()?;
                Some(())
            })
            .expect("Couldn't append canvas to document body.");
    }

    let mut context = WgpuContext::new(&window).await;

    context.add_shader("canvas.vert", include_str!("./shaders/canvas.vert.wgsl"));
    context.add_shader("canvas.frag", include_str!("./shaders/canvas.frag.wgsl"));
    context.add_shader("raycast.comp", include_str!("./shaders/raycast.comp.wgsl"));

    let scene = Scene::new(&context);

    let mut runtime = Runtime::new(context, window, scene);
    event_loop
        .run(move |event, target, control_flow| runtime.main_loop(event, target, control_flow));
}
