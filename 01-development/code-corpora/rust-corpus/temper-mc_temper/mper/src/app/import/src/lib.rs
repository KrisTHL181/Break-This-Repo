use bin_cli::args::ImportArgs;
use temper_config::ServerConfig;
use temper_general_purpose::paths::get_root_path;
use temper_threadpool::ThreadPool;
use temper_world::World;
use tracing::{error, info};

/// Handles importing a world from an external source.
pub fn handle_import(import_args: ImportArgs, config: ServerConfig) {
    info!("Importing world...");

    let mut world =
        World::new(config.database.db_path.clone(), &config).expect("Failed to create world");

    let root_path = get_root_path();
    let mut import_path = root_path.join(&import_args.import_path);
    if import_path.is_relative() {
        import_path = root_path.join(import_path);
    }

    if let Err(e) = world.import(import_path, ThreadPool::new()) {
        error!("Could not import world: {}", e.to_string());
    }
}
