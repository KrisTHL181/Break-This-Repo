use quote::quote;
use std::collections::BTreeMap;

pub(crate) fn build_mapping(_: proc_macro::TokenStream) -> proc_macro::TokenStream {
    let tag_packets: BTreeMap<String, BTreeMap<String, Vec<i32>>> =
        serde_json::from_str(temper_assets::generated::TAG_PACKETS).unwrap();

    let tag_packets = tag_packets
        .into_iter()
        .map(|(registry_id, tags)| (registry_id, tags.into_iter().collect::<Vec<_>>()))
        .collect::<Vec<_>>();
    let raw_packets_data = bitcode::encode(&tag_packets);

    quote! {
        vec![#(#raw_packets_data),*]
    }
    .into()
}
