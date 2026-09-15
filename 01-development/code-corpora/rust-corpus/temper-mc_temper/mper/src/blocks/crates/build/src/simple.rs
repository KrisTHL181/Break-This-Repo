use crate::config::BuildConfig;
use heck::{ToPascalCase, ToShoutySnakeCase};
use proc_macro2::TokenStream;
use quote::{format_ident, quote};
use std::collections::HashMap;

fn separate_enums(
    build_config: &BuildConfig,
    mut simple_blocks: Vec<(u32, String)>,
) -> HashMap<String, Vec<(u32, String)>> {
    simple_blocks.sort_by_key(|(id, _)| *id);

    let mut enums = HashMap::new();

    for (block_id, enum_name) in build_config.block_overrides.iter() {
        let Some((idx, _)) = simple_blocks
            .iter()
            .enumerate()
            .find(|(_, (_, id))| id == block_id)
        else {
            continue;
        };

        let entry = enums.entry(enum_name.clone()).or_insert_with(Vec::new);

        entry.push(simple_blocks.remove(idx));
    }

    enums.insert("SimpleBlock".to_string(), simple_blocks);

    enums
}

pub fn fill_simple_block_mappings(
    build_config: &BuildConfig,
    simple_blocks: Vec<(u32, String)>,
    mappings: &mut [TokenStream],
) -> TokenStream {
    let enums = separate_enums(build_config, simple_blocks);

    let mut vtables = Vec::new();

    for (enum_name, variants) in enums {
        let vtable_name = format_ident!("VTABLE_{}", enum_name.to_shouty_snake_case());
        let enum_name = format_ident!("{}", enum_name);

        for (id, _) in variants {
            mappings[id as usize] =
                quote! { crate::StateBehaviorTable::spin_off(&#vtable_name, #id) };
        }

        vtables.push(quote! {
            const #vtable_name: crate::BlockBehaviorTable = crate::BlockBehaviorTable::from::<#enum_name>();
        });
    }

    quote! {
        #(#vtables)*
    }
}

pub fn generate_simple_block_enum(
    build_config: &BuildConfig,
    simple_blocks: Vec<(u32, String)>,
) -> (TokenStream, TokenStream) {
    let enums = separate_enums(build_config, simple_blocks);

    let (enums, impls): (Vec<TokenStream>, Vec<TokenStream>) = enums
        .into_iter()
        .map(|(enum_name, enum_blocks)| {
            let mut map_entries = Vec::new();
            let mut from_arms = Vec::new();
            let mut enum_variants = Vec::new();

            let map_name = format_ident!("{}_BLOCK_MAP", enum_name.to_shouty_snake_case());
            let enum_name = format_ident!("{}", enum_name);

            for (id, name) in enum_blocks {
                let variant = name.strip_prefix("minecraft:").unwrap_or(&name);
                let variant = format_ident!("{}", variant.to_pascal_case());

                enum_variants.push(quote! { #variant });
                from_arms.push(quote! { #id => Ok(#enum_name::#variant) });
                map_entries.push(quote! { #id });
            }

            (
                quote! {
                    #[repr(usize)]
                    #[derive(Clone, Debug, Eq, PartialEq)]
                    pub enum #enum_name {
                        #(#enum_variants),*
                    }
                },
                quote! {
                    const #map_name: &[u32] = &[
                        #(#map_entries),*
                    ];

                    impl TryFrom<u32> for #enum_name {
                        type Error = ();

                        fn try_from(data: u32) -> Result<Self, Self::Error> {
                            match data {
                                #(#from_arms),*,
                                _ => Err(()),
                            }
                        }
                    }

                    impl TryInto<u32> for #enum_name {
                        type Error = ();

                        fn try_into(self) -> Result<u32, Self::Error> {
                            Ok(#map_name[self as usize])
                        }
                    }
                },
            )
        })
        .unzip();

    (
        quote! {
            #(#enums)*
        },
        quote! {
            #(#impls)*
        },
    )
}
