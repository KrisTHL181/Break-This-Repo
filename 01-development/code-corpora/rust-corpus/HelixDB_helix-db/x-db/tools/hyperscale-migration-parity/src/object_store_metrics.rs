use std::fmt;
use std::num::NonZeroU64;
use std::ops::Range;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use bytes::Bytes;
use futures::stream::BoxStream;
use futures::{StreamExt, TryStreamExt};
use serde::Serialize;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(usize)]
pub(crate) enum Operation {
    Get,
    Head,
    Put,
    Multipart,
    List,
    Delete,
    Copy,
}

/// Failure classes exercised by the object-storage recovery harness.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum FaultKind {
    Transient,
    Timeout,
    Throttled,
    ConnectionLoss,
}

impl FaultKind {
    pub(crate) fn parse(value: &str) -> Option<Self> {
        match value {
            "transient" => Some(Self::Transient),
            "timeout" => Some(Self::Timeout),
            "throttled" | "throttle" => Some(Self::Throttled),
            "connection-loss" | "connection_loss" => Some(Self::ConnectionLoss),
            _ => None,
        }
    }

    const fn io_error_kind(self) -> std::io::ErrorKind {
        match self {
            Self::Transient => std::io::ErrorKind::Interrupted,
            Self::Timeout => std::io::ErrorKind::TimedOut,
            Self::Throttled => std::io::ErrorKind::WouldBlock,
            Self::ConnectionLoss => std::io::ErrorKind::ConnectionReset,
        }
    }
}

const OPERATION_COUNT: usize = 7;

impl Operation {
    const fn name(self) -> &'static str {
        match self {
            Self::Get => "get",
            Self::Head => "head",
            Self::Put => "put",
            Self::Multipart => "multipart",
            Self::List => "list",
            Self::Delete => "delete",
            Self::Copy => "copy",
        }
    }

    pub(crate) fn parse(value: &str) -> Option<Self> {
        match value {
            "get" => Some(Self::Get),
            "head" => Some(Self::Head),
            "put" => Some(Self::Put),
            "multipart" => Some(Self::Multipart),
            "list" => Some(Self::List),
            "delete" => Some(Self::Delete),
            "copy" => Some(Self::Copy),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy)]
pub(crate) struct FaultPolicy {
    latency: Duration,
    injection: FaultInjection,
}

#[derive(Debug, Clone, Copy)]
enum FaultInjection {
    Disabled,
    Enabled {
        kind: FaultKind,
        operation: Operation,
        every: NonZeroU64,
        maximum_failures: NonZeroU64,
    },
}

impl FaultPolicy {
    pub(crate) const fn latency(latency: Duration) -> Self {
        Self {
            latency,
            injection: FaultInjection::Disabled,
        }
    }

    pub(crate) const fn failing(
        latency: Duration,
        kind: FaultKind,
        operation: Operation,
        every: NonZeroU64,
    ) -> Self {
        Self {
            latency,
            injection: FaultInjection::Enabled {
                kind,
                operation,
                every,
                maximum_failures: NonZeroU64::MIN,
            },
        }
    }
}

#[derive(Debug)]
pub(crate) struct ObjectStoreRecorder {
    policy: FaultPolicy,
    requests: [AtomicU64; OPERATION_COUNT],
    errors: [AtomicU64; OPERATION_COUNT],
    injected_errors: AtomicU64,
    bytes_read: AtomicU64,
    bytes_written: AtomicU64,
    maximum_read_request_bytes: AtomicU64,
    maximum_write_request_bytes: AtomicU64,
    maximum_wal_object_bytes: AtomicU64,
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
pub(crate) struct ObjectStoreMetrics {
    pub(crate) get_requests: u64,
    pub(crate) head_requests: u64,
    pub(crate) put_requests: u64,
    pub(crate) multipart_requests: u64,
    pub(crate) list_requests: u64,
    pub(crate) delete_requests: u64,
    pub(crate) copy_requests: u64,
    pub(crate) errors: u64,
    pub(crate) injected_errors: u64,
    pub(crate) bytes_read: u64,
    pub(crate) bytes_written: u64,
    pub(crate) maximum_read_request_bytes: u64,
    pub(crate) maximum_write_request_bytes: u64,
    pub(crate) maximum_wal_object_bytes: u64,
    pub(crate) added_latency_millis: u64,
}

impl ObjectStoreMetrics {
    pub(crate) fn delta_since(&self, earlier: &Self) -> Self {
        Self {
            get_requests: self.get_requests.saturating_sub(earlier.get_requests),
            head_requests: self.head_requests.saturating_sub(earlier.head_requests),
            put_requests: self.put_requests.saturating_sub(earlier.put_requests),
            multipart_requests: self
                .multipart_requests
                .saturating_sub(earlier.multipart_requests),
            list_requests: self.list_requests.saturating_sub(earlier.list_requests),
            delete_requests: self.delete_requests.saturating_sub(earlier.delete_requests),
            copy_requests: self.copy_requests.saturating_sub(earlier.copy_requests),
            errors: self.errors.saturating_sub(earlier.errors),
            injected_errors: self.injected_errors.saturating_sub(earlier.injected_errors),
            bytes_read: self.bytes_read.saturating_sub(earlier.bytes_read),
            bytes_written: self.bytes_written.saturating_sub(earlier.bytes_written),
            maximum_read_request_bytes: if self.maximum_read_request_bytes
                > earlier.maximum_read_request_bytes
            {
                self.maximum_read_request_bytes
            } else {
                0
            },
            maximum_write_request_bytes: if self.maximum_write_request_bytes
                > earlier.maximum_write_request_bytes
            {
                self.maximum_write_request_bytes
            } else {
                0
            },
            maximum_wal_object_bytes: if self.maximum_wal_object_bytes
                > earlier.maximum_wal_object_bytes
            {
                self.maximum_wal_object_bytes
            } else {
                0
            },
            added_latency_millis: self.added_latency_millis,
        }
    }
}

impl ObjectStoreRecorder {
    pub(crate) fn new(policy: FaultPolicy) -> Arc<Self> {
        Arc::new(Self {
            policy,
            requests: std::array::from_fn(|_| AtomicU64::new(0)),
            errors: std::array::from_fn(|_| AtomicU64::new(0)),
            injected_errors: AtomicU64::new(0),
            bytes_read: AtomicU64::new(0),
            bytes_written: AtomicU64::new(0),
            maximum_read_request_bytes: AtomicU64::new(0),
            maximum_write_request_bytes: AtomicU64::new(0),
            maximum_wal_object_bytes: AtomicU64::new(0),
        })
    }

    async fn before(&self, operation: Operation) -> Option<FaultKind> {
        let request = self.requests[operation as usize].fetch_add(1, Ordering::Relaxed) + 1;
        if !self.policy.latency.is_zero() {
            tokio::time::sleep(self.policy.latency).await;
        }
        let FaultInjection::Enabled {
            kind,
            operation: failed_operation,
            every,
            maximum_failures,
        } = self.policy.injection
        else {
            return None;
        };
        let should_fail = failed_operation == operation
            && self.injected_errors.load(Ordering::Relaxed) < maximum_failures.get()
            && request.is_multiple_of(every.get());
        if should_fail {
            self.errors[operation as usize].fetch_add(1, Ordering::Relaxed);
            self.injected_errors.fetch_add(1, Ordering::Relaxed);
        }
        should_fail.then_some(kind)
    }

    fn record_result<T, E>(&self, operation: Operation, result: &Result<T, E>) {
        if result.is_err() {
            self.errors[operation as usize].fetch_add(1, Ordering::Relaxed);
        }
    }

    fn record_read(&self, bytes: usize) {
        let bytes = u64::try_from(bytes).unwrap_or(u64::MAX);
        self.bytes_read.fetch_add(bytes, Ordering::Relaxed);
        self.maximum_read_request_bytes
            .fetch_max(bytes, Ordering::Relaxed);
    }

    fn record_written(&self, bytes: usize, is_wal: bool) {
        let bytes = u64::try_from(bytes).unwrap_or(u64::MAX);
        self.bytes_written.fetch_add(bytes, Ordering::Relaxed);
        self.maximum_write_request_bytes
            .fetch_max(bytes, Ordering::Relaxed);
        if is_wal {
            self.maximum_wal_object_bytes
                .fetch_max(bytes, Ordering::Relaxed);
        }
    }

    pub(crate) fn snapshot(&self) -> ObjectStoreMetrics {
        let request =
            |operation: Operation| self.requests[operation as usize].load(Ordering::Relaxed);
        ObjectStoreMetrics {
            get_requests: request(Operation::Get),
            head_requests: request(Operation::Head),
            put_requests: request(Operation::Put),
            multipart_requests: request(Operation::Multipart),
            list_requests: request(Operation::List),
            delete_requests: request(Operation::Delete),
            copy_requests: request(Operation::Copy),
            errors: self
                .errors
                .iter()
                .map(|count| count.load(Ordering::Relaxed))
                .sum(),
            injected_errors: self.injected_errors.load(Ordering::Relaxed),
            bytes_read: self.bytes_read.load(Ordering::Relaxed),
            bytes_written: self.bytes_written.load(Ordering::Relaxed),
            maximum_read_request_bytes: self.maximum_read_request_bytes.load(Ordering::Relaxed),
            maximum_write_request_bytes: self.maximum_write_request_bytes.load(Ordering::Relaxed),
            maximum_wal_object_bytes: self.maximum_wal_object_bytes.load(Ordering::Relaxed),
            added_latency_millis: u64::try_from(self.policy.latency.as_millis())
                .unwrap_or(u64::MAX),
        }
    }
}

fn injected_error_012(kind: FaultKind, operation: Operation) -> object_store::Error {
    object_store::Error::Generic {
        store: "migration-fault-proxy-0.12",
        source: std::io::Error::new(
            kind.io_error_kind(),
            format!("injected {kind:?} {} failure", operation.name()),
        )
        .into(),
    }
}

fn injected_error_014(kind: FaultKind, operation: Operation) -> object_store_014::Error {
    object_store_014::Error::Generic {
        store: "migration-fault-proxy-0.14",
        source: std::io::Error::new(
            kind.io_error_kind(),
            format!("injected {kind:?} {} failure", operation.name()),
        )
        .into(),
    }
}

#[derive(Debug)]
pub(crate) struct InstrumentedStore012 {
    inner: Arc<dyn object_store::ObjectStore>,
    recorder: Arc<ObjectStoreRecorder>,
}

impl InstrumentedStore012 {
    pub(crate) fn new(
        inner: Arc<dyn object_store::ObjectStore>,
        recorder: Arc<ObjectStoreRecorder>,
    ) -> Self {
        Self { inner, recorder }
    }
}

impl fmt::Display for InstrumentedStore012 {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(formatter, "instrumented-0.12({})", self.inner)
    }
}

#[derive(Debug)]
struct Multipart012 {
    inner: Box<dyn object_store::MultipartUpload>,
    recorder: Arc<ObjectStoreRecorder>,
    is_wal: bool,
}

#[async_trait]
impl object_store::MultipartUpload for Multipart012 {
    fn put_part(&mut self, data: object_store::PutPayload) -> object_store::UploadPart {
        let bytes = data.content_length();
        let future = self.inner.put_part(data);
        let recorder = Arc::clone(&self.recorder);
        let is_wal = self.is_wal;
        Box::pin(async move {
            if let Some(kind) = recorder.before(Operation::Multipart).await {
                return Err(injected_error_012(kind, Operation::Multipart));
            }
            let result = future.await;
            recorder.record_result(Operation::Multipart, &result);
            if result.is_ok() {
                recorder.record_written(bytes, is_wal);
            }
            result
        })
    }

    async fn complete(&mut self) -> object_store::Result<object_store::PutResult> {
        self.inner.complete().await
    }

    async fn abort(&mut self) -> object_store::Result<()> {
        self.inner.abort().await
    }
}

#[async_trait]
impl object_store::ObjectStore for InstrumentedStore012 {
    async fn put_opts(
        &self,
        location: &object_store::path::Path,
        payload: object_store::PutPayload,
        options: object_store::PutOptions,
    ) -> object_store::Result<object_store::PutResult> {
        let bytes = payload.content_length();
        if let Some(kind) = self.recorder.before(Operation::Put).await {
            return Err(injected_error_012(kind, Operation::Put));
        }
        let result = self.inner.put_opts(location, payload, options).await;
        self.recorder.record_result(Operation::Put, &result);
        if result.is_ok() {
            self.recorder
                .record_written(bytes, is_wal_path(location.as_ref()));
        }
        result
    }

    async fn put_multipart_opts(
        &self,
        location: &object_store::path::Path,
        options: object_store::PutMultipartOptions,
    ) -> object_store::Result<Box<dyn object_store::MultipartUpload>> {
        if let Some(kind) = self.recorder.before(Operation::Multipart).await {
            return Err(injected_error_012(kind, Operation::Multipart));
        }
        let result = self.inner.put_multipart_opts(location, options).await;
        self.recorder.record_result(Operation::Multipart, &result);
        result.map(|inner| {
            Box::new(Multipart012 {
                inner,
                recorder: Arc::clone(&self.recorder),
                is_wal: is_wal_path(location.as_ref()),
            }) as Box<dyn object_store::MultipartUpload>
        })
    }

    async fn get_opts(
        &self,
        location: &object_store::path::Path,
        options: object_store::GetOptions,
    ) -> object_store::Result<object_store::GetResult> {
        let operation = if options.head {
            Operation::Head
        } else {
            Operation::Get
        };
        if let Some(kind) = self.recorder.before(operation).await {
            return Err(injected_error_012(kind, operation));
        }
        let mut result = self.inner.get_opts(location, options).await;
        self.recorder.record_result(operation, &result);
        if let Ok(response) = &mut result {
            let object_store::GetResultPayload::Stream(stream) = &mut response.payload else {
                if operation == Operation::Get {
                    self.recorder.record_read(
                        usize::try_from(response.range.end.saturating_sub(response.range.start))
                            .unwrap_or(usize::MAX),
                    );
                }
                return result;
            };
            let recorder = Arc::clone(&self.recorder);
            let original = std::mem::replace(stream, futures::stream::empty().boxed());
            *stream = original
                .map(move |chunk| {
                    recorder.record_result(Operation::Get, &chunk);
                    if let Ok(bytes) = &chunk {
                        recorder.record_read(bytes.len());
                    }
                    chunk
                })
                .boxed();
        }
        result
    }

    async fn get_ranges(
        &self,
        location: &object_store::path::Path,
        ranges: &[Range<u64>],
    ) -> object_store::Result<Vec<Bytes>> {
        if let Some(kind) = self.recorder.before(Operation::Get).await {
            return Err(injected_error_012(kind, Operation::Get));
        }
        let result = self.inner.get_ranges(location, ranges).await;
        self.recorder.record_result(Operation::Get, &result);
        if let Ok(values) = &result {
            self.recorder
                .record_read(values.iter().map(Bytes::len).sum());
        }
        result
    }

    async fn delete(&self, location: &object_store::path::Path) -> object_store::Result<()> {
        if let Some(kind) = self.recorder.before(Operation::Delete).await {
            return Err(injected_error_012(kind, Operation::Delete));
        }
        let result = self.inner.delete(location).await;
        self.recorder.record_result(Operation::Delete, &result);
        result
    }

    fn list(
        &self,
        prefix: Option<&object_store::path::Path>,
    ) -> BoxStream<'static, object_store::Result<object_store::ObjectMeta>> {
        let inner = Arc::clone(&self.inner);
        let recorder = Arc::clone(&self.recorder);
        let prefix = prefix.cloned();
        futures::stream::once(async move {
            if let Some(kind) = recorder.before(Operation::List).await {
                return Err(injected_error_012(kind, Operation::List));
            }
            Ok(inner.list(prefix.as_ref()))
        })
        .try_flatten()
        .boxed()
    }

    async fn list_with_delimiter(
        &self,
        prefix: Option<&object_store::path::Path>,
    ) -> object_store::Result<object_store::ListResult> {
        if let Some(kind) = self.recorder.before(Operation::List).await {
            return Err(injected_error_012(kind, Operation::List));
        }
        let result = self.inner.list_with_delimiter(prefix).await;
        self.recorder.record_result(Operation::List, &result);
        result
    }

    async fn copy(
        &self,
        from: &object_store::path::Path,
        to: &object_store::path::Path,
    ) -> object_store::Result<()> {
        if let Some(kind) = self.recorder.before(Operation::Copy).await {
            return Err(injected_error_012(kind, Operation::Copy));
        }
        let result = self.inner.copy(from, to).await;
        self.recorder.record_result(Operation::Copy, &result);
        result
    }

    async fn copy_if_not_exists(
        &self,
        from: &object_store::path::Path,
        to: &object_store::path::Path,
    ) -> object_store::Result<()> {
        if let Some(kind) = self.recorder.before(Operation::Copy).await {
            return Err(injected_error_012(kind, Operation::Copy));
        }
        let result = self.inner.copy_if_not_exists(from, to).await;
        self.recorder.record_result(Operation::Copy, &result);
        result
    }
}

#[derive(Debug)]
pub(crate) struct InstrumentedStore014 {
    inner: Arc<dyn object_store_014::ObjectStore>,
    recorder: Arc<ObjectStoreRecorder>,
}

impl InstrumentedStore014 {
    pub(crate) fn new(
        inner: Arc<dyn object_store_014::ObjectStore>,
        recorder: Arc<ObjectStoreRecorder>,
    ) -> Self {
        Self { inner, recorder }
    }
}

impl fmt::Display for InstrumentedStore014 {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(formatter, "instrumented-0.14({})", self.inner)
    }
}

#[derive(Debug)]
struct Multipart014 {
    inner: Box<dyn object_store_014::MultipartUpload>,
    recorder: Arc<ObjectStoreRecorder>,
    is_wal: bool,
}

#[async_trait]
impl object_store_014::MultipartUpload for Multipart014 {
    fn put_part(&mut self, data: object_store_014::PutPayload) -> object_store_014::UploadPart {
        let bytes = data.content_length();
        let future = self.inner.put_part(data);
        let recorder = Arc::clone(&self.recorder);
        let is_wal = self.is_wal;
        Box::pin(async move {
            if let Some(kind) = recorder.before(Operation::Multipart).await {
                return Err(injected_error_014(kind, Operation::Multipart));
            }
            let result = future.await;
            recorder.record_result(Operation::Multipart, &result);
            if result.is_ok() {
                recorder.record_written(bytes, is_wal);
            }
            result
        })
    }

    async fn complete(&mut self) -> object_store_014::Result<object_store_014::PutResult> {
        self.inner.complete().await
    }

    async fn abort(&mut self) -> object_store_014::Result<()> {
        self.inner.abort().await
    }
}

#[async_trait]
impl object_store_014::ObjectStore for InstrumentedStore014 {
    async fn put_opts(
        &self,
        location: &object_store_014::path::Path,
        payload: object_store_014::PutPayload,
        options: object_store_014::PutOptions,
    ) -> object_store_014::Result<object_store_014::PutResult> {
        let bytes = payload.content_length();
        if let Some(kind) = self.recorder.before(Operation::Put).await {
            return Err(injected_error_014(kind, Operation::Put));
        }
        let result = self.inner.put_opts(location, payload, options).await;
        self.recorder.record_result(Operation::Put, &result);
        if result.is_ok() {
            self.recorder
                .record_written(bytes, is_wal_path(location.as_ref()));
        }
        result
    }

    async fn put_multipart_opts(
        &self,
        location: &object_store_014::path::Path,
        options: object_store_014::PutMultipartOptions,
    ) -> object_store_014::Result<Box<dyn object_store_014::MultipartUpload>> {
        if let Some(kind) = self.recorder.before(Operation::Multipart).await {
            return Err(injected_error_014(kind, Operation::Multipart));
        }
        let result = self.inner.put_multipart_opts(location, options).await;
        self.recorder.record_result(Operation::Multipart, &result);
        result.map(|inner| {
            Box::new(Multipart014 {
                inner,
                recorder: Arc::clone(&self.recorder),
                is_wal: is_wal_path(location.as_ref()),
            }) as Box<dyn object_store_014::MultipartUpload>
        })
    }

    async fn get_opts(
        &self,
        location: &object_store_014::path::Path,
        options: object_store_014::GetOptions,
    ) -> object_store_014::Result<object_store_014::GetResult> {
        let operation = if options.head {
            Operation::Head
        } else {
            Operation::Get
        };
        if let Some(kind) = self.recorder.before(operation).await {
            return Err(injected_error_014(kind, operation));
        }
        let mut result = self.inner.get_opts(location, options).await;
        self.recorder.record_result(operation, &result);
        if let Ok(response) = &mut result {
            let object_store_014::GetResultPayload::Stream(stream) = &mut response.payload else {
                if operation == Operation::Get {
                    self.recorder.record_read(
                        usize::try_from(response.range.end.saturating_sub(response.range.start))
                            .unwrap_or(usize::MAX),
                    );
                }
                return result;
            };
            let recorder = Arc::clone(&self.recorder);
            let original = std::mem::replace(stream, futures::stream::empty().boxed());
            *stream = original
                .map(move |chunk| {
                    recorder.record_result(Operation::Get, &chunk);
                    if let Ok(bytes) = &chunk {
                        recorder.record_read(bytes.len());
                    }
                    chunk
                })
                .boxed();
        }
        result
    }

    async fn get_ranges(
        &self,
        location: &object_store_014::path::Path,
        ranges: &[Range<u64>],
    ) -> object_store_014::Result<Vec<Bytes>> {
        if let Some(kind) = self.recorder.before(Operation::Get).await {
            return Err(injected_error_014(kind, Operation::Get));
        }
        let result = self.inner.get_ranges(location, ranges).await;
        self.recorder.record_result(Operation::Get, &result);
        if let Ok(values) = &result {
            self.recorder
                .record_read(values.iter().map(Bytes::len).sum());
        }
        result
    }

    fn delete_stream(
        &self,
        locations: BoxStream<'static, object_store_014::Result<object_store_014::path::Path>>,
    ) -> BoxStream<'static, object_store_014::Result<object_store_014::path::Path>> {
        let inner = Arc::clone(&self.inner);
        let recorder = Arc::clone(&self.recorder);
        futures::stream::once(async move {
            if let Some(kind) = recorder.before(Operation::Delete).await {
                return Err(injected_error_014(kind, Operation::Delete));
            }
            Ok(inner.delete_stream(locations))
        })
        .try_flatten()
        .boxed()
    }

    fn list(
        &self,
        prefix: Option<&object_store_014::path::Path>,
    ) -> BoxStream<'static, object_store_014::Result<object_store_014::ObjectMeta>> {
        let inner = Arc::clone(&self.inner);
        let recorder = Arc::clone(&self.recorder);
        let prefix = prefix.cloned();
        futures::stream::once(async move {
            if let Some(kind) = recorder.before(Operation::List).await {
                return Err(injected_error_014(kind, Operation::List));
            }
            Ok(inner.list(prefix.as_ref()))
        })
        .try_flatten()
        .boxed()
    }

    async fn list_with_delimiter(
        &self,
        prefix: Option<&object_store_014::path::Path>,
    ) -> object_store_014::Result<object_store_014::ListResult> {
        if let Some(kind) = self.recorder.before(Operation::List).await {
            return Err(injected_error_014(kind, Operation::List));
        }
        let result = self.inner.list_with_delimiter(prefix).await;
        self.recorder.record_result(Operation::List, &result);
        result
    }

    async fn copy_opts(
        &self,
        from: &object_store_014::path::Path,
        to: &object_store_014::path::Path,
        options: object_store_014::CopyOptions,
    ) -> object_store_014::Result<()> {
        if let Some(kind) = self.recorder.before(Operation::Copy).await {
            return Err(injected_error_014(kind, Operation::Copy));
        }
        let result = self.inner.copy_opts(from, to, options).await;
        self.recorder.record_result(Operation::Copy, &result);
        result
    }
}

fn is_wal_path(path: &str) -> bool {
    path.split('/').any(|segment| segment == "wal")
}

#[cfg(test)]
mod tests {
    use std::num::NonZeroU64;

    use futures::TryStreamExt;
    use object_store::ObjectStore as _;
    use object_store_014::ObjectStore as _;
    use object_store_014::ObjectStoreExt as _;

    use super::*;

    #[tokio::test]
    async fn records_012_bytes_requests_and_injected_errors() {
        let recorder = ObjectStoreRecorder::new(FaultPolicy::failing(
            Duration::ZERO,
            FaultKind::Transient,
            Operation::Get,
            NonZeroU64::new(2).expect("two is nonzero"),
        ));
        let store = InstrumentedStore012::new(
            Arc::new(object_store::memory::InMemory::new()),
            Arc::clone(&recorder),
        );
        let path = object_store::path::Path::from("object");
        store
            .put(&path, Bytes::from_static(b"payload").into())
            .await
            .unwrap();
        assert_eq!(
            store.get(&path).await.unwrap().bytes().await.unwrap(),
            b"payload"[..]
        );
        assert!(store.get(&path).await.is_err());
        assert_eq!(
            recorder.snapshot(),
            ObjectStoreMetrics {
                get_requests: 2,
                head_requests: 0,
                put_requests: 1,
                multipart_requests: 0,
                list_requests: 0,
                delete_requests: 0,
                copy_requests: 0,
                errors: 1,
                injected_errors: 1,
                bytes_read: 7,
                bytes_written: 7,
                maximum_read_request_bytes: 7,
                maximum_write_request_bytes: 7,
                maximum_wal_object_bytes: 0,
                added_latency_millis: 0,
            }
        );
    }

    #[tokio::test]
    async fn records_014_bytes_requests_and_injected_errors() {
        let recorder = ObjectStoreRecorder::new(FaultPolicy::failing(
            Duration::ZERO,
            FaultKind::Timeout,
            Operation::Head,
            NonZeroU64::new(1).expect("one is nonzero"),
        ));
        let store = InstrumentedStore014::new(
            Arc::new(object_store_014::memory::InMemory::new()),
            Arc::clone(&recorder),
        );
        let path = object_store_014::path::Path::from("graph/wal/object");
        store
            .put(&path, Bytes::from_static(b"payload").into())
            .await
            .unwrap();
        assert!(store.head(&path).await.is_err());
        assert_eq!(recorder.snapshot().head_requests, 1);
        assert_eq!(recorder.snapshot().errors, 1);
        assert_eq!(recorder.snapshot().bytes_written, 7);
        assert_eq!(recorder.snapshot().maximum_write_request_bytes, 7);
        assert_eq!(recorder.snapshot().maximum_wal_object_bytes, 7);
    }

    #[tokio::test]
    async fn every_014_fault_class_and_operation_is_retryable() {
        let kinds = [
            FaultKind::Transient,
            FaultKind::Timeout,
            FaultKind::Throttled,
            FaultKind::ConnectionLoss,
        ];
        let operations = [
            Operation::Get,
            Operation::Head,
            Operation::Put,
            Operation::Multipart,
            Operation::List,
            Operation::Delete,
            Operation::Copy,
        ];

        for kind in kinds {
            for operation in operations {
                let recorder = ObjectStoreRecorder::new(FaultPolicy::failing(
                    Duration::ZERO,
                    kind,
                    operation,
                    NonZeroU64::MIN,
                ));
                let store = InstrumentedStore014::new(
                    Arc::new(object_store_014::memory::InMemory::new()),
                    Arc::clone(&recorder),
                );
                let source = object_store_014::path::Path::from("source");
                let target = object_store_014::path::Path::from("target");

                match operation {
                    Operation::Get => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.get(&source).await.is_err());
                        store.get(&source).await.expect("get retry succeeds");
                    }
                    Operation::Head => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.head(&source).await.is_err());
                        store.head(&source).await.expect("head retry succeeds");
                    }
                    Operation::Put => {
                        assert!(store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .is_err());
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("put retry succeeds");
                    }
                    Operation::Multipart => {
                        assert!(store.put_multipart(&source).await.is_err());
                        let mut upload = store
                            .put_multipart(&source)
                            .await
                            .expect("multipart retry starts");
                        upload
                            .put_part(Bytes::from_static(b"payload").into())
                            .await
                            .expect("multipart retry part writes");
                        upload.complete().await.expect("multipart retry completes");
                    }
                    Operation::List => {
                        assert!(store.list(None).try_collect::<Vec<_>>().await.is_err());
                        store
                            .list(None)
                            .try_collect::<Vec<_>>()
                            .await
                            .expect("list retry succeeds");
                    }
                    Operation::Delete => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.delete(&source).await.is_err());
                        store.delete(&source).await.expect("delete retry succeeds");
                    }
                    Operation::Copy => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.copy(&source, &target).await.is_err());
                        store
                            .copy(&source, &target)
                            .await
                            .expect("copy retry succeeds");
                    }
                }

                let metrics = recorder.snapshot();
                assert_eq!(metrics.injected_errors, 1, "{kind:?} {operation:?}");
                assert_eq!(metrics.errors, 1, "{kind:?} {operation:?}");
            }
        }
    }

    #[tokio::test]
    async fn every_012_fault_class_and_operation_is_retryable() {
        let kinds = [
            FaultKind::Transient,
            FaultKind::Timeout,
            FaultKind::Throttled,
            FaultKind::ConnectionLoss,
        ];
        let operations = [
            Operation::Get,
            Operation::Head,
            Operation::Put,
            Operation::Multipart,
            Operation::List,
            Operation::Delete,
            Operation::Copy,
        ];

        for kind in kinds {
            for operation in operations {
                let recorder = ObjectStoreRecorder::new(FaultPolicy::failing(
                    Duration::ZERO,
                    kind,
                    operation,
                    NonZeroU64::MIN,
                ));
                let store = InstrumentedStore012::new(
                    Arc::new(object_store::memory::InMemory::new()),
                    Arc::clone(&recorder),
                );
                let source = object_store::path::Path::from("source");
                let target = object_store::path::Path::from("target");

                match operation {
                    Operation::Get => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.get(&source).await.is_err());
                        store.get(&source).await.expect("get retry succeeds");
                    }
                    Operation::Head => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.head(&source).await.is_err());
                        store.head(&source).await.expect("head retry succeeds");
                    }
                    Operation::Put => {
                        assert!(store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .is_err());
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("put retry succeeds");
                    }
                    Operation::Multipart => {
                        assert!(store.put_multipart(&source).await.is_err());
                        let mut upload = store
                            .put_multipart(&source)
                            .await
                            .expect("multipart retry starts");
                        upload
                            .put_part(Bytes::from_static(b"payload").into())
                            .await
                            .expect("multipart retry part writes");
                        upload.complete().await.expect("multipart retry completes");
                    }
                    Operation::List => {
                        assert!(store.list(None).try_collect::<Vec<_>>().await.is_err());
                        store
                            .list(None)
                            .try_collect::<Vec<_>>()
                            .await
                            .expect("list retry succeeds");
                    }
                    Operation::Delete => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.delete(&source).await.is_err());
                        store.delete(&source).await.expect("delete retry succeeds");
                    }
                    Operation::Copy => {
                        store
                            .put(&source, Bytes::from_static(b"payload").into())
                            .await
                            .expect("fixture put succeeds");
                        assert!(store.copy(&source, &target).await.is_err());
                        store
                            .copy(&source, &target)
                            .await
                            .expect("copy retry succeeds");
                    }
                }

                let metrics = recorder.snapshot();
                assert_eq!(metrics.injected_errors, 1, "{kind:?} {operation:?}");
                assert_eq!(metrics.errors, 1, "{kind:?} {operation:?}");
            }
        }
    }
}
