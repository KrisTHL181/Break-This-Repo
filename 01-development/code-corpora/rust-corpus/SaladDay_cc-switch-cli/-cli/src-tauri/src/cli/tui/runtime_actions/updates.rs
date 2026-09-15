use crate::cli::i18n::texts;
use crate::error::AppError;

use super::super::app::{LoadingKind, Overlay, ToastKind};
use super::super::runtime_systems::UpdateReq;
use super::RuntimeActionContext;

pub(super) fn check(ctx: &mut RuntimeActionContext<'_>) -> Result<(), AppError> {
    if matches!(ctx.app.overlay, Overlay::UpdateDownloading { .. }) {
        return Ok(());
    }
    let Some(tx) = ctx.update_req_tx else {
        ctx.app.push_toast(
            texts::tui_toast_update_check_failed(texts::tui_update_err_worker_unavailable()),
            ToastKind::Warning,
        );
        return Ok(());
    };
    let request_id = ctx.update_check.start();
    ctx.app.overlay = Overlay::Loading {
        kind: LoadingKind::UpdateCheck,
        title: texts::tui_update_checking_title().to_string(),
        message: texts::tui_loading().to_string(),
    };
    if let Err(err) = tx.send(UpdateReq::Check { request_id }) {
        ctx.update_check.cancel();
        ctx.app.overlay = Overlay::None;
        ctx.app.push_toast(
            texts::tui_toast_update_check_failed(&err.to_string()),
            ToastKind::Error,
        );
    }
    Ok(())
}

pub(super) fn confirm(ctx: &mut RuntimeActionContext<'_>) -> Result<(), AppError> {
    let Some(tx) = ctx.update_req_tx else {
        return Ok(());
    };
    ctx.app.overlay = Overlay::UpdateDownloading {
        downloaded: 0,
        total: None,
    };
    if let Err(err) = tx.send(UpdateReq::Download) {
        ctx.app.overlay = Overlay::None;
        ctx.app.push_toast(
            texts::tui_toast_update_bg_failed(&err.to_string()),
            ToastKind::Error,
        );
    }
    Ok(())
}
