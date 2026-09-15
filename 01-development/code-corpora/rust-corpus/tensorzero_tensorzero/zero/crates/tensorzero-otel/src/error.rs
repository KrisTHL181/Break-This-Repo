//! Errors produced by `tensorzero-otel` during observability setup and request
//! handling. Kept crate-local so consumers don't have to depend on
//! `tensorzero-error`. Variants intentionally carry a `String` message rather
//! than richer payloads — the only callers either log the error or convert it
//! to their own error type.

use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use thiserror::Error;

#[derive(Clone, Debug, Error)]
pub enum ObservabilityError {
    /// Observability subsystem failure (exporter build, propagator init,
    /// filter reload, prometheus install, etc.).
    #[error("{0}")]
    Observability(String),

    /// Request-level validation failure (malformed custom OTLP headers, etc.).
    /// Produced by middleware that parses HTTP request headers.
    #[error("{0}")]
    InvalidRequest(String),

    /// Internal logic failure that should not occur in practice (e.g. failing
    /// to parse a built-in log directive string).
    #[error("{0}")]
    Internal(String),
}

impl ObservabilityError {
    pub fn observability(message: impl Into<String>) -> Self {
        Self::Observability(message.into())
    }

    pub fn invalid_request(message: impl Into<String>) -> Self {
        Self::InvalidRequest(message.into())
    }

    pub fn internal(message: impl Into<String>) -> Self {
        Self::Internal(message.into())
    }

    /// `true` if this represents an `InvalidRequest` — useful for middleware
    /// that wants to translate request-level errors to a 400 response.
    pub fn is_invalid_request(&self) -> bool {
        matches!(self, Self::InvalidRequest(_))
    }

    /// HTTP status this error maps to when surfaced via [`IntoResponse`].
    pub fn status_code(&self) -> StatusCode {
        match self {
            Self::InvalidRequest(_) => StatusCode::BAD_REQUEST,
            Self::Observability(_) | Self::Internal(_) => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }
}

impl IntoResponse for ObservabilityError {
    fn into_response(self) -> Response {
        (self.status_code(), self.to_string()).into_response()
    }
}
