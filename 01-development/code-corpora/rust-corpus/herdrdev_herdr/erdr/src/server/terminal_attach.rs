pub(crate) fn paste_payload_for_runtime(
    runtime: &crate::terminal::TerminalRuntime,
    text: &str,
) -> String {
    if runtime.bracketed_paste_enabled() {
        format!("\x1b[200~{text}\x1b[201~")
    } else {
        text.to_owned()
    }
}
