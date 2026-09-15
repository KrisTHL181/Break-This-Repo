use crate::write_if_changed;
use serde_json::Value;
use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::path::{Path, PathBuf};

pub fn write_registry_packets(out_dir: &Path, assets_dir: &Path) -> PathBuf {
    let mut registry_packets = BTreeMap::new();

    for (registry_id, entries) in read_synced_registries(assets_dir) {
        registry_packets.insert(namespaced(&registry_id), entries);
    }

    let path = out_dir.join("registry_packets.json");
    let content =
        serde_json::to_string(&registry_packets).expect("Failed to serialize registry packets");
    write_if_changed(&path, content).expect("Failed to write registry packets");
    path
}

pub(crate) fn synced_registry_ids(assets_dir: &Path) -> BTreeSet<String> {
    read_synced_registries(assets_dir)
        .into_keys()
        .map(|registry_id| namespaced(&registry_id))
        .collect()
}

fn read_synced_registries(assets_dir: &Path) -> BTreeMap<String, BTreeMap<String, Value>> {
    let path = synced_registries_path(assets_dir);

    println!("cargo:rerun-if-changed={}", path.display());

    serde_json::from_str(&fs::read_to_string(&path).expect("Failed to read synced registries"))
        .expect("Failed to parse synced registries")
}

fn synced_registries_path(assets_dir: &Path) -> PathBuf {
    let assets_root = assets_dir
        .parent()
        .expect("Generated assets directory should have a parent");
    let extracted_path = assets_root.join("extracted").join("synced_registries.json");
    let new_extract_path = assets_root
        .join("new-extract")
        .join("synced_registries.json");

    if extracted_path.exists() {
        extracted_path
    } else if new_extract_path.exists() {
        new_extract_path
    } else {
        panic!(
            "Missing synced_registries.json at {} or {}",
            extracted_path.display(),
            new_extract_path.display()
        );
    }
}

fn namespaced(registry_id: &str) -> String {
    if registry_id.contains(':') {
        registry_id.to_string()
    } else {
        format!("minecraft:{registry_id}")
    }
}
