use bevy_ecs::prelude::{MessageWriter, Query, Res};
use bevy_math::Vec3A;
use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
use temper_components::player::position::Position;
use temper_core::dimension::Dimension::Overworld;
use temper_macros::Command;
use temper_messages::particle::SendParticle;
use temper_particles::ParticleType;
use temper_state::GlobalStateResource;

#[derive(Command)]
#[command("heightmap")]
struct ShowHeightmap;

impl CommandHandler for ShowHeightmap {
    type SystemParam<'w, 's> = (
        Res<'w, GlobalStateResource>,
        Query<'w, 's, &'static Position>,
        MessageWriter<'w, SendParticle>,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (global_state, positions, particle_writer) = params;

        let player_pos = match source {
            CommandSource::Server => return Err("Only players can view heightmap.".into()),
            CommandSource::Player(entity) => {
                positions.get(entity).map_err(|_| "sender does not exist")?
            }
        };

        let chunk = global_state
            .0
            .world
            .get_chunk(player_pos.chunk(), Overworld)
            .expect("Chunk not found");

        for x in 0..16 {
            for z in 0..16 {
                let motion_blocking_height = chunk.heightmaps.motion_blocking.get_height(x, z);

                let particle_pos = Vec3A::new(
                    ((player_pos.chunk().x() * 16) + i32::from(x)) as f32 + 0.5,
                    f32::from(motion_blocking_height) + 1.5,
                    ((player_pos.chunk().z() * 16) + i32::from(z)) as f32 + 0.5,
                );

                particle_writer.write(SendParticle {
                    particle_type: ParticleType::EndRod,
                    position: particle_pos,
                    offset: Default::default(),
                    speed: 0.0,
                    count: 5,
                });

                let surface_height = chunk.heightmaps.world_surface.get_height(x, z);

                let particle_pos = Vec3A::new(
                    ((player_pos.chunk().x() * 16) + i32::from(x)) as f32 + 0.5,
                    f32::from(surface_height) + 1.5,
                    ((player_pos.chunk().z() * 16) + i32::from(z)) as f32 + 0.5,
                );

                particle_writer.write(SendParticle {
                    particle_type: ParticleType::Flame,
                    position: particle_pos,
                    offset: Default::default(),
                    speed: 0.0,
                    count: 5,
                });
            }
        }

        Ok(())
    }
}
