//! Bounded-memory external sorting and exact merge-join parity.
//!
//! The verifier deliberately does not use the database's indexes or retain a
//! graph-sized map. Producers emit canonical `(key, value)` records in any
//! order; sorted runs are spilled below the configured memory budget and a
//! merge join reports exact multiplicity and value differences.

use std::cmp::{Ordering, Reverse};
use std::collections::BinaryHeap;
use std::fs::{self, File};
use std::io::{BufReader, BufWriter, Read, Write};
use std::num::NonZeroUsize;
use std::path::{Path, PathBuf};

use anyhow::{bail, Context, Result};
use serde::Serialize;
use sha2::{Digest, Sha256};

const LENGTH_BYTES: usize = core::mem::size_of::<u64>();
const MERGE_FAN_IN: usize = 128;

#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
pub(crate) struct Record {
    pub(crate) key: Vec<u8>,
    pub(crate) value: Vec<u8>,
}

impl Record {
    pub(crate) fn new(key: impl Into<Vec<u8>>, value: impl Into<Vec<u8>>) -> Self {
        Self {
            key: key.into(),
            value: value.into(),
        }
    }

    fn memory_bytes(&self) -> Result<usize> {
        self.key
            .len()
            .checked_add(self.value.len())
            .and_then(|bytes| bytes.checked_add(core::mem::size_of::<Self>()))
            .context("external-sort record size overflowed usize")
    }
}

#[derive(Debug, Clone, Copy)]
pub(crate) struct SortConfig {
    pub(crate) buffer_bytes: NonZeroUsize,
}

#[derive(Debug, Clone, Default, Serialize)]
pub(crate) struct SortStats {
    pub(crate) records: u64,
    pub(crate) key_bytes: u64,
    pub(crate) value_bytes: u64,
    pub(crate) spilled_runs: u64,
    pub(crate) merge_passes: u64,
    pub(crate) sha256: String,
}

pub(crate) struct ExternalSorter {
    config: SortConfig,
    directory: PathBuf,
    name: String,
    records: Vec<Record>,
    buffered_bytes: usize,
    runs: Vec<PathBuf>,
    stats: SortStats,
}

impl ExternalSorter {
    pub(crate) fn new(
        directory: impl Into<PathBuf>,
        name: impl Into<String>,
        config: SortConfig,
    ) -> Result<Self> {
        let directory = directory.into();
        fs::create_dir_all(&directory).with_context(|| {
            format!(
                "failed to create external-sort directory {}",
                directory.display()
            )
        })?;
        Ok(Self {
            config,
            directory,
            name: name.into(),
            records: Vec::new(),
            buffered_bytes: 0,
            runs: Vec::new(),
            stats: SortStats::default(),
        })
    }

    pub(crate) fn push(&mut self, record: Record) -> Result<()> {
        let record_bytes = record.memory_bytes()?;
        if !self.records.is_empty()
            && self
                .buffered_bytes
                .checked_add(record_bytes)
                .context("external-sort buffer size overflowed usize")?
                > self.config.buffer_bytes.get()
        {
            self.spill()?;
        }
        self.stats.records = self.stats.records.saturating_add(1);
        self.stats.key_bytes = self
            .stats
            .key_bytes
            .checked_add(u64::try_from(record.key.len())?)
            .context("external-sort key byte count overflowed u64")?;
        self.stats.value_bytes = self
            .stats
            .value_bytes
            .checked_add(u64::try_from(record.value.len())?)
            .context("external-sort value byte count overflowed u64")?;
        self.buffered_bytes = self
            .buffered_bytes
            .checked_add(record_bytes)
            .context("external-sort buffer size overflowed usize")?;
        self.records.push(record);
        if self.buffered_bytes >= self.config.buffer_bytes.get() {
            self.spill()?;
        }
        Ok(())
    }

    pub(crate) fn finish(mut self, output: impl AsRef<Path>) -> Result<SortStats> {
        if !self.records.is_empty() {
            self.spill()?;
        }
        let output = output.as_ref();
        if self.runs.is_empty() {
            BufWriter::new(File::create(output)?).flush()?;
            self.stats.sha256 = hex::encode(Sha256::digest([]));
            return Ok(self.stats);
        }

        let mut pass = 0_usize;
        while self.runs.len() > 1 {
            let mut merged = Vec::with_capacity(self.runs.len().div_ceil(MERGE_FAN_IN));
            for (group, inputs) in self.runs.chunks(MERGE_FAN_IN).enumerate() {
                let path = self
                    .directory
                    .join(format!("{}.merge-{pass:03}-{group:06}.bin", self.name));
                merge_runs(inputs, &path)?;
                for input in inputs {
                    fs::remove_file(input).with_context(|| {
                        format!("failed to remove merged run {}", input.display())
                    })?;
                }
                merged.push(path);
            }
            self.runs = merged;
            pass = pass.saturating_add(1);
            self.stats.merge_passes = self.stats.merge_passes.saturating_add(1);
        }

        fs::rename(&self.runs[0], output).or_else(|rename_error| {
            fs::copy(&self.runs[0], output)?;
            fs::remove_file(&self.runs[0])?;
            if output.exists() {
                Ok(())
            } else {
                Err(rename_error)
            }
        })?;
        self.stats.sha256 = sha256_file(output)?;
        Ok(self.stats)
    }

    fn spill(&mut self) -> Result<()> {
        self.records.sort_unstable();
        let path = self
            .directory
            .join(format!("{}.run-{:06}.bin", self.name, self.runs.len()));
        let mut writer = BufWriter::new(
            File::create(&path)
                .with_context(|| format!("failed to create run {}", path.display()))?,
        );
        for record in &self.records {
            write_record(&mut writer, record)?;
        }
        writer.flush()?;
        self.records.clear();
        self.buffered_bytes = 0;
        self.runs.push(path);
        self.stats.spilled_runs = self.stats.spilled_runs.saturating_add(1);
        Ok(())
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize)]
#[serde(rename_all = "snake_case")]
pub(crate) enum DifferenceKind {
    MissingFromTarget,
    UnexpectedInTarget,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
pub(crate) struct Difference {
    pub(crate) kind: DifferenceKind,
    pub(crate) key_hex: String,
    pub(crate) value_sha256: String,
    pub(crate) value_bytes: u64,
}

#[derive(Debug, Clone, Default, Serialize)]
pub(crate) struct Comparison {
    pub(crate) source_records: u64,
    pub(crate) target_records: u64,
    pub(crate) equal_records: u64,
    pub(crate) differences: u64,
    pub(crate) first_differences: Vec<Difference>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum ComparisonPolicy {
    Exact,
    SourceSubset,
}

impl Comparison {
    pub(crate) const fn is_equal(&self) -> bool {
        self.differences == 0
    }
}

pub(crate) fn compare_sorted(
    source: impl AsRef<Path>,
    target: impl AsRef<Path>,
    first_difference_limit: usize,
) -> Result<Comparison> {
    compare_sorted_with_policy(
        source,
        target,
        first_difference_limit,
        ComparisonPolicy::Exact,
    )
}

pub(crate) fn compare_sorted_with_policy(
    source: impl AsRef<Path>,
    target: impl AsRef<Path>,
    first_difference_limit: usize,
    policy: ComparisonPolicy,
) -> Result<Comparison> {
    let mut source = RecordReader::open(source.as_ref())?;
    let mut target = RecordReader::open(target.as_ref())?;
    let mut source_record = source.next_record()?;
    let mut target_record = target.next_record()?;
    let mut comparison = Comparison::default();

    while source_record.is_some() || target_record.is_some() {
        match (&source_record, &target_record) {
            (Some(left), Some(right)) => match left.cmp(right) {
                Ordering::Equal => {
                    comparison.source_records = comparison.source_records.saturating_add(1);
                    comparison.target_records = comparison.target_records.saturating_add(1);
                    comparison.equal_records = comparison.equal_records.saturating_add(1);
                    source_record = source.next_record()?;
                    target_record = target.next_record()?;
                }
                Ordering::Less => {
                    comparison.source_records = comparison.source_records.saturating_add(1);
                    record_difference(
                        &mut comparison,
                        DifferenceKind::MissingFromTarget,
                        left,
                        first_difference_limit,
                    )?;
                    source_record = source.next_record()?;
                }
                Ordering::Greater => {
                    comparison.target_records = comparison.target_records.saturating_add(1);
                    if policy == ComparisonPolicy::Exact {
                        record_difference(
                            &mut comparison,
                            DifferenceKind::UnexpectedInTarget,
                            right,
                            first_difference_limit,
                        )?;
                    }
                    target_record = target.next_record()?;
                }
            },
            (Some(left), None) => {
                comparison.source_records = comparison.source_records.saturating_add(1);
                record_difference(
                    &mut comparison,
                    DifferenceKind::MissingFromTarget,
                    left,
                    first_difference_limit,
                )?;
                source_record = source.next_record()?;
            }
            (None, Some(right)) => {
                comparison.target_records = comparison.target_records.saturating_add(1);
                if policy == ComparisonPolicy::Exact {
                    record_difference(
                        &mut comparison,
                        DifferenceKind::UnexpectedInTarget,
                        right,
                        first_difference_limit,
                    )?;
                }
                target_record = target.next_record()?;
            }
            (None, None) => break,
        }
    }
    Ok(comparison)
}

/// Merge current edge output with legacy candidates while using a separate
/// pre-migration current stream to decide legacy pair equivalence.
///
/// Proper resolves legacy pair duplicates before vector properties are
/// materialized. Keeping the equivalence stream separate models that ordering
/// exactly while the output stream carries the post-materialization values.
pub(crate) fn merge_current_and_legacy_edges_with_equivalence(
    current_output: impl AsRef<Path>,
    current_equivalence: impl AsRef<Path>,
    legacy: impl AsRef<Path>,
    output: impl AsRef<Path>,
) -> Result<SortStats> {
    let output = output.as_ref();
    let file_name = output
        .file_name()
        .and_then(|name| name.to_str())
        .context("edge merge output path has no UTF-8 file name")?;
    let filtered_legacy = output.with_file_name(format!("{file_name}.filtered-legacy"));
    filter_equivalent_legacy_edges(current_equivalence, legacy, &filtered_legacy)?;
    let result = merge_sorted_preserving_all(current_output, &filtered_legacy, output);
    fs::remove_file(&filtered_legacy).with_context(|| {
        format!(
            "failed to remove filtered legacy edge stream {}",
            filtered_legacy.display()
        )
    })?;
    result
}

fn filter_equivalent_legacy_edges(
    current_equivalence: impl AsRef<Path>,
    legacy: impl AsRef<Path>,
    output: impl AsRef<Path>,
) -> Result<()> {
    let mut current = RecordReader::open(current_equivalence.as_ref())?;
    let mut legacy = RecordReader::open(legacy.as_ref())?;
    let mut current_record = current.next_record()?;
    let mut legacy_record = legacy.next_record()?;
    let mut writer = BufWriter::new(File::create(output.as_ref())?);

    while current_record.is_some() || legacy_record.is_some() {
        match (&current_record, &legacy_record) {
            (Some(current_value), Some(legacy_value)) => match current_value.cmp(legacy_value) {
                Ordering::Less => {
                    current_record = current.next_record()?;
                }
                Ordering::Equal => {
                    let matched = legacy_value.clone();
                    while current_record.as_ref() == Some(&matched) {
                        current_record = current.next_record()?;
                    }
                    while legacy_record.as_ref() == Some(&matched) {
                        legacy_record = legacy.next_record()?;
                    }
                }
                Ordering::Greater => {
                    write_record(&mut writer, legacy_value)?;
                    legacy_record = legacy.next_record()?;
                }
            },
            (Some(_), None) => {
                current_record = current.next_record()?;
            }
            (None, Some(legacy_value)) => {
                write_record(&mut writer, legacy_value)?;
                legacy_record = legacy.next_record()?;
            }
            (None, None) => break,
        }
    }
    writer.flush()?;
    Ok(())
}

fn merge_sorted_preserving_all(
    left: impl AsRef<Path>,
    right: impl AsRef<Path>,
    output: impl AsRef<Path>,
) -> Result<SortStats> {
    let mut left = RecordReader::open(left.as_ref())?;
    let mut right = RecordReader::open(right.as_ref())?;
    let mut left_record = left.next_record()?;
    let mut right_record = right.next_record()?;
    let mut writer = BufWriter::new(File::create(output.as_ref())?);
    let mut stats = SortStats::default();
    while left_record.is_some() || right_record.is_some() {
        let take_left = match (&left_record, &right_record) {
            (Some(left), Some(right)) => left <= right,
            (Some(_), None) => true,
            (None, Some(_)) => false,
            (None, None) => break,
        };
        let record = if take_left {
            let record = left_record
                .take()
                .expect("left edge record is present when selected");
            left_record = left.next_record()?;
            record
        } else {
            let record = right_record
                .take()
                .expect("right edge record is present when selected");
            right_record = right.next_record()?;
            record
        };
        write_record(&mut writer, &record)?;
        include_output_record(&mut stats, &record)?;
    }
    writer.flush()?;
    stats.sha256 = sha256_file(output.as_ref())?;
    Ok(stats)
}

pub(crate) fn deduplicate_sorted(
    input: impl AsRef<Path>,
    output: impl AsRef<Path>,
) -> Result<SortStats> {
    let mut reader = RecordReader::open(input.as_ref())?;
    let mut writer = BufWriter::new(File::create(output.as_ref())?);
    let mut previous = None;
    let mut stats = SortStats::default();
    while let Some(record) = reader.next_record()? {
        if previous.as_ref() == Some(&record) {
            continue;
        }
        write_record(&mut writer, &record)?;
        include_output_record(&mut stats, &record)?;
        previous = Some(record);
    }
    writer.flush()?;
    stats.sha256 = sha256_file(output.as_ref())?;
    Ok(stats)
}

fn include_output_record(stats: &mut SortStats, record: &Record) -> Result<()> {
    stats.records = stats.records.saturating_add(1);
    stats.key_bytes = stats
        .key_bytes
        .checked_add(u64::try_from(record.key.len())?)
        .context("sorted-stream key bytes overflowed u64")?;
    stats.value_bytes = stats
        .value_bytes
        .checked_add(u64::try_from(record.value.len())?)
        .context("sorted-stream value bytes overflowed u64")?;
    Ok(())
}

fn sha256_file(path: &Path) -> Result<String> {
    let mut reader = BufReader::new(
        File::open(path).with_context(|| format!("failed to hash {}", path.display()))?,
    );
    let mut digest = Sha256::new();
    let mut buffer = [0_u8; 64 * 1024];
    loop {
        let read = reader.read(&mut buffer)?;
        if read == 0 {
            break;
        }
        digest.update(&buffer[..read]);
    }
    Ok(hex::encode(digest.finalize()))
}

fn record_difference(
    comparison: &mut Comparison,
    kind: DifferenceKind,
    record: &Record,
    limit: usize,
) -> Result<()> {
    comparison.differences = comparison.differences.saturating_add(1);
    if comparison.first_differences.len() < limit {
        comparison.first_differences.push(Difference {
            kind,
            key_hex: hex::encode(&record.key),
            value_sha256: hex::encode(Sha256::digest(&record.value)),
            value_bytes: u64::try_from(record.value.len())?,
        });
    }
    Ok(())
}

fn merge_runs(inputs: &[PathBuf], output: &Path) -> Result<()> {
    let mut readers = inputs
        .iter()
        .map(|path| RecordReader::open(path))
        .collect::<Result<Vec<_>>>()?;
    let mut heap = BinaryHeap::new();
    for (index, reader) in readers.iter_mut().enumerate() {
        if let Some(record) = reader.next_record()? {
            heap.push(Reverse(HeapRecord { record, index }));
        }
    }
    let mut writer = BufWriter::new(File::create(output)?);
    while let Some(Reverse(HeapRecord { record, index })) = heap.pop() {
        write_record(&mut writer, &record)?;
        if let Some(next) = readers[index].next_record()? {
            heap.push(Reverse(HeapRecord {
                record: next,
                index,
            }));
        }
    }
    writer.flush()?;
    Ok(())
}

#[derive(Debug, Eq)]
struct HeapRecord {
    record: Record,
    index: usize,
}

impl Ord for HeapRecord {
    fn cmp(&self, other: &Self) -> Ordering {
        self.record
            .cmp(&other.record)
            .then_with(|| self.index.cmp(&other.index))
    }
}

impl PartialOrd for HeapRecord {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl PartialEq for HeapRecord {
    fn eq(&self, other: &Self) -> bool {
        self.record == other.record && self.index == other.index
    }
}

pub(crate) struct RecordReader {
    reader: BufReader<File>,
}

impl RecordReader {
    pub(crate) fn open(path: &Path) -> Result<Self> {
        Ok(Self {
            reader: BufReader::new(
                File::open(path)
                    .with_context(|| format!("failed to open sorted stream {}", path.display()))?,
            ),
        })
    }

    pub(crate) fn next_record(&mut self) -> Result<Option<Record>> {
        let Some(key_len) = read_length(&mut self.reader)? else {
            return Ok(None);
        };
        let value_len = read_length(&mut self.reader)?
            .context("truncated sorted stream between key and value lengths")?;
        let mut key = vec![0_u8; usize::try_from(key_len)?];
        let mut value = vec![0_u8; usize::try_from(value_len)?];
        self.reader
            .read_exact(&mut key)
            .context("truncated sorted-stream key")?;
        self.reader
            .read_exact(&mut value)
            .context("truncated sorted-stream value")?;
        Ok(Some(Record { key, value }))
    }
}

fn write_record(writer: &mut impl Write, record: &Record) -> Result<()> {
    writer.write_all(&u64::try_from(record.key.len())?.to_be_bytes())?;
    writer.write_all(&u64::try_from(record.value.len())?.to_be_bytes())?;
    writer.write_all(&record.key)?;
    writer.write_all(&record.value)?;
    Ok(())
}

fn read_length(reader: &mut impl Read) -> Result<Option<u64>> {
    let mut bytes = [0_u8; LENGTH_BYTES];
    match reader.read(&mut bytes[0..1])? {
        0 => Ok(None),
        1 => {
            reader
                .read_exact(&mut bytes[1..LENGTH_BYTES])
                .context("truncated sorted-stream length")?;
            Ok(Some(u64::from_be_bytes(bytes)))
        }
        count => bail!("length reader returned impossible byte count {count}"),
    }
}

#[cfg(test)]
mod tests {
    use tempfile::tempdir;

    use super::*;

    fn config(bytes: usize) -> SortConfig {
        SortConfig {
            buffer_bytes: NonZeroUsize::new(bytes).expect("positive test buffer"),
        }
    }

    #[test]
    fn spills_merges_and_preserves_duplicates_exactly() {
        let directory = tempdir().expect("temp directory");
        let mut left =
            ExternalSorter::new(directory.path(), "left", config(64)).expect("left sorter opens");
        let mut right =
            ExternalSorter::new(directory.path(), "right", config(37)).expect("right sorter opens");
        let records = [
            Record::new(b"z".to_vec(), b"3".to_vec()),
            Record::new(b"a".to_vec(), b"2".to_vec()),
            Record::new(b"a".to_vec(), b"1".to_vec()),
            Record::new(b"a".to_vec(), b"1".to_vec()),
        ];
        for record in records.iter().cloned() {
            left.push(record).expect("left record pushes");
        }
        for record in records.iter().rev().cloned() {
            right.push(record).expect("right record pushes");
        }
        let left_path = directory.path().join("left.sorted");
        let right_path = directory.path().join("right.sorted");
        let left_stats = left.finish(&left_path).expect("left finishes");
        let right_stats = right.finish(&right_path).expect("right finishes");

        assert!(left_stats.spilled_runs > 1);
        assert_eq!(left_stats.sha256, right_stats.sha256);
        let comparison = compare_sorted(left_path, right_path, 10).expect("streams compare");
        assert!(comparison.is_equal());
        assert_eq!(comparison.equal_records, 4);
    }

    #[test]
    fn reports_missing_and_unexpected_records_with_bounded_evidence() {
        let directory = tempdir().expect("temp directory");
        let mut left = ExternalSorter::new(directory.path(), "left-diff", config(64))
            .expect("left sorter opens");
        let mut right = ExternalSorter::new(directory.path(), "right-diff", config(64))
            .expect("right sorter opens");
        left.push(Record::new(b"same".to_vec(), b"value".to_vec()))
            .expect("same left pushes");
        right
            .push(Record::new(b"same".to_vec(), b"value".to_vec()))
            .expect("same right pushes");
        left.push(Record::new(b"changed".to_vec(), b"old".to_vec()))
            .expect("old pushes");
        right
            .push(Record::new(b"changed".to_vec(), b"new".to_vec()))
            .expect("new pushes");
        let left_path = directory.path().join("left-diff.sorted");
        let right_path = directory.path().join("right-diff.sorted");
        left.finish(&left_path).expect("left finishes");
        right.finish(&right_path).expect("right finishes");

        let comparison = compare_sorted(left_path, right_path, 1).expect("streams compare");
        assert_eq!(comparison.differences, 2);
        assert_eq!(comparison.first_differences.len(), 1);
    }

    #[test]
    fn corruption_oracle_detects_delete_duplicate_alter_and_misindex() {
        let directory = tempdir().expect("temp directory");
        let source_records = [
            Record::new(b"index/a".to_vec(), b"1".to_vec()),
            Record::new(b"index/b".to_vec(), b"2".to_vec()),
        ];
        let cases = [
            ("delete", vec![source_records[0].clone()]),
            (
                "duplicate",
                vec![
                    source_records[0].clone(),
                    source_records[1].clone(),
                    source_records[1].clone(),
                ],
            ),
            (
                "alter",
                vec![
                    source_records[0].clone(),
                    Record::new(b"index/b".to_vec(), b"changed".to_vec()),
                ],
            ),
            (
                "misindex",
                vec![
                    source_records[0].clone(),
                    Record::new(b"index/c".to_vec(), b"2".to_vec()),
                ],
            ),
        ];

        for (name, target_records) in cases {
            let mut source = ExternalSorter::new(
                directory.path(),
                format!("corrupt-source-{name}"),
                config(64),
            )
            .expect("source sorter opens");
            let mut target = ExternalSorter::new(
                directory.path(),
                format!("corrupt-target-{name}"),
                config(64),
            )
            .expect("target sorter opens");
            for record in source_records.iter().cloned() {
                source.push(record).expect("source record pushes");
            }
            for record in target_records {
                target.push(record).expect("target record pushes");
            }
            let source_path = directory
                .path()
                .join(format!("corrupt-source-{name}.sorted"));
            let target_path = directory
                .path()
                .join(format!("corrupt-target-{name}.sorted"));
            source.finish(&source_path).expect("source finishes");
            target.finish(&target_path).expect("target finishes");

            let comparison =
                compare_sorted(&source_path, &target_path, 2).expect("corrupt streams compare");
            assert!(
                comparison.differences > 0,
                "{name} corruption escaped detection"
            );
            assert!(
                !comparison.first_differences.is_empty(),
                "{name} corruption omitted first-difference evidence"
            );
            assert!(
                comparison.first_differences[0]
                    .key_hex
                    .starts_with("696e6465782f"),
                "{name} corruption report omitted the affected index key"
            );
        }
    }

    #[test]
    fn merges_legacy_fact_only_when_no_equivalent_current_fact_exists() {
        let directory = tempdir().expect("temp directory");
        let mut current = ExternalSorter::new(directory.path(), "current", config(64))
            .expect("current sorter opens");
        let mut legacy = ExternalSorter::new(directory.path(), "legacy", config(64))
            .expect("legacy sorter opens");
        let equivalent = Record::new(b"1-2".to_vec(), b"same".to_vec());
        current
            .push(equivalent.clone())
            .expect("first current pushes");
        current
            .push(equivalent.clone())
            .expect("parallel current pushes");
        legacy.push(equivalent).expect("equivalent legacy pushes");
        legacy
            .push(Record::new(b"2-3".to_vec(), b"legacy-only".to_vec()))
            .expect("legacy-only pushes");
        let current_path = directory.path().join("current.sorted");
        let legacy_path = directory.path().join("legacy.sorted");
        let output_path = directory.path().join("expected.sorted");
        current.finish(&current_path).expect("current finishes");
        legacy.finish(&legacy_path).expect("legacy finishes");

        let stats = merge_current_and_legacy_edges_with_equivalence(
            &current_path,
            &current_path,
            legacy_path,
            &output_path,
        )
        .expect("edge streams merge");

        assert_eq!(stats.records, 3);
        assert_eq!(stats.sha256.len(), 64);
        let mut reader = RecordReader::open(&output_path).expect("merged stream opens");
        let mut records = Vec::new();
        while let Some(record) = reader.next_record().expect("record reads") {
            records.push(record);
        }
        assert_eq!(records.len(), 3);
        assert_eq!(records[0], records[1]);
        assert_eq!(records[2].value, b"legacy-only");
    }

    #[test]
    fn resolves_pair_equivalence_before_current_value_materialization() {
        let directory = tempdir().expect("temp directory");
        let mut current_output =
            ExternalSorter::new(directory.path(), "current-output", config(64))
                .expect("current output sorter opens");
        let mut current_equivalence =
            ExternalSorter::new(directory.path(), "current-equivalence", config(64))
                .expect("current equivalence sorter opens");
        let mut legacy =
            ExternalSorter::new(directory.path(), "legacy-materialization", config(64))
                .expect("legacy sorter opens");
        current_output
            .push(Record::new(b"1-3".to_vec(), b"base+embedding".to_vec()))
            .expect("materialized current pushes");
        current_equivalence
            .push(Record::new(b"1-3".to_vec(), b"base".to_vec()))
            .expect("pre-materialization current pushes");
        legacy
            .push(Record::new(b"1-3".to_vec(), b"base".to_vec()))
            .expect("equivalent legacy pushes");
        legacy
            .push(Record::new(b"2-4".to_vec(), b"legacy-only".to_vec()))
            .expect("distinct legacy pushes");
        let current_output_path = directory.path().join("current-output.sorted");
        let current_equivalence_path = directory.path().join("current-equivalence.sorted");
        let legacy_path = directory.path().join("legacy-materialization.sorted");
        let output_path = directory.path().join("expected-materialization.sorted");
        current_output
            .finish(&current_output_path)
            .expect("current output finishes");
        current_equivalence
            .finish(&current_equivalence_path)
            .expect("current equivalence finishes");
        legacy.finish(&legacy_path).expect("legacy finishes");

        let stats = merge_current_and_legacy_edges_with_equivalence(
            current_output_path,
            current_equivalence_path,
            legacy_path,
            &output_path,
        )
        .expect("ordered migration edge streams merge");

        assert_eq!(stats.records, 2);
        let mut reader = RecordReader::open(&output_path).expect("merged stream opens");
        assert_eq!(
            reader.next_record().expect("record reads"),
            Some(Record::new(b"1-3".to_vec(), b"base+embedding".to_vec()))
        );
        assert_eq!(
            reader.next_record().expect("record reads"),
            Some(Record::new(b"2-4".to_vec(), b"legacy-only".to_vec()))
        );
        assert_eq!(reader.next_record().expect("eof reads"), None);
    }
}
