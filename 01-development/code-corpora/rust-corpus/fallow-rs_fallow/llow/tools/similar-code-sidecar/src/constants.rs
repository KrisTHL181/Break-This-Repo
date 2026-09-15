use serde::Serialize;

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize)]
pub struct ArtifactSpec {
    pub path: &'static str,
    pub size: u64,
    pub sha256: &'static str,
}

include!(concat!(env!("OUT_DIR"), "/protocol.rs"));

pub const DOWNLOAD_BYTES: u64 = artifact_bytes();
pub const MODEL_HUB_URL: &str = "https://huggingface.co/jinaai/jina-embeddings-v2-base-code";
pub const MODEL_LICENSE_URL: &str = "https://www.apache.org/licenses/LICENSE-2.0";
pub const DEFAULT_MAX_FUNCTIONS: usize = 256;
pub const HARD_MAX_FUNCTIONS: usize = 4_096;
pub const DEFAULT_MAX_TOTAL_SOURCE_BYTES: usize = 8 * 1024 * 1024;
pub const HARD_MAX_TOTAL_SOURCE_BYTES: usize = 32 * 1024 * 1024;
pub const DEFAULT_MAX_SOURCE_BYTES_PER_FUNCTION: usize = 256 * 1024;
pub const HARD_MAX_SOURCE_BYTES_PER_FUNCTION: usize = 1024 * 1024;
pub const DEFAULT_TIMEOUT_MS: u64 = 120_000;
pub const HARD_MAX_TIMEOUT_MS: u64 = 600_000;
pub const MAX_BATCH_SIZE: usize = 1;
pub const MAX_JSONL_LINE_BYTES: usize = 40 * 1024 * 1024;

const fn artifact_bytes() -> u64 {
    let mut total = 0;
    let mut index = 0;
    while index < ARTIFACTS.len() {
        total += ARTIFACTS[index].size;
        index += 1;
    }
    total
}
