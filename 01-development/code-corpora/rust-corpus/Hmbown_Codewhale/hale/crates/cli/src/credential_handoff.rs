use super::{runtime_overrides_for_provider, xai_auth_diagnostics};
use anyhow::{Context, Result, bail, ensure};
use codewhale_config::{
    CliRuntimeOverrides, ConfigStore, ProviderKind, RuntimeApiKeySource,
    auth_mode_uses_kimi_imported_token,
};
use codewhale_secrets::Secrets;
use std::io::{ErrorKind, Write};
use zeroize::Zeroizing;

const TERMINAL_REFUSAL: &str =
    "refusing terminal output; pipe credential handoff to the intended local client";

pub(crate) fn prepare_stdout(stdout_is_terminal: bool) -> Result<()> {
    ensure!(!stdout_is_terminal, TERMINAL_REFUSAL);
    #[cfg(unix)]
    // SAFETY: this one-shot CLI exits before another command can inherit it.
    unsafe {
        let _ = libc::signal(libc::SIGPIPE, libc::SIG_IGN);
    }
    Ok(())
}

pub(crate) fn resolve_api_key(
    store: &ConfigStore,
    secrets: &Secrets,
    provider: ProviderKind,
    runtime_overrides: &CliRuntimeOverrides,
) -> Result<String> {
    let resolved = store.config.resolve_runtime_options_with_secrets(
        &runtime_overrides_for_provider(runtime_overrides, provider),
        secrets,
    );
    if resolved.provider != provider {
        bail!("resolved a different provider");
    }
    let source = resolved.api_key_source;
    if source != Some(RuntimeApiKeySource::Cli) {
        if provider == ProviderKind::OpenaiCodex {
            bail!("bearer credentials are not an API key");
        }
        let uses_api_key = provider != ProviderKind::Xai
            || xai_auth_diagnostics(store, runtime_overrides).evaluates_runtime_api_key();
        ensure!(uses_api_key, "OAuth bearer credentials are not an API key");
        let kimi_bearer = provider == ProviderKind::Moonshot
            && resolved
                .auth_mode
                .as_deref()
                .is_some_and(auth_mode_uses_kimi_imported_token);
        ensure!(!kimi_bearer, "bearer credentials are not an API key");
    }
    ensure!(source.is_some(), "no runtime-effective API key");
    resolved
        .api_key
        .filter(|value| !value.trim().is_empty())
        .context("no usable runtime-effective API key")
}

pub(crate) fn handoff_secret_line(
    writer: &mut impl Write,
    stdout_is_terminal: bool,
    resolve: impl FnOnce() -> Result<String>,
) -> Result<()> {
    prepare_stdout(stdout_is_terminal)?;
    let secret = Zeroizing::new(resolve().map_err(|_| anyhow::anyhow!("unavailable credential"))?);
    ensure!(!secret.trim().is_empty(), "credential handoff was empty");
    let written = writeln!(writer, "{}", secret.as_str());
    if written.is_ok() || written.is_err_and(|error| error.kind() == ErrorKind::BrokenPipe) {
        return Ok(());
    }
    bail!("credential handoff could not write to stdout")
}
#[cfg(test)]
mod tests;
