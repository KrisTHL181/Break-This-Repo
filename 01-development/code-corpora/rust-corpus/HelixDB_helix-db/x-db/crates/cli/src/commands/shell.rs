use crate::project::ProjectContext;
use eyre::Result;
use std::io::{self, Write as _};

/// Run an interactive JSON shell. Each non-command line must be one complete
/// v3 query request. Local requests keep the auth-disabled local path; Cloud
/// requests use the session-authenticated query broker.
pub async fn run(instance: Option<String>, compact: bool) -> Result<()> {
    let project = ProjectContext::find_and_load(None)?;
    let instance = super::query::resolve_instance_target(&project, instance)?;
    println!("Helix JSON shell for {instance}. Enter one v3 request per line; :quit exits.");

    loop {
        print!("helix> ");
        io::stdout().flush()?;
        let mut line = String::new();
        if io::stdin().read_line(&mut line)? == 0 {
            return Ok(());
        }
        let line = line.trim();
        if line.is_empty() {
            continue;
        }
        if matches!(line, ":quit" | ":exit" | "quit" | "exit") {
            return Ok(());
        }
        let Ok(request) = serde_json::from_str(line) else {
            eprintln!("invalid v3 query JSON");
            continue;
        };
        if let Err(error) =
            super::query::execute(&project, &instance, request, false, None, None, compact).await
        {
            eprintln!("{error}");
        }
    }
}
