// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

//! UI-independent IDE snapshots exposed to plugins.

/// Project metadata visible through the plugin host API.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct IdePluginProjectSnapshot {
    pub node_id: String,
    pub root_path: String,
    pub name: String,
    pub is_git_repo: bool,
    pub git_branch: Option<String>,
}

/// Open-file metadata visible through the plugin host API.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct IdePluginFileSnapshot {
    pub path: String,
    pub name: String,
    pub language: String,
    pub is_dirty: bool,
    pub is_active: bool,
    pub is_pinned: bool,
}

/// Current IDE state visible through the plugin host API.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct IdePluginSnapshot {
    pub project: IdePluginProjectSnapshot,
    pub open_files: Vec<IdePluginFileSnapshot>,
    pub active_file: Option<IdePluginFileSnapshot>,
}
