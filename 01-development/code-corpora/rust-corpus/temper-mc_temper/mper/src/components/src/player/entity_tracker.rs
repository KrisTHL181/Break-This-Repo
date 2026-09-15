use bevy_ecs::entity::EntityHashSet;
use bevy_ecs::prelude::{Component, Entity};
use crossbeam_queue::SegQueue;
use uuid::Uuid;

/// Tracks entities that a player should start tracking, as well as entities that are currently being
/// tracked and entities that should be untracked. To track an entity, add its UUID and **entity type ID** to the `to_track` queue.
/// To untrack an entity, add its Entity to the `to_untrack` queue. The `tracking` set contains the ECS Entity IDs of currently tracked entities.
///
/// This component has several uses:
/// - It allows the server to keep track of which entities a player should be aware of and send the appropriate spawn and destroy packets.
/// - It can be used to optimize entity updates by only sending updates for entities that are currently being tracked by the player.
/// - It can be used to manage entity visibility and interactions, ensuring that players only interact with entities they are supposed to be aware of.
#[derive(Component, Default)]
pub struct EntityTracker {
    pub to_track: SegQueue<(Uuid, u16)>,
    pub tracking: EntityHashSet,
    pub to_untrack: SegQueue<Entity>,
}
