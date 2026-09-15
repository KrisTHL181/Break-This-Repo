use std::str::FromStr;
use temper_macros::Discriminant;
use temper_text::TextComponent;

#[derive(Clone)]
pub struct BossBarData {
    pub title: TextComponent,
    pub health: f32,
    pub max: f32,
    pub color: BossbarColor,
    pub dividers: BossbarDividers,
    pub flags: BossbarFlags,
}

impl BossBarData {
    pub fn new(title: TextComponent, health: f32, max: f32, color: BossbarColor) -> Self {
        Self {
            title,
            health,
            max,
            color,
            dividers: BossbarDividers::None,
            flags: BossbarFlags::none(),
        }
    }
}

impl std::fmt::Display for BossBarData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            r#"{{ "title": "{}", "health": {}/{}, "color": "{:?}", "dividers": "{:?}", "flags": "{:?}" }}"#,
            self.title,
            self.health,
            self.max,
            self.color.to_string(),
            self.dividers.to_string(),
            self.flags.to_string(),
        )
    }
}

#[derive(Clone, Discriminant, Copy)]
pub enum BossbarColor {
    Pink,
    Blue,
    Red,
    Green,
    Yellow,
    Purple,
    White,
}

impl FromStr for BossbarColor {
    type Err = ();

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        Ok(match input.to_ascii_lowercase().as_str() {
            "blue" => Self::Blue,
            "green" => Self::Green,
            "pink" => Self::Pink,
            "purple" => Self::Purple,
            "red" => Self::Red,
            "white" => Self::White,
            "yellow" => Self::Yellow,
            _ => Err(())?,
        })
    }
}

impl std::fmt::Display for BossbarColor {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{}",
            match self {
                BossbarColor::Pink => "pink",
                BossbarColor::Blue => "blue",
                BossbarColor::Red => "red",
                BossbarColor::Green => "green",
                BossbarColor::Yellow => "yellow",
                BossbarColor::Purple => "purple",
                BossbarColor::White => "white",
            }
        )
    }
}

#[derive(Clone, Discriminant, Copy)]
pub enum BossbarDividers {
    None,
    SixNotches,
    TenNotches,
    TwelveNotches,
    TwentyNotches,
}

impl FromStr for BossbarDividers {
    type Err = ();

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        Ok(match input.to_ascii_lowercase().as_str() {
            "progress" => Self::None,
            "notched_6" => Self::SixNotches,
            "notched_10" => Self::TenNotches,
            "notched_12" => Self::TwelveNotches,
            "notched_20" => Self::TwentyNotches,
            _ => Err(())?,
        })
    }
}

impl std::fmt::Display for BossbarDividers {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{}",
            match self {
                BossbarDividers::None => "none",
                BossbarDividers::SixNotches => "6_notches",
                BossbarDividers::TenNotches => "10_notches",
                BossbarDividers::TwelveNotches => "12_notches",
                BossbarDividers::TwentyNotches => "20_notches",
            }
        )
    }
}

#[derive(Clone)]
pub struct BossbarFlags(u8);

impl std::fmt::Display for BossbarFlags {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        if self.0 == Self::NONE {
            return write!(f, "none");
        }

        let mut flags = vec![];
        if self.0 & Self::DARKSKY != 0 {
            flags.push("darkened_sky");
        }
        if self.0 & Self::DRAGON != 0 {
            flags.push("dragon_bar");
        }
        if self.0 & Self::FOG != 0 {
            flags.push("fog");
        }

        write!(f, "[{}]", flags.join(", "))
    }
}

impl BossbarFlags {
    const NONE: u8 = 0x0;
    const DARKSKY: u8 = 0x1;
    const DRAGON: u8 = 0x2;
    const FOG: u8 = 0x4;

    pub fn none() -> BossbarFlags {
        BossbarFlags(Self::NONE)
    }

    pub fn get(&self) -> u8 {
        self.0
    }

    pub fn add_fog(&mut self) {
        self.0 |= Self::FOG;
    }

    pub fn add_dragon_bar(&mut self) {
        self.0 |= Self::DRAGON;
    }

    pub fn add_darkened_sky(&mut self) {
        self.0 |= Self::DARKSKY;
    }
}
