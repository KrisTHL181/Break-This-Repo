// Standard esp-idf-sys build.rs entry — emits ldproxy / linker flags.

fn main() {
    embuild::espidf::sysenv::output();
}
