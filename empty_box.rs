// 一个空盒子
#[derive(Debug, Default)]
struct EmptyBox;

fn main() {
    let empty = EmptyBox::default();
    println!("{:?}", empty);
}
