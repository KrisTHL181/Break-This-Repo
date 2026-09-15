use core::panic;
use std::{
    error::Error,
    fs::{create_dir, File},
    io::{self, Write},
};

use clap::{Arg, Command};
use env_logger::Env;
use fastanvil::Region;
use fastnbt::Value;

fn main() -> Result<(), Box<dyn Error>> {
    env_logger::Builder::from_env(Env::default().default_filter_or("info"))
        .format_timestamp(None)
        .init();

    let matches = Command::new("region-dump")
        .arg(Arg::new("file").required(true))
        .arg(
            Arg::new("format")
                .long("format")
                .short('f')
                .required(false)
                .default_value("rust")
                .value_parser(["rust", "rust-pretty", "json", "json-pretty", "nbt", "snbt"])
                .help("output format"),
        )
        .arg(
            Arg::new("out-dir")
                .long("out-dir")
                .short('o')
                .required(false)
                .help("optionally separate each chunk into a file in the specified directory"),
        )
        .get_matches();

    let file = matches
        .get_one::<String>("file")
        .map(|s| s.as_str())
        .expect("file is required");
    let file = File::open(file).expect("file does not exist");
    let output_format = matches
        .get_one::<String>("format")
        .map(|s| s.as_str())
        .expect("no output format specified");
    let out_dir = matches.get_one::<String>("out-dir").map(|s| s.as_str());

    let mut region = Region::from_stream(file).unwrap();

    if let Some(dir) = out_dir {
        create_dir(dir).unwrap_or_default();
    }

    for z in 0..32 {
        for x in 0..32 {
            match region.read_chunk(x, z) {
                Ok(Some(data)) => {
                    if !should_output_chunk(&data) {
                        continue;
                    }

                    let mut out: Box<dyn Write> = if let Some(dir) = out_dir {
                        let ext = match output_format {
                            "nbt" => "nbt",
                            "json" | "json-pretty" => "json",
                            _ => "txt",
                        };
                        Box::new(File::create(format!("{}/{}.{}.{}", dir, x, z, ext)).unwrap())
                    } else {
                        Box::new(io::stdout())
                    };

                    let chunk: Value = fastnbt::from_bytes(&data).unwrap();

                    match output_format {
                        "rust" => {
                            write!(&mut out, "{:?}", chunk).unwrap();
                        }
                        "rust-pretty" => {
                            write!(&mut out, "{:#?}", chunk).unwrap();
                        }
                        "nbt" => {
                            out.write_all(&data).unwrap();
                        }
                        "json" => {
                            serde_json::ser::to_writer(out, &chunk).unwrap();
                        }
                        "json-pretty" => {
                            serde_json::ser::to_writer_pretty(out, &chunk).unwrap();
                        }
                        "snbt" => {
                            let s = fastsnbt::to_string(&chunk).unwrap();
                            out.write_all(s.as_bytes()).unwrap();
                        }
                        _ => panic!("unknown output format '{}'", output_format),
                    }
                }
                Ok(None) => {}
                Err(e) => return Err(e.into()),
            }
        }
    }
    Ok(())
}

fn should_output_chunk(_data: &[u8]) -> bool {
    // If you're trying to locate a misbehaving chunk, you can filter out chunks here.
    // let chunk = JavaChunk::from_bytes(data).unwrap();
    true
}
