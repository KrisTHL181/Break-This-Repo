use std::{collections::HashMap, fs::File};

use clap::{Arg, Command};
use fastanvil::Region;
use fastnbt::Value;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
struct Chunk {
    sections: Vec<Section>,

    #[serde(flatten)]
    other: HashMap<String, Value>,
}

#[derive(Serialize, Deserialize)]
struct Section {
    block_states: Blockstates,
    #[serde(flatten)]
    other: HashMap<String, Value>,
}

#[derive(Serialize, Deserialize)]
struct Blockstates {
    palette: Vec<PaletteItem>,
    #[serde(flatten)]
    other: HashMap<String, Value>,
}

#[derive(Serialize, Deserialize)]
struct PaletteItem {
    #[serde(rename = "Name")]
    name: String,
    #[serde(rename = "Properties")]
    properties: Option<Value>,
}

fn main() {
    let matches = Command::new("anvil-palette-swap")
        .arg(Arg::new("region").required(true))
        .arg(
            Arg::new("from")
                .long("from")
                .short('f')
                .required(true)
                .help("blockstate to transform from, eg minecraft:oak_leaves"),
        )
        .arg(
            Arg::new("out-file")
                .long("out-file")
                .short('o')
                .required(true)
                .help("full path to write the resulting region file"),
        )
        .arg(
            Arg::new("to")
                .long("to")
                .short('t')
                .required(true)
                .help("blockstate to transform to, eg minecraft:diamond_block"),
        )
        .get_matches();

    let region = matches.get_one::<String>("region").unwrap();
    let out_path = matches.get_one::<String>("out-file").unwrap();
    let from = matches.get_one::<String>("from").unwrap().as_str();
    let to = matches.get_one::<String>("to").unwrap().as_str();

    let region = File::open(region).unwrap();
    let mut region = Region::from_stream(region).unwrap();

    let out_file = File::options()
        .read(true)
        .write(true)
        .create_new(true)
        .open(out_path)
        .unwrap();

    let mut new_region = Region::create(out_file).unwrap();

    for z in 0..32 {
        for x in 0..32 {
            match region.read_chunk(x, z) {
                Ok(Some(data)) => {
                    let mut chunk: Chunk = fastnbt::from_bytes(&data).unwrap();
                    for section in chunk.sections.iter_mut() {
                        let palette: &mut Vec<PaletteItem> = &mut section.block_states.palette;
                        for item in palette {
                            if item.name == from {
                                item.name = to.to_owned();
                            }
                        }
                    }
                    let ser = fastnbt::to_bytes(&chunk).unwrap();
                    new_region.write_chunk(x, z, &ser).unwrap();
                }
                Ok(None) => {}
                Err(e) => eprintln!("{e}"),
            }
        }
    }
}
