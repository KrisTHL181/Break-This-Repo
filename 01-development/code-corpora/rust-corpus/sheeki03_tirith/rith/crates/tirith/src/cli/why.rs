use std::io::{self, Write};

use super::last_trigger::{load_last_trigger_record, LastTriggerRecord};

pub fn run(json: bool) -> i32 {
    let record = match load_last_trigger_record() {
        Ok(Some(record)) => record,
        Ok(None) => {
            eprintln!("tirith: no recent trigger found");
            return 1;
        }
        Err(error) => {
            eprintln!(
                "tirith: {}",
                super::sanitize_for_human_output(&error, false)
            );
            return 1;
        }
    };

    if json {
        let mut stdout = std::io::stdout().lock();
        return if serde_json::to_writer_pretty(&mut stdout, &record).is_ok()
            && writeln!(stdout).is_ok()
        {
            0
        } else {
            1
        };
    }

    let cwd = std::env::current_dir()
        .ok()
        .map(|path| path.display().to_string());
    let policy = tirith_core::policy::Policy::discover_local_only(cwd.as_deref());
    let mut stderr = std::io::stderr().lock();
    if render_human_to(&mut stderr, &record, &policy.dlp_custom_patterns).is_ok() {
        0
    } else {
        1
    }
}

fn safe_human(value: &str, custom_patterns: &[String], allow_multiline: bool) -> String {
    let redacted = tirith_core::redact::redact_with_custom(value, custom_patterns);
    super::sanitize_for_human_output(&redacted, allow_multiline)
}

fn render_human_to<W: Write>(
    out: &mut W,
    record: &LastTriggerRecord,
    custom_patterns: &[String],
) -> io::Result<()> {
    writeln!(out, "tirith: last trigger")?;
    if !record.timestamp.is_empty() {
        writeln!(
            out,
            "  when: {}",
            safe_human(&record.timestamp, custom_patterns, false)
        )?;
    }
    if !record.command_redacted.is_empty() {
        let redacted =
            tirith_core::redact::redact_command_text(&record.command_redacted, custom_patterns);
        writeln!(
            out,
            "  command: {}",
            super::sanitize_for_human_output(&redacted, false)
        )?;
    }
    if !record.severity.is_empty() {
        writeln!(
            out,
            "  severity: {}",
            safe_human(&record.severity, custom_patterns, false)
        )?;
    }
    for rule in &record.rule_ids {
        writeln!(out, "  rule: {}", safe_human(rule, custom_patterns, false))?;
    }
    for finding in &record.findings {
        if let Some(title) = finding.get("title").and_then(serde_json::Value::as_str) {
            writeln!(out, "  - {}", safe_human(title, custom_patterns, false))?;
        }
        if let Some(description) = finding
            .get("description")
            .and_then(serde_json::Value::as_str)
        {
            writeln!(
                out,
                "    {}",
                safe_human(description, custom_patterns, true)
            )?;
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn human_renderer_reredacts_and_terminal_encodes_tampered_record() {
        let record = LastTriggerRecord {
            rule_ids: vec!["rule\u{1b}[2J\nFORGED".to_string()],
            severity: "high\u{202e}".to_string(),
            command_redacted: "TOKEN=verysecretvalue echo 包\u{1b}]52;c;payload\u{7}".to_string(),
            findings: vec![serde_json::json!({
                "title": "title ghp_abcdefghijklmnopqrstuvwxyz1234567890\nROW",
                "description": "description\u{200b}\u{1b}[31m"
            })],
            timestamp: "now\nROW".to_string(),
            extra: serde_json::Map::new(),
        };
        let mut out = Vec::new();
        render_human_to(&mut out, &record, &[]).unwrap();
        let text = String::from_utf8(out).unwrap();

        for forbidden in ['\u{1b}', '\u{7}', '\u{202e}', '\u{200b}'] {
            assert!(
                !text.contains(forbidden),
                "unsafe character survived: {text:?}"
            );
        }
        assert!(!text.contains("verysecretvalue"));
        assert!(!text.contains("ghp_abcdefghijklmnopqrstuvwxyz1234567890"));
        assert!(text.contains("TOKEN=[REDACTED]"));
        assert!(text.contains("包"), "legitimate Unicode must survive");
        assert!(
            text.contains("nowROW"),
            "dynamic newlines cannot forge rows"
        );
    }

    #[test]
    fn parsed_json_projection_remains_structured() {
        let record = LastTriggerRecord {
            rule_ids: vec!["raw\u{1b}[31m".to_string()],
            severity: "high".to_string(),
            command_redacted: "echo ok".to_string(),
            findings: vec![],
            timestamp: "now".to_string(),
            extra: [("future".to_string(), serde_json::json!({"kept": true}))]
                .into_iter()
                .collect(),
        };
        let bytes = serde_json::to_vec_pretty(&record).unwrap();
        let parsed: serde_json::Value = serde_json::from_slice(&bytes).unwrap();
        assert_eq!(parsed["rule_ids"][0], "raw\u{1b}[31m");
        assert_eq!(parsed["future"]["kept"], true);
    }
}
