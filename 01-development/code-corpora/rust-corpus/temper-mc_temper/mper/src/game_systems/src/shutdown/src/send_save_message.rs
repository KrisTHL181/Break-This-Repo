use bevy_ecs::prelude::{MessageWriter, Res};
use temper_messages::save_chunk_entities::SaveChunkEntities;
use temper_state::GlobalStateResource;

pub fn send_save_message(
    mut writer: MessageWriter<SaveChunkEntities>,
    state: Res<GlobalStateResource>,
) {
    for entry in state.0.world.get_cache() {
        writer.write(SaveChunkEntities(entry.key().0));
    }
}
