//! Deterministic serde adapter for maps whose typed keys are not JSON strings.

use std::collections::HashMap;
use std::hash::Hash;

use serde::de::Error as DeError;
use serde::ser::Error as SerError;
use serde::{Deserialize, Deserializer, Serialize, Serializer};

pub(crate) fn serialize<K, V, S>(map: &HashMap<K, V>, serializer: S) -> Result<S::Ok, S::Error>
where
    K: Serialize,
    V: Serialize,
    S: Serializer,
{
    let mut entries = map
        .iter()
        .map(|(key, value)| {
            serde_json::to_string(key)
                .map(|sort_key| (sort_key, key, value))
                .map_err(S::Error::custom)
        })
        .collect::<Result<Vec<_>, _>>()?;
    entries.sort_unstable_by(|left, right| left.0.cmp(&right.0));
    entries
        .into_iter()
        .map(|(_, key, value)| (key, value))
        .collect::<Vec<_>>()
        .serialize(serializer)
}

pub(crate) fn deserialize<'de, K, V, D>(deserializer: D) -> Result<HashMap<K, V>, D::Error>
where
    K: Deserialize<'de> + Eq + Hash,
    V: Deserialize<'de>,
    D: Deserializer<'de>,
{
    let entries = Vec::<(K, V)>::deserialize(deserializer)?;
    let expected = entries.len();
    let map = entries.into_iter().collect::<HashMap<_, _>>();
    if map.len() != expected {
        return Err(D::Error::custom(
            "typed planner map contains a duplicate key",
        ));
    }
    Ok(map)
}
