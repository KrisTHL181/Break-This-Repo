//! Typed JSON contract and bounded HTTP transport for anonymous telemetry.

use std::time::Duration;

use chrono::{DateTime, SecondsFormat, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use thiserror::Error;
use tokio::sync::{mpsc, oneshot};
use url::Url;
use uuid::Uuid;

pub const DEFAULT_TELEMETRY_ENDPOINT: &str = "https://telemetry.helix-db.com/v1/events";
pub const MAX_ENVELOPE_EVENTS: usize = 500;
pub const MAX_ENVELOPE_BYTES: usize = 1024 * 1024;
pub const MAX_PROPERTIES_BYTES: usize = 16 * 1024;

const MAX_CLIENT_STRING_BYTES: usize = 64;
const MAX_EVENT_NAME_BYTES: usize = 128;
const QUEUE_EVENTS: usize = 1_000;
const FLUSH_INTERVAL: Duration = Duration::from_secs(1);
const REQUEST_TIMEOUT: Duration = Duration::from_secs(2);
const SHUTDOWN_TIMEOUT: Duration = Duration::from_secs(5);

#[derive(Debug, Error)]
pub enum TelemetryError {
    #[error("telemetry endpoint must use HTTPS except for loopback tests")]
    InsecureEndpoint,
    #[error("telemetry endpoint is invalid")]
    InvalidEndpoint,
    #[error("telemetry envelope must contain between 1 and {MAX_ENVELOPE_EVENTS} events")]
    InvalidEventCount,
    #[error("telemetry envelope exceeds {MAX_ENVELOPE_BYTES} bytes")]
    EnvelopeTooLarge,
    #[error("telemetry event properties must be a JSON object")]
    PropertiesNotObject,
    #[error("telemetry event properties exceed {MAX_PROPERTIES_BYTES} bytes")]
    PropertiesTooLarge,
    #[error("telemetry event name is invalid")]
    InvalidEventName,
    #[error("{0} exceeds {MAX_CLIENT_STRING_BYTES} bytes or contains a control character")]
    InvalidClientField(&'static str),
    #[error("{0} must be RFC3339")]
    InvalidTimestamp(&'static str),
    #[error("telemetry schema version must be 1")]
    InvalidSchemaVersion,
    #[error(transparent)]
    Json(#[from] serde_json::Error),
    #[error(transparent)]
    Http(#[from] reqwest::Error),
}

#[derive(Clone, Copy, Debug, Deserialize, Eq, PartialEq, Serialize)]
pub enum Source {
    #[serde(rename = "helix-cli")]
    Cli,
    #[serde(rename = "helix-server")]
    Server,
    #[serde(rename = "helix-embedded")]
    Embedded,
}

#[derive(Clone, Debug, Deserialize, Eq, PartialEq, Serialize)]
pub struct ClientInfo {
    pub version: String,
    pub os: String,
    pub arch: String,
    pub installation_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub user_id: Option<String>,
}

impl ClientInfo {
    pub fn new(
        installation_id: impl Into<String>,
        user_id: Option<String>,
    ) -> Result<Self, TelemetryError> {
        let client = Self {
            version: env!("CARGO_PKG_VERSION").to_owned(),
            os: std::env::consts::OS.to_owned(),
            arch: std::env::consts::ARCH.to_owned(),
            installation_id: installation_id.into(),
            user_id,
        };
        client.validate()?;
        Ok(client)
    }

    fn validate(&self) -> Result<(), TelemetryError> {
        validate_client_field("client.version", &self.version)?;
        validate_client_field("client.os", &self.os)?;
        validate_client_field("client.arch", &self.arch)?;
        validate_client_field("client.installation_id", &self.installation_id)?;
        if let Some(user_id) = &self.user_id {
            validate_client_field("client.user_id", user_id)?;
        }
        Ok(())
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct Event {
    pub event_id: String,
    pub name: String,
    pub occurred_at: String,
    pub properties: Value,
}

impl Event {
    pub fn new(name: impl Into<String>, properties: Value) -> Result<Self, TelemetryError> {
        Self::with_identity(
            Uuid::now_v7().to_string(),
            name,
            timestamp_now(),
            properties,
        )
    }

    pub fn with_identity(
        event_id: impl Into<String>,
        name: impl Into<String>,
        occurred_at: impl Into<String>,
        properties: Value,
    ) -> Result<Self, TelemetryError> {
        let event = Self {
            event_id: event_id.into(),
            name: name.into(),
            occurred_at: occurred_at.into(),
            properties,
        };
        event.validate()?;
        Ok(event)
    }

    fn validate(&self) -> Result<(), TelemetryError> {
        if self.name.is_empty()
            || self.name.len() > MAX_EVENT_NAME_BYTES
            || !self
                .name
                .bytes()
                .all(|byte| byte.is_ascii_alphanumeric() || matches!(byte, b'.' | b'_' | b'-'))
        {
            return Err(TelemetryError::InvalidEventName);
        }
        validate_timestamp("occurred_at", &self.occurred_at)?;
        if !self.properties.is_object() {
            return Err(TelemetryError::PropertiesNotObject);
        }
        if serde_json::to_vec(&self.properties)?.len() > MAX_PROPERTIES_BYTES {
            return Err(TelemetryError::PropertiesTooLarge);
        }
        Ok(())
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct Envelope {
    pub schema_version: u8,
    pub source: Source,
    pub sent_at: String,
    pub client: ClientInfo,
    pub events: Vec<Event>,
}

impl Envelope {
    pub fn new(
        source: Source,
        client: ClientInfo,
        events: Vec<Event>,
    ) -> Result<Self, TelemetryError> {
        Self::with_sent_at(source, timestamp_now(), client, events)
    }

    pub fn with_sent_at(
        source: Source,
        sent_at: impl Into<String>,
        client: ClientInfo,
        events: Vec<Event>,
    ) -> Result<Self, TelemetryError> {
        let envelope = Self {
            schema_version: 1,
            source,
            sent_at: sent_at.into(),
            client,
            events,
        };
        envelope.validate()?;
        Ok(envelope)
    }

    pub fn from_slice(bytes: &[u8]) -> Result<Self, TelemetryError> {
        let envelope: Self = serde_json::from_slice(bytes)?;
        envelope.validate()?;
        Ok(envelope)
    }

    pub fn to_vec(&self) -> Result<Vec<u8>, TelemetryError> {
        self.validate()?;
        let encoded = serde_json::to_vec(self)?;
        if encoded.len() > MAX_ENVELOPE_BYTES {
            return Err(TelemetryError::EnvelopeTooLarge);
        }
        Ok(encoded)
    }

    fn validate(&self) -> Result<(), TelemetryError> {
        if self.schema_version != 1 {
            return Err(TelemetryError::InvalidSchemaVersion);
        }
        if self.events.is_empty() || self.events.len() > MAX_ENVELOPE_EVENTS {
            return Err(TelemetryError::InvalidEventCount);
        }
        validate_timestamp("sent_at", &self.sent_at)?;
        self.client.validate()?;
        self.events.iter().try_for_each(Event::validate)
    }
}

/// Splits events without exceeding the anonymous ingestion limits.
pub fn encode_envelopes(
    source: Source,
    client: &ClientInfo,
    events: Vec<Event>,
) -> Result<Vec<Vec<u8>>, TelemetryError> {
    let sent_at = timestamp_now();
    client.validate()?;
    validate_timestamp("sent_at", &sent_at)?;
    let empty_envelope_bytes = serde_json::to_vec(&Envelope {
        schema_version: 1,
        source,
        sent_at: sent_at.clone(),
        client: client.clone(),
        events: Vec::new(),
    })?
    .len();
    let mut encoded = Vec::new();
    let mut batch = Vec::new();
    let mut batch_bytes = empty_envelope_bytes;
    for event in events {
        event.validate()?;
        let event_bytes = serde_json::to_vec(&event)?.len();
        let separator_bytes = usize::from(!batch.is_empty());
        let candidate_bytes = batch_bytes
            .checked_add(separator_bytes)
            .and_then(|bytes| bytes.checked_add(event_bytes))
            .ok_or(TelemetryError::EnvelopeTooLarge)?;
        if batch.len() == MAX_ENVELOPE_EVENTS || candidate_bytes > MAX_ENVELOPE_BYTES {
            if batch.is_empty() {
                return Err(TelemetryError::EnvelopeTooLarge);
            }
            let envelope = Envelope::with_sent_at(source, sent_at.clone(), client.clone(), batch)?;
            encoded.push(envelope.to_vec()?);
            batch = vec![event];
            batch_bytes = empty_envelope_bytes
                .checked_add(event_bytes)
                .ok_or(TelemetryError::EnvelopeTooLarge)?;
        } else {
            batch.push(event);
            batch_bytes = candidate_bytes;
        }
    }
    if !batch.is_empty() {
        encoded.push(Envelope::with_sent_at(source, sent_at, client.clone(), batch)?.to_vec()?);
    }
    Ok(encoded)
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Delivery {
    Accepted,
    Rejected,
    NoResponse,
}

pub async fn post_envelope(client: &reqwest::Client, endpoint: &str, body: Vec<u8>) -> Delivery {
    match client
        .post(endpoint)
        .header(reqwest::header::CONTENT_TYPE, "application/json")
        .body(body)
        .send()
        .await
    {
        Ok(response) if response.status() == reqwest::StatusCode::ACCEPTED => Delivery::Accepted,
        Ok(_) => Delivery::Rejected,
        Err(_) => Delivery::NoResponse,
    }
}

#[derive(Clone)]
pub struct Recorder {
    sender: mpsc::Sender<Command>,
}

impl Recorder {
    /// Enqueues an event without waiting for network delivery.
    #[must_use]
    pub fn record(&self, event: Event) -> bool {
        self.sender.try_send(Command::Event(event)).is_ok()
    }
}

pub struct Runtime {
    sender: mpsc::Sender<Command>,
    join: Option<tokio::task::JoinHandle<()>>,
}

impl Runtime {
    /// Flushes queued events for a bounded amount of time.
    pub async fn shutdown(mut self) {
        let (complete_sender, complete_receiver) = oneshot::channel();
        let mut join = self
            .join
            .take()
            .expect("telemetry runtime can only be shut down once");
        if tokio::time::timeout(SHUTDOWN_TIMEOUT, async {
            let _ = self.sender.send(Command::Shutdown(complete_sender)).await;
            let _ = complete_receiver.await;
            let _ = (&mut join).await;
        })
        .await
        .is_err()
        {
            join.abort();
        }
    }
}

impl Drop for Runtime {
    fn drop(&mut self) {
        if let Some(join) = self.join.as_ref() {
            join.abort();
        }
    }
}

enum Command {
    Event(Event),
    Shutdown(oneshot::Sender<()>),
}

pub struct Started {
    pub recorder: Recorder,
    pub runtime: Runtime,
}

pub fn start(
    source: Source,
    client: ClientInfo,
    endpoint: impl Into<String>,
) -> Result<Started, TelemetryError> {
    let endpoint = endpoint.into();
    validate_endpoint(&endpoint)?;
    let http = reqwest::Client::builder()
        .timeout(REQUEST_TIMEOUT)
        .build()?;
    let (sender, receiver) = mpsc::channel(QUEUE_EVENTS);
    let join = tokio::spawn(run_transport(receiver, source, client, endpoint, http));
    Ok(Started {
        recorder: Recorder {
            sender: sender.clone(),
        },
        runtime: Runtime {
            sender,
            join: Some(join),
        },
    })
}

async fn run_transport(
    mut receiver: mpsc::Receiver<Command>,
    source: Source,
    client: ClientInfo,
    endpoint: String,
    http: reqwest::Client,
) {
    let mut events = Vec::new();
    let mut interval = tokio::time::interval(FLUSH_INTERVAL);
    interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Delay);
    interval.tick().await;
    loop {
        tokio::select! {
            command = receiver.recv() => {
                let Some(command) = command else {
                    deliver_events(source, &client, &endpoint, &http, std::mem::take(&mut events)).await;
                    return;
                };
                match command {
                    Command::Event(event) => {
                        events.push(event);
                        if events.len() == MAX_ENVELOPE_EVENTS {
                            deliver_events(source, &client, &endpoint, &http, std::mem::take(&mut events)).await;
                        }
                    }
                    Command::Shutdown(complete) => {
                        deliver_events(source, &client, &endpoint, &http, std::mem::take(&mut events)).await;
                        let _ = complete.send(());
                        return;
                    }
                }
            }
            _ = interval.tick() => {
                deliver_events(source, &client, &endpoint, &http, std::mem::take(&mut events)).await;
            }
        }
    }
}

async fn deliver_events(
    source: Source,
    client: &ClientInfo,
    endpoint: &str,
    http: &reqwest::Client,
    events: Vec<Event>,
) {
    let Ok(envelopes) = encode_envelopes(source, client, events) else {
        return;
    };
    for envelope in envelopes {
        let _ = post_envelope(http, endpoint, envelope).await;
    }
}

pub fn validate_endpoint(endpoint: &str) -> Result<(), TelemetryError> {
    let url = Url::parse(endpoint).map_err(|_| TelemetryError::InvalidEndpoint)?;
    if url.host_str().is_none()
        || !url.username().is_empty()
        || url.password().is_some()
        || url.query().is_some()
        || url.fragment().is_some()
    {
        return Err(TelemetryError::InvalidEndpoint);
    }
    let loopback = url
        .host_str()
        .and_then(|host| host.parse::<std::net::IpAddr>().ok())
        .is_some_and(|address| address.is_loopback())
        || matches!(url.host_str(), Some("localhost"));
    if url.scheme() != "https" && !(url.scheme() == "http" && loopback) {
        return Err(TelemetryError::InsecureEndpoint);
    }
    Ok(())
}

#[must_use]
pub fn timestamp_now() -> String {
    Utc::now().to_rfc3339_opts(SecondsFormat::Secs, true)
}

fn validate_timestamp(field: &'static str, timestamp: &str) -> Result<(), TelemetryError> {
    DateTime::parse_from_rfc3339(timestamp)
        .map(|_| ())
        .map_err(|_| TelemetryError::InvalidTimestamp(field))
}

fn validate_client_field(field: &'static str, value: &str) -> Result<(), TelemetryError> {
    if value.len() > MAX_CLIENT_STRING_BYTES
        || value
            .bytes()
            .any(|byte| byte.is_ascii_control() || byte == 0x7f)
    {
        return Err(TelemetryError::InvalidClientField(field));
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn client() -> ClientInfo {
        ClientInfo {
            version: "1.2.3".to_owned(),
            os: "darwin".to_owned(),
            arch: "arm64".to_owned(),
            installation_id: "installation-1".to_owned(),
            user_id: Some("user-1".to_owned()),
        }
    }

    fn event(properties: Value) -> Event {
        Event::with_identity(
            "018f6f1e-0000-7000-8000-000000000001",
            "query.execute",
            "2026-07-28T12:00:00Z",
            properties,
        )
        .expect("valid event")
    }

    #[test]
    fn exact_envelope_is_stable() {
        let envelope = Envelope::with_sent_at(
            Source::Server,
            "2026-07-28T12:00:01Z",
            client(),
            vec![event(serde_json::json!({"outcome": "succeeded"}))],
        )
        .expect("valid envelope");
        assert_eq!(
            String::from_utf8(envelope.to_vec().expect("encode envelope")).expect("UTF-8"),
            r#"{"schema_version":1,"source":"helix-server","sent_at":"2026-07-28T12:00:01Z","client":{"version":"1.2.3","os":"darwin","arch":"arm64","installation_id":"installation-1","user_id":"user-1"},"events":[{"event_id":"018f6f1e-0000-7000-8000-000000000001","name":"query.execute","occurred_at":"2026-07-28T12:00:00Z","properties":{"outcome":"succeeded"}}]}"#
        );
    }

    #[test]
    fn rejects_properties_over_backend_limit() {
        let properties = serde_json::json!({"value": "x".repeat(MAX_PROPERTIES_BYTES)});
        assert!(matches!(
            Event::new("query.execute", properties),
            Err(TelemetryError::PropertiesTooLarge)
        ));
    }

    #[test]
    fn splits_at_backend_event_limit() {
        let events = (0..MAX_ENVELOPE_EVENTS + 1)
            .map(|_| event(serde_json::json!({})))
            .collect();
        let envelopes =
            encode_envelopes(Source::Embedded, &client(), events).expect("valid envelopes");
        assert_eq!(envelopes.len(), 2);
        assert_eq!(
            Envelope::from_slice(&envelopes[0])
                .expect("first envelope")
                .events
                .len(),
            MAX_ENVELOPE_EVENTS
        );
        assert_eq!(
            Envelope::from_slice(&envelopes[1])
                .expect("second envelope")
                .events
                .len(),
            1
        );
    }

    #[test]
    fn enforces_wire_body_limit() {
        let mut events = Vec::new();
        for _ in 0..MAX_ENVELOPE_EVENTS {
            events.push(event(serde_json::json!({"value": "x".repeat(4_000)})));
        }
        let envelopes = encode_envelopes(Source::Cli, &client(), events).expect("split envelopes");
        assert!(envelopes.len() > 1);
        assert!(envelopes
            .iter()
            .all(|envelope| envelope.len() <= MAX_ENVELOPE_BYTES));
    }
}
