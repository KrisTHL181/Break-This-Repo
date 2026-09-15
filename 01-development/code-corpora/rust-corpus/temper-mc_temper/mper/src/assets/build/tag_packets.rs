use crate::registry_packets::synced_registry_ids;
use crate::write_if_changed;
use serde_json::Value;
use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::path::{Path, PathBuf};

type RegistryIds = BTreeMap<String, i32>;
type TagPackets = BTreeMap<String, BTreeMap<String, Vec<i32>>>;
type RawTags = BTreeMap<String, BTreeMap<String, Vec<TagValue>>>;

#[derive(Clone, Debug)]
struct TagValue {
    id: String,
    required: bool,
}

pub fn write_tag_packets(out_dir: &Path, assets_dir: &Path, data_dir: &Path) -> PathBuf {
    let extracted_dir = assets_dir
        .parent()
        .expect("Assets directory should have a parent")
        .join("extracted");
    let tags_path = extracted_dir.join("tags.json");

    println!("cargo:rerun-if-changed={}", tags_path.display());

    let report_registry_ids = read_report_registry_ids(assets_dir);
    let extracted_registry_ids = read_extracted_registry_ids(&extracted_dir);
    let synced_registry_ids = synced_registry_ids(assets_dir);
    let tags = read_tags(
        assets_dir,
        data_dir,
        &report_registry_ids,
        &extracted_registry_ids,
        &synced_registry_ids,
    );

    let mut tag_packets = TagPackets::new();
    for (tag_registry, tag_set) in tags {
        let registry_ids = report_registry_ids
            .get(&tag_registry)
            .cloned()
            .or_else(|| read_data_registry_ids_if_exists(data_dir, &tag_registry))
            .or_else(|| extracted_registry_ids.get(&tag_registry).cloned())
            .unwrap_or_else(|| read_data_registry_ids(data_dir, &tag_registry));

        let mut packet_tags = BTreeMap::new();
        for tag_name in tag_set.keys() {
            let mut resolving = BTreeSet::new();
            let values = resolve_tag(
                &tag_registry,
                tag_name,
                &tag_set,
                &registry_ids,
                &mut resolving,
            );

            packet_tags.insert(tag_name.clone(), values);
        }

        tag_packets.insert(tag_registry, packet_tags);
    }

    let path = out_dir.join("tag_packets.json");
    let content = serde_json::to_string(&tag_packets).expect("Failed to serialize tag packets");
    write_if_changed(&path, content).expect("Failed to write tag packets");
    path
}

fn read_tags(
    assets_dir: &Path,
    data_dir: &Path,
    report_registry_ids: &BTreeMap<String, RegistryIds>,
    extracted_registry_ids: &BTreeMap<String, RegistryIds>,
    synced_registry_ids: &BTreeSet<String>,
) -> RawTags {
    let generated_tags_dir = data_dir.join("minecraft").join("tags");
    if generated_tags_dir.exists() {
        return read_generated_tags(
            &generated_tags_dir,
            report_registry_ids,
            extracted_registry_ids,
            synced_registry_ids,
        );
    }

    let extracted_tags_path = assets_dir
        .parent()
        .expect("Assets directory should have a parent")
        .join("extracted")
        .join("tags.json");

    let tags: BTreeMap<String, BTreeMap<String, Vec<String>>> = serde_json::from_str(
        &fs::read_to_string(&extracted_tags_path).expect("Failed to read tags.json"),
    )
    .expect("Failed to parse tags.json");

    tags.into_iter()
        .map(|(registry_id, tags)| {
            (
                namespaced(&registry_id),
                tags.into_iter()
                    .map(|(tag_id, values)| {
                        (
                            namespaced(&tag_id),
                            values
                                .into_iter()
                                .map(|id| TagValue { id, required: true })
                                .collect(),
                        )
                    })
                    .collect(),
            )
        })
        .collect()
}

fn read_generated_tags(
    tags_dir: &Path,
    report_registry_ids: &BTreeMap<String, RegistryIds>,
    extracted_registry_ids: &BTreeMap<String, RegistryIds>,
    synced_registry_ids: &BTreeSet<String>,
) -> RawTags {
    let mut tags = RawTags::new();
    read_generated_tags_dir(
        &mut tags,
        tags_dir,
        tags_dir,
        report_registry_ids,
        extracted_registry_ids,
        synced_registry_ids,
    );
    tags
}

fn read_generated_tags_dir(
    tags: &mut RawTags,
    dir: &Path,
    root: &Path,
    report_registry_ids: &BTreeMap<String, RegistryIds>,
    extracted_registry_ids: &BTreeMap<String, RegistryIds>,
    synced_registry_ids: &BTreeSet<String>,
) {
    let mut entries = fs::read_dir(dir)
        .expect("Failed to read generated tags directory")
        .collect::<Result<Vec<_>, _>>()
        .expect("Failed to read generated tags directory entry");

    entries.sort_by_key(|entry| entry.path());

    for entry in entries {
        let path = entry.path();
        if path.is_dir() {
            read_generated_tags_dir(
                tags,
                &path,
                root,
                report_registry_ids,
                extracted_registry_ids,
                synced_registry_ids,
            );
            continue;
        }

        if path.extension().and_then(|extension| extension.to_str()) != Some("json") {
            continue;
        }

        let relative_path = path
            .strip_prefix(root)
            .expect("Generated tag should be inside tags root")
            .with_extension("");
        let components = relative_path
            .components()
            .map(|component| component.as_os_str().to_string_lossy().to_string())
            .collect::<Vec<_>>();
        let Some(registry_width) = registry_width(
            report_registry_ids,
            extracted_registry_ids,
            synced_registry_ids,
            &components,
        ) else {
            continue;
        };

        let registry_id = format!("minecraft:{}", components[..registry_width].join("/"));
        let tag_id = namespaced(&components[registry_width..].join("/"));
        let values = read_tag_values(&path);

        tags.entry(registry_id).or_default().insert(tag_id, values);
    }
}

fn registry_width(
    report_registry_ids: &BTreeMap<String, RegistryIds>,
    extracted_registry_ids: &BTreeMap<String, RegistryIds>,
    synced_registry_ids: &BTreeSet<String>,
    components: &[String],
) -> Option<usize> {
    (1..components.len()).rev().find(|width| {
        let registry_id = format!("minecraft:{}", components[..*width].join("/"));
        report_registry_ids.contains_key(&registry_id)
            || extracted_registry_ids.contains_key(&registry_id)
            || synced_registry_ids.contains(&registry_id)
    })
}

fn read_tag_values(path: &Path) -> Vec<TagValue> {
    let tag: Value =
        serde_json::from_str(&fs::read_to_string(path).expect("Failed to read generated tag"))
            .expect("Failed to parse generated tag");
    let values = tag
        .get("values")
        .and_then(Value::as_array)
        .expect("Generated tag should have values");

    values
        .iter()
        .map(|value| match value {
            Value::String(id) => TagValue {
                id: id.clone(),
                required: true,
            },
            Value::Object(value) => {
                let id = value
                    .get("id")
                    .and_then(Value::as_str)
                    .expect("Generated tag object should have an id")
                    .to_string();
                let required = value
                    .get("required")
                    .and_then(Value::as_bool)
                    .unwrap_or(true);

                TagValue { id, required }
            }
            _ => panic!("Generated tag value should be a string or object"),
        })
        .collect()
}

fn resolve_tag(
    registry_id: &str,
    tag_id: &str,
    tags: &BTreeMap<String, Vec<TagValue>>,
    registry_ids: &RegistryIds,
    resolving: &mut BTreeSet<String>,
) -> Vec<i32> {
    if !resolving.insert(tag_id.to_string()) {
        panic!("Circular tag reference while resolving {tag_id} in {registry_id}");
    }

    let values = tags
        .get(tag_id)
        .unwrap_or_else(|| panic!("Missing tag {tag_id} in {registry_id}"));

    let mut resolved = Vec::new();
    for value in values {
        let Some(tag_id) = value.id.strip_prefix('#') else {
            let id = namespaced(&value.id);
            if let Some(protocol_id) = registry_ids.get(&id) {
                resolved.push(*protocol_id);
            } else if value.required {
                panic!("No protocol id for tag value {id} in {registry_id}");
            }
            continue;
        };

        let tag_id = namespaced(tag_id);
        if tags.contains_key(&tag_id) {
            resolved.extend(resolve_tag(
                registry_id,
                &tag_id,
                tags,
                registry_ids,
                resolving,
            ));
        } else if value.required {
            panic!("Missing tag reference {tag_id} in {registry_id}");
        }
    }

    resolving.remove(tag_id);
    resolved
}

fn read_report_registry_ids(assets_dir: &Path) -> BTreeMap<String, RegistryIds> {
    let path = assets_dir
        .join("generated")
        .join("reports")
        .join("registries.json");
    let registries: Value =
        serde_json::from_str(&fs::read_to_string(path).expect("Failed to read registries report"))
            .expect("Failed to parse registries report");

    let mut ids = BTreeMap::new();
    let Some(registries) = registries.as_object() else {
        return ids;
    };

    for (registry_id, registry) in registries {
        let Some(entries) = registry.get("entries").and_then(Value::as_object) else {
            continue;
        };

        let mut registry_ids = RegistryIds::new();
        for (entry_id, entry) in entries {
            let protocol_id = entry
                .get("protocol_id")
                .and_then(Value::as_i64)
                .expect("Registry report entry missing protocol_id");
            registry_ids.insert(namespaced(entry_id), protocol_id as i32);
        }

        ids.insert(registry_id.clone(), registry_ids);
    }

    ids
}

fn read_extracted_registry_ids(extracted_dir: &Path) -> BTreeMap<String, RegistryIds> {
    let mut ids = BTreeMap::new();

    read_extracted_ids(
        &mut ids,
        "minecraft:enchantment",
        &extracted_dir.join("enchantments.json"),
    );
    read_extracted_ids(
        &mut ids,
        "minecraft:entity_type",
        &extracted_dir.join("entities.json"),
    );

    ids
}

fn read_extracted_ids(ids: &mut BTreeMap<String, RegistryIds>, registry_id: &str, path: &Path) {
    if !path.exists() {
        return;
    }

    let entries: Value =
        serde_json::from_str(&fs::read_to_string(path).expect("Failed to read extracted registry"))
            .expect("Failed to parse extracted registry");
    let Some(entries) = entries.as_object() else {
        return;
    };

    let registry_ids = ids.entry(registry_id.to_string()).or_default();
    for (entry_id, entry) in entries {
        let Some(id) = entry.get("id").and_then(Value::as_i64) else {
            continue;
        };
        registry_ids
            .entry(namespaced(entry_id))
            .or_insert(id as i32);
    }
}

fn read_data_registry_ids(data_dir: &Path, registry_id: &str) -> RegistryIds {
    let registry_dir = data_dir.join(registry_id.replacen(':', "/", 1));
    assert!(
        registry_dir.exists(),
        "Generated registry data missing for {registry_id}"
    );

    read_registry_entries(&registry_dir, &registry_dir)
        .into_keys()
        .enumerate()
        .map(|(id, entry)| (entry, id as i32))
        .collect()
}

fn read_data_registry_ids_if_exists(data_dir: &Path, registry_id: &str) -> Option<RegistryIds> {
    data_dir
        .join(registry_id.replacen(':', "/", 1))
        .exists()
        .then(|| read_data_registry_ids(data_dir, registry_id))
}

fn read_registry_entries(dir: &Path, root: &Path) -> BTreeMap<String, ()> {
    let mut entries = fs::read_dir(dir)
        .expect("Failed to read registry directory")
        .collect::<Result<Vec<_>, _>>()
        .expect("Failed to read registry directory entry");

    entries.sort_by_key(|entry| entry.path());

    let mut registry_entries = BTreeMap::new();
    for entry in entries {
        let path = entry.path();
        if path.is_dir() {
            registry_entries.extend(read_registry_entries(&path, root));
            continue;
        }

        if path.extension().and_then(|extension| extension.to_str()) != Some("json") {
            continue;
        }

        let id = path
            .strip_prefix(root)
            .expect("Registry entry should be inside registry root")
            .with_extension("")
            .components()
            .map(|component| component.as_os_str().to_string_lossy())
            .collect::<Vec<_>>()
            .join("/");

        registry_entries.insert(namespaced(&id), ());
    }

    registry_entries
}

fn namespaced(value: &str) -> String {
    if value.contains(':') {
        value.to_string()
    } else {
        format!("minecraft:{value}")
    }
}
