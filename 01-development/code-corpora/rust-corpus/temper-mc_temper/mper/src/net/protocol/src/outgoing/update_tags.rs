use lazy_static::lazy_static;
use temper_codec::net_types::{length_prefixed_vec::LengthPrefixedVec, var_int::VarInt};
use temper_macros::{NetEncode, build_update_tags, packet};

#[derive(NetEncode)]
#[packet(packet_id = "update_tags", state = "configuration")]
pub struct UpdateTagsPacket {
    pub registries: LengthPrefixedVec<TagRegistry>,
}

impl Default for UpdateTagsPacket {
    fn default() -> Self {
        Self::new()
    }
}

impl UpdateTagsPacket {
    pub fn new() -> Self {
        Self {
            registries: LengthPrefixedVec::new(process_tag_packets()),
        }
    }
}

lazy_static! {
    pub static ref UPDATE_TAGS_PACKET: UpdateTagsPacket = UpdateTagsPacket::new();
}

fn process_tag_packets() -> Vec<TagRegistry> {
    let raw_packets = build_update_tags!();
    let decoded: Vec<(String, Vec<(String, Vec<i32>)>)> =
        bitcode::decode(&raw_packets).expect("Generated update tags payload should decode");

    decoded
        .into_iter()
        .map(|(registry_id, tags)| TagRegistry {
            registry_id,
            tags: LengthPrefixedVec::new(
                tags.into_iter()
                    .map(|(name, entries)| Tag {
                        name,
                        entries: LengthPrefixedVec::new(
                            entries.into_iter().map(VarInt::new).collect(),
                        ),
                    })
                    .collect(),
            ),
        })
        .collect()
}

#[derive(Clone, Debug, NetEncode)]
pub struct TagRegistry {
    pub registry_id: String,
    pub tags: LengthPrefixedVec<Tag>,
}

#[derive(Clone, Debug, NetEncode)]
pub struct Tag {
    pub name: String,
    pub entries: LengthPrefixedVec<VarInt>,
}

#[cfg(test)]
mod tests {
    use crate::outgoing::update_tags::UPDATE_TAGS_PACKET;

    #[test]
    fn includes_fluid_water_tags() {
        let fluids = UPDATE_TAGS_PACKET
            .registries
            .data
            .iter()
            .find(|registry| registry.registry_id == "minecraft:fluid")
            .expect("fluid tags should be present");

        let water = fluids
            .tags
            .data
            .iter()
            .find(|tag| tag.name == "minecraft:water")
            .expect("minecraft:water fluid tag should be present");

        let entries = water
            .entries
            .data
            .iter()
            .map(|entry| entry.0)
            .collect::<Vec<_>>();

        assert_eq!(entries, vec![2, 1]);
    }

    #[test]
    fn includes_enchantment_tags() {
        let enchantments = UPDATE_TAGS_PACKET
            .registries
            .data
            .iter()
            .find(|registry| registry.registry_id == "minecraft:enchantment")
            .expect("enchantment tags should be present");

        let tradeable = enchantments
            .tags
            .data
            .iter()
            .find(|tag| tag.name == "minecraft:tradeable")
            .expect("minecraft:tradeable enchantment tag should be present");

        let entries = tradeable
            .entries
            .data
            .iter()
            .map(|entry| entry.0)
            .collect::<Vec<_>>();

        assert_eq!(entries[0], 28);
        assert!(entries.contains(&21));
    }
}
