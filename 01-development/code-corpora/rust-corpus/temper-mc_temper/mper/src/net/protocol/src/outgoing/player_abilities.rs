use temper_components::player::abilities::PlayerAbilities as PlayerAbilitiesComponent;
use temper_macros::{NetEncode, packet};

#[derive(NetEncode)]
#[packet(packet_id = "player_abilities", state = "play")]
pub struct PlayerAbilities {
    pub flags: u8,                   // Bit field, see below.
    pub flying_speed: f32,           // 0.05 by default.
    pub field_of_view_modifier: f32, // Modifies field of view, like a speed potion.
}

// About flags
// Field            Bit
// Invulnerable:    0x01
// Flying:          0x02
// Allow Flying:    0x04
// Creative Mode:   0x08

impl PlayerAbilities {
    pub fn from_abilities(abilities: &PlayerAbilitiesComponent) -> Self {
        let flags = u8::from(abilities.invulnerable)
            | (u8::from(abilities.flying) * 0x02)
            | (u8::from(abilities.may_fly) * 0x04)
            | (u8::from(abilities.creative_mode) * 0x08);

        Self {
            flags,
            flying_speed: abilities.flying_speed,
            field_of_view_modifier: abilities.walking_speed,
        }
    }
}
