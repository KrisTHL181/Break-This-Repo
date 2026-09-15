use crate::write_if_changed;
use std::collections::BTreeMap;
use std::fs;
use std::path::{Path, PathBuf};

pub fn write_item_to_block_mapping(out_dir: &Path, reports_dir: &Path) -> PathBuf {
    let registries = read_json(&reports_dir.join("registries.json"));
    let blocks = read_json(&reports_dir.join("blocks.json"));
    let block_ids = block_ids_by_name(&blocks);

    let items = registries
        .get("minecraft:item")
        .and_then(|registry| registry.get("entries"))
        .and_then(serde_json::Value::as_object)
        .expect("Generated registries report should have item entries");
    let mut item_to_block = BTreeMap::new();

    for (item_name, item) in items {
        let Some(block_id) = block_ids.get(item_name) else {
            continue;
        };
        let protocol_id = item
            .get("protocol_id")
            .and_then(serde_json::Value::as_i64)
            .expect("Generated item should have a protocol id");

        item_to_block.insert(protocol_id.to_string(), block_id.clone());
    }

    let path = out_dir.join("item_to_block_mapping.json");
    let content =
        serde_json::to_string(&item_to_block).expect("Failed to serialize item to block mapping");
    write_if_changed(&path, content).expect("Failed to write item to block mapping");
    path
}

fn block_ids_by_name(blocks: &serde_json::Value) -> BTreeMap<String, String> {
    let blocks = blocks
        .as_object()
        .expect("Generated blocks report should be an object");
    let mut block_ids = BTreeMap::new();

    for (name, block) in blocks {
        let states = block
            .get("states")
            .and_then(serde_json::Value::as_array)
            .expect("Generated block should have states");

        for state in states {
            let id = state
                .get("id")
                .and_then(serde_json::Value::as_u64)
                .expect("Generated block state should have an id")
                .to_string();
            let is_default = state
                .get("default")
                .and_then(serde_json::Value::as_bool)
                .unwrap_or(false);

            if is_default || !block_ids.contains_key(name) {
                block_ids.insert(name.clone(), id);
            }
        }
    }

    block_ids
}

fn read_json(path: &Path) -> serde_json::Value {
    let content = fs::read_to_string(path).expect("Failed to read generated report");
    serde_json::from_str(&content).expect("Failed to parse generated report")
}
