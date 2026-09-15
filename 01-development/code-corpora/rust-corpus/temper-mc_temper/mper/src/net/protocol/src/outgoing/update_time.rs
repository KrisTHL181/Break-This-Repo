use temper_codec::net_types::length_prefixed_vec::LengthPrefixedVec;
use temper_codec::net_types::var_int::VarInt;
use temper_macros::{NetEncode, packet};

#[derive(NetEncode)]
#[packet(packet_id = "set_time", state = "play")]
pub struct UpdateTimePacket {
    pub world_age: u64,
    pub clocks: LengthPrefixedVec<Clock>,
}

#[derive(NetEncode)]
pub struct Clock {
    pub clock_id: VarInt,
    pub time: VarInt,
    pub fractional_time: f32,
    pub rate: f32,
}
