use rand::prelude::IteratorRandom;
use std::ops::Deref;

use bevy_ecs::entity::Entity;
use temper_components::entity_identity::Identity;
use temper_components::player::player_marker::PlayerMarker;
use uuid::Uuid;

use crate::{ArgumentSpec, CommandArg, CommandReader, ParseError, SuggestionProviderKind};

#[derive(Clone, Debug, Eq, PartialEq)]
struct EntitySelector(String);

macro_rules! entity_arg {
    ($name:ident, single: $single:literal, players_only: $players_only:literal) => {
        #[derive(Clone, Debug, Eq, PartialEq)]
        pub struct $name(EntitySelector);

        impl $name {
            pub fn resolve<'a>(
                &self,
                iter: impl Iterator<Item = (Entity, &'a Identity, Option<&'a PlayerMarker>)>,
            ) -> Vec<Entity> {
                self.0.resolve(iter)
            }
        }

        impl Deref for $name {
            type Target = str;

            fn deref(&self) -> &Self::Target {
                &self.0
            }
        }

        impl CommandArg for $name {
            type Raw<'a> = &'a str;

            fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
                recognize_entity(reader)
            }

            fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
                Ok(Self(EntitySelector(raw.to_string())))
            }

            fn argument_spec() -> ArgumentSpec {
                ArgumentSpec::entity($single, $players_only)
            }

            const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;
        }
    };
}

entity_arg!(EntityArg, single: true, players_only: false);
entity_arg!(EntitiesArg, single: false, players_only: false);
entity_arg!(PlayerArg, single: true, players_only: true);
entity_arg!(PlayersArg, single: false, players_only: true);

impl Deref for EntitySelector {
    type Target = str;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl EntitySelector {
    fn resolve<'a>(
        &self,
        iter: impl Iterator<Item = (Entity, &'a Identity, Option<&'a PlayerMarker>)>,
    ) -> Vec<Entity> {
        match &**self {
            "@e" => iter.map(|(entity, _, _)| entity).collect(),
            "@a" => iter
                .filter_map(|(entity, _, marker)| marker.map(|_| entity))
                .collect(),
            "@r" => iter
                .filter_map(|(entity, _, marker)| marker.map(|_| entity))
                .sample(&mut rand::rng(), 1),
            raw => {
                let uuid = Uuid::parse_str(raw).ok();

                iter.filter_map(|(entity, identity, _)| {
                    if identity.name.as_deref() == Some(raw) || Some(identity.uuid) == uuid {
                        Some(entity)
                    } else {
                        None
                    }
                })
                .collect()
            }
        }
    }
}

fn recognize_entity<'a>(reader: &mut CommandReader<'a>) -> Result<&'a str, ParseError> {
    let cursor = reader.cursor();
    let span = reader.read_word_span()?;

    if span.is_empty() {
        Err(ParseError::expected(cursor, "entity"))
    } else {
        Ok(span)
    }
}
