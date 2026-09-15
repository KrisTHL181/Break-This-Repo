use crate::bossbar::{BossBarData, BossbarColor, BossbarDividers, BossbarFlags};
use std::fmt::Display;
use temper_text::TextComponent;

#[derive(Clone)]
pub enum UpdateBBKind {
    Add {
        data: BossBarData,
    },
    Remove,
    UpdateHealth {
        new_health: f32,
        new_max: f32,
    },
    UpdateTitle {
        title: TextComponent,
    },
    UpdateStyle {
        color: BossbarColor,
        dividers: BossbarDividers,
    },
    UpdateFlags {
        flags: BossbarFlags,
    },
    UpdateNetworking {
        additive: bool,
    },
}

impl Display for UpdateBBKind {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            UpdateBBKind::Add { data } => write!(f, "Add {}", data),
            UpdateBBKind::Remove => write!(f, "Remove"),
            UpdateBBKind::UpdateHealth {
                new_health,
                new_max,
            } => write!(f, "Update Health {new_health} / {new_max}"),
            UpdateBBKind::UpdateTitle { title } => write!(f, "Update Title {}", title),
            UpdateBBKind::UpdateStyle { color, dividers } => {
                write!(f, "Update Style {} & {}", color, dividers)
            }
            UpdateBBKind::UpdateFlags { flags } => write!(f, "Update Flags {}", flags),
            _ => write!(f, "Updating"),
        }
    }
}
