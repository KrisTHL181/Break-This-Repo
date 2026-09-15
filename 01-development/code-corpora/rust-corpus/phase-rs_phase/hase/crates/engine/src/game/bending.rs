use crate::types::events::{BendingType, GameEvent};
use crate::types::game_state::GameState;
use crate::types::identifiers::ObjectId;
use crate::types::player::PlayerId;

pub fn record_bending(
    state: &mut GameState,
    events: &mut Vec<GameEvent>,
    kind: BendingType,
    source_id: ObjectId,
    controller: PlayerId,
) {
    let event = match kind {
        BendingType::Fire => GameEvent::Firebend {
            source_id,
            controller,
        },
        BendingType::Air => GameEvent::Airbend {
            source_id,
            controller,
        },
        BendingType::Earth => GameEvent::Earthbend {
            source_id,
            controller,
        },
        BendingType::Water => GameEvent::Waterbend {
            source_id,
            controller,
        },
    };
    events.push(event);

    if let Some(player) = state
        .players
        .iter_mut()
        .find(|player| player.id == controller)
    {
        player.bending_types_this_turn.insert(kind);
    }
}
