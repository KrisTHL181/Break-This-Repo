// Shared library target for kaku-gui: exposes non-GUI modules to the `k` CLI binary.
// GUI-only modules (overlay, termwindow, renderstate, etc.) are not included here.
#![allow(clippy::collapsible_if)]
#![allow(clippy::collapsible_else_if)]
#![allow(clippy::assign_op_pattern)]
#![allow(clippy::enum_variant_names)]
#![allow(clippy::extra_unused_lifetimes)]
#![allow(clippy::field_reassign_with_default)]
#![allow(clippy::manual_range_contains)]
#![allow(clippy::needless_return)]
#![allow(clippy::redundant_closure)]

pub mod ai_chat_engine;
pub mod ai_client;
pub mod ai_conversations;
pub mod ai_tools;
pub mod cli_chat;
#[doc(hidden)]
pub mod inline_ai_control;
pub mod soul;

mod ai_auth;
mod codex_connection;
pub mod thread_util;
