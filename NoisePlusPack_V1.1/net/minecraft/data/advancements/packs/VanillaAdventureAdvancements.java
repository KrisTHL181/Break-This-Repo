package net.minecraft.data.advancements.packs;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.ChanneledLightningTrigger;
import net.minecraft.advancements.criterion.DamagePredicate;
import net.minecraft.advancements.criterion.DamageSourcePredicate;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.DistancePredicate;
import net.minecraft.advancements.criterion.DistanceTrigger;
import net.minecraft.advancements.criterion.EntityEquipmentPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.FallAfterExplosionTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.criterion.KilledByArrowTrigger;
import net.minecraft.advancements.criterion.KilledTrigger;
import net.minecraft.advancements.criterion.LightningBoltPredicate;
import net.minecraft.advancements.criterion.LightningStrikeTrigger;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.LootTableTrigger;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.PlayerHurtEntityTrigger;
import net.minecraft.advancements.criterion.PlayerInteractTrigger;
import net.minecraft.advancements.criterion.PlayerPredicate;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.advancements.criterion.RecipeCraftedTrigger;
import net.minecraft.advancements.criterion.ShotCrossbowTrigger;
import net.minecraft.advancements.criterion.SlideDownBlockTrigger;
import net.minecraft.advancements.criterion.SpearMobsTrigger;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.advancements.criterion.SummonedEntityTrigger;
import net.minecraft.advancements.criterion.TagPredicate;
import net.minecraft.advancements.criterion.TargetBlockTrigger;
import net.minecraft.advancements.criterion.TradeTrigger;
import net.minecraft.advancements.criterion.UsedTotemTrigger;
import net.minecraft.advancements.criterion.UsingItemTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.JukeboxPlayablePredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class VanillaAdventureAdvancements implements AdvancementSubProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DISTANCE_FROM_BOTTOM_TO_TOP = 384;
   private static final int Y_COORDINATE_AT_TOP = 320;
   private static final int Y_COORDINATE_AT_BOTTOM = -64;
   private static final int BEDROCK_THICKNESS = 5;
   private static final Map<MobCategory, Set<EntityType<?>>> EXCEPTIONS_BY_EXPECTED_CATEGORIES = Map.of(
      MobCategory.MONSTER, Set.of(EntityType.GIANT, EntityType.ILLUSIONER, EntityType.WARDEN)
   );
   private static final List<EntityType<?>> MOBS_TO_KILL = Arrays.asList(
      EntityType.BLAZE,
      EntityType.BOGGED,
      EntityType.BREEZE,
      EntityType.CAMEL_HUSK,
      EntityType.CAVE_SPIDER,
      EntityType.CREAKING,
      EntityType.CREEPER,
      EntityType.DROWNED,
      EntityType.ELDER_GUARDIAN,
      EntityType.ENDER_DRAGON,
      EntityType.ENDERMAN,
      EntityType.ENDERMITE,
      EntityType.EVOKER,
      EntityType.GHAST,
      EntityType.GUARDIAN,
      EntityType.HOGLIN,
      EntityType.HUSK,
      EntityType.MAGMA_CUBE,
      EntityType.PARCHED,
      EntityType.PHANTOM,
      EntityType.PIGLIN,
      EntityType.PIGLIN_BRUTE,
      EntityType.PILLAGER,
      EntityType.RAVAGER,
      EntityType.SHULKER,
      EntityType.SILVERFISH,
      EntityType.SKELETON,
      EntityType.SLIME,
      EntityType.SPIDER,
      EntityType.STRAY,
      EntityType.VEX,
      EntityType.VINDICATOR,
      EntityType.WITCH,
      EntityType.WITHER_SKELETON,
      EntityType.WITHER,
      EntityType.ZOGLIN,
      EntityType.ZOMBIE_VILLAGER,
      EntityType.ZOMBIE,
      EntityType.ZOMBIE_HORSE,
      EntityType.ZOMBIFIED_PIGLIN,
      EntityType.ZOMBIE_NAUTILUS
   );

   private static Criterion<LightningStrikeTrigger.TriggerInstance> fireCountAndBystander(MinMaxBounds.Ints p_460792_, Optional<EntityPredicate> p_300450_) {
      return LightningStrikeTrigger.TriggerInstance.lightningStrike(
         Optional.of(
            EntityPredicate.Builder.entity()
               .distance(DistancePredicate.absolute(MinMaxBounds.Doubles.atMost(30.0)))
               .subPredicate(LightningBoltPredicate.blockSetOnFire(p_460792_))
               .build()
         ),
         p_300450_
      );
   }

   private static Criterion<UsingItemTrigger.TriggerInstance> lookAtThroughItem(EntityPredicate.Builder p_454054_, ItemPredicate.Builder p_458092_) {
      return UsingItemTrigger.TriggerInstance.lookingAt(
         EntityPredicate.Builder.entity().subPredicate(PlayerPredicate.Builder.player().setLookingAt(p_454054_).build()), p_458092_
      );
   }

   @Override
   public void generate(HolderLookup.Provider p_255887_, Consumer<AdvancementHolder> p_256428_) {
      HolderLookup<EntityType<?>> holderlookup = p_255887_.lookupOrThrow(Registries.ENTITY_TYPE);
      HolderLookup<Item> holderlookup1 = p_255887_.lookupOrThrow(Registries.ITEM);
      HolderLookup<Block> holderlookup2 = p_255887_.lookupOrThrow(Registries.BLOCK);
      AdvancementHolder advancementholder = Advancement.Builder.advancement()
         .display(
            Items.MAP,
            Component.translatable("advancements.adventure.root.title"),
            Component.translatable("advancements.adventure.root.description"),
            Identifier.withDefaultNamespace("gui/advancements/backgrounds/adventure"),
            AdvancementType.TASK,
            false,
            false,
            false
         )
         .requirements(AdvancementRequirements.Strategy.OR)
         .addCriterion("killed_something", KilledTrigger.TriggerInstance.playerKilledEntity())
         .addCriterion("killed_by_something", KilledTrigger.TriggerInstance.entityKilledPlayer())
         .save(p_256428_, "adventure/root");
      AdvancementHolder advancementholder1 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Blocks.RED_BED,
            Component.translatable("advancements.adventure.sleep_in_bed.title"),
            Component.translatable("advancements.adventure.sleep_in_bed.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("slept_in_bed", PlayerTrigger.TriggerInstance.sleptInBed())
         .save(p_256428_, "adventure/sleep_in_bed");
      createAdventuringTime(p_255887_, p_256428_, advancementholder1, MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);
      AdvancementHolder advancementholder2 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.EMERALD,
            Component.translatable("advancements.adventure.trade.title"),
            Component.translatable("advancements.adventure.trade.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("traded", TradeTrigger.TriggerInstance.tradedWithVillager())
         .save(p_256428_, "adventure/trade");
      Advancement.Builder.advancement()
         .parent(advancementholder2)
         .display(
            Items.EMERALD,
            Component.translatable("advancements.adventure.trade_at_world_height.title"),
            Component.translatable("advancements.adventure.trade_at_world_height.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "trade_at_world_height",
            TradeTrigger.TriggerInstance.tradedWithVillager(
               EntityPredicate.Builder.entity().located(LocationPredicate.Builder.atYLocation(MinMaxBounds.Doubles.atLeast(319.0)))
            )
         )
         .save(p_256428_, "adventure/trade_at_world_height");
      AdvancementHolder advancementholder3 = createMonsterHunterAdvancement(
         advancementholder, p_256428_, holderlookup, validateMobsToKill(MOBS_TO_KILL, holderlookup)
      );
      AdvancementHolder advancementholder4 = Advancement.Builder.advancement()
         .parent(advancementholder3)
         .display(
            Items.BOW,
            Component.translatable("advancements.adventure.shoot_arrow.title"),
            Component.translatable("advancements.adventure.shoot_arrow.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "shot_arrow",
            PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntityWithDamage(
               DamagePredicate.Builder.damageInstance()
                  .type(
                     DamageSourcePredicate.Builder.damageType()
                        .tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE))
                        .direct(EntityPredicate.Builder.entity().of(holderlookup, EntityTypeTags.ARROWS))
                  )
            )
         )
         .save(p_256428_, "adventure/shoot_arrow");
      AdvancementHolder advancementholder5 = Advancement.Builder.advancement()
         .parent(advancementholder3)
         .display(
            Items.TRIDENT,
            Component.translatable("advancements.adventure.throw_trident.title"),
            Component.translatable("advancements.adventure.throw_trident.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "shot_trident",
            PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntityWithDamage(
               DamagePredicate.Builder.damageInstance()
                  .type(
                     DamageSourcePredicate.Builder.damageType()
                        .tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE))
                        .direct(EntityPredicate.Builder.entity().of(holderlookup, EntityType.TRIDENT))
                  )
            )
         )
         .save(p_256428_, "adventure/throw_trident");
      Advancement.Builder.advancement()
         .parent(advancementholder5)
         .display(
            Items.TRIDENT,
            Component.translatable("advancements.adventure.very_very_frightening.title"),
            Component.translatable("advancements.adventure.very_very_frightening.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "struck_villager",
            ChanneledLightningTrigger.TriggerInstance.channeledLightning(EntityPredicate.Builder.entity().of(holderlookup, EntityType.VILLAGER))
         )
         .save(p_256428_, "adventure/very_very_frightening");
      Advancement.Builder.advancement()
         .parent(advancementholder2)
         .display(
            Blocks.CARVED_PUMPKIN,
            Component.translatable("advancements.adventure.summon_iron_golem.title"),
            Component.translatable("advancements.adventure.summon_iron_golem.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion(
            "summoned_golem", SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(holderlookup, EntityType.IRON_GOLEM))
         )
         .save(p_256428_, "adventure/summon_iron_golem");
      Advancement.Builder.advancement()
         .parent(advancementholder4)
         .display(
            Items.ARROW,
            Component.translatable("advancements.adventure.sniper_duel.title"),
            Component.translatable("advancements.adventure.sniper_duel.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(50))
         .addCriterion(
            "killed_skeleton",
            KilledTrigger.TriggerInstance.playerKilledEntity(
               EntityPredicate.Builder.entity()
                  .of(holderlookup, EntityType.SKELETON)
                  .distance(DistancePredicate.horizontal(MinMaxBounds.Doubles.atLeast(50.0))),
               DamageSourcePredicate.Builder.damageType().tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE))
            )
         )
         .save(p_256428_, "adventure/sniper_duel");
      Advancement.Builder.advancement()
         .parent(advancementholder3)
         .display(
            Items.TOTEM_OF_UNDYING,
            Component.translatable("advancements.adventure.totem_of_undying.title"),
            Component.translatable("advancements.adventure.totem_of_undying.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion("used_totem", UsedTotemTrigger.TriggerInstance.usedTotem(holderlookup1, Items.TOTEM_OF_UNDYING))
         .save(p_256428_, "adventure/totem_of_undying");
      Advancement.Builder.advancement()
         .parent(advancementholder3)
         .display(
            Items.IRON_SPEAR,
            Component.translatable("advancements.adventure.spear_many_mobs.title"),
            Component.translatable("advancements.adventure.spear_many_mobs.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion("spear_many_mobs", SpearMobsTrigger.TriggerInstance.spearMobs(5))
         .save(p_256428_, "adventure/spear_many_mobs");
      AdvancementHolder advancementholder6 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.CROSSBOW,
            Component.translatable("advancements.adventure.ol_betsy.title"),
            Component.translatable("advancements.adventure.ol_betsy.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("shot_crossbow", ShotCrossbowTrigger.TriggerInstance.shotCrossbow(holderlookup1, Items.CROSSBOW))
         .save(p_256428_, "adventure/ol_betsy");
      Advancement.Builder.advancement()
         .parent(advancementholder6)
         .display(
            Items.CROSSBOW,
            Component.translatable("advancements.adventure.whos_the_pillager_now.title"),
            Component.translatable("advancements.adventure.whos_the_pillager_now.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "kill_pillager",
            KilledByArrowTrigger.TriggerInstance.crossbowKilled(holderlookup1, EntityPredicate.Builder.entity().of(holderlookup, EntityType.PILLAGER))
         )
         .save(p_256428_, "adventure/whos_the_pillager_now");
      Advancement.Builder.advancement()
         .parent(advancementholder6)
         .display(
            Items.CROSSBOW,
            Component.translatable("advancements.adventure.two_birds_one_arrow.title"),
            Component.translatable("advancements.adventure.two_birds_one_arrow.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(65))
         .addCriterion(
            "two_birds",
            KilledByArrowTrigger.TriggerInstance.crossbowKilled(
               holderlookup1,
               EntityPredicate.Builder.entity().of(holderlookup, EntityType.PHANTOM),
               EntityPredicate.Builder.entity().of(holderlookup, EntityType.PHANTOM)
            )
         )
         .save(p_256428_, "adventure/two_birds_one_arrow");
      Advancement.Builder.advancement()
         .parent(advancementholder6)
         .display(
            Items.CROSSBOW,
            Component.translatable("advancements.adventure.arbalistic.title"),
            Component.translatable("advancements.adventure.arbalistic.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            true
         )
         .rewards(AdvancementRewards.Builder.experience(85))
         .addCriterion("arbalistic", KilledByArrowTrigger.TriggerInstance.crossbowKilled(holderlookup1, MinMaxBounds.Ints.exactly(5)))
         .save(p_256428_, "adventure/arbalistic");
      HolderLookup.RegistryLookup<BannerPattern> registrylookup = p_255887_.lookupOrThrow(Registries.BANNER_PATTERN);
      AdvancementHolder advancementholder7 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Raid.getOminousBannerInstance(registrylookup),
            Component.translatable("advancements.adventure.voluntary_exile.title"),
            Component.translatable("advancements.adventure.voluntary_exile.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            true
         )
         .addCriterion(
            "voluntary_exile",
            KilledTrigger.TriggerInstance.playerKilledEntity(
               EntityPredicate.Builder.entity()
                  .of(holderlookup, EntityTypeTags.RAIDERS)
                  .equipment(EntityEquipmentPredicate.captainPredicate(holderlookup1, registrylookup))
            )
         )
         .save(p_256428_, "adventure/voluntary_exile");
      Advancement.Builder.advancement()
         .parent(advancementholder7)
         .display(
            Raid.getOminousBannerInstance(registrylookup),
            Component.translatable("advancements.adventure.hero_of_the_village.title"),
            Component.translatable("advancements.adventure.hero_of_the_village.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            true
         )
         .rewards(AdvancementRewards.Builder.experience(100))
         .addCriterion("hero_of_the_village", PlayerTrigger.TriggerInstance.raidWon())
         .save(p_256428_, "adventure/hero_of_the_village");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Blocks.HONEY_BLOCK.asItem(),
            Component.translatable("advancements.adventure.honey_block_slide.title"),
            Component.translatable("advancements.adventure.honey_block_slide.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("honey_block_slide", SlideDownBlockTrigger.TriggerInstance.slidesDownBlock(Blocks.HONEY_BLOCK))
         .save(p_256428_, "adventure/honey_block_slide");
      Advancement.Builder.advancement()
         .parent(advancementholder4)
         .display(
            Blocks.TARGET.asItem(),
            Component.translatable("advancements.adventure.bullseye.title"),
            Component.translatable("advancements.adventure.bullseye.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(50))
         .addCriterion(
            "bullseye",
            TargetBlockTrigger.TriggerInstance.targetHit(
               MinMaxBounds.Ints.exactly(15),
               Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().distance(DistancePredicate.horizontal(MinMaxBounds.Doubles.atLeast(30.0)))))
            )
         )
         .save(p_256428_, "adventure/bullseye");
      Advancement.Builder.advancement()
         .parent(advancementholder1)
         .display(
            Items.LEATHER_BOOTS,
            Component.translatable("advancements.adventure.walk_on_powder_snow_with_leather_boots.title"),
            Component.translatable("advancements.adventure.walk_on_powder_snow_with_leather_boots.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "walk_on_powder_snow_with_leather_boots",
            PlayerTrigger.TriggerInstance.walkOnBlockWithEquipment(holderlookup2, holderlookup1, Blocks.POWDER_SNOW, Items.LEATHER_BOOTS)
         )
         .save(p_256428_, "adventure/walk_on_powder_snow_with_leather_boots");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.LIGHTNING_ROD,
            Component.translatable("advancements.adventure.lightning_rod_with_villager_no_fire.title"),
            Component.translatable("advancements.adventure.lightning_rod_with_villager_no_fire.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "lightning_rod_with_villager_no_fire",
            fireCountAndBystander(MinMaxBounds.Ints.exactly(0), Optional.of(EntityPredicate.Builder.entity().of(holderlookup, EntityType.VILLAGER).build()))
         )
         .save(p_256428_, "adventure/lightning_rod_with_villager_no_fire");
      AdvancementHolder advancementholder8 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.SPYGLASS,
            Component.translatable("advancements.adventure.spyglass_at_parrot.title"),
            Component.translatable("advancements.adventure.spyglass_at_parrot.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "spyglass_at_parrot",
            lookAtThroughItem(
               EntityPredicate.Builder.entity().of(holderlookup, EntityType.PARROT), ItemPredicate.Builder.item().of(holderlookup1, Items.SPYGLASS)
            )
         )
         .save(p_256428_, "adventure/spyglass_at_parrot");
      AdvancementHolder advancementholder9 = Advancement.Builder.advancement()
         .parent(advancementholder8)
         .display(
            Items.SPYGLASS,
            Component.translatable("advancements.adventure.spyglass_at_ghast.title"),
            Component.translatable("advancements.adventure.spyglass_at_ghast.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "spyglass_at_ghast",
            lookAtThroughItem(
               EntityPredicate.Builder.entity().of(holderlookup, EntityType.GHAST), ItemPredicate.Builder.item().of(holderlookup1, Items.SPYGLASS)
            )
         )
         .save(p_256428_, "adventure/spyglass_at_ghast");
      Advancement.Builder.advancement()
         .parent(advancementholder1)
         .display(
            Items.JUKEBOX,
            Component.translatable("advancements.adventure.play_jukebox_in_meadows.title"),
            Component.translatable("advancements.adventure.play_jukebox_in_meadows.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "play_jukebox_in_meadows",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location()
                  .setBiomes(HolderSet.direct(p_255887_.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.MEADOW)))
                  .setBlock(BlockPredicate.Builder.block().of(holderlookup2, Blocks.JUKEBOX)),
               ItemPredicate.Builder.item()
                  .withComponents(
                     DataComponentMatchers.Builder.components().partial(DataComponentPredicates.JUKEBOX_PLAYABLE, JukeboxPlayablePredicate.any()).build()
                  )
            )
         )
         .save(p_256428_, "adventure/play_jukebox_in_meadows");
      Advancement.Builder.advancement()
         .parent(advancementholder9)
         .display(
            Items.SPYGLASS,
            Component.translatable("advancements.adventure.spyglass_at_dragon.title"),
            Component.translatable("advancements.adventure.spyglass_at_dragon.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "spyglass_at_dragon",
            lookAtThroughItem(
               EntityPredicate.Builder.entity().of(holderlookup, EntityType.ENDER_DRAGON), ItemPredicate.Builder.item().of(holderlookup1, Items.SPYGLASS)
            )
         )
         .save(p_256428_, "adventure/spyglass_at_dragon");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.WATER_BUCKET,
            Component.translatable("advancements.adventure.fall_from_world_height.title"),
            Component.translatable("advancements.adventure.fall_from_world_height.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "fall_from_world_height",
            DistanceTrigger.TriggerInstance.fallFromHeight(
               EntityPredicate.Builder.entity().located(LocationPredicate.Builder.atYLocation(MinMaxBounds.Doubles.atMost(-59.0))),
               DistancePredicate.vertical(MinMaxBounds.Doubles.atLeast(379.0)),
               LocationPredicate.Builder.atYLocation(MinMaxBounds.Doubles.atLeast(319.0))
            )
         )
         .save(p_256428_, "adventure/fall_from_world_height");
      Advancement.Builder.advancement()
         .parent(advancementholder3)
         .display(
            Blocks.SCULK_CATALYST,
            Component.translatable("advancements.adventure.kill_mob_near_sculk_catalyst.title"),
            Component.translatable("advancements.adventure.kill_mob_near_sculk_catalyst.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .addCriterion("kill_mob_near_sculk_catalyst", KilledTrigger.TriggerInstance.playerKilledEntityNearSculkCatalyst())
         .save(p_256428_, "adventure/kill_mob_near_sculk_catalyst");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Blocks.SCULK_SENSOR,
            Component.translatable("advancements.adventure.avoid_vibration.title"),
            Component.translatable("advancements.adventure.avoid_vibration.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("avoid_vibration", PlayerTrigger.TriggerInstance.avoidVibration())
         .save(p_256428_, "adventure/avoid_vibration");
      AdvancementHolder advancementholder10 = respectingTheRemnantsCriterions(holderlookup1, Advancement.Builder.advancement())
         .parent(advancementholder)
         .display(
            Items.BRUSH,
            Component.translatable("advancements.adventure.salvage_sherd.title"),
            Component.translatable("advancements.adventure.salvage_sherd.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_256428_, "adventure/salvage_sherd");
      Advancement.Builder.advancement()
         .parent(advancementholder10)
         .display(
            DecoratedPotBlockEntity.createDecoratedPotItem(
               new PotDecorations(Optional.empty(), Optional.of(Items.HEART_POTTERY_SHERD), Optional.empty(), Optional.of(Items.EXPLORER_POTTERY_SHERD))
            ),
            Component.translatable("advancements.adventure.craft_decorated_pot_using_only_sherds.title"),
            Component.translatable("advancements.adventure.craft_decorated_pot_using_only_sherds.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "pot_crafted_using_only_sherds",
            RecipeCraftedTrigger.TriggerInstance.craftedItem(
               ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("decorated_pot")),
               List.of(
                  ItemPredicate.Builder.item().of(holderlookup1, ItemTags.DECORATED_POT_SHERDS),
                  ItemPredicate.Builder.item().of(holderlookup1, ItemTags.DECORATED_POT_SHERDS),
                  ItemPredicate.Builder.item().of(holderlookup1, ItemTags.DECORATED_POT_SHERDS),
                  ItemPredicate.Builder.item().of(holderlookup1, ItemTags.DECORATED_POT_SHERDS)
               )
            )
         )
         .save(p_256428_, "adventure/craft_decorated_pot_using_only_sherds");
      AdvancementHolder advancementholder11 = craftingANewLook(Advancement.Builder.advancement())
         .parent(advancementholder)
         .display(
            new ItemStack(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE),
            Component.translatable("advancements.adventure.trim_with_any_armor_pattern.title"),
            Component.translatable("advancements.adventure.trim_with_any_armor_pattern.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_256428_, "adventure/trim_with_any_armor_pattern");
      smithingWithStyle(Advancement.Builder.advancement())
         .parent(advancementholder11)
         .display(
            new ItemStack(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE),
            Component.translatable("advancements.adventure.trim_with_all_exclusive_armor_patterns.title"),
            Component.translatable("advancements.adventure.trim_with_all_exclusive_armor_patterns.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(150))
         .save(p_256428_, "adventure/trim_with_all_exclusive_armor_patterns");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.CHISELED_BOOKSHELF,
            Component.translatable("advancements.adventure.read_power_from_chiseled_bookshelf.title"),
            Component.translatable("advancements.adventure.read_power_from_chiseled_bookshelf.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .requirements(AdvancementRequirements.Strategy.OR)
         .addCriterion("chiseled_bookshelf", placedBlockReadByComparator(holderlookup2, Blocks.CHISELED_BOOKSHELF))
         .addCriterion("comparator", placedComparatorReadingBlock(holderlookup2, Blocks.CHISELED_BOOKSHELF))
         .save(p_256428_, "adventure/read_power_of_chiseled_bookshelf");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.ARMADILLO_SCUTE,
            Component.translatable("advancements.adventure.brush_armadillo.title"),
            Component.translatable("advancements.adventure.brush_armadillo.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "brush_armadillo",
            PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(
               ItemPredicate.Builder.item().of(holderlookup1, Items.BRUSH),
               Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(holderlookup, EntityType.ARMADILLO)))
            )
         )
         .save(p_256428_, "adventure/brush_armadillo");
      AdvancementHolder advancementholder12 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Blocks.CHISELED_TUFF,
            Component.translatable("advancements.adventure.minecraft_trials_edition.title"),
            Component.translatable("advancements.adventure.minecraft_trials_edition.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "minecraft_trials_edition",
            PlayerTrigger.TriggerInstance.located(
               LocationPredicate.Builder.inStructure(p_255887_.lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.TRIAL_CHAMBERS))
            )
         )
         .save(p_256428_, "adventure/minecraft_trials_edition");
      Advancement.Builder.advancement()
         .parent(advancementholder12)
         .display(
            Items.COPPER_BULB,
            Component.translatable("advancements.adventure.lighten_up.title"),
            Component.translatable("advancements.adventure.lighten_up.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "lighten_up",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location()
                  .setBlock(
                     BlockPredicate.Builder.block()
                        .of(
                           holderlookup2,
                           Blocks.OXIDIZED_COPPER_BULB,
                           Blocks.WEATHERED_COPPER_BULB,
                           Blocks.EXPOSED_COPPER_BULB,
                           Blocks.WAXED_OXIDIZED_COPPER_BULB,
                           Blocks.WAXED_WEATHERED_COPPER_BULB,
                           Blocks.WAXED_EXPOSED_COPPER_BULB
                        )
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CopperBulbBlock.LIT, true))
                  ),
               ItemPredicate.Builder.item().of(holderlookup1, VanillaHusbandryAdvancements.WAX_SCRAPING_TOOLS)
            )
         )
         .save(p_256428_, "adventure/lighten_up");
      AdvancementHolder advancementholder13 = Advancement.Builder.advancement()
         .parent(advancementholder12)
         .display(
            Items.TRIAL_KEY,
            Component.translatable("advancements.adventure.under_lock_and_key.title"),
            Component.translatable("advancements.adventure.under_lock_and_key.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "under_lock_and_key",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location()
                  .setBlock(
                     BlockPredicate.Builder.block()
                        .of(holderlookup2, Blocks.VAULT)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(VaultBlock.OMINOUS, false))
                  ),
               ItemPredicate.Builder.item().of(holderlookup1, Items.TRIAL_KEY)
            )
         )
         .save(p_256428_, "adventure/under_lock_and_key");
      Advancement.Builder.advancement()
         .parent(advancementholder13)
         .display(
            Items.OMINOUS_TRIAL_KEY,
            Component.translatable("advancements.adventure.revaulting.title"),
            Component.translatable("advancements.adventure.revaulting.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion(
            "revaulting",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location()
                  .setBlock(
                     BlockPredicate.Builder.block()
                        .of(holderlookup2, Blocks.VAULT)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(VaultBlock.OMINOUS, true))
                  ),
               ItemPredicate.Builder.item().of(holderlookup1, Items.OMINOUS_TRIAL_KEY)
            )
         )
         .save(p_256428_, "adventure/revaulting");
      Advancement.Builder.advancement()
         .parent(advancementholder12)
         .display(
            Items.WIND_CHARGE,
            Component.translatable("advancements.adventure.blowback.title"),
            Component.translatable("advancements.adventure.blowback.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(40))
         .addCriterion(
            "blowback",
            KilledTrigger.TriggerInstance.playerKilledEntity(
               EntityPredicate.Builder.entity().of(holderlookup, EntityType.BREEZE),
               DamageSourcePredicate.Builder.damageType()
                  .tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE))
                  .direct(EntityPredicate.Builder.entity().of(holderlookup, EntityType.BREEZE_WIND_CHARGE))
            )
         )
         .save(p_256428_, "adventure/blowback");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.CRAFTER,
            Component.translatable("advancements.adventure.crafters_crafting_crafters.title"),
            Component.translatable("advancements.adventure.crafters_crafting_crafters.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "crafter_crafted_crafter",
            RecipeCraftedTrigger.TriggerInstance.crafterCraftedItem(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("crafter")))
         )
         .save(p_256428_, "adventure/crafters_crafting_crafters");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.LODESTONE,
            Component.translatable("advancements.adventure.use_lodestone.title"),
            Component.translatable("advancements.adventure.use_lodestone.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "use_lodestone",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(holderlookup2, Blocks.LODESTONE)),
               ItemPredicate.Builder.item().of(holderlookup1, Items.COMPASS)
            )
         )
         .save(p_256428_, "adventure/use_lodestone");
      Advancement.Builder.advancement()
         .parent(advancementholder12)
         .display(
            Items.WIND_CHARGE,
            Component.translatable("advancements.adventure.who_needs_rockets.title"),
            Component.translatable("advancements.adventure.who_needs_rockets.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "who_needs_rockets",
            FallAfterExplosionTrigger.TriggerInstance.fallAfterExplosion(
               DistancePredicate.vertical(MinMaxBounds.Doubles.atLeast(7.0)), EntityPredicate.Builder.entity().of(holderlookup, EntityType.WIND_CHARGE)
            )
         )
         .save(p_256428_, "adventure/who_needs_rockets");
      Advancement.Builder.advancement()
         .parent(advancementholder12)
         .display(
            Items.MACE,
            Component.translatable("advancements.adventure.overoverkill.title"),
            Component.translatable("advancements.adventure.overoverkill.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(50))
         .addCriterion(
            "overoverkill",
            PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntityWithDamage(
               DamagePredicate.Builder.damageInstance()
                  .dealtDamage(MinMaxBounds.Doubles.atLeast(100.0))
                  .type(
                     DamageSourcePredicate.Builder.damageType()
                        .tag(TagPredicate.is(DamageTypeTags.IS_MACE_SMASH))
                        .direct(
                           EntityPredicate.Builder.entity()
                              .of(holderlookup, EntityType.PLAYER)
                              .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(ItemPredicate.Builder.item().of(holderlookup1, Items.MACE)))
                        )
                  )
            )
         )
         .save(p_256428_, "adventure/overoverkill");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Blocks.CREAKING_HEART,
            Component.translatable("advancements.adventure.heart_transplanter.title"),
            Component.translatable("advancements.adventure.heart_transplanter.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .requirements(AdvancementRequirements.Strategy.OR)
         .addCriterion(
            "place_creaking_heart_dormant",
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlockWithProperties(
               Blocks.CREAKING_HEART, BlockStateProperties.CREAKING_HEART_STATE, CreakingHeartState.DORMANT
            )
         )
         .addCriterion(
            "place_creaking_heart_awake",
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlockWithProperties(
               Blocks.CREAKING_HEART, BlockStateProperties.CREAKING_HEART_STATE, CreakingHeartState.AWAKE
            )
         )
         .addCriterion("place_pale_oak_log", placedBlockActivatesCreakingHeart(holderlookup2, BlockTags.PALE_OAK_LOGS))
         .save(p_256428_, "adventure/heart_transplanter");
   }

   public static AdvancementHolder createMonsterHunterAdvancement(
      AdvancementHolder p_309635_, Consumer<AdvancementHolder> p_309544_, HolderGetter<EntityType<?>> p_365631_, List<EntityType<?>> p_310276_
   ) {
      AdvancementHolder advancementholder = addMobsToKill(Advancement.Builder.advancement(), p_365631_, p_310276_)
         .parent(p_309635_)
         .display(
            Items.IRON_SWORD,
            Component.translatable("advancements.adventure.kill_a_mob.title"),
            Component.translatable("advancements.adventure.kill_a_mob.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .requirements(AdvancementRequirements.Strategy.OR)
         .save(p_309544_, "adventure/kill_a_mob");
      addMobsToKill(Advancement.Builder.advancement(), p_365631_, p_310276_)
         .parent(advancementholder)
         .display(
            Items.DIAMOND_SWORD,
            Component.translatable("advancements.adventure.kill_all_mobs.title"),
            Component.translatable("advancements.adventure.kill_all_mobs.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(100))
         .save(p_309544_, "adventure/kill_all_mobs");
      return advancementholder;
   }

   private static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockReadByComparator(HolderGetter<Block> p_365736_, Block p_286401_) {
      LootItemCondition.Builder[] alootitemcondition$builder = ComparatorBlock.FACING
         .getPossibleValues()
         .stream()
         .map(
            p_448647_ -> {
               StatePropertiesPredicate.Builder statepropertiespredicate$builder = StatePropertiesPredicate.Builder.properties()
                  .hasProperty(ComparatorBlock.FACING, p_448647_);
               BlockPredicate.Builder blockpredicate$builder = BlockPredicate.Builder.block()
                  .of(p_365736_, Blocks.COMPARATOR)
                  .setProperties(statepropertiespredicate$builder);
               return LocationCheck.checkLocation(
                  LocationPredicate.Builder.location().setBlock(blockpredicate$builder), new BlockPos(p_448647_.getOpposite().getUnitVec3i())
               );
            }
         )
         .toArray(LootItemCondition.Builder[]::new);
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(
         LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_286401_), AnyOfCondition.anyOf(alootitemcondition$builder)
      );
   }

   private static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedComparatorReadingBlock(HolderGetter<Block> p_366873_, Block p_286250_) {
      LootItemCondition.Builder[] alootitemcondition$builder = ComparatorBlock.FACING
         .getPossibleValues()
         .stream()
         .map(
            p_358184_ -> {
               StatePropertiesPredicate.Builder statepropertiespredicate$builder = StatePropertiesPredicate.Builder.properties()
                  .hasProperty(ComparatorBlock.FACING, p_358184_);
               LootItemBlockStatePropertyCondition.Builder lootitemblockstatepropertycondition$builder = new LootItemBlockStatePropertyCondition.Builder(
                     Blocks.COMPARATOR
                  )
                  .setProperties(statepropertiespredicate$builder);
               LootItemCondition.Builder lootitemcondition$builder = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(p_366873_, p_286250_)),
                  new BlockPos(p_358184_.getUnitVec3i())
               );
               return AllOfCondition.allOf(lootitemblockstatepropertycondition$builder, lootitemcondition$builder);
            }
         )
         .toArray(LootItemCondition.Builder[]::new);
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AnyOfCondition.anyOf(alootitemcondition$builder));
   }

   private static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockActivatesCreakingHeart(HolderGetter<Block> p_409694_, TagKey<Block> p_410498_) {
      LootItemCondition.Builder[] alootitemcondition$builder = Stream.of(Direction.values())
         .map(
            p_405057_ -> {
               StatePropertiesPredicate.Builder statepropertiespredicate$builder = StatePropertiesPredicate.Builder.properties()
                  .hasProperty(CreakingHeartBlock.AXIS, p_405057_.getAxis());
               BlockPredicate.Builder blockpredicate$builder = BlockPredicate.Builder.block()
                  .of(p_409694_, p_410498_)
                  .setProperties(statepropertiespredicate$builder);
               Vec3i vec3i = p_405057_.getUnitVec3i();
               LootItemCondition.Builder lootitemcondition$builder = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location().setBlock(blockpredicate$builder)
               );
               LootItemCondition.Builder lootitemcondition$builder1 = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location()
                     .setBlock(BlockPredicate.Builder.block().of(p_409694_, Blocks.CREAKING_HEART).setProperties(statepropertiespredicate$builder)),
                  new BlockPos(vec3i)
               );
               LootItemCondition.Builder lootitemcondition$builder2 = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location().setBlock(blockpredicate$builder), new BlockPos(vec3i.multiply(2))
               );
               return AllOfCondition.allOf(lootitemcondition$builder, lootitemcondition$builder1, lootitemcondition$builder2);
            }
         )
         .toArray(LootItemCondition.Builder[]::new);
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AnyOfCondition.anyOf(alootitemcondition$builder));
   }

   private static Advancement.Builder smithingWithStyle(Advancement.Builder p_285368_) {
      p_285368_.requirements(AdvancementRequirements.Strategy.AND);
      Set<Item> set = Set.of(
         Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
         Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
         Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
         Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
         Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
         Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
         Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
         Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE
      );
      VanillaRecipeProvider.smithingTrims()
         .filter(p_308497_ -> set.contains(p_308497_.template()))
         .forEach(
            p_448636_ -> p_285368_.addCriterion(
               "armor_trimmed_" + p_448636_.recipeId().identifier(), RecipeCraftedTrigger.TriggerInstance.craftedItem(p_448636_.recipeId())
            )
         );
      return p_285368_;
   }

   private static Advancement.Builder craftingANewLook(Advancement.Builder p_285062_) {
      p_285062_.requirements(AdvancementRequirements.Strategy.OR);
      VanillaRecipeProvider.smithingTrims()
         .map(VanillaRecipeProvider.TrimTemplate::recipeId)
         .forEach(
            p_448645_ -> p_285062_.addCriterion(
               "armor_trimmed_" + p_448645_.identifier(), RecipeCraftedTrigger.TriggerInstance.craftedItem((ResourceKey<Recipe<?>>)p_448645_)
            )
         );
      return p_285062_;
   }

   private static Advancement.Builder respectingTheRemnantsCriterions(HolderGetter<Item> p_364424_, Advancement.Builder p_285170_) {
      List<Pair<String, Criterion<LootTableTrigger.TriggerInstance>>> list = List.of(
         Pair.of("desert_pyramid", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY)),
         Pair.of("desert_well", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY)),
         Pair.of("ocean_ruin_cold", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY)),
         Pair.of("ocean_ruin_warm", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY)),
         Pair.of("trail_ruins_rare", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE)),
         Pair.of("trail_ruins_common", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON))
      );
      list.forEach(p_308495_ -> p_285170_.addCriterion((String)p_308495_.getFirst(), (Criterion<?>)p_308495_.getSecond()));
      String s = "has_sherd";
      p_285170_.addCriterion(
         "has_sherd", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(p_364424_, ItemTags.DECORATED_POT_SHERDS))
      );
      p_285170_.requirements(new AdvancementRequirements(List.of(list.stream().<String>map(Pair::getFirst).toList(), List.of("has_sherd"))));
      return p_285170_;
   }

   protected static void createAdventuringTime(
      HolderLookup.Provider p_334518_, Consumer<AdvancementHolder> p_275645_, AdvancementHolder p_298014_, MultiNoiseBiomeSourceParameterList.Preset p_275211_
   ) {
      addBiomes(Advancement.Builder.advancement(), p_334518_, p_275211_.usedBiomes().toList())
         .parent(p_298014_)
         .display(
            Items.DIAMOND_BOOTS,
            Component.translatable("advancements.adventure.adventuring_time.title"),
            Component.translatable("advancements.adventure.adventuring_time.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(500))
         .save(p_275645_, "adventure/adventuring_time");
   }

   private static Advancement.Builder addMobsToKill(Advancement.Builder p_248814_, HolderGetter<EntityType<?>> p_363074_, List<EntityType<?>> p_309412_) {
      p_309412_.forEach(
         p_448639_ -> p_248814_.addCriterion(
            BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>)p_448639_).toString(),
            KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(p_363074_, (EntityType<?>)p_448639_))
         )
      );
      return p_248814_;
   }

   protected static Advancement.Builder addBiomes(Advancement.Builder p_249250_, HolderLookup.Provider p_334548_, List<ResourceKey<Biome>> p_251338_) {
      HolderGetter<Biome> holdergetter = p_334548_.lookupOrThrow(Registries.BIOME);

      for (ResourceKey<Biome> resourcekey : p_251338_) {
         p_249250_.addCriterion(
            resourcekey.identifier().toString(), PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inBiome(holdergetter.getOrThrow(resourcekey)))
         );
      }

      return p_249250_;
   }

   private static List<EntityType<?>> validateMobsToKill(List<EntityType<?>> p_408691_, HolderLookup<EntityType<?>> p_406512_) {
      List<String> list = new ArrayList<>();
      Set<? extends EntityType<?>> set = Set.copyOf(p_408691_);
      Set<MobCategory> set1 = set.stream().map(EntityType::getCategory).collect(Collectors.toSet());
      Set<MobCategory> set2 = Sets.symmetricDifference(EXCEPTIONS_BY_EXPECTED_CATEGORIES.keySet(), set1);
      if (!set2.isEmpty()) {
         list.add(
            "Found EntityType with MobCategory only in either expected exceptions or kill_all_mobs advancement: "
               + set2.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
         );
      }

      Set<EntityType<?>> set3 = Sets.intersection(
         EXCEPTIONS_BY_EXPECTED_CATEGORIES.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()), set
      );
      if (!set3.isEmpty()) {
         list.add(
            "Found EntityType in both expected exceptions and kill_all_mobs advancement: "
               + set3.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
         );
      }

      Map<MobCategory, Set<EntityType<?>>> map = p_406512_.listElements()
         .map(Holder.Reference::value)
         .filter(Predicate.not(set::contains))
         .collect(Collectors.groupingBy(EntityType::getCategory, Collectors.toSet()));
      EXCEPTIONS_BY_EXPECTED_CATEGORIES.forEach(
         (p_448642_, p_448643_) -> {
            Set<EntityType<?>> set4 = Sets.difference(map.getOrDefault(p_448642_, Set.of()), p_448643_);
            if (!set4.isEmpty()) {
               list.add(
                  String.format(
                     Locale.ROOT,
                     "Found (new?) EntityType with MobCategory %s which are in neither expected exceptions nor kill_all_mobs advancement: %s",
                     p_448642_,
                     set4.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
                  )
               );
            }
         }
      );
      if (!list.isEmpty()) {
         list.forEach(LOGGER::error);
         throw new IllegalStateException("Found inconsistencies with kill_all_mobs advancement");
      } else {
         return p_408691_;
      }
   }
}
