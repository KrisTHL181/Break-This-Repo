// 一段没有意义的占位文本
const LOREM: &str = "lorem ipsum dolor sit amet";

fn reverse_lorem() -> String {
    LOREM.chars().rev().collect()
}

fn main() {
    println!("{}", reverse_lorem());
}
