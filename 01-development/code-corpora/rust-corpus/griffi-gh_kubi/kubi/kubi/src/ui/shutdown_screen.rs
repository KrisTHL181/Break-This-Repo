use hui::element::{progress_bar::ProgressBar, text::Text, UiElementExt};
use shipyard::{AllStoragesView, AllStoragesViewMut, IntoWorkload, NonSendSync, SystemModificator, Unique, UniqueView, UniqueViewMut, Workload, WorkloadModificator};
use kubi_shared::data::io_thread::{IOResponse, IOThreadManager, TerminationStage};
use crate::{
  control_flow::RequestExit, cursor_lock::CursorLock, hui_integration::UiState, loading_screen::loading_screen_base, networking::is_singleplayer, rendering::Renderer, state::{is_ingame, is_ingame_or_shutting_down, is_shutting_down, GameState, NextState}
};

// TODO move shutdown non-UI logic to a separate file

#[derive(Unique)]
struct ShutdownState {
  // iota: IOThreadManager,
  termination: TerminationStage,
}

fn intercept_exit(
  mut exit: UniqueViewMut<RequestExit>,
  mut state: UniqueViewMut<NextState>,
  cur_state: UniqueView<GameState>,
  termination_state: Option<UniqueView<ShutdownState>>,
  iota: Option<UniqueView<IOThreadManager>>,
) {
  // If iota is missing, don't bother with shutdown state (likely running without a save file)
  if iota.is_none() {
    return;
  }
  if exit.0 {
    if *cur_state == GameState::ShuttingDown {
      // If we're already shutting down, check if we're done
      // If not, ignore the exit request
      if let Some(termination_state) = termination_state {
        if termination_state.termination != TerminationStage::Terminated {
          log::warn!("Exit request intercepted, ignoring as we're still shutting down");
          exit.0 = false;
        }
      }
    } else if state.0 != Some(GameState::ShuttingDown) {
      log::info!("Exit request intercepted, transitioning to shutdown state");
      exit.0 = false;
      state.0 = Some(GameState::ShuttingDown);
    }
  }
}

pub fn init_shutdown_state(
  storages: AllStoragesView,
) {
  storages.add_unique(ShutdownState {
    termination: TerminationStage::Starting,
  });

  // HACK: Tell iota to kys (todo do on state transition instead)
  log::info!("IO Thread stopping gracefully... (stop_async)");
  let mut iota = storages.borrow::<UniqueViewMut<IOThreadManager>>().unwrap();
  iota.stop_async();

  // ..and make sure to disable cursor lock
  let mut lock = storages.borrow::<UniqueViewMut<CursorLock>>().unwrap();
  lock.0 = false;
}

pub fn update_shutdown_state(
  storages: AllStoragesViewMut,
) {
  let Ok(iota) = storages.borrow::<UniqueViewMut<IOThreadManager>>() else {
    log::warn!("IO Thread not found, skipping shutdown state update");
    return;
  };
  let mut state = storages.borrow::<UniqueViewMut<ShutdownState>>().unwrap();

  let mut do_drop_iota = false;

  //poll the IO thread for progress
  for response in iota.poll() {
    match response {
      IOResponse::KysProgressInformational(stage) => {
        state.termination = stage;
      },
      IOResponse::Terminated => {
        state.termination = TerminationStage::Terminated;

        do_drop_iota = true;

        // Request exit
        let mut exit = storages.borrow::<UniqueViewMut<RequestExit>>().unwrap();
        exit.0 = true;
      },
      _ => {}
    }
  }

  drop(iota);

  // Hard-stop and drop the iota
  if do_drop_iota {
    let mut iota = storages.remove_unique::<IOThreadManager>().unwrap();
    iota.stop_async_block_on();
    log::info!("IO Thread terminated on stop_async_block_on");
    drop(iota);
  }
}

fn render_shutdown_ui(
  mut ui: NonSendSync<UniqueViewMut<UiState>>,
  ren: UniqueView<Renderer>,
  state: UniqueView<ShutdownState>,
) {
  loading_screen_base(1., |ui| {
    Text::new("Shutting down...")
      .with_text_size(16)
      .add_child(ui);
    match state.termination {
      TerminationStage::Starting => {
        Text::new("Please wait...")
          .with_text_size(16)
          .add_child(ui);
      },
      TerminationStage::SaveQueue { progress, total } => {
        Text::new(format!("Saving chunks: {}/{}", progress, total))
          .with_text_size(16)
          .add_child(ui);
        ProgressBar::default()
          .with_value(progress as f32 / total as f32)
          .add_child(ui);
      },
      TerminationStage::ProcessRx => {
        Text::new("Processing remaining save requests...")
          .with_text_size(16)
          .add_child(ui);
      },
      TerminationStage::Terminated => {
        Text::new("Terminated.")
          .with_text_size(16)
          .add_child(ui);
      }
    }
  }).add_root(&mut ui.hui, ren.size_vec2())
}

pub fn update_shutdown_screen() -> Workload {
  (
    init_shutdown_state
      .run_if_missing_unique::<ShutdownState>(),
    update_shutdown_state,
    render_shutdown_ui,
  ).into_sequential_workload()
}

pub fn late_intercept() -> Workload {
  (
    intercept_exit,
  ).into_workload()
    .run_if(is_singleplayer)
    .run_if(is_ingame_or_shutting_down)
}
