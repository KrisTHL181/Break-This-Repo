package net.minecraft.core.registries;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicates;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.component.DataComponentInitializers;
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
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.item.crafting.RecipeSerializers;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
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
import net.minecraft.world.level.levelgen.carver.WorldCarverTypes;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeatureTypes;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderTypes;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierTypes;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacements;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBindings;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorTypes;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionTypes;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviderTypes;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import org.slf4j.Logger;

public class BuiltInRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<Identifier, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
   private static final WritableRegistry<WritableRegistry<?>> WRITABLE_REGISTRY = new MappedRegistry<>(
      ResourceKey.createRegistryKey(Registries.ROOT_REGISTRY_NAME), Lifecycle.stable()
   );
   public static final DataComponentInitializers DATA_COMPONENT_INITIALIZERS = new DataComponentInitializers();
   public static final DefaultedRegistry<GameEvent> GAME_EVENT = registerDefaulted(Registries.GAME_EVENT, "step", GameEvent::bootstrap);
   public static final Registry<SoundEvent> SOUND_EVENT = registerSimple(Registries.SOUND_EVENT, registry -> SoundEvents.ITEM_PICKUP);
   public static final DefaultedRegistry<Fluid> FLUID = registerDefaultedWithIntrusiveHolders(Registries.FLUID, "empty", registry -> Fluids.EMPTY);
   public static final Registry<MobEffect> MOB_EFFECT = registerSimple(Registries.MOB_EFFECT, MobEffects::bootstrap);
   public static final DefaultedRegistry<Block> BLOCK = registerDefaultedWithIntrusiveHolders(Registries.BLOCK, "air", registry -> Blocks.AIR);
   public static final Registry<DebugSubscription<?>> DEBUG_SUBSCRIPTION = registerSimple(Registries.DEBUG_SUBSCRIPTION, DebugSubscriptions::bootstrap);
   public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = registerDefaultedWithIntrusiveHolders(
      Registries.ENTITY_TYPE, "pig", registry -> EntityTypes.PIG
   );
   public static final DefaultedRegistry<Item> ITEM = registerDefaultedWithIntrusiveHolders(Registries.ITEM, "air", registry -> Items.AIR);
   public static final Registry<Potion> POTION = registerSimple(Registries.POTION, Potions::bootstrap);
   public static final Registry<ParticleType<?>> PARTICLE_TYPE = registerSimple(Registries.PARTICLE_TYPE, registry -> ParticleTypes.BLOCK);
   public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = registerSimpleWithIntrusiveHolders(
      Registries.BLOCK_ENTITY_TYPE, registry -> BlockEntityTypes.FURNACE
   );
   public static final Registry<Identifier> CUSTOM_STAT = registerSimple(Registries.CUSTOM_STAT, registry -> Stats.JUMP);
   public static final DefaultedRegistry<ChunkStatus> CHUNK_STATUS = registerDefaulted(Registries.CHUNK_STATUS, "empty", registry -> ChunkStatus.EMPTY);
   public static final Registry<RuleTestType<?>> RULE_TEST = registerSimple(Registries.RULE_TEST, registry -> RuleTestType.ALWAYS_TRUE_TEST);
   public static final Registry<RuleBlockEntityModifierType<?>> RULE_BLOCK_ENTITY_MODIFIER = registerSimple(
      Registries.RULE_BLOCK_ENTITY_MODIFIER, registry -> RuleBlockEntityModifierType.PASSTHROUGH
   );
   public static final Registry<PosRuleTestType<?>> POS_RULE_TEST = registerSimple(Registries.POS_RULE_TEST, registry -> PosRuleTestType.ALWAYS_TRUE_TEST);
   public static final Registry<MenuType<?>> MENU = registerSimple(Registries.MENU, registry -> MenuType.ANVIL);
   public static final Registry<RecipeType<?>> RECIPE_TYPE = registerSimple(Registries.RECIPE_TYPE, registry -> RecipeType.CRAFTING);
   public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZER = registerSimple(Registries.RECIPE_SERIALIZER, RecipeSerializers::bootstrap);
   public static final Registry<Attribute> ATTRIBUTE = registerSimple(Registries.ATTRIBUTE, Attributes::bootstrap);
   public static final Registry<PositionSourceType<?>> POSITION_SOURCE_TYPE = registerSimple(
      Registries.POSITION_SOURCE_TYPE, registry -> PositionSourceType.BLOCK
   );
   public static final Registry<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPE = registerSimple(Registries.COMMAND_ARGUMENT_TYPE, ArgumentTypeInfos::bootstrap);
   public static final Registry<StatType<?>> STAT_TYPE = registerSimple(Registries.STAT_TYPE, registry -> Stats.ITEM_USED);
   public static final DefaultedRegistry<VillagerType> VILLAGER_TYPE = registerDefaulted(Registries.VILLAGER_TYPE, "plains", VillagerType::bootstrap);
   public static final DefaultedRegistry<VillagerProfession> VILLAGER_PROFESSION = registerDefaulted(
      Registries.VILLAGER_PROFESSION, "none", VillagerProfession::bootstrap
   );
   public static final Registry<PoiType> POINT_OF_INTEREST_TYPE = registerSimple(Registries.POINT_OF_INTEREST_TYPE, PoiTypes::bootstrap);
   public static final DefaultedRegistry<MemoryModuleType<?>> MEMORY_MODULE_TYPE = registerDefaulted(
      Registries.MEMORY_MODULE_TYPE, "dummy", registry -> MemoryModuleType.DUMMY
   );
   public static final DefaultedRegistry<SensorType<?>> SENSOR_TYPE = registerDefaulted(Registries.SENSOR_TYPE, "dummy", registry -> SensorType.DUMMY);
   public static final Registry<Activity> ACTIVITY = registerSimple(Registries.ACTIVITY, registry -> Activity.IDLE);
   public static final Registry<MapCodec<? extends LootPoolEntryContainer>> LOOT_POOL_ENTRY_TYPE = registerSimple(
      Registries.LOOT_POOL_ENTRY_TYPE, LootPoolEntries::bootstrap
   );
   public static final Registry<MapCodec<? extends LootItemFunction>> LOOT_FUNCTION_TYPE = registerSimple(
      Registries.LOOT_FUNCTION_TYPE, LootItemFunctions::bootstrap
   );
   public static final Registry<MapCodec<? extends LootItemCondition>> LOOT_CONDITION_TYPE = registerSimple(
      Registries.LOOT_CONDITION_TYPE, LootItemConditionTypes::bootstrap
   );
   public static final Registry<MapCodec<? extends NumberProvider>> LOOT_NUMBER_PROVIDER_TYPE = registerSimple(
      Registries.LOOT_NUMBER_PROVIDER_TYPE, NumberProviderTypes::bootstrap
   );
   public static final Registry<MapCodec<? extends NbtProvider>> LOOT_NBT_PROVIDER_TYPE = registerSimple(
      Registries.LOOT_NBT_PROVIDER_TYPE, NbtProviders::bootstrap
   );
   public static final Registry<MapCodec<? extends ScoreboardNameProvider>> LOOT_SCORE_PROVIDER_TYPE = registerSimple(
      Registries.LOOT_SCORE_PROVIDER_TYPE, ScoreboardNameProviders::bootstrap
   );
   public static final Registry<MapCodec<? extends FloatProvider>> FLOAT_PROVIDER_TYPE = registerSimple(
      Registries.FLOAT_PROVIDER_TYPE, FloatProviders::bootstrap
   );
   public static final Registry<MapCodec<? extends IntProvider>> INT_PROVIDER_TYPE = registerSimple(Registries.INT_PROVIDER_TYPE, IntProviders::bootstrap);
   public static final Registry<HeightProviderType<?>> HEIGHT_PROVIDER_TYPE = registerSimple(
      Registries.HEIGHT_PROVIDER_TYPE, registry -> HeightProviderType.CONSTANT
   );
   public static final Registry<BlockPredicateType<?>> BLOCK_PREDICATE_TYPE = registerSimple(
      Registries.BLOCK_PREDICATE_TYPE, registry -> BlockPredicateType.NOT
   );
   public static final Registry<MapCodec<? extends WorldCarver>> CARVER_TYPE = registerSimple(Registries.CARVER_TYPE, WorldCarverTypes::bootstrap);
   public static final Registry<MapCodec<? extends Feature>> FEATURE_TYPE = registerSimple(Registries.FEATURE_TYPE, FeatureTypes::bootstrap);
   public static final Registry<MapCodec<? extends StructurePlacement>> STRUCTURE_PLACEMENT = registerSimple(
      Registries.STRUCTURE_PLACEMENT, StructurePlacements::bootstrap
   );
   public static final Registry<StructurePieceType> STRUCTURE_PIECE = registerSimple(Registries.STRUCTURE_PIECE, registry -> StructurePieceType.MINE_SHAFT_ROOM);
   public static final Registry<StructureType<?>> STRUCTURE_TYPE = registerSimple(Registries.STRUCTURE_TYPE, registry -> StructureType.JIGSAW);
   public static final Registry<MapCodec<? extends PlacementModifier>> PLACEMENT_MODIFIER_TYPE = registerSimple(
      Registries.PLACEMENT_MODIFIER_TYPE, PlacementModifierTypes::bootstrap
   );
   public static final Registry<MapCodec<? extends BlockStateProvider>> BLOCK_STATE_PROVIDER_TYPE = registerSimple(
      Registries.BLOCK_STATE_PROVIDER_TYPE, BlockStateProviderTypes::bootstrap
   );
   public static final Registry<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPE = registerSimple(
      Registries.FOLIAGE_PLACER_TYPE, registry -> FoliagePlacerType.BLOB_FOLIAGE_PLACER
   );
   public static final Registry<TrunkPlacerType<?>> TRUNK_PLACER_TYPE = registerSimple(
      Registries.TRUNK_PLACER_TYPE, registry -> TrunkPlacerType.STRAIGHT_TRUNK_PLACER
   );
   public static final Registry<RootPlacerType<?>> ROOT_PLACER_TYPE = registerSimple(
      Registries.ROOT_PLACER_TYPE, registry -> RootPlacerType.MANGROVE_ROOT_PLACER
   );
   public static final Registry<TreeDecoratorType<?>> TREE_DECORATOR_TYPE = registerSimple(
      Registries.TREE_DECORATOR_TYPE, registry -> TreeDecoratorType.LEAVE_VINE
   );
   public static final Registry<FeatureSizeType<?>> FEATURE_SIZE_TYPE = registerSimple(
      Registries.FEATURE_SIZE_TYPE, registry -> FeatureSizeType.TWO_LAYERS_FEATURE_SIZE
   );
   public static final Registry<MapCodec<? extends BiomeSource>> BIOME_SOURCE = registerSimple(Registries.BIOME_SOURCE, BiomeSources::bootstrap);
   public static final Registry<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATOR = registerSimple(Registries.CHUNK_GENERATOR, ChunkGenerators::bootstrap);
   public static final Registry<MapCodec<? extends SurfaceRules.ConditionSource>> MATERIAL_CONDITION_TYPE = registerSimple(
      Registries.MATERIAL_CONDITION_TYPE, SurfaceRules.ConditionSource::bootstrap
   );
   public static final Registry<MapCodec<? extends SurfaceRules.RuleSource>> MATERIAL_RULE_TYPE = registerSimple(
      Registries.MATERIAL_RULE_TYPE, SurfaceRules.RuleSource::bootstrap
   );
   public static final Registry<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPE = registerSimple(
      Registries.DENSITY_FUNCTION_TYPE, DensityFunctions::bootstrap
   );
   public static final Registry<MapCodec<? extends StructureProcessor>> STRUCTURE_PROCESSOR = registerSimple(
      Registries.STRUCTURE_PROCESSOR, StructureProcessorTypes::bootstrap
   );
   public static final Registry<StructurePoolElementType<?>> STRUCTURE_POOL_ELEMENT = registerSimple(
      Registries.STRUCTURE_POOL_ELEMENT, registry -> StructurePoolElementType.EMPTY
   );
   public static final Registry<MapCodec<? extends PoolAliasBinding>> POOL_ALIAS_BINDING_TYPE = registerSimple(
      Registries.POOL_ALIAS_BINDING, PoolAliasBindings::bootstrap
   );
   public static final Registry<CreativeModeTab> CREATIVE_MODE_TAB = registerSimple(Registries.CREATIVE_MODE_TAB, CreativeModeTabs::bootstrap);
   public static final Registry<CriterionTrigger<?>> TRIGGER_TYPES = registerSimple(Registries.TRIGGER_TYPE, CriteriaTriggers::bootstrap);
   public static final Registry<NumberFormatType<?>> NUMBER_FORMAT_TYPE = registerSimple(Registries.NUMBER_FORMAT_TYPE, NumberFormatTypes::bootstrap);
   public static final Registry<DataComponentType<?>> DATA_COMPONENT_TYPE = registerSimple(Registries.DATA_COMPONENT_TYPE, DataComponents::bootstrap);
   public static final Registry<GameRule<?>> GAME_RULE = registerSimple(Registries.GAME_RULE, GameRules::bootstrap);
   public static final Registry<Codec<? extends EntitySubPredicate>> ENTITY_SUB_PREDICATE_TYPE = registerSimple(
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
      Registries.CONSUME_EFFECT_TYPE, registry -> ConsumeEffect.Type.APPLY_EFFECTS
   );
   public static final Registry<RecipeDisplay.Type<?>> RECIPE_DISPLAY = registerSimple(Registries.RECIPE_DISPLAY, RecipeDisplays::bootstrap);
   public static final Registry<SlotDisplay.Type<?>> SLOT_DISPLAY = registerSimple(Registries.SLOT_DISPLAY, SlotDisplays::bootstrap);
   public static final Registry<RecipeBookCategory> RECIPE_BOOK_CATEGORY = registerSimple(Registries.RECIPE_BOOK_CATEGORY, RecipeBookCategories::bootstrap);
   public static final Registry<TicketType> TICKET_TYPE = registerSimple(Registries.TICKET_TYPE, registry -> TicketType.UNKNOWN);
   public static final Registry<IncomingRpcMethod<?, ?>> INCOMING_RPC_METHOD = registerSimple(Registries.INCOMING_RPC_METHOD, IncomingRpcMethods::bootstrap);
   public static final Registry<OutgoingRpcMethod<?, ?>> OUTGOING_RPC_METHOD = registerSimple(
      Registries.OUTGOING_RPC_METHOD, registry -> OutgoingRpcMethods.SERVER_STARTED
   );
   public static final Registry<MapCodec<? extends TestEnvironmentDefinition<?>>> TEST_ENVIRONMENT_DEFINITION_TYPE = registerSimple(
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

   private static <T> Registry<T> registerSimple(final ResourceKey<? extends Registry<T>> name, final BuiltInRegistries.RegistryBootstrap<T> loader) {
      return internalRegister(name, new MappedRegistry<>(name, Lifecycle.stable(), false), loader);
   }

   private static <T> Registry<T> registerSimpleWithIntrusiveHolders(
      final ResourceKey<? extends Registry<T>> name, final BuiltInRegistries.RegistryBootstrap<T> loader
   ) {
      return internalRegister(name, new MappedRegistry<>(name, Lifecycle.stable(), true), loader);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(
      final ResourceKey<? extends Registry<T>> name, final String defaultKey, final BuiltInRegistries.RegistryBootstrap<T> loader
   ) {
      return internalRegister(name, new DefaultedMappedRegistry<>(defaultKey, name, Lifecycle.stable(), false), loader);
   }

   private static <T> DefaultedRegistry<T> registerDefaultedWithIntrusiveHolders(
      final ResourceKey<? extends Registry<T>> name, final String defaultKey, final BuiltInRegistries.RegistryBootstrap<T> loader
   ) {
      return internalRegister(name, new DefaultedMappedRegistry<>(defaultKey, name, Lifecycle.stable(), true), loader);
   }

   private static <T, R extends WritableRegistry<T>> R internalRegister(
      final ResourceKey<? extends Registry<T>> name, final R registry, final BuiltInRegistries.RegistryBootstrap<T> loader
   ) {
      Bootstrap.checkBootstrapCalled(() -> "registry " + name.identifier());
      Identifier key = name.identifier();
      LOADERS.put(key, () -> loader.run(registry));
      WRITABLE_REGISTRY.register((ResourceKey<WritableRegistry<?>>)name, registry, RegistrationInfo.BUILT_IN);
      return registry;
   }

   public static void bootStrap() {
      createContents();
      freeze();
      validate(REGISTRY);
   }

   private static void createContents() {
      LOADERS.forEach((key, value) -> {
         if (value.get() == null) {
            LOGGER.error("Unable to bootstrap registry '{}'", key);
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

   private static <T extends Registry<?>> void validate(final Registry<T> registry) {
      registry.forEach(r -> {
         if (r.keySet().isEmpty()) {
            Util.logAndPauseIfInIde("Registry '" + registry.getKey((T)r) + "' was empty after loading");
         }

         if (r instanceof DefaultedRegistry) {
            Identifier key = ((DefaultedRegistry)r).getDefaultKey();
            Objects.requireNonNull(r.getValue(key), "Missing default of DefaultedMappedRegistry: " + key);
         }
      });
   }

   public static <T> HolderGetter<T> acquireBootstrapRegistrationLookup(final Registry<T> registry) {
      return ((WritableRegistry)registry).createRegistrationLookup();
   }

   private static void bindBootstrappedTagsToEmpty(final Registry<?> registry) {
      ((MappedRegistry)registry).bindAllTagsToEmpty();
   }

   @FunctionalInterface
   private interface RegistryBootstrap<T> {
      Object run(Registry<T> registry);
   }
}
