use proc_macro::TokenStream;
use syn::{parse_macro_input, DeriveInput};

mod attrs;
mod expand;
mod fields;

pub fn derive(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);

    match expand::expand(input) {
        Ok(tokens) => tokens.into(),
        Err(err) => err.to_compile_error().into(),
    }
}
