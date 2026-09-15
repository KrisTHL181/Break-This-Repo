//! Dedicated Tokio runtime support for the UniFFI bindings.
//!
//! Foreign runtimes may poll UniFFI futures on callback threads with much
//! smaller stacks than Rust's Tokio workers. Database futures therefore run as
//! owned tasks here; only the small join future is polled by the foreign host.

use std::future::Future;
use std::num::NonZeroUsize;
use std::sync::OnceLock;

use tokio::runtime::{Builder, Runtime};
use tokio::task::JoinError;

const RUNTIME_THREADS_ENV: &str = "HELIX_UNIFFI_RUNTIME_THREADS";
const FALLBACK_WORKER_THREADS: usize = 4;

/// Runs one owned database future on the dedicated binding runtime.
pub(crate) async fn run<F>(future: F) -> Result<F::Output, JoinError>
where
    F: Future + Send + 'static,
    F::Output: Send + 'static,
{
    runtime().spawn(future).await
}

fn runtime() -> &'static Runtime {
    static RUNTIME: OnceLock<Runtime> = OnceLock::new();
    RUNTIME.get_or_init(|| {
        Builder::new_multi_thread()
            .worker_threads(configured_worker_threads(
                std::env::var(RUNTIME_THREADS_ENV).ok().as_deref(),
            ))
            .enable_all()
            .thread_name("helixdb-uniffi-rt")
            .build()
            .expect("failed to build HelixDB UniFFI runtime")
    })
}

fn configured_worker_threads(value: Option<&str>) -> usize {
    value
        .and_then(|value| value.trim().parse::<usize>().ok())
        .filter(|threads| *threads > 0)
        .unwrap_or_else(|| {
            std::thread::available_parallelism()
                .map(NonZeroUsize::get)
                .unwrap_or(FALLBACK_WORKER_THREADS)
        })
}

#[cfg(test)]
mod tests {
    use std::thread;

    use tokio::runtime::{Handle, RuntimeFlavor};

    use super::configured_worker_threads;

    #[test]
    fn worker_threads_uses_positive_env_value() {
        assert_eq!(configured_worker_threads(Some("8")), 8);
        assert_eq!(configured_worker_threads(Some(" 3 ")), 3);
    }

    #[test]
    fn worker_threads_falls_back_for_invalid_env_values() {
        let default = configured_worker_threads(None);

        assert_eq!(configured_worker_threads(Some("0")), default);
        assert_eq!(configured_worker_threads(Some("-1")), default);
        assert_eq!(configured_worker_threads(Some("not-a-number")), default);
    }

    #[tokio::test(flavor = "current_thread")]
    async fn run_moves_work_to_the_dedicated_multi_thread_runtime() {
        let caller = thread::current().id();
        let (flavor, worker, worker_name) = super::run(async {
            (
                Handle::current().runtime_flavor(),
                thread::current().id(),
                thread::current().name().map(str::to_owned),
            )
        })
        .await
        .expect("binding runtime task joins");

        assert_eq!(flavor, RuntimeFlavor::MultiThread);
        assert_ne!(worker, caller);
        assert_eq!(worker_name.as_deref(), Some("helixdb-uniffi-rt"));
    }
}
