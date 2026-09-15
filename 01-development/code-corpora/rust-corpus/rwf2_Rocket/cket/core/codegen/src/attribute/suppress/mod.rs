use proc_macro2::TokenStream;
use devise::Spanned;

mod lint;

pub use lint::Lint;

pub fn suppress_attribute(
    args: proc_macro::TokenStream,
    input: proc_macro::TokenStream
) -> TokenStream {
    let input: TokenStream = input.into();
    match Lint::suppress_tokens(args.into(), input.span()) {
        Ok(_) => input,
        Err(e) => {
            let error: TokenStream = e.to_compile_error().into();
            quote!(#error #input)
        }
    }
}
