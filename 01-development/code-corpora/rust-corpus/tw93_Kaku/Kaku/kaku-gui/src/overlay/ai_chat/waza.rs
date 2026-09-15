//! Built-in Waza skill snapshots for Kaku AI chat.
//!
//! These are compact, Kaku-compatible mode instructions. They intentionally do
//! not depend on a local Waza checkout, Claude-only features, subagents, or
//! external scripts.

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) struct Skill {
    pub(crate) command: &'static str,
    pub(crate) description: &'static str,
    pub(crate) instruction: &'static str,
    pub(crate) default_request: Option<&'static str>,
    pub(crate) missing_input: Option<&'static str>,
}

#[derive(Debug, Clone, Copy)]
pub(crate) struct Invocation<'a> {
    pub(crate) skill: &'static Skill,
    pub(crate) request: &'a str,
}

pub(crate) fn all() -> &'static [Skill] {
    &SKILLS
}

pub(crate) fn find(command: &str) -> Option<&'static Skill> {
    SKILLS.iter().find(|skill| skill.command == command)
}

pub(crate) fn parse_invocation(input: &str) -> Option<Invocation<'_>> {
    let trimmed = input.trim();
    if !trimmed.starts_with('/') {
        return None;
    }
    let mut parts = trimmed.splitn(2, char::is_whitespace);
    let command = parts.next().unwrap_or("");
    let skill = find(command)?;
    let request = parts.next().unwrap_or("").trim();
    Some(Invocation { skill, request })
}

pub(crate) fn request_text(invocation: Invocation<'_>) -> Result<String, String> {
    if !invocation.request.is_empty() {
        return Ok(invocation.request.to_string());
    }
    if let Some(default_request) = invocation.skill.default_request {
        return Ok(default_request.to_string());
    }
    Err(invocation
        .skill
        .missing_input
        .unwrap_or("Add a request after the Waza command.")
        .to_string())
}

pub(crate) fn system_instruction(skill: &Skill) -> String {
    format!(
        "{}\n\nActive skill: {}\n\n{}",
        WAZA_PREAMBLE, skill.command, skill.instruction
    )
}

const WAZA_PREAMBLE: &str = "\
You are running a built-in Waza skill inside Kaku AI chat. This instruction is \
active for the current user turn only.

Adapt the workflow to Kaku's available tools. Use grep_search for code search, \
fs_read/fs_list for file inspection, shell_exec for verification, web_fetch for \
URLs, web_search/read_url only when configured, and existing Kaku approval gates \
for any mutating operation. Keep the answer concise and operational. Do not rely \
on Claude-only commands, subagents, local Waza files, or external scripts unless \
the user explicitly provides them.";

const CHECK_INSTRUCTION: &str = "\
Goal: review before shipping.

First inspect the real diff and classify the review depth from the size and risk \
of the change. Check whether the diff matches the user's stated goal, then focus \
on correctness, regressions, security, data mutation, dependency changes, and \
missing tests. If a safe typo-level fix is obvious, mention it; do not mutate \
files unless the user asked for implementation in this turn.

For GitHub issue or PR triage requests, inspect live issue/PR state with the \
available tools, verify whether fixes already exist in the current branch or \
since the latest release tag, and draft replies before any GitHub write.

Finish with findings first, ordered by severity with file or command evidence. \
If no issues are found, say that clearly and list the verification that was run \
or still remains.";

const HUNT_INSTRUCTION: &str = "\
Goal: diagnose before fixing.

Do not patch until you can state the root cause in one sentence with concrete \
evidence: file, function, condition, command output, or failing test. Start from \
the repro, error text, or visible terminal context. Trace the execution path, \
form a testable hypothesis, then confirm or discard it with one targeted check.

If the same symptom remains after a fix, stop and re-read the execution path. \
After three failed hypotheses, summarize what was checked, what was ruled out, \
and what is still unknown.";

const THINK_INSTRUCTION: &str = "\
Goal: turn a rough feature or architecture idea into a decision-complete plan.

Read the relevant local context first. Prefer the project's built-in patterns \
and standard libraries over custom machinery. Present two or three options only \
when there are real tradeoffs, include one minimal option, then recommend one.

Pressure-test the recommendation for dependency failure, scale, rollback cost, \
and fragile assumptions. The final answer should be an implementation-ready \
plan with summary, key changes, interfaces, tests, and assumptions. Do not edit \
files during this planning turn.";

const READ_INSTRUCTION: &str = "\
Goal: fetch or inspect a URL, GitHub page, web article, or PDF as accurately as \
Kaku's tools allow.

For GitHub URLs, prefer direct raw or repository inspection when possible. For \
general pages, use web_fetch; if a search provider is configured, use \
web_search/read_url when it improves coverage. Watch for login pages, paywalls, \
empty JS shells, and failed fetches. Do not silently summarize empty or partial \
content as if it succeeded.

Return the source title or URL, what was successfully read, and the requested \
analysis or summary. If the request is only to save content and Kaku lacks a \
dedicated save pipeline, state the limitation and provide the clean extracted \
content instead.";

const WRITE_INSTRUCTION: &str = "\
Goal: edit prose so it sounds natural and useful, not performative.

Preserve meaning, structure, names, and audience intent unless the user asked \
to cut or restructure. If the target text is missing, ask for the text in one \
sentence. Detect the language from the text being edited. For Chinese/English \
mixed text, keep terminology consistent and use appropriate punctuation.

Return only the rewritten prose unless the user explicitly asks for notes.";

const LEARN_INSTRUCTION: &str = "\
Goal: help the user understand a domain or turn collected material into a \
publish-ready explanation.

Choose the lightest useful mode: quick reference for a fast mental model, deep \
research for unfamiliar domains, or write-to-learn when sources already exist. \
Prefer primary sources: papers, official docs, canonical repos, and builder \
writeups. Track contradictions instead of hiding them.

Build an outline before drafting. Each major section should be grounded in \
source material or the user's own notes. Refine by cutting filler, fixing flow, \
and flagging claims that need stronger evidence.";

const DESIGN_INSTRUCTION: &str = "\
Goal: produce or critique UI with a clear point of view.

Before proposing UI changes, identify the user, context, visual direction, hard \
constraints, and the one memorable interaction or visual decision. If the user \
provided a screenshot complaint, first state the concrete visual problem and \
then trace the responsible code.

Avoid generic defaults. Favor stable dimensions, clear hierarchy, accessible \
contrast, restrained motion, and the existing product language. For app shells, \
prioritize utility and surface hierarchy over decorative hero sections.";

const HEALTH_INSTRUCTION: &str = "\
Goal: audit an AI assistant setup for instruction, tool, hook, skill, MCP, and \
verification problems.

Start by identifying the project tier from size, CI, contributors, and config \
complexity. Inspect available assistant config files and local guidance before \
judging. Treat missing tooling output as insufficient evidence, not proof of a \
problem.

Report concrete findings by severity, naming the misaligned layer and the \
evidence. Keep recommendations proportional to project complexity.";

const SKILLS: [Skill; 8] = [
    Skill {
        command: "/check",
        description: "Review diff before shipping",
        instruction: CHECK_INSTRUCTION,
        default_request: Some(
            "Review the current working directory diff before shipping. Start by inspecting the diff and verification state.",
        ),
        missing_input: None,
    },
    Skill {
        command: "/hunt",
        description: "Find root cause first",
        instruction: HUNT_INSTRUCTION,
        default_request: None,
        missing_input: Some("Describe the bug, error, failing test, or unexpected behavior after /hunt."),
    },
    Skill {
        command: "/think",
        description: "Plan a feature or decision",
        instruction: THINK_INSTRUCTION,
        default_request: None,
        missing_input: Some("Describe the feature, design, or architecture decision after /think."),
    },
    Skill {
        command: "/read",
        description: "Read URL or document",
        instruction: READ_INSTRUCTION,
        default_request: None,
        missing_input: Some("Add a URL, GitHub link, or PDF path after /read."),
    },
    Skill {
        command: "/write",
        description: "Polish prose naturally",
        instruction: WRITE_INSTRUCTION,
        default_request: None,
        missing_input: Some("Paste the prose to edit after /write."),
    },
    Skill {
        command: "/learn",
        description: "Research and explain",
        instruction: LEARN_INSTRUCTION,
        default_request: None,
        missing_input: Some("Describe the topic, source set, or article goal after /learn."),
    },
    Skill {
        command: "/design",
        description: "Design or critique UI",
        instruction: DESIGN_INSTRUCTION,
        default_request: None,
        missing_input: Some("Describe the UI, screenshot issue, or visual goal after /design."),
    },
    Skill {
        command: "/health",
        description: "Audit AI setup",
        instruction: HEALTH_INSTRUCTION,
        default_request: Some(
            "Audit the current project's AI assistant setup and report any misaligned instructions, skills, hooks, tools, or verification gaps.",
        ),
        missing_input: None,
    },
];

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_invocation_matches_known_command() {
        let invocation = parse_invocation("/check review this").expect("known command");
        assert_eq!(invocation.skill.command, "/check");
        assert_eq!(invocation.request, "review this");
    }

    #[test]
    fn parse_invocation_ignores_unknown_command() {
        assert!(parse_invocation("/unknown do work").is_none());
    }

    #[test]
    fn request_text_uses_default_for_check() {
        let invocation = parse_invocation("/check").expect("known command");
        let request = request_text(invocation).expect("default request");
        assert!(request.contains("current working directory diff"));
    }

    #[test]
    fn request_text_requires_input_for_write() {
        let invocation = parse_invocation("/write").expect("known command");
        let err = request_text(invocation).unwrap_err();
        assert!(err.contains("Paste the prose"));
    }

    #[test]
    fn system_instruction_identifies_active_skill() {
        let skill = find("/hunt").expect("skill");
        let instruction = system_instruction(skill);
        assert!(instruction.contains("Active skill: /hunt"));
        assert!(instruction.contains("diagnose before fixing"));
        assert!(instruction.contains("current user turn only"));
    }
}
