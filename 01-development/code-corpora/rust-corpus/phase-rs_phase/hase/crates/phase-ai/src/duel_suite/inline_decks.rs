//! Hardcoded starter + metagame decks referenced by `MatchupSpec` via
//! `DeckRef::Inline`. These were originally defined inline in `ai_duel.rs`;
//! they've been moved into the library so the static `MATCHUPS` registry can
//! reference them as fn pointers.
//!
//! Card names must match entries in `client/public/card-data.json`
//! (case-insensitive). Every deck resolves to exactly 60 cards.

fn repeat(name: &str, count: usize) -> Vec<String> {
    vec![name.to_string(); count]
}

pub fn deck_red_aggro() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Mountain", 20));
    d.extend(repeat("Goblin Guide", 4));
    d.extend(repeat("Monastery Swiftspear", 4));
    d.extend(repeat("Raging Goblin", 4));
    d.extend(repeat("Jackal Pup", 4));
    d.extend(repeat("Mogg Fanatic", 4));
    d.extend(repeat("Lightning Bolt", 4));
    d.extend(repeat("Shock", 4));
    d.extend(repeat("Lava Spike", 4));
    d.extend(repeat("Searing Spear", 4));
    d.extend(repeat("Skullcrack", 4));
    d
}

pub fn deck_green_midrange() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Forest", 22));
    d.extend(repeat("Llanowar Elves", 4));
    d.extend(repeat("Elvish Mystic", 4));
    d.extend(repeat("Grizzly Bears", 4));
    d.extend(repeat("Kalonian Tusker", 4));
    d.extend(repeat("Centaur Courser", 4));
    d.extend(repeat("Leatherback Baloth", 2));
    d.extend(repeat("Giant Growth", 4));
    d.extend(repeat("Rancor", 4));
    d.extend(repeat("Titanic Growth", 4));
    d.extend(repeat("Rabid Bite", 4));
    d
}

pub fn deck_blue_control() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Island", 26));
    d.extend(repeat("Counterspell", 4));
    d.extend(repeat("Mana Leak", 4));
    d.extend(repeat("Essence Scatter", 2));
    d.extend(repeat("Negate", 2));
    d.extend(repeat("Unsummon", 4));
    d.extend(repeat("Divination", 4));
    d.extend(repeat("Opt", 4));
    d.extend(repeat("Think Twice", 2));
    d.extend(repeat("Air Elemental", 4));
    d.extend(repeat("Frost Titan", 2));
    d.extend(repeat("Mulldrifter", 2));
    d
}

pub fn deck_black_midrange() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Swamp", 24));
    d.extend(repeat("Vampire Nighthawk", 4));
    d.extend(repeat("Gifted Aetherborn", 4));
    d.extend(repeat("Hypnotic Specter", 4));
    d.extend(repeat("Gray Merchant of Asphodel", 4));
    d.extend(repeat("Nighthawk Scavenger", 4));
    d.extend(repeat("Doom Blade", 4));
    d.extend(repeat("Go for the Throat", 4));
    d.extend(repeat("Sign in Blood", 4));
    d.extend(repeat("Read the Bones", 2));
    d.extend(repeat("Duress", 2));
    d
}

pub fn deck_white_weenie() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Plains", 20));
    d.extend(repeat("Savannah Lions", 4));
    d.extend(repeat("Elite Vanguard", 4));
    d.extend(repeat("Soldier of the Pantheon", 4));
    d.extend(repeat("Thalia, Guardian of Thraben", 4));
    d.extend(repeat("Serra Angel", 4));
    d.extend(repeat("Benalish Marshal", 4));
    d.extend(repeat("Swords to Plowshares", 4));
    d.extend(repeat("Raise the Alarm", 4));
    d.extend(repeat("Glorious Anthem", 4));
    d.extend(repeat("Honor of the Pure", 4));
    d
}

pub fn deck_azorius_control() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Floodfarm Verge", 4));
    d.extend(repeat("Hallowed Fountain", 4));
    d.extend(repeat("Deserted Beach", 3));
    d.extend(repeat("Meticulous Archive", 2));
    d.extend(repeat("Restless Anchorage", 2));
    d.extend(repeat("Fountainport", 2));
    d.extend(repeat("Island", 2));
    d.extend(repeat("Plains", 2));
    d.extend(repeat("Eiganjo, Seat of the Empire", 1));
    d.extend(repeat("Field of Ruin", 2));
    d.extend(repeat("Hall of Storm Giants", 1));
    d.extend(repeat("Otawara, Soaring City", 1));
    d.extend(repeat("No More Lies", 4));
    d.extend(repeat("Dovin's Veto", 1));
    d.extend(repeat("Change the Equation", 1));
    d.extend(repeat("March of Otherworldly Light", 4));
    d.extend(repeat("Get Lost", 3));
    d.extend(repeat("Supreme Verdict", 2));
    d.extend(repeat("Farewell", 1));
    d.extend(repeat("Consult the Star Charts", 4));
    d.extend(repeat("Stock Up", 2));
    d.extend(repeat("Three Steps Ahead", 1));
    d.extend(repeat("Pinnacle Starcage", 3));
    d.extend(repeat("The Wandering Emperor", 3));
    d.extend(repeat("Teferi, Hero of Dominaria", 2));
    d.extend(repeat("Beza, the Bounding Spring", 2));
    d.extend(repeat("Elspeth, Storm Slayer", 1));
    d
}

pub fn deck_mono_red_prowess() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Mountain", 14));
    d.extend(repeat("Den of the Bugbear", 2));
    d.extend(repeat("Ramunap Ruins", 3));
    d.extend(repeat("Rockface Village", 2));
    d.extend(repeat("Sokenzan, Crucible of Defiance", 1));
    d.extend(repeat("Monastery Swiftspear", 4));
    d.extend(repeat("Soul-Scar Mage", 4));
    d.extend(repeat("Emberheart Challenger", 4));
    d.extend(repeat("Screaming Nemesis", 4));
    d.extend(repeat("Sunspine Lynx", 3));
    d.extend(repeat("Burst Lightning", 4));
    d.extend(repeat("Monstrous Rage", 4));
    d.extend(repeat("Reckless Rage", 4));
    d.extend(repeat("Kumano Faces Kakkazan", 4));
    d.extend(repeat("Lightning Strike", 2));
    d.extend(repeat("Abrade", 1));
    d
}

pub fn deck_gruul_prowess() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Mountain", 6));
    d.extend(repeat("Stomping Ground", 4));
    d.extend(repeat("Copperline Gorge", 4));
    d.extend(repeat("Thornspire Verge", 4));
    d.extend(repeat("Den of the Bugbear", 1));
    d.extend(repeat("Ramunap Ruins", 1));
    d.extend(repeat("Sokenzan, Crucible of Defiance", 1));
    d.extend(repeat("Monastery Swiftspear", 4));
    d.extend(repeat("Soul-Scar Mage", 4));
    d.extend(repeat("Emberheart Challenger", 4));
    d.extend(repeat("Questing Druid", 4));
    d.extend(repeat("Cori-Steel Cutter", 4));
    d.extend(repeat("Screaming Nemesis", 2));
    d.extend(repeat("Burst Lightning", 4));
    d.extend(repeat("Kumano Faces Kakkazan", 4));
    d.extend(repeat("Academic Dispute", 4));
    d.extend(repeat("Reckless Rage", 3));
    d.extend(repeat("Monstrous Rage", 2));
    d
}

pub fn deck_izzet_delver() -> Vec<String> {
    let mut d = Vec::with_capacity(60);
    d.extend(repeat("Volcanic Island", 4));
    d.extend(repeat("Wasteland", 4));
    d.extend(repeat("Scalding Tarn", 2));
    d.extend(repeat("Misty Rainforest", 2));
    d.extend(repeat("Flooded Strand", 3));
    d.extend(repeat("Polluted Delta", 2));
    d.extend(repeat("Island", 1));
    d.extend(repeat("Thundering Falls", 1));
    d.extend(repeat("Delver of Secrets", 4));
    d.extend(repeat("Dragon's Rage Channeler", 4));
    d.extend(repeat("Cori-Steel Cutter", 3));
    d.extend(repeat("Murktide Regent", 2));
    d.extend(repeat("Brazen Borrower", 1));
    d.extend(repeat("Brainstorm", 4));
    d.extend(repeat("Ponder", 4));
    d.extend(repeat("Mishra's Bauble", 4));
    d.extend(repeat("Preordain", 1));
    d.extend(repeat("Force of Will", 4));
    d.extend(repeat("Daze", 4));
    d.extend(repeat("Spell Pierce", 1));
    d.extend(repeat("Lightning Bolt", 4));
    d.extend(repeat("Unholy Heat", 1));
    d
}
