// 🦀 ferris 什么也没做
struct Ferris {
    legs: u8,
    happy: bool,
}

impl Ferris {
    fn wave(&self) -> &'static str {
        if self.happy { "hi" } else { "..." }
    }
}

fn main() {
    let f = Ferris { legs: 6, happy: true };
    println!("{} {}", f.wave(), f.legs);
}
