use futures_util::future::BoxFuture;
use tokio::sync::mpsc;

use crate::{AiChatMessage, AiChatStreamConfig, AiStreamEvent};

/// The agent loop owns the request future, including its credentials and network stream.
/// Dropping a cancelled loop must not leave an independently spawned model request alive.
pub struct AgentModelRequest {
    request: Option<BoxFuture<'static, ()>>,
    events: mpsc::UnboundedReceiver<AiStreamEvent>,
}

impl AgentModelRequest {
    pub fn start(config: AiChatStreamConfig, history: Vec<AiChatMessage>) -> Self {
        let (sender, events) = mpsc::unbounded_channel();
        let history = crate::sanitize_api_messages_for_provider(history);
        Self {
            request: Some(Box::pin(crate::stream_chat_completion(
                config, history, sender,
            ))),
            events,
        }
    }

    pub async fn next_event(&mut self) -> Option<AiStreamEvent> {
        if let Some(request) = self.request.as_mut() {
            tokio::select! {
                event = self.events.recv() => return event,
                () = request => self.request = None,
            }
        }
        // Providers can finish after enqueueing several events. Deliver those before EOF.
        self.events.recv().await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn dropping_a_model_request_cancels_its_pending_work() {
        let (sender, events) = mpsc::unbounded_channel();
        let (finished, mut completion) = tokio::sync::oneshot::channel::<()>();
        let mut request = AgentModelRequest {
            request: Some(Box::pin(async move {
                let _finished = finished;
                sender
                    .send(AiStreamEvent::Content("started".into()))
                    .unwrap();
                std::future::pending::<()>().await;
            })),
            events,
        };
        assert!(matches!(
            request.next_event().await,
            Some(AiStreamEvent::Content(_))
        ));
        assert!(matches!(
            completion.try_recv(),
            Err(tokio::sync::oneshot::error::TryRecvError::Empty)
        ));
        drop(request);
        assert!(completion.await.is_err());
    }

    #[tokio::test]
    async fn provider_completion_does_not_discard_queued_events() {
        let (sender, events) = mpsc::unbounded_channel();
        let mut request = AgentModelRequest {
            request: Some(Box::pin(async move {
                sender
                    .send(AiStreamEvent::Content("answer".into()))
                    .unwrap();
                sender.send(AiStreamEvent::Done).unwrap();
            })),
            events,
        };
        assert!(
            matches!(request.next_event().await, Some(AiStreamEvent::Content(text)) if text == "answer")
        );
        assert!(matches!(
            request.next_event().await,
            Some(AiStreamEvent::Done)
        ));
        assert!(request.next_event().await.is_none());
    }
}
