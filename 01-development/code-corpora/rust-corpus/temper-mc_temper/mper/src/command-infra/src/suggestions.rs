use std::any::type_name;
use std::sync::{LazyLock, RwLock};

use bevy_ecs::entity::Entity;
use bevy_ecs::world::World;

use crate::{CommandArg, SuggestionProviderKind};

static ARG_SUGGESTIONS: LazyLock<RwLock<Vec<RegisteredArgSuggestions>>> =
    LazyLock::new(|| RwLock::new(Vec::new()));

#[derive(Clone, Copy, Debug)]
pub struct SuggestionInput<'a> {
    pub full_input: &'a str,
    pub current_token: &'a str,
    pub source: Entity,
}

#[derive(Clone, Copy)]
struct RegisteredArgSuggestions {
    id: &'static str,
    suggest: for<'a> fn(&mut World, SuggestionInput<'a>) -> Vec<String>,
}

pub fn command_arg_suggestion_id<A: CommandArg + 'static>() -> &'static str {
    type_name::<A>()
}

pub fn register_command_arg_suggestions<A: CommandArg + 'static>() {
    if !matches!(A::SUGGESTIONS, SuggestionProviderKind::Server) {
        return;
    }

    let id = command_arg_suggestion_id::<A>();

    if let Ok(mut suggestions) = ARG_SUGGESTIONS.write() {
        if suggestions.iter().any(|suggestion| suggestion.id == id) {
            return;
        }

        suggestions.push(RegisteredArgSuggestions {
            id,
            suggest: suggest_for_arg::<A>,
        });
    }
}

pub fn suggest_command_arg(
    id: &str,
    world: &mut World,
    input: SuggestionInput<'_>,
) -> Option<Vec<String>> {
    let suggest = ARG_SUGGESTIONS
        .read()
        .ok()
        .and_then(|suggestions| {
            suggestions
                .iter()
                .find(|suggestion| suggestion.id == id)
                .copied()
        })
        .map(|suggestion| suggestion.suggest)?;

    Some(suggest(world, input))
}

fn suggest_for_arg<A: CommandArg + 'static>(
    world: &mut World,
    input: SuggestionInput<'_>,
) -> Vec<String> {
    A::suggest(input, world)
}
