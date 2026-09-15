//! Rules audit binary — tracks MTG Comprehensive Rules coverage in the engine.
//!
//! Modes:
//!   --generate           Parse CompRules.txt + scan annotations → skeleton TOMLs
//!   --generate --update  Add new rules only, preserve existing entries
//!   --ci                 Validate registry, exit 1 on errors
//!   --summary            Print human-readable coverage stats
//!   (default)            Validate registry, report to stdout/stderr

use std::collections::{BTreeMap, BTreeSet, HashMap};
use std::path::{Path, PathBuf};
use std::process;

use serde::{Deserialize, Serialize};

// ── CLI ──────────────────────────────────────────────────────────────────────

struct Config {
    rules_dir: PathBuf,
    engine_src: PathBuf,
    comp_rules: PathBuf,
    generate: bool,
    update: bool,
    ci: bool,
    summary: bool,
}

fn parse_args() -> Config {
    let args: Vec<String> = std::env::args().collect();

    let mut generate = false;
    let mut update = false;
    let mut ci = false;
    let mut summary = false;
    let mut engine_src: Option<String> = None;
    let mut comp_rules: Option<String> = None;

    let mut args_iter = args.iter().skip(1).peekable();
    while let Some(arg) = args_iter.next() {
        match arg.as_str() {
            "--generate" => generate = true,
            "--update" => update = true,
            "--ci" => ci = true,
            "--summary" => summary = true,
            "--engine-src" => engine_src = args_iter.next().cloned(),
            "--comp-rules" => comp_rules = args_iter.next().cloned(),
            _ => {}
        }
    }

    let rules_dir = args
        .iter()
        .skip(1)
        .find(|a| !a.starts_with("--"))
        .cloned()
        .unwrap_or_else(|| "rules".to_string());

    Config {
        rules_dir: PathBuf::from(rules_dir),
        engine_src: PathBuf::from(engine_src.unwrap_or_else(|| "crates/engine/src".to_string())),
        comp_rules: PathBuf::from(
            comp_rules.unwrap_or_else(|| "docs/MagicCompRules.txt".to_string()),
        ),
        generate,
        update,
        ci,
        summary,
    }
}

// ── CompRules Parser ─────────────────────────────────────────────────────────

#[derive(Debug, Clone)]
struct CompRule {
    number: String, // e.g. "704.5a"
    text: String,   // rule text (first line only, no examples)
    section: u32,   // e.g. 704
}

/// Sections included in the registry (engine-relevant).
fn is_included_section(section: u32) -> bool {
    matches!(
        section,
        100..=122
            | 200..=202
            | 204..=205
            | 207..=208
            | 300..=308
            | 310
            | 400..=408
            | 500..=514
            | 600..=616
            | 700..=732
            | 800
            | 903
    )
}

/// Parse MagicCompRules.txt into structured rules.
///
/// Each rule starts at the beginning of a line with a number like `704.5a`.
/// Rules are separated by blank lines. Example/continuation lines are skipped.
/// The TOC duplicates section headers — we skip rules appearing before the actual
/// content by tracking whether we've passed the TOC.
fn parse_comp_rules(path: &Path) -> Result<Vec<CompRule>, String> {
    let content = std::fs::read_to_string(path)
        .map_err(|e| format!("Failed to read {}: {}", path.display(), e))?;

    let mut rules = Vec::new();
    let mut seen_numbers: BTreeSet<String> = BTreeSet::new();
    // The TOC ends around line ~180; actual rules start with repeated section headers.
    // We detect the transition by looking for the first "100. General" after the TOC.
    let mut past_toc = false;
    let mut toc_header_count = 0;

    for line in content.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() {
            continue;
        }

        // Detect "100. General" — appears once in TOC and once in content
        if trimmed == "100. General" {
            toc_header_count += 1;
            if toc_header_count >= 2 {
                past_toc = true;
            }
            continue;
        }
        if !past_toc {
            continue;
        }

        // Match lines starting with a rule number: digits.digits[letter]
        let Some(number) = extract_rule_number(trimmed) else {
            continue;
        };

        // Skip section-only headers (e.g., "704. State-Based Actions") in the content
        // — these have text after the number that is just the section title
        if is_section_header(&number) {
            // Still record it if it has a meaningful body (e.g., "704. State-Based Actions")
            // but only as metadata, not as an auditable rule
            continue;
        }

        // Extract the text after the rule number
        let text = trimmed[number.len()..]
            .trim()
            .trim_start_matches('.')
            .trim()
            .to_string();

        let section = parse_section(&number);

        if !is_included_section(section) {
            continue;
        }

        if seen_numbers.insert(number.clone()) {
            rules.push(CompRule {
                number,
                text,
                section,
            });
        }
    }

    Ok(rules)
}

/// Extract the rule number from the start of a line.
/// Matches patterns like: 100.1, 100.1a, 704.5g, 702.2c
fn extract_rule_number(line: &str) -> Option<String> {
    let bytes = line.as_bytes();
    if bytes.len() < 4 {
        return None;
    }

    // Must start with 3 digits
    if !bytes[0].is_ascii_digit() || !bytes[1].is_ascii_digit() || !bytes[2].is_ascii_digit() {
        return None;
    }

    // Must have a dot after the 3 digits
    if bytes[3] != b'.' {
        return None;
    }

    let mut end = 4;

    // Consume digits after the dot (e.g., "5" in "704.5")
    while end < bytes.len() && bytes[end].is_ascii_digit() {
        end += 1;
    }

    // Optionally consume a single lowercase letter (e.g., "a" in "704.5a")
    if end < bytes.len() && bytes[end].is_ascii_lowercase() {
        // Peek ahead: the next char must be non-alphanumeric or end-of-string
        // to avoid matching words like "704.5about"
        if end + 1 >= bytes.len() || !bytes[end + 1].is_ascii_alphanumeric() {
            end += 1;
        }
    }

    // Must be followed by a non-alphanumeric character or end-of-string
    // (allows colon, dash, space, comma, paren, etc. after the rule number)
    if end < bytes.len() && bytes[end].is_ascii_alphanumeric() {
        return None;
    }

    Some(line[..end].to_string())
}

/// Check if a rule number is a section header (e.g., "704" with no sub-number).
fn is_section_header(number: &str) -> bool {
    // Section headers are just "NNN" or "NNN." with no sub-number
    let after_dot = number.split('.').nth(1).unwrap_or("");
    after_dot.is_empty()
}

/// Parse the section number from a rule number (e.g., "704" from "704.5a").
fn parse_section(number: &str) -> u32 {
    number[..3].parse().unwrap_or(0)
}

// ── Annotation Scanner ───────────────────────────────────────────────────────

#[derive(Debug, Clone)]
struct Annotation {
    file: String, // relative to engine_src
    line: u32,
    rule_number: String,
}

/// Scan engine source files for CR annotations.
/// Matches patterns like: CR 704.5a, Rule 704.5a, MTG Rule 704.5, MTG CR 704.5a
fn scan_annotations(engine_src: &Path) -> Result<Vec<Annotation>, String> {
    let mut annotations = Vec::new();

    let walker = walk_rs_files(engine_src)?;
    for entry in walker {
        let rel_path = entry
            .strip_prefix(engine_src)
            .unwrap_or(&entry)
            .to_string_lossy()
            .to_string();

        let content = std::fs::read_to_string(&entry)
            .map_err(|e| format!("Failed to read {}: {}", entry.display(), e))?;

        for (line_num, line) in content.lines().enumerate() {
            // Only search in comment lines
            let trimmed = line.trim();
            if !trimmed.starts_with("//") && !trimmed.starts_with("///") {
                continue;
            }

            // Find CR annotations in this comment line
            for rule_num in extract_cr_annotations(trimmed) {
                annotations.push(Annotation {
                    file: rel_path.clone(),
                    line: (line_num + 1) as u32,
                    rule_number: rule_num,
                });
            }
        }
    }

    Ok(annotations)
}

/// Extract CR rule numbers from a comment line.
/// Handles: "CR 704.5a", "Rule 704.5a", "MTG Rule 704.5", "MTG CR 704.5a"
fn extract_cr_annotations(line: &str) -> Vec<String> {
    let mut results = Vec::new();
    let lower = line.to_lowercase();

    // Search for patterns: "cr ", "rule ", "mtg rule ", "mtg cr "
    let prefixes = ["cr ", "rule ", "mtg rule ", "mtg cr "];

    for prefix in &prefixes {
        let mut search_from = 0;
        while let Some(pos) = lower[search_from..].find(prefix) {
            let abs_pos = search_from + pos + prefix.len();
            if abs_pos < line.len() {
                if let Some(num) = extract_rule_number(&line[abs_pos..]) {
                    // Normalize: ensure it looks like a valid CR number
                    let section = parse_section(&num);
                    if (100..=999).contains(&section) {
                        results.push(num);
                    }
                }
            }
            search_from = abs_pos;
        }
    }

    results
}

/// Walk a directory for .rs files, returning sorted paths.
fn walk_rs_files(dir: &Path) -> Result<Vec<PathBuf>, String> {
    let mut files = Vec::new();
    walk_dir_recursive(dir, &mut files)?;
    files.sort();
    Ok(files)
}

fn walk_dir_recursive(dir: &Path, files: &mut Vec<PathBuf>) -> Result<(), String> {
    let entries = std::fs::read_dir(dir)
        .map_err(|e| format!("Failed to read directory {}: {}", dir.display(), e))?;

    for entry in entries {
        let entry = entry.map_err(|e| format!("Failed to read dir entry: {}", e))?;
        let path = entry.path();
        if path.is_dir() {
            walk_dir_recursive(&path, files)?;
        } else if path.extension().is_some_and(|ext| ext == "rs") {
            files.push(path);
        }
    }
    Ok(())
}

// ── TOML Registry ────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
struct RuleEntry {
    rule: String,
    text: String,
    status: String,
    impl_files: Vec<String>,
    test_files: Vec<String>,
    notes: String,
}

/// Convert a rule number to a TOML table key: "704.5a" → "cr_704_5a"
fn rule_to_key(number: &str) -> String {
    format!("cr_{}", number.replace('.', "_"))
}

/// Section titles for included sections.
fn section_title(section: u32) -> &'static str {
    match section {
        100 => "General",
        101 => "The Magic Golden Rules",
        102 => "Players",
        103 => "Starting the Game",
        104 => "Ending the Game",
        105 => "Colors",
        106 => "Mana",
        107 => "Numbers and Symbols",
        108 => "Cards",
        109 => "Objects",
        110 => "Permanents",
        111 => "Tokens",
        112 => "Spells",
        113 => "Abilities",
        114 => "Emblems",
        115 => "Targets",
        116 => "Special Actions",
        117 => "Timing and Priority",
        118 => "Costs",
        119 => "Life",
        120 => "Damage",
        121 => "Drawing a Card",
        122 => "Counters",
        200 => "General (Parts of a Card)",
        201 => "Name",
        202 => "Mana Cost and Color",
        204 => "Mana Value",
        205 => "Type Line",
        207 => "Text Box",
        208 => "Power/Toughness",
        300 => "General (Card Types)",
        301 => "Artifacts",
        302 => "Creatures",
        303 => "Enchantments",
        304 => "Instants",
        305 => "Lands",
        306 => "Planeswalkers",
        307 => "Sorceries",
        308 => "Tribals",
        310 => "Battles",
        400 => "General (Zones)",
        401 => "Library",
        402 => "Hand",
        403 => "Battlefield",
        404 => "Graveyard",
        405 => "Stack",
        406 => "Exile",
        407 => "Ante",
        408 => "Command",
        500 => "General (Turn Structure)",
        501 => "Beginning Phase",
        502 => "Untap Step",
        503 => "Upkeep Step",
        504 => "Draw Step",
        505 => "Main Phase",
        506 => "Combat Phase",
        507 => "Beginning of Combat Step",
        508 => "Declare Attackers Step",
        509 => "Declare Blockers Step",
        510 => "Combat Damage Step",
        511 => "End of Combat Step",
        512 => "Ending Phase",
        513 => "End Step",
        514 => "Cleanup Step",
        600 => "General (Spells, Abilities, and Effects)",
        601 => "Casting Spells",
        602 => "Activating Activated Abilities",
        603 => "Handling Triggered Abilities",
        604 => "Handling Static Abilities",
        605 => "Mana Abilities",
        606 => "Loyalty Abilities",
        607 => "Linked Abilities",
        608 => "Resolving Spells and Abilities",
        609 => "Effects",
        610 => "One-Shot Effects",
        611 => "Continuous Effects",
        612 => "Text-Changing Effects",
        613 => "Interaction of Continuous Effects",
        614 => "Replacement Effects",
        615 => "Prevention Effects",
        616 => "Interaction of Replacement and/or Prevention Effects",
        700 => "General (Additional Rules)",
        701 => "Keyword Actions",
        702 => "Keyword Abilities",
        703 => "Turn-Based Actions",
        704 => "State-Based Actions",
        705 => "Flipping a Coin",
        706 => "Rolling a Die",
        707 => "Copying Objects",
        708 => "Face-Down Spells and Permanents",
        709 => "Split Cards",
        710 => "Flip Cards",
        711 => "Leveler Cards",
        712 => "Double-Faced Cards",
        713 => "Substitute Cards",
        714 => "Saga Cards",
        715 => "Adventurer Cards",
        716 => "Class Cards",
        717 => "Attraction Cards",
        718 => "Prototype Cards",
        719 => "Case Cards",
        720 => "Taking Shortcuts",
        721 => "Handling Illegal Actions",
        722 => "Ending Turns and Phases",
        723 => "The Monarch",
        724 => "The Initiative",
        725 => "The Ring Tempts You",
        726 => "Restarting the Game",
        727 => "Subgames",
        728 => "Merging with Permanents",
        729 => "Daybound and Nightbound",
        730 => "Miscellaneous",
        731 => "Controlling Another Player",
        732 => "Ending the Turn",
        800 => "General (Multiplayer Rules)",
        903 => "Commander",
        _ => "Unknown",
    }
}

// ── Generate Mode ────────────────────────────────────────────────────────────

fn generate_registry(config: &Config) -> Result<(), String> {
    let rules = parse_comp_rules(&config.comp_rules)?;
    let annotations = scan_annotations(&config.engine_src)?;

    // Build annotation index: rule_number → Vec<(file, line)>
    let mut annotation_index: HashMap<String, Vec<(String, u32)>> = HashMap::new();
    for ann in &annotations {
        annotation_index
            .entry(ann.rule_number.clone())
            .or_default()
            .push((ann.file.clone(), ann.line));
    }

    // Group rules by section
    let mut by_section: BTreeMap<u32, Vec<&CompRule>> = BTreeMap::new();
    for rule in &rules {
        by_section.entry(rule.section).or_default().push(rule);
    }

    // Create rules directory
    std::fs::create_dir_all(&config.rules_dir)
        .map_err(|e| format!("Failed to create {}: {}", config.rules_dir.display(), e))?;

    // Load existing entries if --update
    let existing: HashMap<String, HashMap<String, toml::Value>> = if config.update {
        load_existing_entries(&config.rules_dir)?
    } else {
        HashMap::new()
    };

    let mut total_rules = 0;
    let mut pre_populated = 0;
    let mut skipped_existing = 0;
    let mut files_written = 0;

    for (section, section_rules) in &by_section {
        let filename = format!("cr_{}.toml", section);
        let filepath = config.rules_dir.join(&filename);

        // In update mode, load existing file and only add new entries
        let existing_keys: BTreeSet<String> = existing
            .get(&filename)
            .map(|entries| entries.keys().cloned().collect())
            .unwrap_or_default();

        let mut output = String::new();

        // Header
        let title = section_title(*section);
        output.push_str(&format!(
            "# Comprehensive Rules Section {}: {}\n\n",
            section, title
        ));
        output.push_str(&format!(
            "[_meta]\nsection = {}\ntitle = \"{}\"\n",
            section, title
        ));

        let mut section_new = 0;
        for rule in section_rules {
            let key = rule_to_key(&rule.number);

            if config.update && existing_keys.contains(&key) {
                skipped_existing += 1;
                continue;
            }

            // Determine status from annotations
            let (status, impl_files) = if let Some(anns) = annotation_index.get(&rule.number) {
                let files: Vec<String> = anns.iter().map(|(f, l)| format!("{}:{}", f, l)).collect();
                pre_populated += 1;
                ("implemented".to_string(), files)
            } else {
                ("missing".to_string(), Vec::new())
            };

            // Escape TOML strings
            let escaped_text = escape_toml_string(&rule.text);

            output.push_str(&format!("\n[{}]\n", key));
            output.push_str(&format!("rule = \"{}\"\n", rule.number));
            output.push_str(&format!("text = \"{}\"\n", escaped_text));
            output.push_str(&format!("status = \"{}\"\n", status));
            output.push_str(&format!(
                "impl_files = [{}]\n",
                format_string_array(&impl_files)
            ));
            output.push_str("test_files = []\n");
            output.push_str("notes = \"\"\n");

            section_new += 1;
            total_rules += 1;
        }

        if config.update && section_new == 0 {
            continue; // No new rules for this section
        }

        if config.update && !existing_keys.is_empty() {
            // Append new entries to existing file
            let existing_content = std::fs::read_to_string(&filepath).unwrap_or_default();
            let mut combined = existing_content;
            combined.push_str(&output);
            std::fs::write(&filepath, combined)
                .map_err(|e| format!("Failed to write {}: {}", filepath.display(), e))?;
        } else {
            std::fs::write(&filepath, output)
                .map_err(|e| format!("Failed to write {}: {}", filepath.display(), e))?;
        }
        files_written += 1;
    }

    eprintln!("Generated registry in {}/", config.rules_dir.display());
    eprintln!("  {} rules across {} files", total_rules, files_written);
    eprintln!(
        "  {} pre-populated as 'implemented' (from CR annotations)",
        pre_populated
    );
    eprintln!(
        "  {} as 'missing' (no annotation found)",
        total_rules - pre_populated
    );
    if config.update {
        eprintln!("  {} existing entries preserved", skipped_existing);
    }

    Ok(())
}

fn escape_toml_string(s: &str) -> String {
    s.replace('\\', "\\\\")
        .replace('"', "\\\"")
        .replace('\n', "\\n")
        .replace('\t', "\\t")
}

fn format_string_array(items: &[String]) -> String {
    if items.is_empty() {
        return String::new();
    }
    items
        .iter()
        .map(|s| format!("\"{}\"", s))
        .collect::<Vec<_>>()
        .join(", ")
}

fn load_existing_entries(
    rules_dir: &Path,
) -> Result<HashMap<String, HashMap<String, toml::Value>>, String> {
    let mut result = HashMap::new();

    let entries = match std::fs::read_dir(rules_dir) {
        Ok(e) => e,
        Err(_) => return Ok(result), // Directory doesn't exist yet
    };

    for entry in entries {
        let entry = entry.map_err(|e| format!("Failed to read dir entry: {}", e))?;
        let path = entry.path();
        if path.extension().is_none_or(|ext| ext != "toml") {
            continue;
        }
        let filename = path.file_name().unwrap().to_string_lossy().to_string();

        let content = std::fs::read_to_string(&path)
            .map_err(|e| format!("Failed to read {}: {}", path.display(), e))?;

        let table: toml::value::Table = toml::from_str(&content)
            .map_err(|e| format!("Failed to parse {}: {}", path.display(), e))?;

        let mut keys = HashMap::new();
        for (key, value) in table {
            if key != "_meta" {
                keys.insert(key, value);
            }
        }
        result.insert(filename, keys);
    }

    Ok(result)
}

// ── Validate Mode ────────────────────────────────────────────────────────────

#[derive(Debug, Default, Serialize)]
struct ValidationReport {
    total_rules: usize,
    implemented: usize,
    partial: usize,
    missing: usize,
    not_applicable: usize,
    coverage_pct: f64,
    untested_implemented: usize,
    errors: Vec<ValidationError>,
    warnings: Vec<String>,
    sections: BTreeMap<String, SectionStats>,
}

#[derive(Debug, Serialize)]
struct ValidationError {
    rule: String,
    file: String,
    error: String,
}

#[derive(Debug, Default, Serialize)]
struct SectionStats {
    implemented: usize,
    partial: usize,
    missing: usize,
    not_applicable: usize,
    total: usize,
}

fn validate_registry(config: &Config) -> Result<ValidationReport, String> {
    let mut report = ValidationReport::default();

    // Load CompRules for rule number validation
    let comp_rules = parse_comp_rules(&config.comp_rules)?;
    let valid_rules: BTreeSet<String> = comp_rules.iter().map(|r| r.number.clone()).collect();

    // Load all TOML files
    let entries = std::fs::read_dir(&config.rules_dir)
        .map_err(|e| format!("Failed to read {}: {}", config.rules_dir.display(), e))?;

    let mut all_entries: Vec<(String, RuleEntry)> = Vec::new();

    for entry in entries {
        let entry = entry.map_err(|e| format!("Dir entry error: {}", e))?;
        let path = entry.path();
        if path.extension().is_none_or(|ext| ext != "toml") {
            continue;
        }
        let filename = path.file_name().unwrap().to_string_lossy().to_string();

        let content = std::fs::read_to_string(&path)
            .map_err(|e| format!("Failed to read {}: {}", path.display(), e))?;

        let table: toml::value::Table = toml::from_str(&content)
            .map_err(|e| format!("Failed to parse {}: {}", path.display(), e))?;

        for (key, value) in &table {
            if key == "_meta" {
                continue;
            }

            let entry: RuleEntry = match value.clone().try_into() {
                Ok(e) => e,
                Err(e) => {
                    report.errors.push(ValidationError {
                        rule: key.clone(),
                        file: filename.clone(),
                        error: format!("Schema error: {}", e),
                    });
                    continue;
                }
            };

            // Validate status enum
            if !matches!(
                entry.status.as_str(),
                "implemented" | "partial" | "missing" | "not-applicable"
            ) {
                report.errors.push(ValidationError {
                    rule: entry.rule.clone(),
                    file: filename.clone(),
                    error: format!("Invalid status: '{}'", entry.status),
                });
            }

            // Validate rule number exists in CompRules
            if !valid_rules.contains(&entry.rule) {
                report.errors.push(ValidationError {
                    rule: entry.rule.clone(),
                    file: filename.clone(),
                    error: "Rule number not found in MagicCompRules.txt".to_string(),
                });
            }

            all_entries.push((filename.clone(), entry));
        }
    }

    // Validate impl_files exist and contain annotations
    for (filename, entry) in &all_entries {
        match entry.status.as_str() {
            "implemented" => {
                report.implemented += 1;
                if entry.test_files.is_empty() {
                    report.untested_implemented += 1;
                }
            }
            "partial" => report.partial += 1,
            "missing" => report.missing += 1,
            "not-applicable" => report.not_applicable += 1,
            _ => {} // already reported as error
        }

        // Track section stats
        let section = &entry.rule[..3];
        let stats = report.sections.entry(section.to_string()).or_default();
        stats.total += 1;
        match entry.status.as_str() {
            "implemented" => stats.implemented += 1,
            "partial" => stats.partial += 1,
            "missing" => stats.missing += 1,
            "not-applicable" => stats.not_applicable += 1,
            _ => {}
        }

        // Validate impl_files for implemented/partial entries
        if entry.status == "implemented" || entry.status == "partial" {
            for impl_file in &entry.impl_files {
                let file_part = impl_file.split(':').next().unwrap_or(impl_file);
                let full_path = config.engine_src.join(file_part);
                if !full_path.exists() {
                    report.errors.push(ValidationError {
                        rule: entry.rule.clone(),
                        file: filename.clone(),
                        error: format!("impl_file not found: {}", impl_file),
                    });
                }
            }
        }

        // Warn on implemented without tests
        if entry.status == "implemented" && entry.test_files.is_empty() {
            report
                .warnings
                .push(format!("{}: implemented but no test_files", entry.rule));
        }
    }

    report.total_rules = all_entries.len();
    let auditable = report.implemented + report.partial + report.missing;
    if auditable > 0 {
        report.coverage_pct =
            ((report.implemented as f64 + report.partial as f64 * 0.5) / auditable as f64) * 100.0;
    }

    Ok(report)
}

// ── Main ─────────────────────────────────────────────────────────────────────

fn main() {
    let config = parse_args();

    if config.generate {
        if let Err(e) = generate_registry(&config) {
            eprintln!("Error: {}", e);
            process::exit(1);
        }
        return;
    }

    // Validate mode
    let report = match validate_registry(&config) {
        Ok(r) => r,
        Err(e) => {
            eprintln!("Error: {}", e);
            process::exit(1);
        }
    };

    // JSON to stdout
    if !config.summary {
        println!("{}", serde_json::to_string_pretty(&report).unwrap());
    }

    // Human-readable to stderr
    eprintln!(
        "Rules coverage: {}/{} implemented ({:.1}%)",
        report.implemented + report.partial,
        report.total_rules,
        report.coverage_pct
    );
    eprintln!(
        "  implemented: {}, partial: {}, missing: {}, not-applicable: {}",
        report.implemented, report.partial, report.missing, report.not_applicable
    );
    if report.untested_implemented > 0 {
        eprintln!(
            "  {} implemented rules without test references",
            report.untested_implemented
        );
    }

    // Section breakdown
    if !report.sections.is_empty() {
        eprintln!();
        eprintln!("By section:");
        for (section, stats) in &report.sections {
            let title = section_title(section.parse().unwrap_or(0));
            let pct = if stats.implemented + stats.missing + stats.partial > 0 {
                (stats.implemented as f64
                    / (stats.implemented + stats.missing + stats.partial) as f64)
                    * 100.0
            } else {
                0.0
            };
            eprintln!(
                "  CR {} ({}) — {}/{} ({:.0}%) impl, {} partial, {} missing, {} n/a",
                section,
                title,
                stats.implemented,
                stats.total,
                pct,
                stats.partial,
                stats.missing,
                stats.not_applicable
            );
        }
    }

    // Errors
    if !report.errors.is_empty() {
        eprintln!();
        eprintln!("Errors ({}):", report.errors.len());
        for err in &report.errors {
            eprintln!("  {} ({}): {}", err.rule, err.file, err.error);
        }
    }

    // CI mode: exit 1 on errors
    if config.ci && !report.errors.is_empty() {
        eprintln!();
        eprintln!("FAIL: {} validation errors found", report.errors.len());
        process::exit(1);
    }
}
