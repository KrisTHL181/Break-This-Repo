//! Clock skill: get current date/time in any timezone.
//!
//! Protocol: `./main <tool_name>` with JSON on stdin, JSON on stdout.

use std::io::Read;

use chrono::Utc;
use serde::Deserialize;
use serde_json::json;

#[derive(Deserialize, Default)]
struct GetTimeInput {
    #[serde(default)]
    timezone: Option<String>,
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let tool_name = args.get(1).map(|s| s.as_str()).unwrap_or("unknown");

    let mut buf = String::new();
    if let Err(e) = std::io::stdin().read_to_string(&mut buf) {
        println!(
            "{}",
            json!({"output": format!("Failed to read stdin: {e}"), "success": false})
        );
        std::process::exit(1);
    }

    match tool_name {
        "get_time" => handle_get_time(&buf),
        _ => {
            println!(
                "{}",
                json!({"output": format!("Unknown tool '{tool_name}'. Expected: get_time"), "success": false})
            );
            std::process::exit(1);
        }
    }
}

fn handle_get_time(input_json: &str) {
    let input: GetTimeInput = serde_json::from_str(input_json).unwrap_or_default();

    let now_utc = Utc::now();

    let output = if let Some(tz_name) = &input.timezone {
        match tz_name.parse::<chrono_tz::Tz>() {
            Ok(tz) => {
                use chrono::TimeZone;
                let now_tz = tz.from_utc_datetime(&now_utc.naive_utc());
                format_time(&now_tz, tz_name)
            }
            Err(_) => {
                // Try common aliases
                let resolved = match tz_name.to_lowercase().as_str() {
                    "cst" | "china" | "beijing" | "shanghai" => Some("Asia/Shanghai"),
                    "jst" | "japan" | "tokyo" => Some("Asia/Tokyo"),
                    "kst" | "korea" | "seoul" => Some("Asia/Seoul"),
                    "est" | "eastern" => Some("US/Eastern"),
                    "cst_us" | "central" => Some("US/Central"),
                    "mst" | "mountain" => Some("US/Mountain"),
                    "pst" | "pacific" => Some("US/Pacific"),
                    "gmt" | "london" => Some("Europe/London"),
                    "cet" | "paris" | "berlin" => Some("Europe/Paris"),
                    "sweden" | "stockholm" => Some("Europe/Stockholm"),
                    "ist" | "india" | "mumbai" => Some("Asia/Kolkata"),
                    "aest" | "sydney" => Some("Australia/Sydney"),
                    "singapore" | "sg" => Some("Asia/Singapore"),
                    "hkt" | "hong kong" | "hongkong" => Some("Asia/Hong_Kong"),
                    "taiwan" | "taipei" => Some("Asia/Taipei"),
                    _ => None,
                };
                match resolved {
                    Some(canonical) => {
                        let tz: chrono_tz::Tz = canonical.parse().unwrap();
                        use chrono::TimeZone;
                        let now_tz = tz.from_utc_datetime(&now_utc.naive_utc());
                        format_time(&now_tz, canonical)
                    }
                    None => {
                        let out = json!({
                            "output": format!("Unknown timezone: '{tz_name}'. Use IANA format like 'Europe/Stockholm', 'Asia/Shanghai', 'US/Eastern', or aliases like 'sweden', 'china', 'japan'."),
                            "success": false
                        });
                        println!("{out}");
                        std::process::exit(1);
                    }
                }
            }
        }
    } else {
        // Local server time
        let local = chrono::Local::now();
        format_time(&local, "Local")
    };

    println!("{}", json!({"output": output, "success": true}));
}

fn format_time<Tz: chrono::TimeZone>(dt: &chrono::DateTime<Tz>, tz_label: &str) -> String
where
    Tz::Offset: std::fmt::Display,
{
    let date = dt.format("%Y-%m-%d").to_string();
    let time = dt.format("%H:%M:%S").to_string();
    let day = dt.format("%A").to_string();
    let offset = dt.format("%:z").to_string();

    format!("{date} {time} (UTC{offset})\n{day}, {tz_label}")
}
