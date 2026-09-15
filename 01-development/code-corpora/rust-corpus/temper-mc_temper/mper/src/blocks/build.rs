use quote::__private::TokenStream;
use quote::quote;
use std::fs;
use std::path::Path;
use temper_blocks_build::complex::fill_complex_block_mappings;
use temper_blocks_build::config::{get_block_states, get_build_config};
use temper_blocks_build::separate_blocks;
use temper_blocks_build::simple::fill_simple_block_mappings;

fn main() {
    println!("cargo:rerun-if-changed=build.rs");
    println!("cargo:rerun-if-changed=build_config.toml");

    let build_config = get_build_config();
    let block_states = get_block_states();

    let mut mappings = Vec::with_capacity(block_states.len());
    mappings.resize(block_states.len(), TokenStream::new());
    let (simple_blocks, complex_blocks) = separate_blocks(block_states);

    let enum_const = fill_simple_block_mappings(&build_config, simple_blocks, &mut mappings);
    let complex_consts = fill_complex_block_mappings(&build_config, complex_blocks, &mut mappings);

    let mapping_const = quote! {
        {
            use temper_blocks_generated::*;

            #enum_const
            #(#complex_consts)*

            &[
                #(#mappings),*
            ]
        }
    };

    let out_dir = std::env::var_os("OUT_DIR").unwrap();
    let dir = Path::new(&out_dir).join("mappings.rs");
    fs::write(dir, mapping_const.to_string()).unwrap();

    let dir = Path::new(&out_dir).join("default_block_states.rs");
    fs::write(dir, default_block_states()).unwrap();
}

fn default_block_states() -> String {
    let blocks: serde_json::Value = serde_json::from_str(temper_assets::generated::reports::BLOCKS)
        .expect("Failed to parse generated blocks report");
    let blocks = blocks
        .as_object()
        .expect("Generated blocks report should be an object");

    let mut entries = blocks
        .iter()
        .map(|(name, block)| {
            let states = block
                .get("states")
                .and_then(serde_json::Value::as_array)
                .expect("Generated block should have states");
            let default_state = states
                .iter()
                .find(|state| {
                    state.get("default").and_then(serde_json::Value::as_bool) == Some(true)
                })
                .unwrap_or_else(|| {
                    states
                        .first()
                        .expect("Generated block should have at least one state")
                });
            let id = default_state
                .get("id")
                .and_then(serde_json::Value::as_u64)
                .expect("Generated default block state should have an id");

            (name.as_str(), id)
        })
        .collect::<Vec<_>>();

    entries.sort_by_key(|(name, _)| *name);

    let mut map = phf_codegen::Map::new();
    let mut map_entries = Vec::new();
    for (name, id) in entries {
        let id = format!("temper_core::block_state_id::BlockStateId::new({id}u32)");
        map_entries.push((name.to_string(), id.clone()));

        if let Some(stripped) = name.strip_prefix("minecraft:") {
            map_entries.push((stripped.to_string(), id));
        }
    }

    for (name, id) in &map_entries {
        map.entry(name, id);
    }

    format!(
        "pub static DEFAULT_BLOCK_STATES: phf::Map<&'static str, temper_core::block_state_id::BlockStateId> = {};\n\n\
         pub fn default_block_state(name: &str) -> Option<temper_core::block_state_id::BlockStateId> {{\n    \
         DEFAULT_BLOCK_STATES.get(name).copied()\n}}\n",
        map.build()
    )
}
