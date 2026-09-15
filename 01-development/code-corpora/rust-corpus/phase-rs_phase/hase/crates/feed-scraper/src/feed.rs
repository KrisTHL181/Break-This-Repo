use serde::Serialize;

#[derive(Debug, Serialize)]
pub struct Feed {
    pub id: String,
    pub name: String,
    pub description: String,
    pub icon: String,
    pub format: String,
    pub version: u32,
    pub updated: String,
    pub source: String,
    pub decks: Vec<FeedDeck>,
}

#[derive(Debug, Serialize)]
pub struct FeedDeck {
    pub name: String,
    pub author: String,
    pub colors: Vec<String>,
    pub tags: Vec<String>,
    pub main: Vec<DeckEntry>,
    pub sideboard: Vec<DeckEntry>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub commander: Option<Vec<String>>,
    /// CR 702.139a: The declared companion card name (lives outside the game).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub companion: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct DeckEntry {
    pub count: u32,
    pub name: String,
}
