// 算一些完全没必要的东西
fn useless(n: u64) -> u64 {
    if n == 0 {
        0
    } else {
        n + useless(n - 1)
    }
}

fn main() {
    println!("{}", useless(10));
}
