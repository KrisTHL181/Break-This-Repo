use super::confirm;
use crate::termwindow::TermWindowNotif;
use crate::TermWindow;
use mux::pane::PaneId;
use mux::tab::TabId;
use mux::termwiztermtab::TermWizTerminal;
use mux::window::WindowId;
use mux::Mux;
use window::WindowOps;

pub fn confirm_close_pane(
    pane_id: PaneId,
    mut term: TermWizTerminal,
    _mux_window_id: WindowId,
    window: ::window::Window,
) -> anyhow::Result<()> {
    if confirm::run_confirmation(
        "Close this pane?\nThe running process in this pane will be terminated.",
        &mut term,
    )? {
        promise::spawn::spawn_into_main_thread(async move {
            let mux = Mux::get();
            // Resolve the pane's own tab rather than whichever tab is active
            // when the prompt is answered: switching tabs while the prompt is
            // up would otherwise aim kill_pane at a tab that never held this
            // pane, and the pane would silently survive the confirmation.
            let Some((_domain_id, _window_id, tab_id)) = mux.resolve_pane_id(pane_id) else {
                return;
            };
            let Some(tab) = mux.get_tab(tab_id) else {
                return;
            };
            tab.kill_pane(pane_id);
        })
        .detach();
    }
    TermWindow::schedule_cancel_overlay_for_pane(window, pane_id);

    Ok(())
}

pub fn confirm_close_tab(
    tab_id: TabId,
    mut term: TermWizTerminal,
    _mux_window_id: WindowId,
    window: ::window::Window,
) -> anyhow::Result<()> {
    if confirm::run_confirmation(
        "Close this tab?\nAll panes in this tab will be terminated.",
        &mut term,
    )? {
        // Record the cwd from here rather than at the call site: the user may
        // still cancel, and only this branch knows they did not. Without it,
        // every tab that needed a confirmation would be missing from
        // ReopenLastClosedTab.
        window.notify(TermWindowNotif::Apply(Box::new(move |term_window| {
            let mux = Mux::get();
            if let Some(tab) = mux.get_tab(tab_id) {
                term_window.record_closed_tab_cwd(&tab);
            }
            mux.remove_tab(tab_id);
        })));
    }
    TermWindow::schedule_cancel_overlay(window, tab_id, None);

    Ok(())
}

#[cfg(not(target_os = "macos"))]
pub fn confirm_close_window(
    mut term: TermWizTerminal,
    mux_window_id: WindowId,
    window: ::window::Window,
    tab_id: TabId,
) -> anyhow::Result<()> {
    if confirm::run_confirmation(
        "Close this window?\nAll tabs and panes in this window will be terminated.",
        &mut term,
    )? {
        promise::spawn::spawn_into_main_thread(async move {
            let mux = Mux::get();
            mux.kill_window(mux_window_id);
        })
        .detach();
    }
    TermWindow::schedule_cancel_overlay(window, tab_id, None);

    Ok(())
}

pub fn confirm_apply_update(
    mut term: TermWizTerminal,
    window: ::window::Window,
    tab_id: TabId,
) -> anyhow::Result<()> {
    if confirm::run_confirmation(
        "Update Kaku now?\nAll windows will close and running tasks will stop.",
        &mut term,
    )? {
        promise::spawn::spawn_into_main_thread(async move {
            crate::frontend::apply_update_now();
        })
        .detach();
    }
    TermWindow::schedule_cancel_overlay(window, tab_id, None);

    Ok(())
}

pub fn confirm_quit_program(
    mut term: TermWizTerminal,
    window: ::window::Window,
    tab_id: TabId,
) -> anyhow::Result<()> {
    if confirm::run_confirmation(
        "Quit Kaku?\nAll open tabs and panes will be closed.",
        &mut term,
    )? {
        promise::spawn::spawn_into_main_thread(async move {
            #[cfg(target_os = "macos")]
            {
                ::window::request_terminate(::window::QuitOrigin::ConfirmQuitOverlay);
            }
            #[cfg(not(target_os = "macos"))]
            {
                use ::window::{Connection, ConnectionOps};
                let con = Connection::get().expect("call on gui thread");
                con.terminate_message_loop();
            }
        })
        .detach();
    }
    TermWindow::schedule_cancel_overlay(window, tab_id, None);

    Ok(())
}
