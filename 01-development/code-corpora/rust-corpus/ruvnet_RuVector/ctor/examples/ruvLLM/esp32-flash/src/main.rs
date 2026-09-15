//! RuvLLM ESP32 — Tiny Agents on Heterogeneous SoCs
//!
//! Implements ADR-165: each ESP32 chip runs **one tiny-agent role** drawn from
//! the ruvllm/ruvector primitive surface defined in `lib.rs`. This is the
//! ADR-aligned replacement for the prior single-chip "tiny LLM" framing — see
//! issue #409 for why the previous transformer skeleton was misleading.
//!
//! Roles (one binary, one chip, one role):
//!   - HnswIndexer       — MicroHNSW kNN index + HashEmbedder
//!   - RagRetriever      — MicroRAG retrieval over embedded knowledge entries
//!   - AnomalySentinel   — AnomalyDetector streaming on embedding drift
//!   - MemoryArchivist   — SemanticMemory with type-tagged entries
//!   - LoraAdapter       — MicroLoRA rank-1/2 on cached activations
//!   - SpeculativeDrafter — SpeculativeDecoder federation drafter
//!   - PipelineRelay     — PipelineNode head/middle/tail
//!
//! Always-on UART CLI: `role`, `stats`, `peers`, `add <text>`, `search <q>`,
//! `recall <q>`, `check <text>`, `learn <text>`, `lora <hex>`, `set-role <name>`,
//! `help`.
//!
//! Build paths:
//!   - `--features esp32`      cross-compile to ESP-IDF, UART CLI on uart0
//!   - `--features host-test`  x86_64 / aarch64 stdio harness for CI + dev
//!   - `--features wasm`       browser shim (calls into the same primitives)

#[cfg(feature = "esp32")]
use esp_idf_svc::sys::link_patches;

use heapless::Vec as HVec;
use heapless::String as HString;
#[allow(unused_imports)]
use log::*;

// Explicit imports — avoid `prelude::*` because it brings in `Result` and
// `Error` aliases that shadow the standard ones (causes E0107/E0277).
use ruvllm_esp32::{
    Esp32Variant,
    MicroHNSW, MicroRAG, SemanticMemory, AnomalyDetector, MicroVector,
    MicroLoRA, LoRAConfig,
    HNSWConfig, RAGConfig, MemoryType, AnomalyConfig, DistanceMetric,
    federation::{ChipId, FederationMode, CommunicationBus, FederationConfig},
};

// ============================================================================
// CONFIGURATION
// ============================================================================

/// Embedding dim shared across federation messages (ADR-165 §2.3).
const EMBED_DIM: usize = 64;
/// HNSW capacity per indexer chip. 256 inflates `TinyAgent` past the main-task
/// stack on real hardware — 32 keeps the on-stack size manageable for the demo
/// while still exercising the full kNN path. CI / production should `Box` the
/// fields and bump capacity (ADR-165 §7 follow-up).
const HNSW_CAPACITY: usize = 32;

// ============================================================================
// TINY-AGENT ROLES (ADR-165 §2.1)
// ============================================================================

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Role {
    HnswIndexer,
    RagRetriever,
    AnomalySentinel,
    MemoryArchivist,
    LoraAdapter,
    SpeculativeDrafter,
    PipelineRelay,
}

impl Role {
    /// Default role per variant (ADR-165 §2.2).
    const fn default_for(variant: Esp32Variant) -> Self {
        match variant {
            Esp32Variant::Esp32 => Role::RagRetriever,
            Esp32Variant::Esp32S2 => Role::AnomalySentinel,
            Esp32Variant::Esp32S3 => Role::SpeculativeDrafter,
            Esp32Variant::Esp32C3 => Role::HnswIndexer,
            Esp32Variant::Esp32C6 => Role::MemoryArchivist,
        }
    }

    fn as_str(&self) -> &'static str {
        match self {
            Role::HnswIndexer => "HnswIndexer",
            Role::RagRetriever => "RagRetriever",
            Role::AnomalySentinel => "AnomalySentinel",
            Role::MemoryArchivist => "MemoryArchivist",
            Role::LoraAdapter => "LoraAdapter",
            Role::SpeculativeDrafter => "SpeculativeDrafter",
            Role::PipelineRelay => "PipelineRelay",
        }
    }

    fn parse(s: &str) -> Option<Self> {
        match s {
            "HnswIndexer" | "hnsw" => Some(Role::HnswIndexer),
            "RagRetriever" | "rag" => Some(Role::RagRetriever),
            "AnomalySentinel" | "anomaly" => Some(Role::AnomalySentinel),
            "MemoryArchivist" | "memory" => Some(Role::MemoryArchivist),
            "LoraAdapter" | "lora" => Some(Role::LoraAdapter),
            "SpeculativeDrafter" | "drafter" => Some(Role::SpeculativeDrafter),
            "PipelineRelay" | "relay" => Some(Role::PipelineRelay),
            _ => None,
        }
    }
}

// ============================================================================
// HASH EMBEDDER (ADR-074 Tier 1)
// ============================================================================

/// Deterministic FNV-1a + char-bigram bag, signed-INT8 normalized to ±64.
/// No floats, no model weights, no cold start. Federation-interoperable
/// because every role on every variant uses the same function.
fn hash_embed(text: &str) -> [i8; EMBED_DIM] {
    let mut acc = [0i32; EMBED_DIM];
    let bytes = text.as_bytes();

    // Unigrams (FNV-1a)
    let mut h: u32 = 0x811C9DC5;
    for (i, &b) in bytes.iter().enumerate() {
        h ^= b as u32;
        h = h.wrapping_mul(0x01000193);
        let slot = (h as usize ^ i) % EMBED_DIM;
        acc[slot] = acc[slot].saturating_add(((h >> 16) & 0xFF) as i32 - 128);
    }

    // Char bigrams
    if bytes.len() >= 2 {
        for win in bytes.windows(2) {
            let mut bh: u32 = 0x811C9DC5;
            bh ^= win[0] as u32; bh = bh.wrapping_mul(0x01000193);
            bh ^= win[1] as u32; bh = bh.wrapping_mul(0x01000193);
            let slot = (bh as usize) % EMBED_DIM;
            acc[slot] = acc[slot].saturating_add(((bh >> 8) & 0xFF) as i32 - 128);
        }
    }

    // L2-style normalize: clamp to ±64 by largest absolute (integer-only).
    let mut max_abs: i32 = 1;
    for &v in &acc { if v.abs() > max_abs { max_abs = v.abs(); } }

    let mut out = [0i8; EMBED_DIM];
    for (i, &v) in acc.iter().enumerate() {
        out[i] = ((v.saturating_mul(64)) / max_abs).clamp(-127, 127) as i8;
    }
    out
}

// ============================================================================
// TINY AGENT
// ============================================================================

struct TinyAgent {
    role: Role,
    variant: Esp32Variant,
    chip_id: ChipId,

    // Primitives — only allocated for the active role.
    hnsw: Option<MicroHNSW<EMBED_DIM, HNSW_CAPACITY>>,
    rag: Option<MicroRAG>,
    memory: Option<SemanticMemory>,
    anomaly: Option<AnomalyDetector>,
    lora: Option<MicroLoRA>,

    // Counters surfaced via `stats`.
    ops: u32,
}

impl TinyAgent {
    fn new(variant: Esp32Variant, role: Role, chip_id: ChipId) -> Self {
        let mut agent = Self {
            role, variant, chip_id,
            hnsw: None, rag: None, memory: None, anomaly: None, lora: None,
            ops: 0,
        };
        agent.activate();
        agent
    }

    fn activate(&mut self) {
        // Reset all primitives, then enable the ones this role needs.
        self.hnsw = None;
        self.rag = None;
        self.memory = None;
        self.anomaly = None;
        self.lora = None;

        match self.role {
            Role::HnswIndexer => {
                self.hnsw = Some(MicroHNSW::new(HNSWConfig {
                    m: if self.variant.has_simd() { 8 } else { 4 },
                    m_max0: if self.variant.has_simd() { 16 } else { 8 },
                    ef_construction: 32,
                    ef_search: 16,
                    metric: DistanceMetric::Euclidean,
                    binary_mode: !self.variant.has_fpu(),
                }));
            }
            Role::RagRetriever => {
                self.rag = Some(MicroRAG::new(RAGConfig::default()));
                self.hnsw = Some(MicroHNSW::new(HNSWConfig::default()));
            }
            Role::AnomalySentinel => {
                self.anomaly = Some(AnomalyDetector::new(AnomalyConfig::default()));
            }
            Role::MemoryArchivist => {
                self.memory = Some(SemanticMemory::new());
            }
            Role::LoraAdapter => {
                let cfg = LoRAConfig { rank: 2, dim: EMBED_DIM, scale: 8, frozen: false };
                self.lora = MicroLoRA::new(cfg, self.chip_id.0 as u32 ^ 0xDEAD_BEEF).ok();
            }
            Role::SpeculativeDrafter => {
                // The drafter holds an HNSW index for context lookup; the
                // SpeculativeDecoder itself is constructed per-request from
                // FederationConfig, so we don't keep it here.
                self.hnsw = Some(MicroHNSW::new(HNSWConfig::default()));
            }
            Role::PipelineRelay => {
                // Relay is stateless on data; routing comes from the
                // FederationConfig::default chain (Pipeline mode, SPI bus).
            }
        }
    }

    fn set_role(&mut self, role: Role) {
        self.role = role;
        self.activate();
    }

    // ---- HnswIndexer + SpeculativeDrafter ----
    fn hnsw_add(&mut self, text: &str) -> Result<usize, &'static str> {
        let hnsw = self.hnsw.as_mut().ok_or("role does not own hnsw")?;
        let emb = hash_embed(text);
        let v = MicroVector::<EMBED_DIM>::from_i8(&emb, hnsw.len() as u32)
            .ok_or("embed dim mismatch")?;
        let idx = hnsw.insert(&v)?;
        self.ops = self.ops.saturating_add(1);
        Ok(idx)
    }

    fn hnsw_search(&mut self, query: &str, k: usize) -> Result<HVec<u32, 8>, &'static str> {
        let hnsw = self.hnsw.as_ref().ok_or("role does not own hnsw")?;
        let emb = hash_embed(query);
        let mut ids = HVec::new();
        for r in hnsw.search(&emb, k).iter().take(k) {
            let _ = ids.push(r.id);
        }
        self.ops = self.ops.saturating_add(1);
        Ok(ids)
    }

    // ---- RagRetriever ----
    fn rag_add(&mut self, text: &str) -> Result<u32, &'static str> {
        let rag = self.rag.as_mut().ok_or("role does not own rag")?;
        let emb = hash_embed(text);
        let id = rag.add_knowledge(text, &emb, "uart-cli", 50)?;
        // mirror into the local hnsw index if present
        if let Some(hnsw) = self.hnsw.as_mut() {
            if let Some(v) = MicroVector::<EMBED_DIM>::from_i8(&emb, id) {
                let _ = hnsw.insert(&v);
            }
        }
        self.ops = self.ops.saturating_add(1);
        Ok(id)
    }

    fn rag_recall(&mut self, query: &str) -> Result<HString<128>, &'static str> {
        let rag = self.rag.as_ref().ok_or("role does not own rag")?;
        let emb = hash_embed(query);
        let res = rag.retrieve(&emb);
        self.ops = self.ops.saturating_add(1);
        let mut out = HString::new();
        if let Some((entry, _score)) = res.entries.first() {
            for c in entry.text.chars().take(127) { let _ = out.push(c); }
        } else {
            let _ = out.push_str("(no match)");
        }
        Ok(out)
    }

    // ---- AnomalySentinel ----
    fn anomaly_learn(&mut self, text: &str) -> Result<bool, &'static str> {
        let det = self.anomaly.as_mut().ok_or("role does not own anomaly")?;
        let emb = hash_embed(text);
        let r = det.add_sample(&emb)?;
        self.ops = self.ops.saturating_add(1);
        Ok(r.is_anomaly)
    }

    fn anomaly_check(&mut self, text: &str) -> Result<bool, &'static str> {
        let det = self.anomaly.as_ref().ok_or("role does not own anomaly")?;
        let emb = hash_embed(text);
        self.ops = self.ops.saturating_add(1);
        Ok(det.check(&emb).is_anomaly)
    }

    // ---- MemoryArchivist ----
    fn mem_remember(&mut self, kind: MemoryType, text: &str) -> Result<u32, &'static str> {
        let mem = self.memory.as_mut().ok_or("role does not own memory")?;
        let emb = hash_embed(text);
        let id = mem.remember(kind, text, &emb)?;
        self.ops = self.ops.saturating_add(1);
        Ok(id)
    }

    fn mem_recall(&mut self, query: &str) -> Result<HString<128>, &'static str> {
        let mem = self.memory.as_mut().ok_or("role does not own memory")?;
        let emb = hash_embed(query);
        let hits = mem.recall(&emb, 1);
        self.ops = self.ops.saturating_add(1);
        let mut out = HString::new();
        if let Some((m, _)) = hits.first() {
            for c in m.text.chars().take(127) { let _ = out.push(c); }
        } else {
            let _ = out.push_str("(no recall)");
        }
        Ok(out)
    }

    // ---- LoraAdapter ----
    fn lora_apply_demo(&mut self) -> Result<(), &'static str> {
        let lora = self.lora.as_mut().ok_or("role does not own lora")?;
        let mut input = [0i8; EMBED_DIM];
        for (i, b) in input.iter_mut().enumerate() {
            *b = ((i as i32 * 13) % 127 - 63) as i8;
        }
        let mut out = [0i32; EMBED_DIM];
        lora.apply(&input, &mut out);
        self.ops = self.ops.saturating_add(1);
        Ok(())
    }

    // ---- Stats ----
    fn stats_line(&self) -> HString<256> {
        let mut s = HString::new();
        let _ = s.push_str("role=");
        let _ = s.push_str(self.role.as_str());
        let _ = s.push_str(" variant=");
        let _ = s.push_str(variant_name(self.variant));
        let _ = s.push_str(" sram_kb=");
        let _ = s.push_str(&format_u32((self.variant.sram_bytes() / 1024) as u32));
        let _ = s.push_str(" ops=");
        let _ = s.push_str(&format_u32(self.ops));
        if let Some(h) = &self.hnsw {
            let _ = s.push_str(" hnsw=");
            let _ = s.push_str(&format_u32(h.len() as u32));
        }
        if let Some(r) = &self.rag {
            let _ = s.push_str(" rag=");
            let _ = s.push_str(&format_u32(r.len() as u32));
        }
        if let Some(m) = &self.memory {
            let _ = s.push_str(" mem=");
            let _ = s.push_str(&format_u32(m.len() as u32));
        }
        if let Some(a) = &self.anomaly {
            let _ = s.push_str(" anomaly_samples=");
            let _ = s.push_str(&format_u32(a.len() as u32));
        }
        s
    }
}

fn variant_name(v: Esp32Variant) -> &'static str {
    match v {
        Esp32Variant::Esp32 => "esp32",
        Esp32Variant::Esp32S2 => "esp32s2",
        Esp32Variant::Esp32S3 => "esp32s3",
        Esp32Variant::Esp32C3 => "esp32c3",
        Esp32Variant::Esp32C6 => "esp32c6",
    }
}

fn parse_variant(s: &str) -> Option<Esp32Variant> {
    match s {
        "esp32" => Some(Esp32Variant::Esp32),
        "esp32s2" => Some(Esp32Variant::Esp32S2),
        "esp32s3" => Some(Esp32Variant::Esp32S3),
        "esp32c3" => Some(Esp32Variant::Esp32C3),
        "esp32c6" => Some(Esp32Variant::Esp32C6),
        _ => None,
    }
}

// ============================================================================
// FEDERATION DESCRIPTOR
// ============================================================================

fn federation_descriptor(chip_id: ChipId) -> FederationConfig {
    FederationConfig {
        num_chips: 5,
        chip_id,
        mode: FederationMode::Pipeline,
        bus: CommunicationBus::Uart,
        layers_per_chip: 1,
        heads_per_chip: 1,
        enable_pipelining: true,
    }
}

// ============================================================================
// COMMAND PROCESSOR (shared by esp32 + host-test)
// ============================================================================

fn process_command(cmd: &str, agent: &mut TinyAgent) -> HString<512> {
    let mut response = HString::new();
    let cmd = cmd.trim();

    if cmd == "role" {
        let _ = response.push_str("role: ");
        let _ = response.push_str(agent.role.as_str());
    } else if cmd == "variant" {
        let _ = response.push_str("variant: ");
        let _ = response.push_str(variant_name(agent.variant));
    } else if cmd == "stats" {
        let s = agent.stats_line();
        let _ = response.push_str(&s);
    } else if cmd == "peers" {
        let fed = federation_descriptor(agent.chip_id);
        let _ = response.push_str("federation: ");
        let _ = response.push_str(&format_u32(fed.num_chips as u32));
        let _ = response.push_str(" chips, mode=Pipeline, bus=Uart, chip_id=");
        let _ = response.push_str(&format_u32(agent.chip_id.0 as u32));
    } else if let Some(rest) = cmd.strip_prefix("set-role ") {
        match Role::parse(rest.trim()) {
            Some(r) => {
                agent.set_role(r);
                let _ = response.push_str("role set to ");
                let _ = response.push_str(agent.role.as_str());
            }
            None => { let _ = response.push_str("unknown role (try: hnsw|rag|anomaly|memory|lora|drafter|relay)"); }
        }
    } else if let Some(rest) = cmd.strip_prefix("add ") {
        let r = match agent.role {
            Role::HnswIndexer | Role::SpeculativeDrafter => agent.hnsw_add(rest).map(|i| i as u32),
            Role::RagRetriever => agent.rag_add(rest),
            _ => Err("`add` requires HnswIndexer / SpeculativeDrafter / RagRetriever"),
        };
        match r {
            Ok(id) => { let _ = response.push_str("added id="); let _ = response.push_str(&format_u32(id)); }
            Err(e) => { let _ = response.push_str("err: "); let _ = response.push_str(e); }
        }
    } else if let Some(rest) = cmd.strip_prefix("search ") {
        match agent.hnsw_search(rest, 4) {
            Ok(ids) => {
                let _ = response.push_str("hits: ");
                for (i, id) in ids.iter().enumerate() {
                    if i > 0 { let _ = response.push_str(","); }
                    let _ = response.push_str(&format_u32(*id));
                }
                if ids.is_empty() { let _ = response.push_str("(none)"); }
            }
            Err(e) => { let _ = response.push_str("err: "); let _ = response.push_str(e); }
        }
    } else if let Some(rest) = cmd.strip_prefix("recall ") {
        let r = match agent.role {
            Role::RagRetriever => agent.rag_recall(rest),
            Role::MemoryArchivist => agent.mem_recall(rest),
            _ => Err("`recall` requires RagRetriever or MemoryArchivist"),
        };
        match r {
            Ok(s) => { let _ = response.push_str("top: "); let _ = response.push_str(&s); }
            Err(e) => { let _ = response.push_str("err: "); let _ = response.push_str(e); }
        }
    } else if let Some(rest) = cmd.strip_prefix("learn ") {
        match agent.anomaly_learn(rest) {
            Ok(was_anom) => {
                let _ = response.push_str("learned, prev_was_anomaly=");
                let _ = response.push_str(if was_anom { "true" } else { "false" });
            }
            Err(e) => { let _ = response.push_str("err: "); let _ = response.push_str(e); }
        }
    } else if let Some(rest) = cmd.strip_prefix("check ") {
        match agent.anomaly_check(rest) {
            Ok(is_anom) => {
                let _ = response.push_str(if is_anom { "ANOMALY" } else { "NORMAL" });
            }
            Err(e) => { let _ = response.push_str("err: "); let _ = response.push_str(e); }
        }
    } else if let Some(rest) = cmd.strip_prefix("remember ") {
        // Format: `remember <type> <text>` where type ∈ {fact,event,context,...}
        let mut parts = rest.splitn(2, ' ');
        let kind_s = parts.next().unwrap_or("");
        let text = parts.next().unwrap_or("");
        let kind = match kind_s {
            "fact" => MemoryType::Fact,
            "event" => MemoryType::Event,
            "context" => MemoryType::Context,
            "preference" => MemoryType::Preference,
            "procedure" => MemoryType::Procedure,
            "entity" => MemoryType::Entity,
            "emotion" => MemoryType::Emotion,
            "state" => MemoryType::State,
            _ => { let _ = response.push_str("unknown memory type"); return response; }
        };
        match agent.mem_remember(kind, text) {
            Ok(id) => { let _ = response.push_str("remembered id="); let _ = response.push_str(&format_u32(id)); }
            Err(e) => { let _ = response.push_str("err: "); let _ = response.push_str(e); }
        }
    } else if cmd == "lora" {
        match agent.lora_apply_demo() {
            Ok(()) => { let _ = response.push_str("lora applied (rank=2, dim=64)"); }
            Err(e) => { let _ = response.push_str("err: "); let _ = response.push_str(e); }
        }
    } else if cmd == "help" || cmd.is_empty() {
        let _ = response.push_str(HELP_TEXT);
    } else {
        let _ = response.push_str("unknown command. type 'help'");
    }

    response
}

const HELP_TEXT: &str = "ruvllm-esp32 tiny-agent (ADR-165). commands:\n\
  role | variant | stats | peers | help\n\
  set-role <hnsw|rag|anomaly|memory|lora|drafter|relay>\n\
  add <text>           (HnswIndexer / RagRetriever / SpeculativeDrafter)\n\
  search <text>        (any role with hnsw)\n\
  recall <text>        (RagRetriever / MemoryArchivist)\n\
  learn <text>         (AnomalySentinel)\n\
  check <text>         (AnomalySentinel)\n\
  remember <type> <t>  (MemoryArchivist) types: fact|event|context|...\n\
  lora                 (LoraAdapter — applies a demo rank-2 update)";

// ============================================================================
// FORMATTING
// ============================================================================

fn format_u32(n: u32) -> HString<16> {
    let mut s = HString::new();
    if n == 0 { let _ = s.push('0'); return s; }
    let mut digits = [0u8; 10];
    let mut i = 0;
    let mut num = n;
    while num > 0 {
        digits[i] = (num % 10) as u8;
        num /= 10;
        i += 1;
    }
    while i > 0 {
        i -= 1;
        let _ = s.push((b'0' + digits[i]) as char);
    }
    s
}

// ============================================================================
// ENTRY POINTS
// ============================================================================

#[cfg(feature = "esp32")]
fn main() -> anyhow::Result<()> {
    link_patches();

    // Portable stdio path — compiles for every ESP32 variant. `eprintln!`
    // routes to whatever ESP-IDF console is configured for the target
    // (USB-Serial/JTAG on S3/C3/C6, UART0 on original ESP32 and S2).
    // ADR-166 §10 documents per-chip output behavior; the interactive CLI
    // polish needs a per-chip driver-install path.

    let variant = match option_env!("RUVLLM_VARIANT") {
        Some(s) => parse_variant(s).unwrap_or(Esp32Variant::Esp32S3),
        None => Esp32Variant::Esp32S3,
    };
    let role = match option_env!("RUVLLM_ROLE") {
        Some(s) => Role::parse(s).unwrap_or_else(|| Role::default_for(variant)),
        None => Role::default_for(variant),
    };
    let chip_id = ChipId(option_env!("RUVLLM_CHIP_ID").and_then(|s| s.parse().ok()).unwrap_or(0));

    use std::io::Write as _;
    let mut err = std::io::stderr();
    let _ = writeln!(err);
    let _ = writeln!(err, "=== ruvllm-esp32 tiny-agent (ADR-165) ===");
    let _ = writeln!(err, "variant={} role={} chip_id={} sram_kb={}",
        variant_name(variant), role.as_str(), chip_id.0,
        variant.sram_bytes() / 1024);
    let _ = err.flush();

    let mut agent = TinyAgent::new(variant, role, chip_id);
    let _ = writeln!(err, "[ready] type 'help' for commands");
    let _ = writeln!(err, "{}", agent.stats_line());
    let _ = err.flush();

    use std::io::{self, BufRead};
    let stdin = io::stdin();
    for line in stdin.lock().lines() {
        let line = match line { Ok(l) => l, Err(_) => continue };
        let resp = process_command(line.trim(), &mut agent);
        let _ = writeln!(err, "{}", resp.as_str());
        let _ = err.flush();
    }

    // stdin closed; keep the device alive.
    loop { std::thread::sleep(std::time::Duration::from_secs(60)); }
}

#[cfg(all(not(feature = "esp32"), feature = "host-test"))]
fn main() {
    use std::io::{self, BufRead, Write};

    // Allow `RUVLLM_VARIANT` / `RUVLLM_ROLE` to drive host smoke tests.
    let variant = std::env::var("RUVLLM_VARIANT")
        .ok()
        .and_then(|s| parse_variant(&s))
        .unwrap_or(Esp32Variant::Esp32S3);
    let role = std::env::var("RUVLLM_ROLE")
        .ok()
        .and_then(|s| Role::parse(&s))
        .unwrap_or_else(|| Role::default_for(variant));
    let chip_id = ChipId(std::env::var("RUVLLM_CHIP_ID")
        .ok().and_then(|s| s.parse().ok()).unwrap_or(0));

    let mut agent = TinyAgent::new(variant, role, chip_id);

    println!("=== ruvllm-esp32 tiny-agent (ADR-165) — host-test ===");
    println!("variant={} role={} chip_id={} sram_kb={}",
        variant_name(variant), role.as_str(), chip_id.0, variant.sram_bytes() / 1024);
    println!("type 'help' for commands. EOF to exit.");

    let stdin = io::stdin();
    let stdout = io::stdout();
    let mut out = stdout.lock();
    let _ = write!(out, "> "); let _ = out.flush();

    for line in stdin.lock().lines() {
        let line = match line { Ok(l) => l, Err(_) => break };
        let resp = process_command(&line, &mut agent);
        println!("{}", resp.as_str());
        let _ = write!(out, "> "); let _ = out.flush();
    }
}

#[cfg(feature = "wasm")]
use wasm_bindgen::prelude::*;

#[cfg(feature = "wasm")]
#[wasm_bindgen]
pub fn wasm_init() -> String {
    "ruvllm-esp32 tiny-agent (ADR-165) WASM shim".to_string()
}

#[cfg(feature = "wasm")]
#[wasm_bindgen]
pub fn wasm_command(cmd: &str) -> String {
    let mut agent = TinyAgent::new(Esp32Variant::Esp32S3, Role::HnswIndexer, ChipId(0));
    let r = process_command(cmd, &mut agent);
    r.as_str().to_string()
}

#[cfg(all(not(feature = "esp32"), not(feature = "host-test"), not(feature = "wasm")))]
fn main() {
    eprintln!("ruvllm-esp32 tiny-agent (ADR-165)");
    eprintln!("Build with one of: --features esp32 | --features host-test | --features wasm");
}
