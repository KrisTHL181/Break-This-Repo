package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.EncoderCache;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.world.LockCode;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.feline.CatSoundVariant;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.PigSoundVariant;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.SulfurCubeContent;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.saveddata.maps.MapId;

public class DataComponents {
    static final EncoderCache ENCODER_CACHE = new EncoderCache(512);
    public static final DataComponentType<CustomData> CUSTOM_DATA = register("custom_data", b -> b.persistent(CustomData.CODEC));
    public static final DataComponentType<Integer> MAX_STACK_SIZE = register(
        "max_stack_size", b -> b.persistent(ExtraCodecs.intRange(1, 99)).networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Integer> MAX_DAMAGE = register(
        "max_damage", b -> b.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Integer> DAMAGE = register(
        "damage", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).ignoreSwapAnimation().networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Unit> UNBREAKABLE = register("unbreakable", b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));
    public static final DataComponentType<UseEffects> USE_EFFECTS = register(
        "use_effects", b -> b.persistent(UseEffects.CODEC).networkSynchronized(UseEffects.STREAM_CODEC)
    );
    public static final DataComponentType<Component> CUSTOM_NAME = register(
        "custom_name", b -> b.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Float> MINIMUM_ATTACK_CHARGE = register(
        "minimum_attack_charge", b -> b.persistent(ExtraCodecs.floatRange(0.0F, 1.0F)).networkSynchronized(ByteBufCodecs.FLOAT)
    );
    public static final DataComponentType<Holder<DamageType>> DAMAGE_TYPE = register(
        "damage_type", b -> b.persistent(DamageType.CODEC).networkSynchronized(DamageType.STREAM_CODEC)
    );
    public static final DataComponentType<Component> ITEM_NAME = register(
        "item_name", b -> b.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Identifier> ITEM_MODEL = register(
        "item_model", b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ItemLore> LORE = register(
        "lore", b -> b.persistent(ItemLore.CODEC).networkSynchronized(ItemLore.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Rarity> RARITY = register("rarity", b -> b.persistent(Rarity.CODEC).networkSynchronized(Rarity.STREAM_CODEC));
    public static final DataComponentType<ItemEnchantments> ENCHANTMENTS = register(
        "enchantments", b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<AdventureModePredicate> CAN_PLACE_ON = register(
        "can_place_on", b -> b.persistent(AdventureModePredicate.CODEC).networkSynchronized(AdventureModePredicate.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<AdventureModePredicate> CAN_BREAK = register(
        "can_break", b -> b.persistent(AdventureModePredicate.CODEC).networkSynchronized(AdventureModePredicate.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ItemAttributeModifiers> ATTRIBUTE_MODIFIERS = register(
        "attribute_modifiers", b -> b.persistent(ItemAttributeModifiers.CODEC).networkSynchronized(ItemAttributeModifiers.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<CustomModelData> CUSTOM_MODEL_DATA = register(
        "custom_model_data", b -> b.persistent(CustomModelData.CODEC).networkSynchronized(CustomModelData.STREAM_CODEC)
    );
    public static final DataComponentType<TooltipDisplay> TOOLTIP_DISPLAY = register(
        "tooltip_display", b -> b.persistent(TooltipDisplay.CODEC).networkSynchronized(TooltipDisplay.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Integer> REPAIR_COST = register(
        "repair_cost", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Unit> CREATIVE_SLOT_LOCK = register("creative_slot_lock", b -> b.networkSynchronized(Unit.STREAM_CODEC));
    public static final DataComponentType<Boolean> ENCHANTMENT_GLINT_OVERRIDE = register(
        "enchantment_glint_override", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );
    public static final DataComponentType<Unit> INTANGIBLE_PROJECTILE = register("intangible_projectile", b -> b.persistent(Unit.CODEC));
    public static final DataComponentType<FoodProperties> FOOD = register(
        "food", b -> b.persistent(FoodProperties.DIRECT_CODEC).networkSynchronized(FoodProperties.DIRECT_STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Consumable> CONSUMABLE = register(
        "consumable", b -> b.persistent(Consumable.CODEC).networkSynchronized(Consumable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<UseRemainder> USE_REMAINDER = register(
        "use_remainder", b -> b.persistent(UseRemainder.CODEC).networkSynchronized(UseRemainder.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<UseCooldown> USE_COOLDOWN = register(
        "use_cooldown", b -> b.persistent(UseCooldown.CODEC).networkSynchronized(UseCooldown.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DamageResistant> DAMAGE_RESISTANT = register(
        "damage_resistant", b -> b.persistent(DamageResistant.CODEC).networkSynchronized(DamageResistant.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Tool> TOOL = register("tool", b -> b.persistent(Tool.CODEC).networkSynchronized(Tool.STREAM_CODEC).cacheEncoding());
    public static final DataComponentType<Weapon> WEAPON = register(
        "weapon", b -> b.persistent(Weapon.CODEC).networkSynchronized(Weapon.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<AttackRange> ATTACK_RANGE = register(
        "attack_range", b -> b.persistent(AttackRange.CODEC).networkSynchronized(AttackRange.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Enchantable> ENCHANTABLE = register(
        "enchantable", b -> b.persistent(Enchantable.CODEC).networkSynchronized(Enchantable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Equippable> EQUIPPABLE = register(
        "equippable", b -> b.persistent(Equippable.CODEC).networkSynchronized(Equippable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Repairable> REPAIRABLE = register(
        "repairable", b -> b.persistent(Repairable.CODEC).networkSynchronized(Repairable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Unit> GLIDER = register("glider", b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));
    public static final DataComponentType<Identifier> TOOLTIP_STYLE = register(
        "tooltip_style", b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DeathProtection> DEATH_PROTECTION = register(
        "death_protection", b -> b.persistent(DeathProtection.CODEC).networkSynchronized(DeathProtection.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<BlocksAttacks> BLOCKS_ATTACKS = register(
        "blocks_attacks", b -> b.persistent(BlocksAttacks.CODEC).networkSynchronized(BlocksAttacks.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<PiercingWeapon> PIERCING_WEAPON = register(
        "piercing_weapon", b -> b.persistent(PiercingWeapon.CODEC).networkSynchronized(PiercingWeapon.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<KineticWeapon> KINETIC_WEAPON = register(
        "kinetic_weapon", b -> b.persistent(KineticWeapon.CODEC).networkSynchronized(KineticWeapon.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<SwingAnimation> SWING_ANIMATION = register(
        "swing_animation", b -> b.persistent(SwingAnimation.CODEC).networkSynchronized(SwingAnimation.STREAM_CODEC)
    );
    public static final DataComponentType<Integer> ADDITIONAL_TRADE_COST = register("additional_trade_cost", b -> b.networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<ItemEnchantments> STORED_ENCHANTMENTS = register(
        "stored_enchantments", b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DyeColor> DYE = register("dye", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC));
    public static final DataComponentType<DyedItemColor> DYED_COLOR = register(
        "dyed_color", b -> b.persistent(DyedItemColor.CODEC).networkSynchronized(DyedItemColor.STREAM_CODEC)
    );
    public static final DataComponentType<MapItemColor> MAP_COLOR = register(
        "map_color", b -> b.persistent(MapItemColor.CODEC).networkSynchronized(MapItemColor.STREAM_CODEC)
    );
    public static final DataComponentType<MapId> MAP_ID = register("map_id", b -> b.persistent(MapId.CODEC).networkSynchronized(MapId.STREAM_CODEC));
    public static final DataComponentType<MapDecorations> MAP_DECORATIONS = register("map_decorations", b -> b.persistent(MapDecorations.CODEC).cacheEncoding());
    public static final DataComponentType<MapPostProcessing> MAP_POST_PROCESSING = register(
        "map_post_processing", b -> b.networkSynchronized(MapPostProcessing.STREAM_CODEC)
    );
    public static final DataComponentType<ChargedProjectiles> CHARGED_PROJECTILES = register(
        "charged_projectiles", b -> b.persistent(ChargedProjectiles.CODEC).networkSynchronized(ChargedProjectiles.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<BundleContents> BUNDLE_CONTENTS = register(
        "bundle_contents", b -> b.persistent(BundleContents.CODEC).networkSynchronized(BundleContents.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<PotionContents> POTION_CONTENTS = register(
        "potion_contents", b -> b.persistent(PotionContents.CODEC).networkSynchronized(PotionContents.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Float> POTION_DURATION_SCALE = register(
        "potion_duration_scale", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).cacheEncoding()
    );
    public static final DataComponentType<SuspiciousStewEffects> SUSPICIOUS_STEW_EFFECTS = register(
        "suspicious_stew_effects", b -> b.persistent(SuspiciousStewEffects.CODEC).networkSynchronized(SuspiciousStewEffects.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<WritableBookContent> WRITABLE_BOOK_CONTENT = register(
        "writable_book_content", b -> b.persistent(WritableBookContent.CODEC).networkSynchronized(WritableBookContent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<WrittenBookContent> WRITTEN_BOOK_CONTENT = register(
        "written_book_content", b -> b.persistent(WrittenBookContent.CODEC).networkSynchronized(WrittenBookContent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ArmorTrim> TRIM = register(
        "trim", b -> b.persistent(ArmorTrim.CODEC).networkSynchronized(ArmorTrim.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DebugStickState> DEBUG_STICK_STATE = register(
        "debug_stick_state", b -> b.persistent(DebugStickState.CODEC).cacheEncoding()
    );
    public static final DataComponentType<TypedEntityData<EntityType<?>>> ENTITY_DATA = register(
        "entity_data", b -> b.persistent(TypedEntityData.codec(EntityType.CODEC)).networkSynchronized(TypedEntityData.streamCodec(EntityType.STREAM_CODEC))
    );
    public static final DataComponentType<CustomData> BUCKET_ENTITY_DATA = register(
        "bucket_entity_data", b -> b.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC)
    );
    public static final DataComponentType<TypedEntityData<BlockEntityType<?>>> BLOCK_ENTITY_DATA = register(
        "block_entity_data",
        b -> b.persistent(TypedEntityData.codec(BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()))
            .networkSynchronized(TypedEntityData.streamCodec(ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE)))
    );
    public static final DataComponentType<InstrumentComponent> INSTRUMENT = register(
        "instrument", b -> b.persistent(InstrumentComponent.CODEC).networkSynchronized(InstrumentComponent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Holder<TrimMaterial>> PROVIDES_TRIM_MATERIAL = register(
        "provides_trim_material", b -> b.persistent(TrimMaterial.CODEC).networkSynchronized(TrimMaterial.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<OminousBottleAmplifier> OMINOUS_BOTTLE_AMPLIFIER = register(
        "ominous_bottle_amplifier", b -> b.persistent(OminousBottleAmplifier.CODEC).networkSynchronized(OminousBottleAmplifier.STREAM_CODEC)
    );
    public static final DataComponentType<JukeboxPlayable> JUKEBOX_PLAYABLE = register(
        "jukebox_playable", b -> b.persistent(JukeboxPlayable.CODEC).networkSynchronized(JukeboxPlayable.STREAM_CODEC)
    );
    public static final DataComponentType<HolderSet<BannerPattern>> PROVIDES_BANNER_PATTERNS = register(
        "provides_banner_patterns",
        b -> b.persistent(RegistryCodecs.homogeneousList(Registries.BANNER_PATTERN))
            .networkSynchronized(ByteBufCodecs.holderSet(Registries.BANNER_PATTERN))
            .cacheEncoding()
    );
    public static final DataComponentType<List<ResourceKey<Recipe<?>>>> RECIPES = register(
        "recipes", b -> b.persistent(Recipe.KEY_CODEC.listOf()).cacheEncoding()
    );
    public static final DataComponentType<LodestoneTracker> LODESTONE_TRACKER = register(
        "lodestone_tracker", b -> b.persistent(LodestoneTracker.CODEC).networkSynchronized(LodestoneTracker.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<FireworkExplosion> FIREWORK_EXPLOSION = register(
        "firework_explosion", b -> b.persistent(FireworkExplosion.CODEC).networkSynchronized(FireworkExplosion.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Fireworks> FIREWORKS = register(
        "fireworks", b -> b.persistent(Fireworks.CODEC).networkSynchronized(Fireworks.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ResolvableProfile> PROFILE = register(
        "profile", b -> b.persistent(ResolvableProfile.CODEC).networkSynchronized(ResolvableProfile.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Identifier> NOTE_BLOCK_SOUND = register(
        "note_block_sound", b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC)
    );
    public static final DataComponentType<BannerPatternLayers> BANNER_PATTERNS = register(
        "banner_patterns", b -> b.persistent(BannerPatternLayers.CODEC).networkSynchronized(BannerPatternLayers.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DyeColor> BASE_COLOR = register(
        "base_color", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<PotDecorations> POT_DECORATIONS = register(
        "pot_decorations", b -> b.persistent(PotDecorations.CODEC).networkSynchronized(PotDecorations.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ItemContainerContents> CONTAINER = register(
        "container", b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<BlockItemStateProperties> BLOCK_STATE = register(
        "block_state", b -> b.persistent(BlockItemStateProperties.CODEC).networkSynchronized(BlockItemStateProperties.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Bees> BEES = register("bees", b -> b.persistent(Bees.CODEC).networkSynchronized(Bees.STREAM_CODEC).cacheEncoding());
    public static final DataComponentType<SulfurCubeContent> SULFUR_CUBE_CONTENT = register(
        "sulfur_cube_content", b -> b.persistent(SulfurCubeContent.CODEC).networkSynchronized(SulfurCubeContent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<LockCode> LOCK = register("lock", b -> b.persistent(LockCode.CODEC));
    public static final DataComponentType<SeededContainerLoot> CONTAINER_LOOT = register("container_loot", b -> b.persistent(SeededContainerLoot.CODEC));
    public static final DataComponentType<Holder<SoundEvent>> BREAK_SOUND = register(
        "break_sound", b -> b.persistent(SoundEvent.CODEC).networkSynchronized(SoundEvent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Holder<VillagerType>> VILLAGER_VARIANT = register(
        "villager/variant", b -> b.persistent(VillagerType.CODEC).networkSynchronized(VillagerType.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<WolfVariant>> WOLF_VARIANT = register(
        "wolf/variant", b -> b.persistent(WolfVariant.CODEC).networkSynchronized(WolfVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<WolfSoundVariant>> WOLF_SOUND_VARIANT = register(
        "wolf/sound_variant", b -> b.persistent(WolfSoundVariant.CODEC).networkSynchronized(WolfSoundVariant.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> WOLF_COLLAR = register(
        "wolf/collar", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<Fox.Variant> FOX_VARIANT = register(
        "fox/variant", b -> b.persistent(Fox.Variant.CODEC).networkSynchronized(Fox.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Salmon.Variant> SALMON_SIZE = register(
        "salmon/size", b -> b.persistent(Salmon.Variant.CODEC).networkSynchronized(Salmon.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Parrot.Variant> PARROT_VARIANT = register(
        "parrot/variant", b -> b.persistent(Parrot.Variant.CODEC).networkSynchronized(Parrot.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<TropicalFish.Pattern> TROPICAL_FISH_PATTERN = register(
        "tropical_fish/pattern", b -> b.persistent(TropicalFish.Pattern.CODEC).networkSynchronized(TropicalFish.Pattern.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> TROPICAL_FISH_BASE_COLOR = register(
        "tropical_fish/base_color", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> TROPICAL_FISH_PATTERN_COLOR = register(
        "tropical_fish/pattern_color", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<MushroomCow.Variant> MOOSHROOM_VARIANT = register(
        "mooshroom/variant", b -> b.persistent(MushroomCow.Variant.CODEC).networkSynchronized(MushroomCow.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Rabbit.Variant> RABBIT_VARIANT = register(
        "rabbit/variant", b -> b.persistent(Rabbit.Variant.CODEC).networkSynchronized(Rabbit.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<PigVariant>> PIG_VARIANT = register(
        "pig/variant", b -> b.persistent(PigVariant.CODEC).networkSynchronized(PigVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<PigSoundVariant>> PIG_SOUND_VARIANT = register(
        "pig/sound_variant", b -> b.persistent(PigSoundVariant.CODEC).networkSynchronized(PigSoundVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<CowVariant>> COW_VARIANT = register(
        "cow/variant", b -> b.persistent(CowVariant.CODEC).networkSynchronized(CowVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<CowSoundVariant>> COW_SOUND_VARIANT = register(
        "cow/sound_variant", b -> b.persistent(CowSoundVariant.CODEC).networkSynchronized(CowSoundVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<ChickenVariant>> CHICKEN_VARIANT = register(
        "chicken/variant", b -> b.persistent(ChickenVariant.CODEC).networkSynchronized(ChickenVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<ChickenSoundVariant>> CHICKEN_SOUND_VARIANT = register(
        "chicken/sound_variant", b -> b.persistent(ChickenSoundVariant.CODEC).networkSynchronized(ChickenSoundVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<ZombieNautilusVariant>> ZOMBIE_NAUTILUS_VARIANT = register(
        "zombie_nautilus/variant", b -> b.persistent(ZombieNautilusVariant.CODEC).networkSynchronized(ZombieNautilusVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<FrogVariant>> FROG_VARIANT = register(
        "frog/variant", b -> b.persistent(FrogVariant.CODEC).networkSynchronized(FrogVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Variant> HORSE_VARIANT = register(
        "horse/variant", b -> b.persistent(Variant.CODEC).networkSynchronized(Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<PaintingVariant>> PAINTING_VARIANT = register(
        "painting/variant", b -> b.persistent(PaintingVariant.CODEC).networkSynchronized(PaintingVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Llama.Variant> LLAMA_VARIANT = register(
        "llama/variant", b -> b.persistent(Llama.Variant.CODEC).networkSynchronized(Llama.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Axolotl.Variant> AXOLOTL_VARIANT = register(
        "axolotl/variant", b -> b.persistent(Axolotl.Variant.CODEC).networkSynchronized(Axolotl.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<CatVariant>> CAT_VARIANT = register(
        "cat/variant", b -> b.persistent(CatVariant.CODEC).networkSynchronized(CatVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<CatSoundVariant>> CAT_SOUND_VARIANT = register(
        "cat/sound_variant", b -> b.persistent(CatSoundVariant.CODEC).networkSynchronized(CatSoundVariant.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> CAT_COLLAR = register(
        "cat/collar", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> SHEEP_COLOR = register(
        "sheep/color", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> SHULKER_COLOR = register(
        "shulker/color", b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentMap COMMON_ITEM_COMPONENTS = DataComponentMap.builder()
        .set(MAX_STACK_SIZE, 64)
        .set(LORE, ItemLore.EMPTY)
        .set(ENCHANTMENTS, ItemEnchantments.EMPTY)
        .set(REPAIR_COST, 0)
        .set(USE_EFFECTS, UseEffects.DEFAULT)
        .set(ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
        .set(RARITY, Rarity.COMMON)
        .set(BREAK_SOUND, SoundEvents.ITEM_BREAK)
        .set(TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
        .set(SWING_ANIMATION, SwingAnimation.DEFAULT)
        .build();

    public static DataComponentType<?> bootstrap(final Registry<DataComponentType<?>> registry) {
        return CUSTOM_DATA;
    }

    private static <T> DataComponentType<T> register(final String id, final UnaryOperator<DataComponentType.Builder<T>> builder) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, builder.apply(DataComponentType.builder()).build());
    }
}