package net.minecraft.core.registries;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.advancements.criterion.EntitySubPredicates;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.numbers.NumberFormatType;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogTypes;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.ActionTypes;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.DialogBodyTypes;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.InputControlTypes;
import net.minecraft.server.jsonrpc.IncomingRpcMethod;
import net.minecraft.server.jsonrpc.IncomingRpcMethods;
import net.minecraft.server.jsonrpc.OutgoingRpcMethod;
import net.minecraft.server.jsonrpc.OutgoingRpcMethods;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionCheckTypes;
import net.minecraft.server.permissions.PermissionTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.variant.SpawnCondition;
import net.minecraft.world.entity.variant.SpawnConditions;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplays;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplays;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.enchantment.providers.EnchantmentProviderTypes;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.slot.SlotSources;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeSources;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGenerators;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBindings;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.score.LootScoreProviderType;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import org.slf4j.Logger;

public class BuiltInRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<Identifier, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
   private static final WritableRegistry<WritableRegistry<?>> WRITABLE_REGISTRY = new MappedRegistry<>(
      ResourceKey.createRegistryKey(Registries.ROOT_REGISTRY_NAME), Lifecycle.stable()
   );
   public static final DefaultedRegistry<GameEvent> GAME_EVENT = registerDefaulted(Registries.GAME_EVENT, "step", GameEvent::bootstrap);
   public static final Registry<SoundEvent> SOUND_EVENT = registerSimple(Registries.SOUND_EVENT, p_260167_ -> SoundEvents.ITEM_PICKUP);
   public static final DefaultedRegistry<Fluid> FLUID = registerDefaultedWithIntrusiveHolders(Registries.FLUID, "empty", p_259453_ -> Fluids.EMPTY);
   public static final Registry<MobEffect> MOB_EFFECT = registerSimple(Registries.MOB_EFFECT, MobEffects::bootstrap);
   public static final DefaultedRegistry<Block> BLOCK = registerDefaultedWithIntrusiveHolders(Registries.BLOCK, "air", p_259909_ -> Blocks.AIR);
   public static final Registry<DebugSubscription<?>> DEBUG_SUBSCRIPTION = registerSimple(Registries.DEBUG_SUBSCRIPTION, DebugSubscriptions::bootstrap);
   public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = registerDefaultedWithIntrusiveHolders(
      Registries.ENTITY_TYPE, "pig", p_259175_ -> EntityType.PIG
   );
   public static final DefaultedRegistry<Item> ITEM = registerDefaultedWithIntrusiveHolders(Registries.ITEM, "air", p_260227_ -> Items.AIR);
   public static final Registry<Potion> POTION = registerSimple(Registries.POTION, Potions::bootstrap);
   public static final Registry<ParticleType<?>> PARTICLE_TYPE = registerSimple(Registries.PARTICLE_TYPE, p_260266_ -> ParticleTypes.BLOCK);
   public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = registerSimpleWithIntrusiveHolders(
      Registries.BLOCK_ENTITY_TYPE, p_259434_ -> BlockEntityType.FURNACE
   );
   public static final Registry<Identifier> CUSTOM_STAT = registerSimple(Registries.CUSTOM_STAT, p_448621_ -> Stats.JUMP);
   public static final DefaultedRegistry<ChunkStatus> CHUNK_STATUS = registerDefaulted(Registries.CHUNK_STATUS, "empty", p_325818_ -> ChunkStatus.EMPTY);
   public static final Registry<RuleTestType<?>> RULE_TEST = registerSimple(Registries.RULE_TEST, p_259641_ -> RuleTestType.ALWAYS_TRUE_TEST);
   public static final Registry<RuleBlockEntityModifierType<?>> RULE_BLOCK_ENTITY_MODIFIER = registerSimple(
      Registries.RULE_BLOCK_ENTITY_MODIFIER, p_277237_ -> RuleBlockEntityModifierType.PASSTHROUGH
   );
   public static final Registry<PosRuleTestType<?>> POS_RULE_TEST = registerSimple(Registries.POS_RULE_TEST, p_259262_ -> PosRuleTestType.ALWAYS_TRUE_TEST);
   public static final Registry<MenuType<?>> MENU = registerSimple(Registries.MENU, p_259341_ -> MenuType.ANVIL);
   public static final Registry<RecipeType<?>> RECIPE_TYPE = registerSimple(Registries.RECIPE_TYPE, p_259086_ -> RecipeType.CRAFTING);
   public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZER = registerSimple(
      Registries.RECIPE_SERIALIZER, p_260230_ -> RecipeSerializer.SHAPELESS_RECIPE
   );
   public static final Registry<Attribute> ATTRIBUTE = registerSimple(Registries.ATTRIBUTE, Attributes::bootstrap);
   public static final Registry<PositionSourceType<?>> POSITION_SOURCE_TYPE = registerSimple(
      Registries.POSITION_SOURCE_TYPE, p_259113_ -> PositionSourceType.BLOCK
   );
   public static final Registry<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPE = registerSimple(Registries.COMMAND_ARGUMENT_TYPE, ArgumentTypeInfos::bootstrap);
   public static final Registry<StatType<?>> STAT_TYPE = registerSimple(Registries.STAT_TYPE, p_259967_ -> Stats.ITEM_USED);
   public static final DefaultedRegistry<VillagerType> VILLAGER_TYPE = registerDefaulted(Registries.VILLAGER_TYPE, "plains", VillagerType::bootstrap);
   public static final DefaultedRegistry<VillagerProfession> VILLAGER_PROFESSION = registerDefaulted(
      Registries.VILLAGER_PROFESSION, "none", VillagerProfession::bootstrap
   );
   public static final Registry<PoiType> POINT_OF_INTEREST_TYPE = registerSimple(Registries.POINT_OF_INTEREST_TYPE, PoiTypes::bootstrap);
   public static final DefaultedRegistry<MemoryModuleType<?>> MEMORY_MODULE_TYPE = registerDefaulted(
      Registries.MEMORY_MODULE_TYPE, "dummy", p_259248_ -> MemoryModuleType.DUMMY
   );
   public static final DefaultedRegistry<SensorType<?>> SENSOR_TYPE = registerDefaulted(Registries.SENSOR_TYPE, "dummy", p_259757_ -> SensorType.DUMMY);
   public static final Registry<Activity> ACTIVITY = registerSimple(Registries.ACTIVITY, p_260197_ -> Activity.IDLE);
   public static final Registry<LootPoolEntryType> LOOT_POOL_ENTRY_TYPE = registerSimple(Registries.LOOT_POOL_ENTRY_TYPE, p_260042_ -> LootPoolEntries.EMPTY);
   public static final Registry<LootItemFunctionType<?>> LOOT_FUNCTION_TYPE = registerSimple(
      Registries.LOOT_FUNCTION_TYPE, p_259836_ -> LootItemFunctions.SET_COUNT
   );
   public static final Registry<LootItemConditionType> LOOT_CONDITION_TYPE = registerSimple(
      Registries.LOOT_CONDITION_TYPE, p_259742_ -> LootItemConditions.INVERTED
   );
   public static final Registry<LootNumberProviderType> LOOT_NUMBER_PROVIDER_TYPE = registerSimple(
      Registries.LOOT_NUMBER_PROVIDER_TYPE, p_259329_ -> NumberProviders.CONSTANT
   );
   public static final Registry<LootNbtProviderType> LOOT_NBT_PROVIDER_TYPE = registerSimple(
      Registries.LOOT_NBT_PROVIDER_TYPE, p_259862_ -> NbtProviders.CONTEXT
   );
   public static final Registry<LootScoreProviderType> LOOT_SCORE_PROVIDER_TYPE = registerSimple(
      Registries.LOOT_SCORE_PROVIDER_TYPE, p_259313_ -> ScoreboardNameProviders.CONTEXT
   );
   public static final Registry<FloatProviderType<?>> FLOAT_PROVIDER_TYPE = registerSimple(
      Registries.FLOAT_PROVIDER_TYPE, p_260093_ -> FloatProviderType.CONSTANT
   );
   public static final Registry<IntProviderType<?>> INT_PROVIDER_TYPE = registerSimple(Registries.INT_PROVIDER_TYPE, p_259607_ -> IntProviderType.CONSTANT);
   public static final Registry<HeightProviderType<?>> HEIGHT_PROVIDER_TYPE = registerSimple(
      Registries.HEIGHT_PROVIDER_TYPE, p_259663_ -> HeightProviderType.CONSTANT
   );
   public static final Registry<BlockPredicateType<?>> BLOCK_PREDICATE_TYPE = registerSimple(
      Registries.BLOCK_PREDICATE_TYPE, p_260006_ -> BlockPredicateType.NOT
   );
   public static final Registry<WorldCarver<?>> CARVER = registerSimple(Registries.CARVER, p_260200_ -> WorldCarver.CAVE);
   public static final Registry<Feature<?>> FEATURE = registerSimple(Registries.FEATURE, p_259143_ -> Feature.ORE);
   public static final Registry<StructurePlacementType<?>> STRUCTURE_PLACEMENT = registerSimple(
      Registries.STRUCTURE_PLACEMENT, p_259179_ -> StructurePlacementType.RANDOM_SPREAD
   );
   public static final Registry<StructurePieceType> STRUCTURE_PIECE = registerSimple(
      Registries.STRUCTURE_PIECE, p_259722_ -> StructurePieceType.MINE_SHAFT_ROOM
   );
   public static final Registry<StructureType<?>> STRUCTURE_TYPE = registerSimple(Registries.STRUCTURE_TYPE, p_259466_ -> StructureType.JIGSAW);
   public static final Registry<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPE = registerSimple(
      Registries.PLACEMENT_MODIFIER_TYPE, p_260335_ -> PlacementModifierType.COUNT
   );
   public static final Registry<BlockStateProviderType<?>> BLOCKSTATE_PROVIDER_TYPE = registerSimple(
      Registries.BLOCK_STATE_PROVIDER_TYPE, p_259345_ -> BlockStateProviderType.SIMPLE_STATE_PROVIDER
   );
   public static final Registry<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPE = registerSimple(
      Registries.FOLIAGE_PLACER_TYPE, p_260329_ -> FoliagePlacerType.BLOB_FOLIAGE_PLACER
   );
   public static final Registry<TrunkPlacerType<?>> TRUNK_PLACER_TYPE = registerSimple(
      Registries.TRUNK_PLACER_TYPE, p_259690_ -> TrunkPlacerType.STRAIGHT_TRUNK_PLACER
   );
   public static final Registry<RootPlacerType<?>> ROOT_PLACER_TYPE = registerSimple(
      Registries.ROOT_PLACER_TYPE, p_259493_ -> RootPlacerType.MANGROVE_ROOT_PLACER
   );
   public static final Registry<TreeDecoratorType<?>> TREE_DECORATOR_TYPE = registerSimple(
      Registries.TREE_DECORATOR_TYPE, p_259122_ -> TreeDecoratorType.LEAVE_VINE
   );
   public static final Registry<FeatureSizeType<?>> FEATURE_SIZE_TYPE = registerSimple(
      Registries.FEATURE_SIZE_TYPE, p_259370_ -> FeatureSizeType.TWO_LAYERS_FEATURE_SIZE
   );
   public static final Registry<MapCodec<? extends BiomeSource>> BIOME_SOURCE = registerSimple(Registries.BIOME_SOURCE, BiomeSources::bootstrap);
   public static final Registry<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATOR = registerSimple(Registries.CHUNK_GENERATOR, ChunkGenerators::bootstrap);
   public static final Registry<MapCodec<? extends SurfaceRules.ConditionSource>> MATERIAL_CONDITION = registerSimple(
      Registries.MATERIAL_CONDITION, SurfaceRules.ConditionSource::bootstrap
   );
   public static final Registry<MapCodec<? extends SurfaceRules.RuleSource>> MATERIAL_RULE = registerSimple(
      Registries.MATERIAL_RULE, SurfaceRules.RuleSource::bootstrap
   );
   public static final Registry<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPE = registerSimple(
      Registries.DENSITY_FUNCTION_TYPE, DensityFunctions::bootstrap
   );
   public static final Registry<MapCodec<? extends Block>> BLOCK_TYPE = registerSimple(Registries.BLOCK_TYPE, BlockTypes::bootstrap);
   public static final Registry<StructureProcessorType<?>> STRUCTURE_PROCESSOR = registerSimple(
      Registries.STRUCTURE_PROCESSOR, p_259305_ -> StructureProcessorType.BLOCK_IGNORE
   );
   public static final Registry<StructurePoolElementType<?>> STRUCTURE_POOL_ELEMENT = registerSimple(
      Registries.STRUCTURE_POOL_ELEMENT, p_259361_ -> StructurePoolElementType.EMPTY
   );
   public static final Registry<MapCodec<? extends PoolAliasBinding>> POOL_ALIAS_BINDING_TYPE = registerSimple(
      Registries.POOL_ALIAS_BINDING, PoolAliasBindings::bootstrap
   );
   public static final Registry<DecoratedPotPattern> DECORATED_POT_PATTERN = registerSimple(Registries.DECORATED_POT_PATTERN, DecoratedPotPatterns::bootstrap);
   public static final Registry<CreativeModeTab> CREATIVE_MODE_TAB = registerSimple(Registries.CREATIVE_MODE_TAB, CreativeModeTabs::bootstrap);
   public static final Registry<CriterionTrigger<?>> TRIGGER_TYPES = registerSimple(Registries.TRIGGER_TYPE, CriteriaTriggers::bootstrap);
   public static final Registry<NumberFormatType<?>> NUMBER_FORMAT_TYPE = registerSimple(Registries.NUMBER_FORMAT_TYPE, NumberFormatTypes::bootstrap);
   public static final Registry<DataComponentType<?>> DATA_COMPONENT_TYPE = registerSimple(Registries.DATA_COMPONENT_TYPE, DataComponents::bootstrap);
   public static final Registry<GameRule<?>> GAME_RULE = registerSimple(Registries.GAME_RULE, GameRules::bootstrap);
   public static final Registry<MapCodec<? extends EntitySubPredicate>> ENTITY_SUB_PREDICATE_TYPE = registerSimple(
      Registries.ENTITY_SUB_PREDICATE_TYPE, EntitySubPredicates::bootstrap
   );
   public static final Registry<DataComponentPredicate.Type<?>> DATA_COMPONENT_PREDICATE_TYPE = registerSimple(
      Registries.DATA_COMPONENT_PREDICATE_TYPE, DataComponentPredicates::bootstrap
   );
   public static final Registry<MapDecorationType> MAP_DECORATION_TYPE = registerSimple(Registries.MAP_DECORATION_TYPE, MapDecorationTypes::bootstrap);
   public static final Registry<DataComponentType<?>> ENCHANTMENT_EFFECT_COMPONENT_TYPE = registerSimple(
      Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, EnchantmentEffectComponents::bootstrap
   );
   public static final Registry<MapCodec<? extends LevelBasedValue>> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE = registerSimple(
      Registries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE, LevelBasedValue::bootstrap
   );
   public static final Registry<MapCodec<? extends EnchantmentEntityEffect>> ENCHANTMENT_ENTITY_EFFECT_TYPE = registerSimple(
      Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, EnchantmentEntityEffect::bootstrap
   );
   public static final Registry<MapCodec<? extends EnchantmentLocationBasedEffect>> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE = registerSimple(
      Registries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE, EnchantmentLocationBasedEffect::bootstrap
   );
   public static final Registry<MapCodec<? extends EnchantmentValueEffect>> ENCHANTMENT_VALUE_EFFECT_TYPE = registerSimple(
      Registries.ENCHANTMENT_VALUE_EFFECT_TYPE, EnchantmentValueEffect::bootstrap
   );
   public static final Registry<MapCodec<? extends EnchantmentProvider>> ENCHANTMENT_PROVIDER_TYPE = registerSimple(
      Registries.ENCHANTMENT_PROVIDER_TYPE, EnchantmentProviderTypes::bootstrap
   );
   public static final Registry<ConsumeEffect.Type<?>> CONSUME_EFFECT_TYPE = registerSimple(
      Registries.CONSUME_EFFECT_TYPE, p_358156_ -> ConsumeEffect.Type.APPLY_EFFECTS
   );
   public static final Registry<RecipeDisplay.Type<?>> RECIPE_DISPLAY = registerSimple(Registries.RECIPE_DISPLAY, RecipeDisplays::bootstrap);
   public static final Registry<SlotDisplay.Type<?>> SLOT_DISPLAY = registerSimple(Registries.SLOT_DISPLAY, SlotDisplays::bootstrap);
   public static final Registry<RecipeBookCategory> RECIPE_BOOK_CATEGORY = registerSimple(Registries.RECIPE_BOOK_CATEGORY, RecipeBookCategories::bootstrap);
   public static final Registry<TicketType> TICKET_TYPE = registerSimple(Registries.TICKET_TYPE, p_389701_ -> TicketType.UNKNOWN);
   public static final Registry<IncomingRpcMethod<?, ?>> INCOMING_RPC_METHOD = registerSimple(Registries.INCOMING_RPC_METHOD, IncomingRpcMethods::bootstrap);
   public static final Registry<OutgoingRpcMethod<?, ?>> OUTGOING_RPC_METHOD = registerSimple(
      Registries.OUTGOING_RPC_METHOD, p_421119_ -> OutgoingRpcMethods.SERVER_STARTED
   );
   public static final Registry<MapCodec<? extends TestEnvironmentDefinition>> TEST_ENVIRONMENT_DEFINITION_TYPE = registerSimple(
      Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, TestEnvironmentDefinition::bootstrap
   );
   public static final Registry<MapCodec<? extends GameTestInstance>> TEST_INSTANCE_TYPE = registerSimple(
      Registries.TEST_INSTANCE_TYPE, GameTestInstance::bootstrap
   );
   public static final Registry<MapCodec<? extends SpawnCondition>> SPAWN_CONDITION_TYPE = registerSimple(
      Registries.SPAWN_CONDITION_TYPE, SpawnConditions::bootstrap
   );
   public static final Registry<MapCodec<? extends Dialog>> DIALOG_TYPE = registerSimple(Registries.DIALOG_TYPE, DialogTypes::bootstrap);
   public static final Registry<MapCodec<? extends Action>> DIALOG_ACTION_TYPE = registerSimple(Registries.DIALOG_ACTION_TYPE, ActionTypes::bootstrap);
   public static final Registry<MapCodec<? extends InputControl>> INPUT_CONTROL_TYPE = registerSimple(
      Registries.INPUT_CONTROL_TYPE, InputControlTypes::bootstrap
   );
   public static final Registry<MapCodec<? extends DialogBody>> DIALOG_BODY_TYPE = registerSimple(Registries.DIALOG_BODY_TYPE, DialogBodyTypes::bootstrap);
   public static final Registry<MapCodec<? extends Permission>> PERMISSION_TYPE = registerSimple(Registries.PERMISSION_TYPE, PermissionTypes::bootstrap);
   public static final Registry<MapCodec<? extends PermissionCheck>> PERMISSION_CHECK_TYPE = registerSimple(
      Registries.PERMISSION_CHECK_TYPE, PermissionCheckTypes::bootstrap
   );
   public static final Registry<EnvironmentAttribute<?>> ENVIRONMENT_ATTRIBUTE = registerSimple(
      Registries.ENVIRONMENT_ATTRIBUTE, EnvironmentAttributes::bootstrap
   );
   public static final Registry<AttributeType<?>> ATTRIBUTE_TYPE = registerSimple(Registries.ATTRIBUTE_TYPE, AttributeTypes::bootstrap);
   public static final Registry<MapCodec<? extends SlotSource>> SLOT_SOURCE_TYPE = registerSimple(Registries.SLOT_SOURCE_TYPE, SlotSources::bootstrap);
   public static final Registry<Consumer<GameTestHelper>> TEST_FUNCTION = registerSimple(Registries.TEST_FUNCTION, BuiltinTestFunctions::bootstrap);
   public static final Registry<? extends Registry<?>> REGISTRY = WRITABLE_REGISTRY;

   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> p_260095_, BuiltInRegistries.RegistryBootstrap<T> p_259057_) {
      return internalRegister(p_260095_, new MappedRegistry<>(p_260095_, Lifecycle.stable(), false), p_259057_);
   }

   private static <T> Registry<T> registerSimpleWithIntrusiveHolders(
      ResourceKey<? extends Registry<T>> p_297531_, BuiltInRegistries.RegistryBootstrap<T> p_298446_
   ) {
      return internalRegister(p_297531_, new MappedRegistry<>(p_297531_, Lifecycle.stable(), true), p_298446_);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(
      ResourceKey<? extends Registry<T>> p_259887_, String p_259325_, BuiltInRegistries.RegistryBootstrap<T> p_259759_
   ) {
      return internalRegister(p_259887_, new DefaultedMappedRegistry<>(p_259325_, p_259887_, Lifecycle.stable(), false), p_259759_);
   }

   private static <T> DefaultedRegistry<T> registerDefaultedWithIntrusiveHolders(
      ResourceKey<? extends Registry<T>> p_259296_, String p_259101_, BuiltInRegistries.RegistryBootstrap<T> p_259485_
   ) {
      return internalRegister(p_259296_, new DefaultedMappedRegistry<>(p_259101_, p_259296_, Lifecycle.stable(), true), p_259485_);
   }

   private static <T, R extends WritableRegistry<T>> R internalRegister(
      ResourceKey<? extends Registry<T>> p_259230_, R p_260327_, BuiltInRegistries.RegistryBootstrap<T> p_259210_
   ) {
      Bootstrap.checkBootstrapCalled(() -> "registry " + p_259230_.identifier());
      Identifier identifier = p_259230_.identifier();
      LOADERS.put(identifier, () -> p_259210_.run(p_260327_));
      WRITABLE_REGISTRY.register((ResourceKey<WritableRegistry<?>>)p_259230_, p_260327_, RegistrationInfo.BUILT_IN);
      return p_260327_;
   }

   public static void bootStrap() {
      createContents();
      freeze();
      validate(REGISTRY);
   }

   private static void createContents() {
      LOADERS.forEach((p_451243_, p_259387_) -> {
         if (p_259387_.get() == null) {
            LOGGER.error("Unable to bootstrap registry '{}'", p_451243_);
         }
      });
   }

   private static void freeze() {
      REGISTRY.freeze();

      for (Registry<?> registry : REGISTRY) {
         bindBootstrappedTagsToEmpty(registry);
         registry.freeze();
      }
   }

   private static <T extends Registry<?>> void validate(Registry<T> p_260209_) {
      p_260209_.forEach(p_448623_ -> {
         if (p_448623_.keySet().isEmpty()) {
            Util.logAndPauseIfInIde("Registry '" + p_260209_.getKey((T)p_448623_) + "' was empty after loading");
         }

         if (p_448623_ instanceof DefaultedRegistry) {
            Identifier identifier = ((DefaultedRegistry)p_448623_).getDefaultKey();
            Objects.requireNonNull(p_448623_.getValue(identifier), "Missing default of DefaultedMappedRegistry: " + identifier);
         }
      });
   }

   public static <T> HolderGetter<T> acquireBootstrapRegistrationLookup(Registry<T> p_363847_) {
      return ((WritableRegistry)p_363847_).createRegistrationLookup();
   }

   private static void bindBootstrappedTagsToEmpty(Registry<?> p_365803_) {
      ((MappedRegistry)p_365803_).bindAllTagsToEmpty();
   }

   @FunctionalInterface
   interface RegistryBootstrap<T> {
      Object run(Registry<T> var1);
   }
}
