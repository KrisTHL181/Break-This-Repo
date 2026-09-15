use bevy_ecs::prelude::{Query, Res};
use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
use temper_components::player::position::Position;
use temper_core::dimension::Dimension::Overworld;
use temper_macros::Command;
use temper_state::GlobalStateResource;

const MIN_Y: i32 = -64;
const NOISE_HEIGHT: usize = 384;
const COLUMN_WIDTH: usize = 16;
const NOISE_3D_LEN: usize = COLUMN_WIDTH * NOISE_HEIGHT * COLUMN_WIDTH;

#[derive(Command)]
#[command(name = "noise", aliases = ["noises"])]
struct NoiseCommand;

impl CommandHandler for NoiseCommand {
    type SystemParam<'w, 's> = (
        Res<'w, GlobalStateResource>,
        Query<'w, 's, &'static Position>,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (global_state, positions) = params;

        let player_pos = match source {
            CommandSource::Server => return Err("Only players can view terrain noise.".into()),
            CommandSource::Player(entity) => {
                positions.get(entity).map_err(|_| "sender does not exist")?
            }
        };

        let block_x = player_pos.x.floor() as i32;
        let block_y = player_pos.y.floor() as i32;
        let block_z = player_pos.z.floor() as i32;
        let chunk_pos = player_pos.chunk();
        let local_x = block_x.rem_euclid(16) as u8;
        let local_z = block_z.rem_euclid(16) as u8;

        let chunk = global_state
            .0
            .world
            .get_chunk(chunk_pos, Overworld)
            .map_err(|err| format!("Chunk {chunk_pos} is not loaded: {err}"))?;

        let x = usize::from(local_x);
        let z = usize::from(local_z);
        let noises = &chunk.noise;

        let base3d = format_3d_noise_sample(&noises.base3d, x, block_y, z);
        let cheese = format_3d_noise_sample(&noises.cheese_caves, x, block_y, z);
        let spaghetti = format_3d_noise_sample(&noises.spaghetti_caves, x, block_y, z);
        let noodle = format_3d_noise_sample(&noises.noddle_caves, x, block_y, z);

        source.send_message(
            format!(
                "Noise at ({block_x}, {block_y}, {block_z}) chunk {chunk_pos} local ({local_x}, {local_z})\n\
                 stage: {}\n\
                 continentalness: {:.4}\n\
                 erosion: {:.4}\n\
                 weirdness: {:.4}\n\
                 jaggedness: {:.4}\n\
                 temperature: {:.4}\n\
                 humidity: {:.4}\n\
                 heightmaps: motion_blocking={} world_surface={}\n\
                 base3d@y: {base3d}\n\
                 cheese@y: {cheese}\n\
                 spaghetti@y: {spaghetti}\n\
                 noodle@y: {noodle}",
                chunk.stage,
                noises.continentalness[z][x],
                noises.erosion[z][x],
                noises.weirdness[z][x],
                noises.jaggedness[z][x],
                noises.temperature[z][x],
                noises.humidity[z][x],
                chunk.heightmaps.motion_blocking.get_height(local_x, local_z),
                chunk.heightmaps.world_surface.get_height(local_x, local_z),
            )
            .into(),
        );

        Ok(())
    }
}

fn format_3d_noise_sample(noise: &[f32], x: usize, world_y: i32, z: usize) -> String {
    match sample_3d_noise(noise, x, world_y, z) {
        Some(value) => format!("{value:.4}"),
        None if noise.is_empty() => "cleared".to_string(),
        None => format!("unavailable(len={})", noise.len()),
    }
}

fn sample_3d_noise(noise: &[f32], x: usize, world_y: i32, z: usize) -> Option<f32> {
    let y = usize::try_from(world_y - MIN_Y).ok()?;

    if y >= NOISE_HEIGHT || noise.len() != NOISE_3D_LEN {
        return None;
    }

    Some(noise[index3d(x, y, z)])
}

fn index3d(x: usize, y: usize, z: usize) -> usize {
    z * (NOISE_HEIGHT * COLUMN_WIDTH) + y * COLUMN_WIDTH + x
}

#[cfg(test)]
mod tests {
    use super::*;
    use temper_world_format::ChunkNoises;

    #[test]
    fn samples_3d_noise_with_generator_layout() {
        let mut noise = vec![0.0; NOISE_3D_LEN];
        noise[index3d(3, 72, 5)] = 0.25;

        assert_eq!(sample_3d_noise(&noise, 3, 8, 5), Some(0.25));
    }

    #[test]
    fn cleared_noise_is_reported_as_cleared() {
        assert_eq!(format_3d_noise_sample(&[], 0, 64, 0), "cleared");
    }

    #[test]
    fn chunk_noise_fields_are_z_then_x() {
        let mut noises = ChunkNoises::default();
        noises.continentalness[5][3] = 0.75;

        assert_eq!(noises.continentalness[5][3], 0.75);
    }
}
