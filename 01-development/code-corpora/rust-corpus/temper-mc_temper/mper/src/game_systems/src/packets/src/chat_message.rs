use bevy_ecs::prelude::*;
use temper_components::entity_identity::Identity;
use temper_core::mq;
use temper_protocol::ChatMessagePacketReceiver;
use temper_text::TextComponent;

pub fn handle(receiver: Res<ChatMessagePacketReceiver>, query: Query<&Identity>) {
    for (message, sender) in receiver.0.try_iter() {
        let Ok(identity) = query.get(sender) else {
            continue;
        };

        let message = format!(
            "<{}> {}",
            identity.name.as_ref().expect("No Player Name"),
            message.message
        );
        mq::broadcast(TextComponent::from(message), false);
    }
}
