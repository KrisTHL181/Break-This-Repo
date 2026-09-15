use std::time::Duration;

use rocket::yansi::Paint;

#[derive(Copy, Clone)]
pub struct Test {
    pub name: &'static str,
    pub run: fn(()) -> Result<(), String>,
}

#[macro_export]
macro_rules! register {
    ($f:ident $( ( $($v:ident: $a:expr),* ) )?) => {
        ::inventory::submit!($crate::Test {
            name: stringify!($f $(($($v = $a),*))?),
            run: |_: ()| $f($($($a),*)?).map_err(|e| e.to_string()),
        });
    };
}

inventory::collect!(Test);

pub fn run() -> std::process::ExitCode {
    procspawn::init();

    let filter = std::env::args().nth(1).unwrap_or_default();
    let filtered = inventory::iter::<Test>
        .into_iter()
        .filter(|t| t.name.contains(&filter));

    let total_tests = inventory::iter::<Test>.into_iter().count();
    println!("running {}/{total_tests} tests", filtered.clone().count());
    let handles = filtered.map(|test| (test, std::thread::spawn(|| {
        let name = test.name;
        let start = std::time::SystemTime::now();
        let mut proc = procspawn::spawn((), test.run);
        let result = loop {
            match proc.join_timeout(Duration::from_secs(10)) {
                Err(e) if e.is_timeout() => {
                    let elapsed = start.elapsed().unwrap().as_secs();
                    println!("{name} has been running for {elapsed} seconds...");

                    if elapsed >= 30 {
                        println!("{name} timeout");
                        break Err(e);
                    }
                },
                result => break result,
            }
        };

        match result.as_ref().map_err(|e| e.panic_info()) {
            Ok(Ok(_)) => println!("test {name} ... {}", "ok".green()),
            Ok(Err(e)) => println!("test {name} ... {}\n  {e}", "fail".red()),
            Err(Some(_)) => println!("test {name} ... {}", "panic".red().underline()),
            Err(None) => println!("test {name} ... {}", "error".magenta()),
        }

        matches!(result, Ok(Ok(())))
    })));

    let mut success = true;
    for (_, handle) in handles {
        success &= handle.join().unwrap_or(false);
    }

    match success {
        true => std::process::ExitCode::SUCCESS,
        false => {
            println!("note: use `NOCAPTURE=1` to see test output");
            std::process::ExitCode::FAILURE
        }
    }
}
