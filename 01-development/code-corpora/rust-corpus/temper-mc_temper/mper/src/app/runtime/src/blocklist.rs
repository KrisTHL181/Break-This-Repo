use std::sync::atomic::Ordering::Relaxed;
use temper_state::GlobalState;
use tracing::debug;

const BLOCKLIST_URL: &str =
    "https://raw.githubusercontent.com/pebblehost/hunter/refs/heads/master/ips.txt";

pub async fn blocklist(state: GlobalState) -> Result<(), String> {
    while !state.shut_down.load(Relaxed) {
        debug!("Fetching blocklist from {}", BLOCKLIST_URL);
        let response = reqwest::get(BLOCKLIST_URL)
            .await
            .map_err(|e| format!("Failed to fetch blocklist: {}", e))?;
        if response.status().is_success() {
            let text = response
                .text()
                .await
                .map_err(|e| format!("Failed to read blocklist response: {}", e))?;
            state.blocked_ips.clear();
            let mut blocked = 0;
            for ip in text
                .lines()
                .map(|line| line.trim().to_string())
                .filter(|line| !line.is_empty())
                .collect::<Vec<_>>()
            {
                blocked += 1;
                state.blocked_ips.insert(ip);
            }
            debug!("Fetched blocklist successfully, {} IPs blocked", blocked);
        } else {
            return Err(format!(
                "Failed to fetch blocklist: HTTP {}",
                response.status()
            ));
        }

        // The list seems to get updated every 3 hours
        tokio::time::sleep(std::time::Duration::from_hours(3)).await;
    }

    Ok(())
}
