use std::fmt::Debug;

use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(untagged)]
pub enum LanguageCode {
    VanillaCode(String),
    CustomCode((String, String)),
}
