use std::path::PathBuf;

fn cli() -> clap::Command {
    use clap::{arg, value_parser, Command};

    Command::new("wechat-dump-rs")
        .version("1.0.31")
        .about("A wechat db dump tool")
        .author("REinject")
        .help_template("{name} ({version}) - {author}\n{about}\n{all-args}")
        .disable_version_flag(true)
        .arg(arg!(-p --pid <PID> "pid of wechat").value_parser(value_parser!(u32)))
        .arg(
            arg!(-k --key <KEY> "key for offline decryption of db file")
                .value_parser(value_parser!(String)),
        )
        .arg(arg!(-f --file <PATH> "special a db file path").value_parser(value_parser!(PathBuf)))
        .arg(
            arg!(-d --"data-dir" <PATH> "special wechat data dir path (pid is required)")
                .value_parser(value_parser!(PathBuf)),
        )
        .arg(
            arg!(-o --output <PATH> "decrypted database output path")
                .value_parser(value_parser!(PathBuf)),
        )
        .arg(arg!(-a --all "dump key and decrypt db files"))
        .arg(
            arg!(--vv <VERSION> "wechat db file version")
                .value_parser(["3", "4"])
                .default_value("4"),
        )
        .arg(arg!(-r --rawkey "convert db key to sqlcipher raw key (file is required)"))
}

fn main() {
    // 解析参数
    let matches = cli().get_matches();

    let all = matches.get_flag("all");
    let output = match matches.get_one::<PathBuf>("output") {
        Some(o) => PathBuf::from(o),
        None => PathBuf::from(format!(
            "{}{}",
            std::env::temp_dir().to_str().unwrap(),
            "wechat_dump"
        )),
    };

    let key_option = matches.get_one::<String>("key");
    let file_option = matches.get_one::<PathBuf>("file");
    let data_dir_option = matches.get_one::<PathBuf>("data-dir");
    let pid_option = matches.get_one::<u32>("pid");

    match (pid_option, key_option, file_option) {
        (None, None, None) => {
            let pids = [
                wxdump::get_pid_by_name("WeChat.exe"),
                wxdump::get_pid_by_name_and_cmd_pattern("Weixin.exe", r#"Weixin\.exe"?\s*$"#),
            ]
            .concat();
            if pids.is_empty() {
                panic!("WeChat is not running!!")
            }
            for pid in pids {
                let wechat_info = wxdump::dump_wechat_info(pid, None);

                // 需要对所有db文件进行解密
                if all {
                    wxdump::dump_all_by_pid(&wechat_info, &output);
                } else {
                    println!("{}", wechat_info);
                    println!();
                }
            }
        }
        (Some(&pid), None, None) => {
            let wechat_info = wxdump::dump_wechat_info(pid, data_dir_option);

            // 需要对所有db文件进行解密
            if all {
                wxdump::dump_all_by_pid(&wechat_info, &output);
            } else {
                println!("{}", wechat_info);
                println!();
            }
        }
        (None, Some(key), Some(file)) => {
            if !file.exists() {
                panic!("the target file does not exist");
            }

            let is_v4 = if matches.get_one::<String>("vv").unwrap() == "4" {
                true
            } else {
                false
            };

            // convert db key to sqlcipher rawkey
            let b_rawkey = matches.get_flag("rawkey");
            if b_rawkey {
                if file.is_dir() {
                    panic!("the target file is a directory.");
                }

                let rawkey = wxdump::convert_to_sqlcipher_rawkey(&key, &file, is_v4).unwrap();
                println!("{}", rawkey);

                return;
            }
            // convert end

            match file.is_dir() {
                true => {
                    let dbfiles =
                        wxdump::scan_db_files(file.to_str().unwrap().to_string()).unwrap();
                    println!(
                        "scanned {} files in {}",
                        dbfiles.len(),
                        &file.to_str().unwrap()
                    );
                    println!("decryption in progress, please wait...");

                    // 创建输出目录
                    if output.is_file() {
                        panic!("the output path must be a directory");
                    }
                    if !output.exists() {
                        std::fs::create_dir_all(&output).unwrap();
                    }

                    for dbfile in dbfiles {
                        let mut db_file_dir = PathBuf::new();
                        let mut dest = PathBuf::new();
                        db_file_dir.push(&output);
                        db_file_dir.push(
                            dbfile
                                .parent()
                                .unwrap()
                                .strip_prefix(PathBuf::from(&file))
                                .unwrap(),
                        );
                        dest.push(db_file_dir.clone());
                        dest.push(dbfile.file_name().unwrap());

                        if !db_file_dir.exists() {
                            std::fs::create_dir_all(db_file_dir).unwrap();
                        }

                        if is_v4 {
                            std::fs::write(
                                dest,
                                wxdump::decrypt_db_file_v4(&dbfile, &key).unwrap(),
                            )
                            .unwrap();
                        } else {
                            std::fs::write(
                                dest,
                                wxdump::decrypt_db_file_v3(&dbfile, &key).unwrap(),
                            )
                            .unwrap();
                        }
                    }
                    println!("decryption complete!!");
                    println!("output to {}", output.to_str().unwrap());
                    println!();
                }
                false => {
                    if is_v4 {
                        std::fs::write(&output, wxdump::decrypt_db_file_v4(&file, &key).unwrap())
                            .unwrap();
                    } else {
                        std::fs::write(&output, wxdump::decrypt_db_file_v3(&file, &key).unwrap())
                            .unwrap();
                    }
                    println!("output to {}", output.to_str().unwrap());
                }
            }
        }
        _ => panic!("param error"),
    }
}
