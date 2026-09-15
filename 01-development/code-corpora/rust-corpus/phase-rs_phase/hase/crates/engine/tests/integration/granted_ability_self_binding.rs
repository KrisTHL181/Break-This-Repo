//! Runtime + grant-clone proofs for the granted-ability self-reference dual
//! binding (S25). CR 201.5a: when an ability's effect grants another ability
//! that refers to the granting object BY NAME, the name refers only to the
//! granting object — never to the host it was granted to.
//!
//! Three independent channels are exercised, and must stay separate. Each now
//! has a TYPED half (the AST the engine resolves) and a DISPLAY half (the
//! `description` string the client renders), and the two must agree:
//!   1. Granter-referential ("Exile/Sacrifice/Return <granter-name>") →
//!      TYPED: `TargetFilter::GrantingObject` → concretized to
//!      `SpecificObject{granter}`. DISPLAY: the granting card's PRINTED name
//!      (`oracle_util::render_granting_self_reference`, CR 201.5a + CR 201.5c).
//!   2. Host-referential ("Sacrifice this permanent") → TYPED: stays `SelfRef` →
//!      host. DISPLAY: stays the host token `~`, which the client substitutes
//!      with the object's own name (CR 201.5b).
//!   3. Host power read ("where X is this creature's power") → TYPED:
//!      `QuantityRef::Power` (never a `TargetFilter`) → unchanged. DISPLAY:
//!      unchanged.
//!
//! A display half that disagreed with its typed half would be strictly worse
//! than a consistent error: the UI would say "sacrifice the Equipment" while the
//! engine sacrificed the creature.
//!
//! Every behavioral test drives the production Layer-6 grant path
//! (`evaluate_layers`) and, for Deconstruction Hammer, the full activate/resolve
//! pipeline asserting which object left the battlefield.

use std::sync::Arc;

use engine::game::game_object::AttachTarget;
use engine::game::layers::evaluate_layers;
use engine::game::scenario::{GameScenario, P0, P1};
use engine::parser::oracle::parse_oracle_text;
use engine::parser::oracle_util::normalize_card_name_refs;
use engine::types::ability::{
    AbilityCost, AbilityDefinition, ContinuousModification, Effect, ObjectScope, QuantityExpr,
    QuantityRef, StaticDefinition, TargetFilter,
};
use engine::types::card_type::CoreType;
use engine::types::identifiers::ObjectId;
use engine::types::mana::{ManaType, ManaUnit};
use engine::types::phase::Phase;
use engine::types::zones::Zone;

fn equipment_types() -> (Vec<String>, Vec<String>) {
    (vec!["Artifact".to_string()], vec!["Equipment".to_string()])
}

/// The `AbilityDefinition` an equipment grants via its "Equipped creature has …"
/// static (the parse-time, pre-concretization body).
fn granted_activated_def(oracle: &str, name: &str) -> AbilityDefinition {
    let (types, subtypes) = equipment_types();
    let parsed = parse_oracle_text(oracle, name, &[], &types, &subtypes);
    grant_ability_static(&parsed.statics)
        .modifications
        .iter()
        .find_map(|m| match m {
            ContinuousModification::GrantAbility { definition } => Some((**definition).clone()),
            _ => None,
        })
        .expect("equipment must grant an activated ability")
}

fn grant_ability_static(statics: &[StaticDefinition]) -> StaticDefinition {
    statics
        .iter()
        .find(|s| {
            s.modifications
                .iter()
                .any(|m| matches!(m, ContinuousModification::GrantAbility { .. }))
        })
        .expect("equipment must have a GrantAbility static")
        .clone()
}

/// Install `grant_static` on a fresh artifact-equipment attached to `host`, then
/// run the production layer engine so the granted ability is cloned onto the
/// host with its granter self-references concretized.
fn equip_and_layer(
    scenario: GameScenario,
    equipment: ObjectId,
    host: ObjectId,
    grant_static: StaticDefinition,
) -> engine::game::scenario::GameRunner {
    let mut runner = scenario.build();
    {
        let st = runner.state_mut();
        let obj = st.objects.get_mut(&equipment).unwrap();
        obj.card_types.core_types = vec![CoreType::Artifact];
        obj.card_types.subtypes = vec!["Equipment".to_string()];
        obj.base_card_types = obj.card_types.clone();
        obj.power = None;
        obj.toughness = None;
        obj.base_power = None;
        obj.base_toughness = None;
        obj.attached_to = Some(AttachTarget::Object(host));
        obj.static_definitions.push(grant_static.clone());
        Arc::make_mut(&mut obj.base_static_definitions).push(grant_static);
        st.layers_dirty.mark_full();
    }
    evaluate_layers(runner.state_mut());
    runner
}

fn granted_ability_index(
    runner: &engine::game::scenario::GameRunner,
    host: ObjectId,
    pred: impl Fn(&AbilityDefinition) -> bool,
) -> usize {
    runner.state().objects[&host]
        .abilities
        .iter()
        .position(pred)
        .expect("host must carry the granted ability after evaluate_layers")
}

// ---------------------------------------------------------------------------
// Direction A — granter-referential COST/EFFECT resolves to the GRANTING object.
// ---------------------------------------------------------------------------

/// A1: Deconstruction Hammer's sacrifice cost sacrifices THE HAMMER (the granting
/// equipment), not the equipped creature. Full activate/resolve pipeline; asserts
/// which object left the battlefield.
///
/// Revert-to-red: remove the `layers.rs` GrantingObject→SpecificObject rewrite →
/// the cost stays `GrantingObject`, the defensive runtime arm resolves it to the
/// ability source (host) → the CREATURE is sacrificed and the Hammer survives →
/// both the concretization `assert_eq!` and the zone assertions flip.
#[test]
fn deconstruction_hammer_sacrifice_hits_the_equipment_not_the_host() {
    let mut scenario = GameScenario::new();
    scenario.at_phase(Phase::PreCombatMain);
    scenario.with_mana_pool(
        P0,
        vec![
            ManaUnit::new(ManaType::White, ObjectId(0), false, vec![]),
            ManaUnit::new(ManaType::White, ObjectId(0), false, vec![]),
            ManaUnit::new(ManaType::White, ObjectId(0), false, vec![]),
        ],
    );
    let host = scenario.add_creature(P0, "Bearer", 2, 2).id();
    let hammer = scenario
        .add_creature(P0, "Deconstruction Hammer", 0, 0)
        .id();
    let victim = scenario.add_creature(P1, "Relic", 0, 0).id();

    let (types, subtypes) = equipment_types();
    let grant_static = grant_ability_static(
        &parse_oracle_text(
            DECONSTRUCTION_HAMMER,
            "Deconstruction Hammer",
            &[],
            &types,
            &subtypes,
        )
        .statics,
    );

    let mut runner = {
        // Make the victim a destructible artifact target BEFORE the layer pass.
        let mut runner = equip_and_layer(scenario, hammer, host, grant_static);
        {
            let v = runner.state_mut().objects.get_mut(&victim).unwrap();
            v.card_types.core_types = vec![CoreType::Artifact];
            v.base_card_types = v.card_types.clone();
            v.power = None;
            v.toughness = None;
            v.base_power = None;
            v.base_toughness = None;
        }
        runner
    };

    let idx = granted_ability_index(&runner, host, |a| {
        a.cost.as_ref().and_then(sacrifice_target).is_some()
    });

    // Concretization proof (the layers.rs seam): the sacrifice cost (inside the
    // `{3},{T},Sacrifice` Composite) targets the Hammer, not `SelfRef`/`GrantingObject`.
    assert_eq!(
        runner.state().objects[&host].abilities[idx]
            .cost
            .as_ref()
            .and_then(sacrifice_target),
        Some(&TargetFilter::SpecificObject { id: hammer }),
        "CR 201.5a: sacrifice cost must target the granting Hammer, not the host"
    );

    // DISPLAY half of the same seam (matrix rows 1 and 3). This MUST run before
    // the activate below: the Hammer is sacrificed, the grant ends, and
    // `objects[&host].abilities` is empty afterwards (measured: index out of
    // bounds, len 0).
    let desc = runner.state().objects[&host].abilities[idx]
        .description
        .clone()
        .expect("the granted ability carries a display description");
    assert_eq!(
        desc, "{3}, {T}, Sacrifice Deconstruction Hammer: Destroy target artifact or enchantment.",
        "CR 201.5a: the granted body's description must name the GRANTING Hammer, \
         not collapse to the host token `~`"
    );
    // CLIENT PARITY, weaker form. `renderDescription(desc, object.name)` on the
    // host must not put the host's name anywhere in this body. This card's
    // effect half carries no `~`, so this proves only "the host name appears
    // NOWHERE"; the discriminating both-halves fixture is
    // `game::effects::token::tests::catalog_toggo_rock_sacrifice_cost_binds_to_rock_not_host`
    // (Rock's printed body carries a CR 201.5a granter reference in the cost AND
    // a CR 201.5b host `~` in the effect).
    let rendered = desc.replace('~', "Bearer");
    assert!(
        rendered.starts_with("{3}, {T}, Sacrifice Deconstruction Hammer:"),
        "CR 201.5a: a blanket `~`-replace would render `Sacrifice Bearer:`; got {rendered}"
    );
    assert_eq!(
        rendered.matches("Bearer").count(),
        0,
        "the host's name must not appear anywhere in this granted body; got {rendered}"
    );

    // Runtime proof: activate the granted ability, paying the sacrifice cost with
    // the Hammer and targeting the artifact, then assert which permanents left the
    // battlefield.
    let outcome = runner
        .activate(host, idx)
        .target_object(victim)
        .pay_with(&[hammer])
        .resolve();
    assert_eq!(
        outcome.zone_of(hammer),
        Zone::Graveyard,
        "CR 701.21a: the Hammer (granting object) is sacrificed to its owner's graveyard"
    );
    assert_eq!(
        outcome.zone_of(host),
        Zone::Battlefield,
        "the equipped creature survives — it is NOT the object named in the cost"
    );
    assert_eq!(
        outcome.zone_of(victim),
        Zone::Graveyard,
        "the targeted artifact is destroyed by the resolved effect"
    );
}

/// A2 + B1: The Dominion Bracelet. The `{15}, Exile <self>` cost exiles THE
/// BRACELET (granter-referential → GrantingObject → SpecificObject{bracelet}),
/// while the `{X} less … this creature's power` reduction stays host-referential
/// (`QuantityRef::Power{Source}`, an untouched third channel).
///
/// Parse-shape supplement proves the two `~`-collapsed referents split; the
/// `evaluate_layers` assertion proves the production concretization. Full {15}
/// activation is impractical, but the Exile-cost runtime resolution reuses the
/// exact `SpecificObject` machinery the Hammer test drives end-to-end.
#[test]
fn the_dominion_bracelet_exile_hits_the_bracelet_reduction_reads_the_host() {
    // Parse-shape: cost = Exile{GrantingObject}; reduction = Power{Source}; no
    // residual Unimplemented reduction node.
    let def = granted_activated_def(THE_DOMINION_BRACELET, "The Dominion Bracelet");
    assert_eq!(
        def.cost.as_ref().and_then(exile_filter),
        Some(&TargetFilter::GrantingObject),
        "the Exile cost names the Bracelet (granter) → GrantingObject, not SelfRef"
    );
    let reduction = def
        .cost_reduction
        .as_ref()
        .expect("the {X}-less reduction must fold into cost_reduction, not stay Unimplemented");
    assert_eq!(
        reduction.count,
        QuantityExpr::Ref {
            qty: QuantityRef::Power {
                scope: ObjectScope::Source
            }
        },
        "the reduction reads the equipped creature's power (host) — untouched third channel"
    );
    assert!(
        find_effect(&def, |e| matches!(e, Effect::Unimplemented { .. })).is_none(),
        "no residual Unimplemented cost-reduction node should remain"
    );

    // Production concretization: after grant-clone the host's Exile cost is
    // SpecificObject{bracelet}.
    let mut scenario = GameScenario::new();
    scenario.at_phase(Phase::PreCombatMain);
    let host = scenario.add_creature(P0, "Bearer", 3, 3).id();
    let bracelet = scenario
        .add_creature(P0, "The Dominion Bracelet", 0, 0)
        .id();
    let (types, subtypes) = equipment_types();
    let grant_static = grant_ability_static(
        &parse_oracle_text(
            THE_DOMINION_BRACELET,
            "The Dominion Bracelet",
            &[],
            &types,
            &subtypes,
        )
        .statics,
    );
    let runner = equip_and_layer(scenario, bracelet, host, grant_static);
    let idx = granted_ability_index(&runner, host, |a| {
        a.cost.as_ref().and_then(exile_filter).is_some()
    });
    assert_eq!(
        runner.state().objects[&host].abilities[idx]
            .cost
            .as_ref()
            .and_then(exile_filter),
        Some(&TargetFilter::SpecificObject { id: bracelet }),
        "CR 201.5a: the concretized Exile cost targets the Bracelet, not the host"
    );
    // Host power read is unchanged by concretization.
    assert_eq!(
        runner.state().objects[&host].abilities[idx]
            .cost_reduction
            .as_ref()
            .map(|r| &r.count),
        Some(&QuantityExpr::Ref {
            qty: QuantityRef::Power {
                scope: ObjectScope::Source
            }
        }),
        "the power reduction remains host-referential after grant-clone"
    );
}

/// A3 (effect-target channel): Trusty Boomerang's "Return <self> to its owner's
/// hand" bounces THE EQUIPMENT. After grant-clone the Bounce effect target is
/// `SpecificObject{boomerang}`, proving the effect channel (parse_self_reference)
/// concretizes just like the cost channel.
///
/// Revert-to-red: without the layers.rs rewrite the Bounce target stays
/// `GrantingObject` (≠ SpecificObject{boomerang}) → assertion fails.
#[test]
fn trusty_boomerang_return_bounces_the_equipment_not_the_host() {
    let mut scenario = GameScenario::new();
    scenario.at_phase(Phase::PreCombatMain);
    let host = scenario.add_creature(P0, "Bearer", 2, 2).id();
    let boomerang = scenario.add_creature(P0, "Trusty Boomerang", 0, 0).id();
    let (types, subtypes) = equipment_types();
    let grant_static = grant_ability_static(
        &parse_oracle_text(TRUSTY_BOOMERANG, "Trusty Boomerang", &[], &types, &subtypes).statics,
    );
    let runner = equip_and_layer(scenario, boomerang, host, grant_static);

    let idx = granted_ability_index(&runner, host, |a| {
        find_effect(a, |e| matches!(e, Effect::Bounce { .. })).is_some()
    });
    let bounce_target = find_effect(&runner.state().objects[&host].abilities[idx], |e| {
        matches!(e, Effect::Bounce { .. })
    })
    .and_then(|e| match e {
        Effect::Bounce { target, .. } => Some(target.clone()),
        _ => None,
    })
    .expect("granted ability must carry a Bounce effect");
    assert_eq!(
        bounce_target,
        TargetFilter::SpecificObject { id: boomerang },
        "CR 201.5a: the granted Return bounces the Boomerang (granter), not the host"
    );
}

// ---------------------------------------------------------------------------
// Direction B — host-referential "this permanent" stays on the HOST.
// ---------------------------------------------------------------------------

const ACIDIC_SLIVER: &str =
    "All Slivers have \"{2}, Sacrifice this permanent: This permanent deals 2 damage to any target.\"";

/// B2: An Acidic-Sliver-style grant to a SECOND Sliver keeps its "Sacrifice this
/// permanent" cost bound to the HOST (`SelfRef`), never rebound to the granting
/// Sliver. This is the discriminating proof that "this permanent" (a
/// `SELF_REF_TYPE_PHRASES` self-ref, never the card name) is NOT masked to a
/// granter reference — a blanket "SelfRef-in-granted → granter" rewrite would
/// make this `SpecificObject{granter}` and fail.
#[test]
fn sliver_host_ref_sacrifice_stays_on_the_host_not_the_granter() {
    let (types, subtypes) = (vec!["Creature".to_string()], vec!["Sliver".to_string()]);
    let parsed = parse_oracle_text(ACIDIC_SLIVER, "Acidic Sliver", &[], &types, &subtypes);
    let granted = grant_ability_static(&parsed.statics)
        .modifications
        .iter()
        .find_map(|m| match m {
            ContinuousModification::GrantAbility { definition } => Some((**definition).clone()),
            _ => None,
        })
        .expect("Slivers grant an activated ability");
    assert_eq!(
        granted.cost.as_ref().and_then(sacrifice_target),
        Some(&TargetFilter::SelfRef),
        "\"Sacrifice this permanent\" is host-referential (SelfRef), never GrantingObject"
    );
    assert!(
        !contains_granting_object(&granted),
        "a host-ref Sliver ability must contain no GrantingObject reference"
    );

    // DISPLAY half. CR 201.5b: a host reference stays the host token `~` and must
    // NOT gain the granting card's name — the render is sentinel-driven, not a
    // blanket name substitution. Reach-guard: the `SelfRef` assertion above proves
    // this body really is the host-referential shape.
    let desc = granted
        .description
        .as_deref()
        .expect("the granted Sliver ability carries a display description");
    assert!(
        desc.contains('~'),
        "CR 201.5b: the host reference must stay `~`; got {desc}"
    );
    assert!(
        !desc.contains("Acidic Sliver"),
        "CR 201.5b: a host reference must never render as the GRANTER's name; got {desc}"
    );
}

// ---------------------------------------------------------------------------
// Direction C — R1 regression guard: `named <self>` name-FILTERS are preserved.
// ---------------------------------------------------------------------------

const FOOD_FIGHT: &str = "Artifacts you control have \"{2}, Sacrifice this artifact: \
It deals damage to any target equal to 1 plus the number of permanents named Food Fight you control.\"";

/// C (R1 negative): Food Fight's "permanents named Food Fight" is a name-FILTER,
/// not a self-reference. The quote masker must SKIP the `named <self>` position,
/// so the name survives to the count filter (and never becomes GrantingObject or
/// the raw placeholder char).
///
/// Revert-to-red: remove the `named`-position skip in
/// `mask_granting_self_reference_in_quotes` → "Food Fight" after `named` is
/// masked to the placeholder, the `named ~`→`named Food Fight` restoration never
/// fires, and the structural AST loses "Food Fight" (gains the placeholder char)
/// → this assertion fails.
#[test]
fn food_fight_named_self_filter_is_not_masked() {
    let (types, subtypes) = (vec!["Artifact".to_string()], Vec::<String>::new());
    let parsed = parse_oracle_text(FOOD_FIGHT, "Food Fight", &[], &types, &subtypes);
    let mut granted = grant_ability_static(&parsed.statics)
        .modifications
        .iter()
        .find_map(|m| match m {
            ContinuousModification::GrantAbility { definition } => Some((**definition).clone()),
            _ => None,
        })
        .expect("Food Fight grants an activated ability");

    // The host self-sacrifice cost is unaffected (positive reach-guard: the body
    // parsed past the cost separator into a real granted ability).
    assert_eq!(
        granted.cost.as_ref().and_then(sacrifice_target),
        Some(&TargetFilter::SelfRef),
        "\"Sacrifice this artifact\" is host-referential (SelfRef)"
    );

    // Structural (description-independent) check: the name survives in the count
    // filter; no GrantingObject and no leaked placeholder char. (The parser
    // lower-cases filter names, so match case-insensitively.)
    granted.description = None;
    let structural = format!("{granted:?}");
    assert!(
        structural.to_lowercase().contains("food fight"),
        "the `named Food Fight` name-filter must preserve the card name; got {structural}"
    );
    let json = serde_json::to_string(&granted).expect("the granted definition serializes");
    assert!(
        !json.contains(PLACEHOLDER),
        "the granting-object placeholder must never leak into the AST"
    );
    assert!(
        !contains_granting_object(&granted),
        "a name-FILTER position must not become a GrantingObject self-reference"
    );
}

// ---------------------------------------------------------------------------
// Recursive AST walkers used by the assertions above.
// ---------------------------------------------------------------------------

fn find_effect(def: &AbilityDefinition, pred: impl Fn(&Effect) -> bool + Copy) -> Option<&Effect> {
    if pred(&def.effect) {
        return Some(&def.effect);
    }
    for child in def
        .sub_ability
        .iter()
        .chain(def.else_ability.iter())
        .map(|b| b.as_ref())
        .chain(def.mode_abilities.iter())
    {
        if let Some(found) = find_effect(child, pred) {
            return Some(found);
        }
    }
    None
}

/// The Sacrifice cost's target filter, searching inside `Composite`/`OneOf`
/// (activation costs like `{3},{T},Sacrifice <x>` parse to a Composite).
fn sacrifice_target(cost: &AbilityCost) -> Option<&TargetFilter> {
    match cost {
        AbilityCost::Sacrifice(sac) => Some(&sac.target),
        AbilityCost::Composite { costs } | AbilityCost::OneOf { costs } => {
            costs.iter().find_map(sacrifice_target)
        }
        _ => None,
    }
}

/// The Exile cost's filter, searching inside `Composite`/`OneOf`.
fn exile_filter(cost: &AbilityCost) -> Option<&TargetFilter> {
    match cost {
        AbilityCost::Exile { filter, .. } => filter.as_ref(),
        AbilityCost::Composite { costs } | AbilityCost::OneOf { costs } => {
            costs.iter().find_map(exile_filter)
        }
        _ => None,
    }
}

/// Sound presence test for the fieldless `TargetFilter::GrantingObject` variant:
/// its debug repr is exactly `GrantingObject`, and no other AST node's debug
/// output contains that substring. Used only for the negative assertions here.
fn contains_granting_object(def: &AbilityDefinition) -> bool {
    format!("{def:?}").contains("GrantingObject")
}

/// The target filter of a single target-bearing effect (subset used here).
fn effect_target(effect: &Effect) -> Option<&TargetFilter> {
    match effect {
        Effect::PutCounter { target, .. }
        | Effect::GainControl { target, .. }
        | Effect::Bounce { target, .. }
        | Effect::Destroy { target, .. } => Some(target),
        _ => None,
    }
}

/// The GrantAbility body an equipment/aura grants via its "…has \"…\"" static.
fn granted_def_from(
    oracle: &str,
    name: &str,
    types: &[&str],
    subtypes: &[&str],
) -> AbilityDefinition {
    let types: Vec<String> = types.iter().map(|s| s.to_string()).collect();
    let subtypes: Vec<String> = subtypes.iter().map(|s| s.to_string()).collect();
    let parsed = parse_oracle_text(oracle, name, &[], &types, &subtypes);
    grant_ability_static(&parsed.statics)
        .modifications
        .iter()
        .find_map(|m| match m {
            ContinuousModification::GrantAbility { definition } => Some((**definition).clone()),
            _ => None,
        })
        .expect("card must grant an activated ability")
}

/// The private-use masker placeholder (U+E0002). Must NEVER survive into the AST.
const PLACEHOLDER: char = '\u{E0002}';

// ---------------------------------------------------------------------------
// CR 201.5a — THE MEASURED CLASS CORPUS.
//
// Every card in `client/public/card-data.json` whose Oracle text contains, in a
// `"`-quoted granted body, its own printed name immediately preceded by an
// allowlisted verb-object prefix (`GRANTER_SELF_REF_VERB_PREFIXES`: `sacrifice `
// / `exile ` / `return ` / `counter on `). Reproduced by the corpus script in
// the plan's Pattern Coverage section, cross-checked against the independent
// "the export carries a `GrantingObject`" query — both methods yield the SAME 16
// names.
//
// Every text below is the VERBATIM export Oracle text, reminder text and all.
// Abbreviated fixtures are what let the round-1 leak ship: a paraphrase can take
// a different parser branch and go green while the real card stays broken.
//
// The seventeenth class member is the predefined token Rock, which is absent
// from the export and reaches the parser through
// `game::effects::token::catalog_rules_text_abilities`; its arm of this corpus
// property lives in
// `game::effects::token::tests::catalog_rules_text_abilities_never_leaks_the_placeholder`.
// ---------------------------------------------------------------------------

const BLAZING_TORCH: &str =
    "Equipped creature can't be blocked by Vampires or Zombies.\nEquipped creature has \"{T}, Sacrifice Blazing Torch: Blazing Torch deals 2 damage to any target.\"\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)";
const CITIZENS_CROWBAR: &str =
    "When this Equipment enters, create a 1/1 green and white Citizen creature token, then attach this Equipment to it.\nEquipped creature gets +1/+1 and has \"{W}, {T}, Sacrifice Citizen's Crowbar: Destroy target artifact or enchantment.\"\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)";
const DECONSTRUCTION_HAMMER: &str =
    "Equipped creature gets +1/+1 and has \"{3}, {T}, Sacrifice Deconstruction Hammer: Destroy target artifact or enchantment.\"\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)";
const FISHING_POLE: &str =
    "Equipped creature has \"{1}, {T}, Tap Fishing Pole: Put a bait counter on Fishing Pole.\"\nWhenever equipped creature becomes untapped, remove a bait counter from this Equipment. If you do, create a 1/1 blue Fish creature token.\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)";
const HANKYU: &str =
    "Equipped creature has \"{T}: Put an aim counter on Hankyu\" and \"{T}, Remove all aim counters from Hankyu: This creature deals damage to any target equal to the number of aim counters removed this way.\"\nEquip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)";
const MEANDERED_TOWERSHELL: &str =
    "Enchant creature\nEnchanted creature has islandwalk and \"Whenever this creature attacks, exile it and Meandered Towershell. Return it to the battlefield under your control tapped and attacking at the beginning of the declare attackers step on your next turn, then return Meandered Towershell to the battlefield under its owner's control attached to that creature.\"";
const NINJAS_KUNAI: &str =
    "Equipped creature has \"{1}, {T}, Sacrifice Ninja's Kunai: Ninja's Kunai deals 3 damage to any target.\"\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)";
const RAKDOS_RITEKNIFE: &str =
    "Equipped creature gets +1/+0 for each blood counter on this Equipment and has \"{T}, Sacrifice a creature: Put a blood counter on Rakdos Riteknife.\"\n{B}{R}, Sacrifice this Equipment: Target player sacrifices a permanent of their choice for each blood counter on this Equipment.\nEquip {2}";
const RAZOR_BOOMERANG: &str =
    "Equipped creature has \"{T}, Unattach Razor Boomerang: It deals 1 damage to any target. Return Razor Boomerang to its owner's hand.\"\nEquip {2}";
const SAKASHIMA_THE_IMPOSTOR: &str =
    "You may have Sakashima the Impostor enter as a copy of any creature on the battlefield, except its name is Sakashima the Impostor, it's legendary in addition to its other types, and it has \"{2}{U}{U}: Return Sakashima the Impostor to its owner's hand at the beginning of the next end step.\"";
const SPARE_DAGGER: &str =
    "Equipped creature gets +1/+0 and has \"Whenever this creature attacks, you may sacrifice Spare Dagger. When you do, this creature deals 1 damage to any target.\"\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)";
const SUNFIRE_TORCH: &str =
    "Equipped creature gets +1/+0 and has \"Whenever this creature attacks, you may sacrifice Sunfire Torch. When you do, this creature deals 2 damage to any target.\"\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)";
const THE_DOMINION_BRACELET: &str =
    "Equipped creature gets +1/+1 and has \"{15}, Exile The Dominion Bracelet: You control target opponent during their next turn. This ability costs {X} less to activate, where X is this creature's power. Activate only as a sorcery.\" (You see all cards that player could see and make all decisions for them.)\nEquip {1}";
const TORALFS_HAMMER: &str =
    "Equipped creature has \"{1}{R}, {T}, Unattach Toralf's Hammer: It deals 3 damage to any target. Return Toralf's Hammer to its owner's hand.\"\nEquipped creature gets +3/+0 as long as it's legendary.\nEquip {1}{R}";
const TRICKSTERS_TALISMAN: &str =
    "Invoke Duplicity \u{2014} Equipped creature gets +1/+1 and has \"Whenever this creature deals combat damage to a player, you may sacrifice Trickster's Talisman. If you do, create a token that's a copy of this creature.\"\nEquip {2}";
const TRUSTY_BOOMERANG: &str =
    "Equipped creature has \"{1}, {T}: Tap target creature. Return Trusty Boomerang to its owner's hand.\"\nEquip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)";

/// `(oracle text, printed name, core types, subtypes)` for all 16 exported class
/// members.
const CLASS_CORPUS: &[(&str, &str, &[&str], &[&str])] = &[
    (
        BLAZING_TORCH,
        "Blazing Torch",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        CITIZENS_CROWBAR,
        "Citizen's Crowbar",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        DECONSTRUCTION_HAMMER,
        "Deconstruction Hammer",
        &["Artifact"],
        &["Equipment"],
    ),
    (FISHING_POLE, "Fishing Pole", &["Artifact"], &["Equipment"]),
    (HANKYU, "Hankyu", &["Artifact"], &["Equipment"]),
    (
        MEANDERED_TOWERSHELL,
        "Meandered Towershell",
        &["Enchantment"],
        &["Aura"],
    ),
    (NINJAS_KUNAI, "Ninja's Kunai", &["Artifact"], &["Equipment"]),
    (
        RAKDOS_RITEKNIFE,
        "Rakdos Riteknife",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        RAZOR_BOOMERANG,
        "Razor Boomerang",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        SAKASHIMA_THE_IMPOSTOR,
        "Sakashima the Impostor",
        &["Creature"],
        &["Human", "Rogue"],
    ),
    (SPARE_DAGGER, "Spare Dagger", &["Artifact"], &["Equipment"]),
    (
        SUNFIRE_TORCH,
        "Sunfire Torch",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        THE_DOMINION_BRACELET,
        "The Dominion Bracelet",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        TORALFS_HAMMER,
        "Toralf's Hammer",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        TRICKSTERS_TALISMAN,
        "Trickster's Talisman",
        &["Artifact"],
        &["Equipment"],
    ),
    (
        TRUSTY_BOOMERANG,
        "Trusty Boomerang",
        &["Artifact"],
        &["Equipment"],
    ),
];

/// CR 201.5a: no raw U+E0002 may survive into ANY string reachable from
/// `ParsedAbilities`' four top-level vectors through the render net's descend
/// set — including the outer static/trigger DESCRIPTION strings that embed the
/// raw quoted text (a granted body's "…has \"…Sacrifice <self>…\"" description).
/// `parser::oracle::render_granting_self_descriptions` renders every residual
/// marker to the granting card's printed name.
///
/// TWO REPAIRS to the round-1 form of this guard, both of which were measured
/// vacuous:
///
/// 1. **`serde_json`, not `format!("{:?}")`.** `Debug` ESCAPES the raw
///    private-use char to the literal text `\u{e0002}`, so searching a `Debug`
///    dump for the real character was ALWAYS false — the guard could not fail.
///    `serde_json` emits it raw, at every `String`, at every depth, which is
///    strictly stronger than any hand-written `visit_*` walk.
/// 2. **The whole measured class, not four constants.** Four cards cannot see a
///    copy-family regression; Sakashima is the only shipped card whose granted
///    description lives inside an `Effect::BecomeCopy` payload.
///
/// SCOPE NOTE: the serde ORACLE is WIDER than the net's REPAIR. It serializes
/// `def.cost` too, so a cost-borne marker would red here even though the net
/// deliberately does not walk the `AbilityCost` axis (the named excluded axis —
/// see `parser::oracle::tests::granted_cost_axis_is_not_walked_and_no_parse_shape_reaches_it`).
/// That is the correct polarity: this guard should red if a marker ever reaches
/// a cost, because nothing downstream would render it.
///
/// Non-vacuity is proved by `placeholder_leak_guard_reports_a_planted_marker`.
///
/// Revert-to-red: remove the render net from `parse_oracle_text` → every card's
/// outer static description carries the raw U+E0002 char.
#[test]
fn placeholder_never_leaks_into_any_description() {
    for &(oracle, name, types, subtypes) in CLASS_CORPUS {
        let types: Vec<String> = types.iter().map(|s| s.to_string()).collect();
        let subtypes: Vec<String> = subtypes.iter().map(|s| s.to_string()).collect();
        let p = parse_oracle_text(oracle, name, &[], &types, &subtypes);
        let json = serde_json::to_string(&p).expect("ParsedAbilities serializes");
        // PER-CARD POSITIVE REACH-GUARD: this card must actually be a class
        // member in the parsed tree — the masker fired and the typed channel
        // consumed the marker as `TargetFilter::GrantingObject`. Without it, a
        // card that silently stopped parsing its granted body would pass the
        // negative below on an empty tree.
        assert!(
            json.contains("GrantingObject"),
            "reach-guard: {name} must carry a granter self-reference in the typed \
             channel, or its leak assertion below is vacuous"
        );
        assert!(
            !json.contains(PLACEHOLDER),
            "{name}: the masker placeholder must render to the granting card's \
             printed name in every description; a raw U+E0002 leaked"
        );
    }
}

/// CR 201.5a — NON-VACUITY PROOF for `placeholder_never_leaks_into_any_description`.
///
/// A negative assertion is only worth what its ability to fail is worth. This
/// plants a marker into a real parsed tree AFTER the net has run and asserts the
/// same `serde_json` oracle DOES report it.
///
/// Revert-to-red: delete the injection — the guard passes on a clean tree and
/// this test's own assertion flips, which is the point.
#[test]
fn placeholder_leak_guard_reports_a_planted_marker() {
    let (types, subtypes) = equipment_types();
    let mut p = parse_oracle_text(
        DECONSTRUCTION_HAMMER,
        "Deconstruction Hammer",
        &[],
        &types,
        &subtypes,
    );
    assert!(
        !serde_json::to_string(&p)
            .expect("ParsedAbilities serializes")
            .contains(PLACEHOLDER),
        "reach-guard: the tree must be clean BEFORE the injection, or this test \
         proves nothing about the guard's sensitivity"
    );
    p.statics[0].description = Some(format!("x{PLACEHOLDER}y"));
    assert!(
        serde_json::to_string(&p)
            .expect("ParsedAbilities serializes")
            .contains(PLACEHOLDER),
        "the `serde_json` leak oracle must REPORT a planted marker — if it cannot \
         fail, `placeholder_never_leaks_into_any_description` is vacuous (which is \
         exactly what the round-1 `format!(\"{{:?}}\")` form was)"
    );
}

/// CR 201.5a — HOSTILE FIXTURE: two self-name occurrences in ONE granted body,
/// in DIFFERENT positions, bound independently.
///
/// Meandered Towershell's granted trigger body says, in order:
///   * "Whenever this creature attacks"  → a HOST reference (CR 201.5b) → `~`
///   * "exile it and Meandered Towershell" → a CR 201.5a granter reference whose
///     lookbehind is `and `, NOT an allowlisted verb-object prefix, so it is
///     host-bound today — the deferred gap documented in the
///     `KNOWN CR 201.5a FOLLOW-UP` block in `oracle_util::mask_name_occurrences_in_segment`.
///   * "return Meandered Towershell to the battlefield" → an ALLOWLISTED
///     (`return `) granter reference → masked → rendered as the printed name.
///
/// The assertions are written so the advertised follow-up sweep CANNOT turn them
/// red: (a) is a positive `contains`, and (b) pins only the LEADING host
/// reference, which the sweep does not touch. A bare `contains('~')` would be
/// the wrong assertion for exactly that reason.
///
/// Revert-to-red: replace the sentinel render with a blanket
/// `text.replace('~', card_name)` → (b) fails, which is precisely the failure a
/// naive implementation produces.
#[test]
fn meandered_towershell_binds_each_occurrence_independently() {
    let parsed = parse_oracle_text(
        MEANDERED_TOWERSHELL,
        "Meandered Towershell",
        &[],
        &["Enchantment".to_string()],
        &["Aura".to_string()],
    );
    let json = serde_json::to_string(&parsed).expect("ParsedAbilities serializes");
    // POSITIVE REACH-GUARD: the allowlisted `return <granter>` occurrence really
    // reached the typed channel.
    assert!(
        json.contains("GrantingObject"),
        "reach-guard: the `return <granter>` occurrence must reach the typed channel"
    );

    let trigger = parsed
        .statics
        .iter()
        .flat_map(|s| s.modifications.iter())
        .find_map(|m| match m {
            ContinuousModification::GrantTrigger { trigger } => Some(trigger.as_ref()),
            _ => None,
        })
        .unwrap_or_else(|| panic!("the quoted body must parse to a GrantTrigger: {parsed:#?}"));
    let desc = trigger
        .description
        .as_deref()
        .expect("the granted trigger carries a display description");

    // (a) CR 201.5a: the allowlisted occurrence renders as the GRANTER's printed
    // name. Stays true after the follow-up sweep lands (it can only add more).
    assert!(
        desc.contains("Meandered Towershell"),
        "CR 201.5a: the `return <granter>` occurrence must render the printed \
         name; got {desc}"
    );
    // (b) CR 201.5b: the LEADING occurrence is a host reference and stays `~`.
    // The follow-up sweep targets the middle (`and <granter>`) occurrence, not
    // this one, so it cannot turn this red.
    assert!(
        desc.starts_with("Whenever ~ attacks"),
        "CR 201.5b: the leading host reference must stay `~` — a blanket \
         `~`-replace renders `Whenever Meandered Towershell attacks`; got {desc}"
    );
}

/// CR 201.5a (last sentence: "This is also true if the second ability is copied
/// onto a new object") + CR 707.2 — HOSTILE FIXTURE: granter == host, via
/// copy-except.
///
/// Sakashima the Impostor is the ONLY shipped card whose granted description
/// lives inside an `Effect::BecomeCopy` payload. `types::ability_visit` treats
/// `BecomeCopy`/`CopySpell`/`CopyTokenOf` as LEAVES, so no walker in the tree
/// reaches this description without the render net's copy-family arm — which is
/// why this card is a load-bearing structural fixture, not a footnote. (Its
/// sibling `SetName` modification means the rendered output is the same before
/// and after this change; the STRUCTURAL claim is what this test pins.)
///
/// Revert-to-red: delete the `BecomeCopy | CopySpell | CopyTokenOf` arm from
/// `render_effect_descriptions` — the nested description retains the raw marker.
#[test]
fn sakashima_copy_except_grant_description_renders_the_granter() {
    let parsed = parse_oracle_text(
        SAKASHIMA_THE_IMPOSTOR,
        "Sakashima the Impostor",
        &[],
        &["Creature".to_string()],
        &["Human".to_string(), "Rogue".to_string()],
    );
    let json = serde_json::to_string(&parsed).expect("ParsedAbilities serializes");
    // POSITIVE REACH-GUARD: the self-grant's `Return <self> to its owner's hand`
    // really reached the typed channel as a granter reference.
    assert!(
        json.contains("GrantingObject"),
        "reach-guard: the copy-except self-grant must reach the typed channel"
    );
    assert!(
        !json.contains(PLACEHOLDER),
        "a raw CR 201.5a marker survived inside an `Effect::BecomeCopy` payload — \
         the copy-family descend arm is missing"
    );
    assert!(
        json.contains("Sakashima the Impostor to its owner"),
        "CR 201.5a: the granted body nested in the copy payload must name the \
         granting object: {json}"
    );
}

/// R4 (counter channel): the `put a … counter on <self>` (PutCounter target)
/// verb-object position emits `GrantingObject`, exactly like the
/// sacrifice/exile/return channels — Fishing Pole (multi-word) and Hankyu
/// (single-word, case-sensitive masking). Proves the position-aware masker's
/// allowlist still covers the counter target after the HIGH narrowing.
///
/// Revert-to-red: drop `counter on ` from `GRANTER_SELF_REF_VERB_PREFIXES` →
/// these bodies host-bind (`~`/SelfRef) → the `GrantingObject` assertion flips.
#[test]
fn r4_counter_channel_targets_the_granter() {
    for (oracle, name) in [(FISHING_POLE, "Fishing Pole"), (HANKYU, "Hankyu")] {
        let def = granted_def_from(oracle, name, &["Artifact"], &["Equipment"]);
        let target = find_effect(&def, |e| effect_target(e).is_some())
            .and_then(effect_target)
            .unwrap_or_else(|| {
                panic!("{name}: expected a target-bearing effect in the granted body")
            });
        assert_eq!(
            target,
            &TargetFilter::GrantingObject,
            "{name}: the PutCounter target names the granting equipment → GrantingObject"
        );
        // `serde_json`, not `format!("{:?}")`: `Debug` ESCAPES the raw private-use
        // char to the literal text `\u{e0002}`, so a Debug search for the real
        // character is always false and this negative would be vacuous.
        assert!(
            !serde_json::to_string(&def)
                .expect("the granted definition serializes")
                .contains(PLACEHOLDER),
            "{name}: no raw placeholder may survive into the AST"
        );
    }
}

// ---------------------------------------------------------------------------
// Round-1 regression guards (R1/HIGH): non-verb-object in-quote self-name refs
// must NOT be masked — they stay `~` (host), BYTE-IDENTICAL to pre-fix. Asserted
// at the masker's direct output (`normalize_card_name_refs`): its ONLY effect is
// inserting the placeholder, so "no placeholder in the normalized string" ⟺ the
// normalized/parsed output is byte-identical to the pre-fix (name→`~`) baseline.
// Re-widening the masker inserts the placeholder into these positions → red.
// ---------------------------------------------------------------------------

/// Assert the masker is a NO-OP for a card's non-verb-object self-name refs: the
/// normalized string carries no placeholder (byte-identical to pre-fix) yet still
/// normalized the self-name/self-ref to `~` (non-vacuous reach-guard).
fn assert_masker_noop(oracle: &str, name: &str) {
    let normalized = normalize_card_name_refs(oracle, name);
    assert!(
        !normalized.contains(PLACEHOLDER),
        "{name}: a non-verb-object self-name position must NOT be masked (byte-identical to pre-fix)"
    );
    assert!(
        normalized.contains('~'),
        "{name}: the self-name/self-ref must still normalize to ~ (reach-guard); got {normalized}"
    );
}

const ARCHERY_TRAINING: &str = "Enchant creature\nAt the beginning of your upkeep, you may put an \
arrow counter on this Aura.\nEnchanted creature has \"{T}: This creature deals X damage to target \
attacking or blocking creature, where X is the number of arrow counters on Archery Training.\"";

/// Archery Training — QuantityRef channel ("number of arrow counters on <self>").
/// Revert-to-red: re-widen the masker → `counters on <placeholder>` appears in the
/// normalized string AND the end-to-end `CountersOn` node is lost → assertions flip.
#[test]
fn archery_training_quantity_ref_channel_not_masked() {
    assert_masker_noop(ARCHERY_TRAINING, "Archery Training");
    // End-to-end: the arrow-counter count still parses to a CountersOn QuantityRef.
    let def = granted_def_from(
        ARCHERY_TRAINING,
        "Archery Training",
        &["Enchantment"],
        &["Aura"],
    );
    assert!(
        format!("{def:?}").contains("CountersOn"),
        "the arrow-counter count must parse to a CountersOn QuantityRef (not dropped)"
    );
    assert!(
        !contains_granting_object(&def),
        "a QuantityRef `counters on <self>` position must never become GrantingObject"
    );

    // DISPLAY half. The class must not silently widen: a non-allowlisted position
    // stays host-bound in the description exactly as it does in the AST. Widening
    // the DISPLAY channel alone would make the UI name the Aura while the engine
    // counted the host's counters — the two channels would diverge.
    // Reach-guards: `assert_masker_noop` above (the masker did not fire here) and
    // the `CountersOn` assertion (the body really parsed).
    let desc = def
        .description
        .as_deref()
        .expect("the granted Archery Training ability carries a display description");
    assert!(
        desc.contains('~'),
        "a non-allowlisted self-name position must stay the host token `~`; got {desc}"
    );
    assert!(
        !desc.contains("Archery Training"),
        "the display channel must not widen ahead of the typed channel; got {desc}"
    );
}

const ANIMAL_FRIEND: &str = "Enchant creature\nEnchanted creature has \"Whenever this creature \
attacks, create a 1/1 green Squirrel creature token. Put a +1/+1 counter on that token for each \
Aura and Equipment attached to this creature other than Animal Friend.\"";

/// Animal Friend — exclusion channel ("other than <self>"). Revert-to-red:
/// re-widen the masker → `other than <placeholder>` in the normalized string → red.
#[test]
fn animal_friend_exclusion_channel_not_masked() {
    assert_masker_noop(ANIMAL_FRIEND, "Animal Friend");
}

const TORRENT_OF_LAVA: &str = "Torrent of Lava deals X damage to each creature without flying.\n\
As long as Torrent of Lava is on the stack, each creature has \"{T}: Prevent the next 1 damage \
that would be dealt to this creature by Torrent of Lava this turn.\"";

/// Torrent of Lava — damage-source channel ("dealt … by <self>"). Revert-to-red:
/// re-widen the masker → `by <placeholder>` in the normalized string → red.
#[test]
fn torrent_of_lava_damage_source_channel_not_masked() {
    assert_masker_noop(TORRENT_OF_LAVA, "Torrent of Lava");
}
