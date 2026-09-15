use std::collections::HashSet;

use bevy_ecs::prelude::*;
use bevy_ecs::system::SystemState;
use temper_codec::net_types::{
    length_prefixed_vec::LengthPrefixedVec, prefixed_optional::PrefixedOptional, var_int::VarInt,
};
use temper_command_infra::{
    CommandPathSegment, CommandRegistry, EntityProperties, ParserKind, ParserProperties,
    SuggestionInput, suggest_command_arg,
};
use temper_net_runtime::connection::StreamWriter;
use temper_permissions::player::PlayerPermission;
use temper_protocol::CommandSuggestionRequestReceiver;
use temper_protocol::outgoing::command_suggestions::{CommandSuggestionsPacket, Match};
use temper_state::GlobalStateResource;
use tracing::error;

pub fn handle(world: &mut World) {
    let requests = {
        let mut system_state = SystemState::<Res<CommandSuggestionRequestReceiver>>::new(world);
        let receiver = system_state.get(world);
        receiver
            .expect("unable to get receiver from world")
            .0
            .try_iter()
            .collect::<Vec<_>>()
    };

    for (request, entity) in requests {
        let Some(suggestions) = suggestion_plan(world, &request.input, entity) else {
            continue;
        };

        let input = request.input.clone();
        let response = suggestions.into_response(|provider, current_token| {
            suggest_command_arg(
                provider,
                world,
                SuggestionInput {
                    full_input: &input,
                    current_token,
                    source: entity,
                },
            )
            .unwrap_or_default()
        });

        let mut system_state = SystemState::<Query<&StreamWriter>>::new(world);
        let query = system_state.get(world).expect("could not query world");
        let Ok(writer) = query.get(entity) else {
            continue;
        };

        send_suggestions(writer, request.transaction_id, response);
    }
}

struct SuggestionResponse {
    start: usize,
    length: usize,
    matches: Vec<Match>,
}

struct SuggestionPlan {
    start: usize,
    length: usize,
    current_token: String,
    candidates: Vec<String>,
    providers: Vec<ProviderSuggestions>,
}

impl SuggestionPlan {
    fn into_response(
        self,
        mut provider_suggestions: impl FnMut(&'static str, &str) -> Vec<String>,
    ) -> SuggestionResponse {
        let current_token_lower = self.current_token.to_lowercase();
        let mut seen = HashSet::new();
        let provider_candidates = self.providers.into_iter().flat_map(|provider| {
            let suggestions = provider_suggestions(provider.id, &self.current_token);

            provider.suggest(suggestions).collect::<Vec<_>>()
        });

        let matches = self
            .candidates
            .into_iter()
            .chain(provider_candidates)
            .filter(|suggestion| suggestion.to_lowercase().starts_with(&current_token_lower))
            .filter(|suggestion| seen.insert(suggestion.clone()))
            .map(|content| Match {
                content,
                tooltip: PrefixedOptional::new(None),
            })
            .collect();

        SuggestionResponse {
            start: self.start,
            length: self.length,
            matches,
        }
    }
}

struct ProviderSuggestions {
    id: &'static str,
    fallback: Vec<String>,
}

impl ProviderSuggestions {
    fn suggest(self, suggestions: Vec<String>) -> impl Iterator<Item = String> {
        suggestions.into_iter().chain(self.fallback)
    }
}

fn suggestion_plan(world: &mut World, input: &str, entity: Entity) -> Option<SuggestionPlan> {
    let mut system_state = SystemState::<(
        Query<&StreamWriter>,
        Query<&PlayerPermission>,
        Res<CommandRegistry>,
        Res<GlobalStateResource>,
    )>::new(world);
    let (query, permissions, registry, state) =
        system_state.get(world).expect("could not get system state");

    if !state.0.players.is_connected(entity) {
        return None;
    }

    if query.get(entity).is_err() {
        return None;
    }

    command_suggestion_plan(input, &registry, &state, permissions.get(entity).ok())
}

#[cfg(test)]
fn command_suggestions(
    input: &str,
    registry: &CommandRegistry,
    state: &GlobalStateResource,
    permissions: Option<&PlayerPermission>,
) -> Option<SuggestionResponse> {
    Some(
        command_suggestion_plan(input, registry, state, permissions)?
            .into_response(|_provider, _current_token| Vec::new()),
    )
}

fn command_suggestion_plan(
    input: &str,
    registry: &CommandRegistry,
    state: &GlobalStateResource,
    permissions: Option<&PlayerPermission>,
) -> Option<SuggestionPlan> {
    let command_input = input.strip_prefix('/').unwrap_or(input);
    let root_end = command_input
        .find(char::is_whitespace)
        .unwrap_or(command_input.len());
    let root = &command_input[..root_end];
    let command = registry
        .commands()
        .iter()
        .find(|command| command.matches_root(root))?;
    let rest = command_input[root_end..].trim_start();
    let current_token = current_token(rest);
    let completed_tokens = completed_tokens(rest);

    let mut candidates = Vec::new();
    let mut providers = Vec::new();

    for segment in command
        .paths
        .iter()
        .filter(|path| path.root == root)
        .filter(|path| {
            path.is_allowed_by(|permission| permissions.is_some_and(|p| p.can(permission)))
        })
        .filter_map(|path| candidate_segment(&path.segments, &completed_tokens))
    {
        match segment_suggestions(segment, state) {
            Some(SegmentSuggestions::Candidates(next_candidates)) => {
                candidates.extend(next_candidates);
            }
            Some(SegmentSuggestions::Provider(provider)) => providers.push(provider),
            None => {}
        }
    }

    Some(SuggestionPlan {
        start: input.len() - current_token.len(),
        length: current_token.len(),
        current_token: current_token.to_string(),
        candidates,
        providers,
    })
}

fn current_token(input: &str) -> &str {
    if input.ends_with(char::is_whitespace) {
        ""
    } else {
        input.split_whitespace().last().unwrap_or("")
    }
}

fn completed_tokens(input: &str) -> Vec<&str> {
    let mut tokens = input.split_whitespace().collect::<Vec<_>>();

    if !input.ends_with(char::is_whitespace) {
        tokens.pop();
    }

    tokens
}

fn candidate_segment<'a>(
    segments: &'a [CommandPathSegment],
    completed_tokens: &[&str],
) -> Option<&'a CommandPathSegment> {
    let mut token_index = 0;

    for (segment_index, segment) in segments.iter().enumerate() {
        let Some(token) = completed_tokens.get(token_index) else {
            return Some(segment);
        };

        if !segment_accepts_token(segment, token) {
            return None;
        }

        if segment_index == segments.len() - 1
            && matches!(
                segment,
                CommandPathSegment::Argument {
                    spec: temper_command_infra::ArgumentSpec {
                        server_suggestions: Some(_),
                        ..
                    },
                    ..
                }
            )
        {
            return Some(segment);
        }

        token_index += segment_width(segment);
    }

    None
}

fn segment_accepts_token(segment: &CommandPathSegment, token: &str) -> bool {
    match segment {
        CommandPathSegment::Literal { name, .. } => name == &token,
        CommandPathSegment::Argument { spec, .. } => match spec.parser {
            ParserKind::Integer => token.parse::<i32>().is_ok(),
            ParserKind::Position => is_coordinate_token(token),
            ParserKind::Word | ParserKind::String | ParserKind::Entity | ParserKind::Resource => {
                !token.is_empty()
            }
        },
    }
}

fn segment_width(segment: &CommandPathSegment) -> usize {
    match segment {
        CommandPathSegment::Argument { spec, .. } if spec.parser == ParserKind::Position => 3,
        _ => 1,
    }
}

fn is_coordinate_token(token: &str) -> bool {
    if let Some(relative) = token.strip_prefix('~') {
        relative.is_empty() || relative.parse::<f64>().is_ok()
    } else {
        token.parse::<f64>().is_ok()
    }
}

enum SegmentSuggestions {
    Candidates(Vec<String>),
    Provider(ProviderSuggestions),
}

fn segment_suggestions(
    segment: &CommandPathSegment,
    state: &GlobalStateResource,
) -> Option<SegmentSuggestions> {
    match segment {
        CommandPathSegment::Literal { name, .. } => {
            Some(SegmentSuggestions::Candidates(vec![(*name).to_string()]))
        }
        CommandPathSegment::Argument { spec, .. } if spec.server_suggestions.is_some() => {
            Some(SegmentSuggestions::Provider(ProviderSuggestions {
                id: spec.server_suggestions.unwrap(),
                fallback: provider_fallback_suggestions(*spec, state),
            }))
        }
        CommandPathSegment::Argument { spec, .. } if is_ask_server(spec.protocol_suggestions) => {
            match spec.parser {
                ParserKind::Entity => Some(SegmentSuggestions::Candidates(
                    entity_fallback_suggestions(*spec, state),
                )),
                _ => Some(SegmentSuggestions::Candidates(Vec::new())),
            }
        }
        _ => None,
    }
}

fn is_ask_server(suggestions: Option<&str>) -> bool {
    matches!(suggestions, Some("ask_server" | "minecraft:ask_server"))
}

fn provider_fallback_suggestions(
    spec: temper_command_infra::ArgumentSpec,
    state: &GlobalStateResource,
) -> Vec<String> {
    match spec.parser {
        ParserKind::Entity => entity_fallback_suggestions(spec, state),
        _ => Vec::new(),
    }
}

fn entity_fallback_suggestions(
    spec: temper_command_infra::ArgumentSpec,
    state: &GlobalStateResource,
) -> Vec<String> {
    let players_only = matches!(
        spec.properties,
        Some(ParserProperties::Entity(EntityProperties {
            players_only: true,
            ..
        }))
    );

    entity_suggestions(state, players_only)
}

fn entity_suggestions(state: &GlobalStateResource, players_only: bool) -> Vec<String> {
    let mut suggestions = if players_only {
        vec!["@r".to_string(), "@a".to_string()]
    } else {
        vec!["@e".to_string(), "@r".to_string(), "@a".to_string()]
    };

    suggestions.extend(
        state
            .0
            .players
            .player_list
            .iter()
            .map(|player| player.value().1.clone()),
    );
    suggestions
}

fn send_suggestions(writer: &StreamWriter, transaction_id: VarInt, response: SuggestionResponse) {
    if let Err(e) = writer.send_packet(CommandSuggestionsPacket {
        transaction_id,
        matches: LengthPrefixedVec::new(response.matches),
        length: VarInt::new(response.length as i32),
        start: VarInt::new(response.start as i32),
    }) {
        error!("failed sending command suggestions to player: {e}")
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use bevy_ecs::entity::Entity;
    use temper_command_infra::{ArgumentSpec, CommandPath, RegisteredCommand};
    use temper_state::create_test_state;

    fn registry() -> CommandRegistry {
        let mut registry = CommandRegistry::default();
        registry.register_command(RegisteredCommand {
            name: "tp",
            aliases: &[],
            permission: None,
            paths: vec![
                CommandPath::new(
                    "tp",
                    vec![CommandPathSegment::argument(
                        "location",
                        ArgumentSpec::new(ParserKind::Position),
                    )],
                ),
                CommandPath::new(
                    "tp",
                    vec![CommandPathSegment::argument(
                        "destination",
                        ArgumentSpec::new(ParserKind::Entity)
                            .with_protocol_suggestions("minecraft:ask_server")
                            .with_server_suggestions("test:entity"),
                    )],
                ),
                CommandPath::new(
                    "tp",
                    vec![
                        CommandPathSegment::argument(
                            "target",
                            ArgumentSpec::new(ParserKind::Entity)
                                .with_protocol_suggestions("minecraft:ask_server")
                                .with_server_suggestions("test:entities"),
                        ),
                        CommandPathSegment::argument(
                            "location",
                            ArgumentSpec::new(ParserKind::Position),
                        ),
                    ],
                ),
                CommandPath::new(
                    "tp",
                    vec![
                        CommandPathSegment::argument(
                            "target",
                            ArgumentSpec::new(ParserKind::Entity)
                                .with_protocol_suggestions("minecraft:ask_server")
                                .with_server_suggestions("test:entities"),
                        ),
                        CommandPathSegment::argument(
                            "destination",
                            ArgumentSpec::new(ParserKind::Entity)
                                .with_protocol_suggestions("minecraft:ask_server")
                                .with_server_suggestions("test:entity"),
                        ),
                    ],
                ),
            ],
        });
        registry
    }

    fn time_registry() -> CommandRegistry {
        let mut registry = CommandRegistry::default();
        registry.register_command(RegisteredCommand {
            name: "time",
            aliases: &[],
            permission: None,
            paths: vec![
                CommandPath::new(
                    "time",
                    vec![
                        CommandPathSegment::literal("set"),
                        CommandPathSegment::literal("day"),
                    ],
                ),
                CommandPath::new(
                    "time",
                    vec![
                        CommandPathSegment::literal("set"),
                        CommandPathSegment::literal("d"),
                    ],
                ),
                CommandPath::new(
                    "time",
                    vec![
                        CommandPathSegment::literal("set"),
                        CommandPathSegment::argument(
                            "value",
                            ArgumentSpec::new(ParserKind::String),
                        ),
                    ],
                ),
            ],
        });
        registry
    }

    fn server_suggested_word_registry() -> CommandRegistry {
        let mut registry = CommandRegistry::default();
        registry.register_command(RegisteredCommand {
            name: "custom",
            aliases: &[],
            permission: None,
            paths: vec![CommandPath::new(
                "custom",
                vec![CommandPathSegment::argument(
                    "value",
                    ArgumentSpec::new(ParserKind::String)
                        .with_protocol_suggestions("minecraft:ask_server")
                        .with_server_suggestions("test:value"),
                )],
            )],
        });
        registry
    }

    #[test]
    fn command_suggestions_include_entities_for_first_tp_arg() {
        let (state, _temp_dir) = create_test_state();
        state
            .0
            .players
            .player_list
            .insert(Entity::PLACEHOLDER, (0, "Alex".to_string()));

        let suggestions = command_suggestions("/tp ", &registry(), &state, None).unwrap();
        let matches = suggestions
            .matches
            .iter()
            .map(|suggestion| suggestion.content.as_str())
            .collect::<Vec<_>>();

        assert_eq!(suggestions.start, 4);
        assert_eq!(suggestions.length, 0);
        assert!(matches.contains(&"@a"));
        assert!(matches.contains(&"Alex"));
    }

    #[test]
    fn command_suggestions_include_entities_for_bare_tp() {
        let (state, _temp_dir) = create_test_state();
        state
            .0
            .players
            .player_list
            .insert(Entity::PLACEHOLDER, (0, "Alex".to_string()));

        let suggestions = command_suggestions("/tp", &registry(), &state, None).unwrap();
        let matches = suggestions
            .matches
            .iter()
            .map(|suggestion| suggestion.content.as_str())
            .collect::<Vec<_>>();

        assert_eq!(suggestions.start, 3);
        assert_eq!(suggestions.length, 0);
        assert!(matches.contains(&"@a"));
        assert!(matches.contains(&"Alex"));
    }

    #[test]
    fn command_suggestions_use_current_token_range() {
        let (state, _temp_dir) = create_test_state();
        state
            .0
            .players
            .player_list
            .insert(Entity::PLACEHOLDER, (0, "Alex".to_string()));

        let suggestions = command_suggestions("/tp Steve A", &registry(), &state, None).unwrap();
        let matches = suggestions
            .matches
            .iter()
            .map(|suggestion| suggestion.content.as_str())
            .collect::<Vec<_>>();

        assert_eq!(suggestions.start, 10);
        assert_eq!(suggestions.length, 1);
        assert_eq!(matches, vec!["Alex"]);
    }

    #[test]
    fn command_suggestions_include_time_literals() {
        let (state, _temp_dir) = create_test_state();

        let suggestions =
            command_suggestions("/time set ", &time_registry(), &state, None).unwrap();
        let matches = suggestions
            .matches
            .iter()
            .map(|suggestion| suggestion.content.as_str())
            .collect::<Vec<_>>();

        assert_eq!(suggestions.start, 10);
        assert_eq!(suggestions.length, 0);
        assert_eq!(matches, vec!["day", "d"]);
    }

    #[test]
    fn server_suggested_non_entity_args_do_not_get_entity_fallbacks() {
        let (state, _temp_dir) = create_test_state();
        state
            .0
            .players
            .player_list
            .insert(Entity::PLACEHOLDER, (0, "Alex".to_string()));

        let suggestions =
            command_suggestions("/custom ", &server_suggested_word_registry(), &state, None)
                .unwrap();

        assert!(suggestions.matches.is_empty());
    }

    #[test]
    fn command_suggestions_use_command_registry_only() {
        let (state, _temp_dir) = create_test_state();

        assert!(command_suggestions("/tp Unknown", &registry(), &state, None).is_some());
        assert!(command_suggestions("/time set ", &registry(), &state, None).is_none());
    }
}
