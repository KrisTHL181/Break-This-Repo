//! Self-learning acoustic memory: a vector index over reconstructed scans plus
//! anatomical graph-coherence checks.
//!
//! This is the `sonic_ct` analogue of RuVector's spatial memory: each scan is
//! embedded into a low-dimensional descriptor and stored in a navigable
//! small-world (NSW) graph, giving sub-linear nearest-neighbour lookup for
//!
//! 1. **longitudinal tracking** — comparing a patient's scans over time,
//! 2. **FWI warm-starting** — retrieving the closest previously solved
//!    reconstruction as an initial model, and
//! 3. **anomaly detection** — flagging reconstructions whose structure violates
//!    simple anatomical rules.
//!
//! The index is dependency-free and round-trips through a compact binary format
//! (`.rvf`-style) so scans become portable, auditable containers (ADR-0003).

use crate::grid::Grid;
use crate::types::Tissue;

/// One archived scan descriptor.
#[derive(Debug, Clone, PartialEq)]
pub struct ScanRecord {
    /// Stable scan identifier.
    pub id: String,
    /// Pseudonymous subject identifier (never raw PII).
    pub patient_id: String,
    /// Acquisition timestamp (unix seconds, monotonic within a subject).
    pub timestamp: u64,
    /// L2-normalised speed-map embedding.
    pub embedding: Vec<f32>,
    /// Mean Dice at archival time (quality provenance).
    pub mean_dice: f32,
    /// Mean absolute speed error at archival time (m/s).
    pub mae: f32,
}

/// Cosine similarity of two equal-length L2-normalised vectors.
#[inline]
fn cosine(a: &[f32], b: &[f32]) -> f32 {
    a.iter().zip(b).map(|(x, y)| x * y).sum()
}

/// A navigable small-world vector index over [`ScanRecord`]s.
#[derive(Debug, Clone)]
pub struct AcousticMemory {
    /// Embedding dimensionality.
    pub dim: usize,
    /// Neighbours connected per inserted node.
    pub m: usize,
    /// Search beam width.
    pub ef: usize,
    records: Vec<ScanRecord>,
    adjacency: Vec<Vec<usize>>,
    entry: Option<usize>,
}

impl AcousticMemory {
    /// Create an empty index for `dim`-dimensional embeddings.
    pub fn new(dim: usize) -> Self {
        AcousticMemory {
            dim,
            m: 8,
            ef: 24,
            records: Vec::new(),
            adjacency: Vec::new(),
            entry: None,
        }
    }

    /// Number of archived scans.
    pub fn len(&self) -> usize {
        self.records.len()
    }

    /// Whether the index is empty.
    pub fn is_empty(&self) -> bool {
        self.records.is_empty()
    }

    /// Read-only access to a record.
    pub fn record(&self, i: usize) -> Option<&ScanRecord> {
        self.records.get(i)
    }

    /// Archive a scan, wiring it into the small-world graph.
    ///
    /// Returns the new node index. Embedding length mismatches are padded or
    /// truncated to `dim` so the index never panics on caller error.
    pub fn insert(&mut self, mut rec: ScanRecord) -> usize {
        rec.embedding.resize(self.dim, 0.0);
        let id = self.records.len();

        // Connect to the M nearest existing nodes (greedy on current graph).
        let neighbours = if id == 0 {
            Vec::new()
        } else {
            self.search_internal(&rec.embedding, self.ef.max(self.m))
                .into_iter()
                .take(self.m)
                .map(|(idx, _)| idx)
                .collect::<Vec<_>>()
        };

        self.records.push(rec);
        self.adjacency.push(neighbours.clone());
        // Back-edges keep the graph navigable in both directions.
        for &nb in &neighbours {
            if !self.adjacency[nb].contains(&id) {
                self.adjacency[nb].push(id);
            }
        }
        if self.entry.is_none() {
            self.entry = Some(id);
        }
        id
    }

    /// Greedy beam search returning up to `k` `(index, similarity)` pairs,
    /// sorted by descending similarity.
    pub fn search(&self, query: &[f32], k: usize) -> Vec<(usize, f32)> {
        let mut q = query.to_vec();
        q.resize(self.dim, 0.0);
        let mut res = self.search_internal(&q, self.ef.max(k));
        res.truncate(k);
        res
    }

    fn search_internal(&self, query: &[f32], ef: usize) -> Vec<(usize, f32)> {
        let entry = match self.entry {
            Some(e) => e,
            None => return Vec::new(),
        };
        let mut visited = vec![false; self.records.len()];
        // Candidate frontier and result set, both kept small.
        let mut frontier: Vec<(usize, f32)> = vec![(entry, cosine(query, &self.records[entry].embedding))];
        visited[entry] = true;
        let mut best: Vec<(usize, f32)> = frontier.clone();

        while let Some((node, _)) = pop_best(&mut frontier) {
            for &nb in &self.adjacency[node] {
                if visited[nb] {
                    continue;
                }
                visited[nb] = true;
                let sim = cosine(query, &self.records[nb].embedding);
                frontier.push((nb, sim));
                best.push((nb, sim));
            }
            // Trim the working set to the beam width.
            best.sort_by(|a, b| b.1.total_cmp(&a.1));
            best.truncate(ef);
            // Keep exploring only the most promising frontier nodes.
            frontier.sort_by(|a, b| b.1.total_cmp(&a.1));
            frontier.truncate(ef);
            if frontier.first().map(|f| f.1).unwrap_or(f32::NEG_INFINITY)
                <= best.last().map(|b| b.1).unwrap_or(f32::NEG_INFINITY)
                && best.len() >= ef
            {
                break;
            }
        }
        best.sort_by(|a, b| b.1.total_cmp(&a.1));
        best
    }

    /// Exact brute-force search (ground truth for tests / small sets).
    pub fn search_exact(&self, query: &[f32], k: usize) -> Vec<(usize, f32)> {
        let mut q = query.to_vec();
        q.resize(self.dim, 0.0);
        let mut all: Vec<(usize, f32)> = self
            .records
            .iter()
            .enumerate()
            .map(|(i, r)| (i, cosine(&q, &r.embedding)))
            .collect();
        all.sort_by(|a, b| b.1.total_cmp(&a.1));
        all.truncate(k);
        all
    }

    /// Retrieve the best-matching prior embedding for FWI warm-starting.
    pub fn warm_start(&self, query: &[f32]) -> Option<&ScanRecord> {
        self.search(query, 1).first().and_then(|&(i, _)| self.records.get(i))
    }

    /// All records for `patient_id`, ordered by ascending timestamp.
    pub fn patient_timeline(&self, patient_id: &str) -> Vec<&ScanRecord> {
        let mut v: Vec<&ScanRecord> =
            self.records.iter().filter(|r| r.patient_id == patient_id).collect();
        v.sort_by_key(|r| r.timestamp);
        v
    }

    /// Longitudinal change between a patient's earliest and latest scans,
    /// expressed as `1 - cosine` (0 == identical, larger == more change).
    pub fn longitudinal_drift(&self, patient_id: &str) -> Option<f32> {
        let tl = self.patient_timeline(patient_id);
        if tl.len() < 2 {
            return None;
        }
        let first = tl.first().unwrap();
        let last = tl.last().unwrap();
        Some(1.0 - cosine(&first.embedding, &last.embedding))
    }
}

fn pop_best(frontier: &mut Vec<(usize, f32)>) -> Option<(usize, f32)> {
    if frontier.is_empty() {
        return None;
    }
    let mut bi = 0;
    for i in 1..frontier.len() {
        if frontier[i].1 > frontier[bi].1 {
            bi = i;
        }
    }
    Some(frontier.swap_remove(bi))
}

/// Build the embedding for a reconstruction (coarse `k×k` descriptor).
pub fn embed_speed(speed: &Grid, k: usize) -> Vec<f32> {
    speed.embedding(k)
}

// ---------------------------------------------------------------------------
// Anatomical graph coherence
// ---------------------------------------------------------------------------

/// Result of the anatomical structure check on a segmentation.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct CoherenceReport {
    /// Bone cells directly adjacent to the water bath (anatomically impossible).
    pub bone_touching_water: usize,
    /// Organ cells directly adjacent to the water bath (organs sit inside the
    /// body wall, so this signals a reconstruction artefact).
    pub organ_touching_water: usize,
    /// Whether any rule was violated beyond a small tolerance.
    pub anomaly: bool,
}

/// Run anatomical coherence rules over a label grid.
///
/// Treats the segmentation as a topological map and checks adjacency
/// constraints, e.g. `MATCH (bone)-[:TOUCHES]->(water)` should not occur.
pub fn check_coherence(labels: &Grid) -> CoherenceReport {
    let w = Tissue::Water as u8 as f32;
    let bone = Tissue::Bone as u8 as f32;
    let organ = Tissue::Organ as u8 as f32;
    let (nx, ny) = (labels.nx, labels.ny);

    let mut bone_water = 0usize;
    let mut organ_water = 0usize;
    let touches_water = |x: usize, y: usize| -> bool {
        let mut t = false;
        let mut check = |xx: i64, yy: i64| {
            if xx >= 0
                && yy >= 0
                && (xx as usize) < nx
                && (yy as usize) < ny
                && labels.data[labels.idx(xx as usize, yy as usize)] == w
            {
                t = true;
            }
        };
        check(x as i64 - 1, y as i64);
        check(x as i64 + 1, y as i64);
        check(x as i64, y as i64 - 1);
        check(x as i64, y as i64 + 1);
        t
    };

    for y in 0..ny {
        for x in 0..nx {
            let v = labels.data[labels.idx(x, y)];
            if v == bone && touches_water(x, y) {
                bone_water += 1;
            } else if v == organ && touches_water(x, y) {
                organ_water += 1;
            }
        }
    }

    // Small tolerance for boundary discretisation noise.
    let tol = (nx.max(ny) / 16).max(1);
    CoherenceReport {
        bone_touching_water: bone_water,
        organ_touching_water: organ_water,
        anomaly: bone_water > 0 || organ_water > tol,
    }
}

// ---------------------------------------------------------------------------
// Portable serialization (.rvf-style binary)
// ---------------------------------------------------------------------------

const MAGIC: &[u8; 4] = b"SCT1";

impl AcousticMemory {
    /// Serialize the index to a compact binary buffer.
    pub fn to_bytes(&self) -> Vec<u8> {
        let mut b = Vec::new();
        b.extend_from_slice(MAGIC);
        b.extend_from_slice(&(self.dim as u32).to_le_bytes());
        b.extend_from_slice(&(self.records.len() as u32).to_le_bytes());
        for r in &self.records {
            put_str(&mut b, &r.id);
            put_str(&mut b, &r.patient_id);
            b.extend_from_slice(&r.timestamp.to_le_bytes());
            b.extend_from_slice(&r.mean_dice.to_le_bytes());
            b.extend_from_slice(&r.mae.to_le_bytes());
            b.extend_from_slice(&(r.embedding.len() as u32).to_le_bytes());
            for &v in &r.embedding {
                b.extend_from_slice(&v.to_le_bytes());
            }
        }
        b
    }

    /// Reconstruct an index from [`Self::to_bytes`] output. The graph is rebuilt
    /// by re-inserting records, so search behaviour is preserved.
    pub fn from_bytes(buf: &[u8]) -> Option<AcousticMemory> {
        let mut c = Cursor { buf, pos: 0 };
        if c.take(4)? != MAGIC {
            return None;
        }
        let dim = c.u32()? as usize;
        let count = c.u32()? as usize;
        let mut mem = AcousticMemory::new(dim);
        for _ in 0..count {
            let id = get_str(&mut c)?;
            let patient_id = get_str(&mut c)?;
            let timestamp = c.u64()?;
            let mean_dice = c.f32()?;
            let mae = c.f32()?;
            let elen = c.u32()? as usize;
            let mut embedding = Vec::with_capacity(elen);
            for _ in 0..elen {
                embedding.push(c.f32()?);
            }
            mem.insert(ScanRecord {
                id,
                patient_id,
                timestamp,
                embedding,
                mean_dice,
                mae,
            });
        }
        Some(mem)
    }
}

fn put_str(b: &mut Vec<u8>, s: &str) {
    b.extend_from_slice(&(s.len() as u32).to_le_bytes());
    b.extend_from_slice(s.as_bytes());
}

struct Cursor<'a> {
    buf: &'a [u8],
    pos: usize,
}

impl<'a> Cursor<'a> {
    fn take(&mut self, n: usize) -> Option<&'a [u8]> {
        if self.pos + n > self.buf.len() {
            return None;
        }
        let s = &self.buf[self.pos..self.pos + n];
        self.pos += n;
        Some(s)
    }
    fn u32(&mut self) -> Option<u32> {
        Some(u32::from_le_bytes(self.take(4)?.try_into().ok()?))
    }
    fn u64(&mut self) -> Option<u64> {
        Some(u64::from_le_bytes(self.take(8)?.try_into().ok()?))
    }
    fn f32(&mut self) -> Option<f32> {
        Some(f32::from_le_bytes(self.take(4)?.try_into().ok()?))
    }
}

fn get_str(c: &mut Cursor) -> Option<String> {
    let n = c.u32()? as usize;
    let bytes = c.take(n)?;
    String::from_utf8(bytes.to_vec()).ok()
}
