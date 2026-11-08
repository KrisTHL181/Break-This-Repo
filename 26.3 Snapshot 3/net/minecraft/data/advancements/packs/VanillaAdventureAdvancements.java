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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.DamagePredicate;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.LightningBoltPredicate;
import net.minecraft.advancements.predicates.entity.PlayerPredicate;
import net.minecraft.advancements.triggers.ChanneledLightningTrigger;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.DistanceTrigger;
import net.minecraft.advancements.triggers.FallAfterExplosionTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.triggers.KilledByArrowTrigger;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.advancements.triggers.LightningStrikeTrigger;
import net.minecraft.advancements.triggers.LootTableTrigger;
import net.minecraft.advancements.triggers.PlayerHurtEntityTrigger;
import net.minecraft.advancements.triggers.PlayerInteractTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.advancements.triggers.RecipeCraftedTrigger;
import net.minecraft.advancements.triggers.ShotCrossbowTrigger;
import net.minecraft.advancements.triggers.SlideDownBlockTrigger;
import net.minecraft.advancements.triggers.SpearMobsTrigger;
import net.minecraft.advancements.triggers.SummonedEntityTrigger;
import net.minecraft.advancements.triggers.TargetBlockTrigger;
import net.minecraft.advancements.triggers.TradeTrigger;
import net.minecraft.advancements.triggers.UsedTotemTrigger;
import net.minecraft.advancements.triggers.UsingItemTrigger;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.JukeboxPlayablePredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class VanillaAdventureAdvancements extends AdvancementSubProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DISTANCE_FROM_BOTTOM_TO_TOP = 384;
   private static final int Y_COORDINATE_AT_TOP = 320;
   private static final int Y_COORDINATE_AT_BOTTOM = -64;
   private static final int BEDROCK_THICKNESS = 5;
   private static final Map<MobCategory, Set<EntityType<?>>> EXCEPTIONS_BY_EXPECTED_CATEGORIES = Map.of(
      MobCategory.MONSTER, Set.of(EntityTypes.GIANT, EntityTypes.ILLUSIONER, EntityTypes.WARDEN, EntityTypes.SULFUR_CUBE)
   );
   private static final List<EntityType<?>> MOBS_TO_KILL = Arrays.asList(
      EntityTypes.BLAZE,
      EntityTypes.BOGGED,
      EntityTypes.BREEZE,
      EntityTypes.CAMEL_HUSK,
      EntityTypes.CAVE_SPIDER,
      EntityTypes.CREAKING,
      EntityTypes.CREEPER,
      EntityTypes.DROWNED,
      EntityTypes.ELDER_GUARDIAN,
      EntityTypes.ENDER_DRAGON,
      EntityTypes.ENDERMAN,
      EntityTypes.ENDERMITE,
      EntityTypes.EVOKER,
      EntityTypes.GHAST,
      EntityTypes.GUARDIAN,
      EntityTypes.HOGLIN,
      EntityTypes.HUSK,
      EntityTypes.MAGMA_CUBE,
      EntityTypes.PARCHED,
      EntityTypes.PHANTOM,
      EntityTypes.PIGLIN,
      EntityTypes.PIGLIN_BRUTE,
      EntityTypes.PILLAGER,
      EntityTypes.RAVAGER,
      EntityTypes.SHULKER,
      EntityTypes.SILVERFISH,
      EntityTypes.SKELETON,
      EntityTypes.SLIME,
      EntityTypes.SPIDER,
      EntityTypes.STRAY,
      EntityTypes.VEX,
      EntityTypes.VINDICATOR,
      EntityTypes.WITCH,
      EntityTypes.WITHER_SKELETON,
      EntityTypes.WITHER,
      EntityTypes.ZOGLIN,
      EntityTypes.ZOMBIE_VILLAGER,
      EntityTypes.ZOMBIE,
      EntityTypes.ZOMBIE_HORSE,
      EntityTypes.ZOMBIFIED_PIGLIN,
      EntityTypes.ZOMBIE_NAUTILUS
   );
   private final HolderGetter<EntityType<?>> entityTypes;
   private final HolderGetter<Item> items;
   private final HolderGetter<Block> blocks;
   private final HolderGetter<Biome> biomes;
   private final HolderGetter<Structure> structures;
   private final HolderGetter<BannerPattern> bannerPatterns;

   public VanillaAdventureAdvancements(final BootstrapContext<Advancement> output) {
      super(output);
      this.entityTypes = output.lookup(Registries.ENTITY_TYPE);
      this.items = output.lookup(Registries.ITEM);
      this.blocks = output.lookup(Registries.BLOCK);
      this.biomes = output.lookup(Registries.BIOME);
      this.structures = output.lookup(Registries.STRUCTURE);
      this.bannerPatterns = output.lookup(Registries.BANNER_PATTERN);
   }

   private static Criterion<LightningStrikeTrigger.TriggerInstance> fireCountAndBystander(
      final MinMaxBounds.Ints fireCount, final Optional<EntityPredicate> bystander
   ) {
      return LightningStrikeTrigger.TriggerInstance.lightningStrike(
         Optional.of(
            EntityPredicate.Builder.entity()
               .distance(DistancePredicate.absolute(MinMaxBounds.Doubles.atMost(30.0)))
               .lightingBolt(LightningBoltPredicate.blockSetOnFire(fireCount))
               .build()
         ),
         bystander
      );
   }

   private static Criterion<UsingItemTrigger.TriggerInstance> lookAtThroughItem(final EntityPredicate.Builder lookingAt, final ItemPredicate.Builder with) {
      return UsingItemTrigger.TriggerInstance.lookingAt(
         EntityPredicate.Builder.entity().player(PlayerPredicate.Builder.player().setLookingAt(lookingAt).build()), with
      );
   }

   @Override
   public void generate() {
      AdvancementHolder root = Advancement.Builder.advancement()
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
         .save(this.output, "adventure/root");
      AdvancementHolder sleepInBed = Advancement.Builder.advancement()
         .parent(root)
         .display(
            Blocks.BED.red(),
            Component.translatable("advancements.adventure.sleep_in_bed.title"),
            Component.translatable("advancements.adventure.sleep_in_bed.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("slept_in_bed", PlayerTrigger.TriggerInstance.sleptInBed())
         .save(this.output, "adventure/sleep_in_bed");
      this.createAdventuringTime(sleepInBed, MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);
      AdvancementHolder trade = Advancement.Builder.advancement()
         .parent(root)
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
         .save(this.output, "adventure/trade");
      Advancement.Builder.advancement()
         .parent(trade)
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
         .save(this.output, "adventure/trade_at_world_height");
      AdvancementHolder killAMob = this.createMonsterHunterAdvancement(root, this.validateMobsToKill(MOBS_TO_KILL));
      AdvancementHolder shootArrow = Advancement.Builder.advancement()
         .parent(killAMob)
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
                        .direct(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypeTags.ARROWS))
                  )
            )
         )
         .save(this.output, "adventure/shoot_arrow");
      AdvancementHolder throwTrident = Advancement.Builder.advancement()
         .parent(killAMob)
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
                        .direct(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.TRIDENT))
                  )
            )
         )
         .save(this.output, "adventure/throw_trident");
      Advancement.Builder.advancement()
         .parent(throwTrident)
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
            ChanneledLightningTrigger.TriggerInstance.channeledLightning(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.VILLAGER))
         )
         .save(this.output, "adventure/very_very_frightening");
      Advancement.Builder.advancement()
         .parent(trade)
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
            "summoned_golem",
            SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.IRON_GOLEM))
         )
         .save(this.output, "adventure/summon_iron_golem");
      Advancement.Builder.advancement()
         .parent(shootArrow)
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
                  .of(this.entityTypes, EntityTypes.SKELETON)
                  .distance(DistancePredicate.horizontal(MinMaxBounds.Doubles.atLeast(50.0))),
               DamageSourcePredicate.Builder.damageType().tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE))
            )
         )
         .save(this.output, "adventure/sniper_duel");
      Advancement.Builder.advancement()
         .parent(killAMob)
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
         .addCriterion("used_totem", UsedTotemTrigger.TriggerInstance.usedTotem(this.items, Items.TOTEM_OF_UNDYING))
         .save(this.output, "adventure/totem_of_undying");
      Advancement.Builder.advancement()
         .parent(killAMob)
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
         .save(this.output, "adventure/spear_many_mobs");
      AdvancementHolder olBetsy = Advancement.Builder.advancement()
         .parent(root)
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
         .addCriterion("shot_crossbow", ShotCrossbowTrigger.TriggerInstance.shotCrossbow(this.items, Items.CROSSBOW))
         .save(this.output, "adventure/ol_betsy");
      Advancement.Builder.advancement()
         .parent(olBetsy)
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
            KilledByArrowTrigger.TriggerInstance.crossbowKilled(this.items, EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.PILLAGER))
         )
         .save(this.output, "adventure/whos_the_pillager_now");
      Advancement.Builder.advancement()
         .parent(olBetsy)
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
               this.items,
               EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.PHANTOM),
               EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.PHANTOM)
            )
         )
         .save(this.output, "adventure/two_birds_one_arrow");
      Advancement.Builder.advancement()
         .parent(olBetsy)
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
         .addCriterion("arbalistic", KilledByArrowTrigger.TriggerInstance.crossbowKilled(this.items, MinMaxBounds.Ints.exactly(5)))
         .save(this.output, "adventure/arbalistic");
      AdvancementHolder raidOmen = Advancement.Builder.advancement()
         .parent(root)
         .display(
            Raid.getOminousBannerTemplate(this.bannerPatterns),
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
                  .of(this.entityTypes, EntityTypeTags.RAIDERS)
                  .equipment(EntityEquipmentPredicate.captainPredicate(this.items, this.bannerPatterns))
            )
         )
         .save(this.output, "adventure/voluntary_exile");
      Advancement.Builder.advancement()
         .parent(raidOmen)
         .display(
            Raid.getOminousBannerTemplate(this.bannerPatterns),
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
         .save(this.output, "adventure/hero_of_the_village");
      Advancement.Builder.advancement()
         .parent(root)
         .display(
            Items.HONEY_BLOCK,
            Component.translatable("advancements.adventure.honey_block_slide.title"),
            Component.translatable("advancements.adventure.honey_block_slide.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("honey_block_slide", SlideDownBlockTrigger.TriggerInstance.slidesDownBlock(Blocks.HONEY_BLOCK))
         .save(this.output, "adventure/honey_block_slide");
      Advancement.Builder.advancement()
         .parent(shootArrow)
         .display(
            Items.TARGET,
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
         .save(this.output, "adventure/bullseye");
      Advancement.Builder.advancement()
         .parent(sleepInBed)
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
            PlayerTrigger.TriggerInstance.walkOnBlockWithEquipment(this.blocks, this.items, Blocks.POWDER_SNOW, Items.LEATHER_BOOTS)
         )
         .save(this.output, "adventure/walk_on_powder_snow_with_leather_boots");
      Advancement.Builder.advancement()
         .parent(root)
         .display(
            Items.LIGHTNING_ROD.weathering().unaffected(),
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
            fireCountAndBystander(
               MinMaxBounds.Ints.exactly(0), Optional.of(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.VILLAGER).build())
            )
         )
         .save(this.output, "adventure/lightning_rod_with_villager_no_fire");
      AdvancementHolder isItABird = Advancement.Builder.advancement()
         .parent(root)
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
               EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.PARROT), ItemPredicate.Builder.item().of(this.items, Items.SPYGLASS)
            )
         )
         .save(this.output, "adventure/spyglass_at_parrot");
      AdvancementHolder isItABalloon = Advancement.Builder.advancement()
         .parent(isItABird)
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
               EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.GHAST), ItemPredicate.Builder.item().of(this.items, Items.SPYGLASS)
            )
         )
         .save(this.output, "adventure/spyglass_at_ghast");
      Advancement.Builder.advancement()
         .parent(sleepInBed)
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
                  .setBiomes(HolderSet.direct(this.biomes.getOrThrow(Biomes.MEADOW)))
                  .setBlock(BlockPredicate.Builder.block().of(this.blocks, Blocks.JUKEBOX)),
               ItemPredicate.Builder.item()
                  .withComponents(
                     DataComponentMatchers.Builder.components().partial(DataComponentPredicates.JUKEBOX_PLAYABLE, JukeboxPlayablePredicate.any()).build()
                  )
            )
         )
         .save(this.output, "adventure/play_jukebox_in_meadows");
      Advancement.Builder.advancement()
         .parent(isItABalloon)
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
               EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.ENDER_DRAGON), ItemPredicate.Builder.item().of(this.items, Items.SPYGLASS)
            )
         )
         .save(this.output, "adventure/spyglass_at_dragon");
      Advancement.Builder.advancement()
         .parent(root)
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
         .save(this.output, "adventure/fall_from_world_height");
      Advancement.Builder.advancement()
         .parent(killAMob)
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
         .save(this.output, "adventure/kill_mob_near_sculk_catalyst");
      Advancement.Builder.advancement()
         .parent(root)
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
         .save(this.output, "adventure/avoid_vibration");
      AdvancementHolder respectingTheRemnants = this.respectingTheRemnantsCriterions(Advancement.Builder.advancement())
         .parent(root)
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
         .save(this.output, "adventure/salvage_sherd");
      Advancement.Builder.advancement()
         .parent(respectingTheRemnants)
         .display(
            DecoratedPotBlockEntity.createDecoratedPotTemplate(
               new PotDecorations(
                  Optional.empty(),
                  Optional.of(new ItemStackTemplate(Items.HEART_POTTERY_SHERD)),
                  Optional.empty(),
                  Optional.of(new ItemStackTemplate(Items.EXPLORER_POTTERY_SHERD))
               )
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
                  ItemPredicate.Builder.item().of(this.items, ItemTags.DECORATED_POT_SHERDS),
                  ItemPredicate.Builder.item().of(this.items, ItemTags.DECORATED_POT_SHERDS),
                  ItemPredicate.Builder.item().of(this.items, ItemTags.DECORATED_POT_SHERDS),
                  ItemPredicate.Builder.item().of(this.items, ItemTags.DECORATED_POT_SHERDS)
               )
            )
         )
         .save(this.output, "adventure/craft_decorated_pot_using_only_sherds");
      AdvancementHolder craftingANewLook = craftingANewLook(Advancement.Builder.advancement())
         .parent(root)
         .display(
            Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Component.translatable("advancements.adventure.trim_with_any_armor_pattern.title"),
            Component.translatable("advancements.adventure.trim_with_any_armor_pattern.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(this.output, "adventure/trim_with_any_armor_pattern");
      smithingWithStyle(Advancement.Builder.advancement())
         .parent(craftingANewLook)
         .display(
            Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Component.translatable("advancements.adventure.trim_with_all_exclusive_armor_patterns.title"),
            Component.translatable("advancements.adventure.trim_with_all_exclusive_armor_patterns.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(150))
         .save(this.output, "adventure/trim_with_all_exclusive_armor_patterns");
      Advancement.Builder.advancement()
         .parent(root)
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
         .addCriterion("chiseled_bookshelf", this.placedBlockReadByComparator(Blocks.CHISELED_BOOKSHELF))
         .addCriterion("comparator", this.placedComparatorReadingBlock(Blocks.CHISELED_BOOKSHELF))
         .save(this.output, "adventure/read_power_of_chiseled_bookshelf");
      Advancement.Builder.advancement()
         .parent(root)
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
               ItemPredicate.Builder.item().of(this.items, Items.BRUSH),
               Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.ARMADILLO)))
            )
         )
         .save(this.output, "adventure/brush_armadillo");
      AdvancementHolder trialsEdition = Advancement.Builder.advancement()
         .parent(root)
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
            PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(this.structures.getOrThrow(BuiltinStructures.TRIAL_CHAMBERS)))
         )
         .save(this.output, "adventure/minecraft_trials_edition");
      Advancement.Builder.advancement()
         .parent(trialsEdition)
         .display(
            Items.COPPER_BULB.weathering().unaffected(),
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
                           this.blocks,
                           Blocks.COPPER_BULB.weathering().exposed(),
                           Blocks.COPPER_BULB.weathering().weathered(),
                           Blocks.COPPER_BULB.weathering().oxidized(),
                           Blocks.COPPER_BULB.waxed().exposed(),
                           Blocks.COPPER_BULB.waxed().weathered(),
                           Blocks.COPPER_BULB.waxed().oxidized()
                        )
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CopperBulbBlock.LIT, true))
                  ),
               ItemPredicate.Builder.item().of(this.items, VanillaHusbandryAdvancements.WAX_SCRAPING_TOOLS)
            )
         )
         .save(this.output, "adventure/lighten_up");
      AdvancementHolder underLockAndKey = Advancement.Builder.advancement()
         .parent(trialsEdition)
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
                        .of(this.blocks, Blocks.VAULT)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(VaultBlock.OMINOUS, false))
                  ),
               ItemPredicate.Builder.item().of(this.items, Items.TRIAL_KEY)
            )
         )
         .save(this.output, "adventure/under_lock_and_key");
      Advancement.Builder.advancement()
         .parent(underLockAndKey)
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
                        .of(this.blocks, Blocks.VAULT)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(VaultBlock.OMINOUS, true))
                  ),
               ItemPredicate.Builder.item().of(this.items, Items.OMINOUS_TRIAL_KEY)
            )
         )
         .save(this.output, "adventure/revaulting");
      Advancement.Builder.advancement()
         .parent(trialsEdition)
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
               EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.BREEZE),
               DamageSourcePredicate.Builder.damageType()
                  .tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE))
                  .direct(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.BREEZE_WIND_CHARGE))
            )
         )
         .save(this.output, "adventure/blowback");
      Advancement.Builder.advancement()
         .parent(root)
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
         .save(this.output, "adventure/crafters_crafting_crafters");
      Advancement.Builder.advancement()
         .parent(root)
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
               LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(this.blocks, Blocks.LODESTONE)),
               ItemPredicate.Builder.item().of(this.items, Items.COMPASS)
            )
         )
         .save(this.output, "adventure/use_lodestone");
      Advancement.Builder.advancement()
         .parent(trialsEdition)
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
               DistancePredicate.vertical(MinMaxBounds.Doubles.atLeast(7.0)), EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.WIND_CHARGE)
            )
         )
         .save(this.output, "adventure/who_needs_rockets");
      Advancement.Builder.advancement()
         .parent(trialsEdition)
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
                              .of(this.entityTypes, EntityTypes.PLAYER)
                              .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(ItemPredicate.Builder.item().of(this.items, Items.MACE)))
                        )
                  )
            )
         )
         .save(this.output, "adventure/overoverkill");
      Advancement.Builder.advancement()
         .parent(root)
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
         .addCriterion("place_pale_oak_log", this.placedBlockActivatesCreakingHeart(BlockTags.PALE_OAK_LOGS))
         .save(this.output, "adventure/heart_transplanter");
   }

   public AdvancementHolder createMonsterHunterAdvancement(final AdvancementHolder parent, final List<EntityType<?>> mobsToKill) {
      AdvancementHolder killAMob = this.addMobsToKill(Advancement.Builder.advancement(), mobsToKill)
         .parent(parent)
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
         .save(this.output, "adventure/kill_a_mob");
      this.addMobsToKill(Advancement.Builder.advancement(), mobsToKill)
         .parent(killAMob)
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
         .save(this.output, "adventure/kill_all_mobs");
      return killAMob;
   }

   private Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockReadByComparator(final Block block) {
      LootItemCondition.Builder[] conditions = ComparatorBlock.FACING.getPossibleValues().stream().map(direction -> {
         StatePropertiesPredicate.Builder comparatorProperties = StatePropertiesPredicate.Builder.properties().hasProperty(ComparatorBlock.FACING, direction);
         BlockPredicate.Builder comparatorTest = BlockPredicate.Builder.block().of(this.blocks, Blocks.COMPARATOR).setProperties(comparatorProperties);
         return LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(comparatorTest), direction.getOpposite());
      }).toArray(LootItemCondition.Builder[]::new);
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(
         LootItemBlockStatePropertyCondition.hasBlockStateProperties(block), AnyOfCondition.anyOf(conditions)
      );
   }

   private Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedComparatorReadingBlock(final Block block) {
      LootItemCondition.Builder[] conditions = ComparatorBlock.FACING
         .getPossibleValues()
         .stream()
         .map(
            direction -> {
               StatePropertiesPredicate.Builder comparatorProperties = StatePropertiesPredicate.Builder.properties()
                  .hasProperty(ComparatorBlock.FACING, direction);
               LootItemBlockStatePropertyCondition.Builder comparatorTest = new LootItemBlockStatePropertyCondition.Builder(Blocks.COMPARATOR)
                  .setProperties(comparatorProperties);
               LootItemCondition.Builder blockTest = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(this.blocks, block)), direction
               );
               return AllOfCondition.allOf(comparatorTest, blockTest);
            }
         )
         .toArray(LootItemCondition.Builder[]::new);
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AnyOfCondition.anyOf(conditions));
   }

   private Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockActivatesCreakingHeart(final TagKey<Block> block) {
      LootItemCondition.Builder[] conditions = Stream.of(Direction.values())
         .map(
            direction -> {
               StatePropertiesPredicate.Builder creakingHeartProperties = StatePropertiesPredicate.Builder.properties()
                  .hasProperty(CreakingHeartBlock.AXIS, direction.getAxis());
               BlockPredicate.Builder placedPaleOakLogBlock = BlockPredicate.Builder.block().of(this.blocks, block).setProperties(creakingHeartProperties);
               Vec3i blockOffset = direction.getUnitVec3i();
               LootItemCondition.Builder placedPaleOakLogTest = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location().setBlock(placedPaleOakLogBlock)
               );
               LootItemCondition.Builder creakingHeartBlockTest = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location()
                     .setBlock(BlockPredicate.Builder.block().of(this.blocks, Blocks.CREAKING_HEART).setProperties(creakingHeartProperties)),
                  blockOffset
               );
               LootItemCondition.Builder existingPaleOakLogTest = LocationCheck.checkLocation(
                  LocationPredicate.Builder.location().setBlock(placedPaleOakLogBlock), blockOffset.multiply(2)
               );
               return AllOfCondition.allOf(placedPaleOakLogTest, creakingHeartBlockTest, existingPaleOakLogTest);
            }
         )
         .toArray(LootItemCondition.Builder[]::new);
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AnyOfCondition.anyOf(conditions));
   }

   private static Advancement.Builder smithingWithStyle(final Advancement.Builder advancement) {
      advancement.requirements(AdvancementRequirements.Strategy.AND);
      Set<Item> required = Set.of(
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
         .filter(trim -> required.contains(trim.template()))
         .forEach(
            trimTemplate -> advancement.addCriterion(
               "armor_trimmed_" + trimTemplate.recipeId().identifier(), RecipeCraftedTrigger.TriggerInstance.craftedItem(trimTemplate.recipeId())
            )
         );
      return advancement;
   }

   private static Advancement.Builder craftingANewLook(final Advancement.Builder advancement) {
      advancement.requirements(AdvancementRequirements.Strategy.OR);
      VanillaRecipeProvider.smithingTrims()
         .map(VanillaRecipeProvider.TrimTemplate::recipeId)
         .forEach(
            recipeId -> advancement.addCriterion(
               "armor_trimmed_" + recipeId.identifier(), RecipeCraftedTrigger.TriggerInstance.craftedItem((ResourceKey<Recipe<?>>)recipeId)
            )
         );
      return advancement;
   }

   private Advancement.Builder respectingTheRemnantsCriterions(final Advancement.Builder advancement) {
      List<Pair<String, Criterion<LootTableTrigger.TriggerInstance>>> lootCriteria = List.of(
         Pair.of("desert_pyramid", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY)),
         Pair.of("desert_well", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY)),
         Pair.of("ocean_ruin_cold", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY)),
         Pair.of("ocean_ruin_warm", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY)),
         Pair.of("trail_ruins_rare", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE)),
         Pair.of("trail_ruins_common", LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON))
      );
      lootCriteria.forEach(p -> advancement.addCriterion((String)p.getFirst(), (Criterion<?>)p.getSecond()));
      String hasSherdCriterion = "has_sherd";
      advancement.addCriterion(
         "has_sherd", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(this.items, ItemTags.DECORATED_POT_SHERDS))
      );
      advancement.requirements(new AdvancementRequirements(List.of(lootCriteria.stream().<String>map(Pair::getFirst).toList(), List.of("has_sherd"))));
      return advancement;
   }

   protected void createAdventuringTime(final AdvancementHolder sleepInBed, final MultiNoiseBiomeSourceParameterList.Preset preset) {
      addBiomes(Advancement.Builder.advancement(), this.biomes, preset.usedBiomes().toList())
         .parent(sleepInBed)
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
         .save(this.output, "adventure/adventuring_time");
   }

   private Advancement.Builder addMobsToKill(final Advancement.Builder advancement, final List<EntityType<?>> mobsToKill) {
      mobsToKill.forEach(
         mob -> advancement.addCriterion(
            BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>)mob).toString(),
            KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(this.entityTypes, (EntityType<?>)mob))
         )
      );
      return advancement;
   }

   public static Advancement.Builder addBiomes(
      final Advancement.Builder advancement, final HolderGetter<Biome> biomes, final List<ResourceKey<Biome>> explorableBiomes
   ) {
      for (ResourceKey<Biome> biome : explorableBiomes) {
         advancement.addCriterion(
            biome.identifier().toString(), PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inBiome(biomes.getOrThrow(biome)))
         );
      }

      return advancement;
   }

   private List<EntityType<?>> validateMobsToKill(final List<EntityType<?>> data) {
      List<String> errors = new ArrayList<>();
      Set<? extends EntityType<?>> mobsToKill = Set.copyOf(data);
      Set<MobCategory> specifiedCategories = mobsToKill.stream().map(EntityType::getCategory).collect(Collectors.toSet());
      Set<MobCategory> categoryDifference = Sets.symmetricDifference(EXCEPTIONS_BY_EXPECTED_CATEGORIES.keySet(), specifiedCategories);
      if (!categoryDifference.isEmpty()) {
         errors.add(
            "Found EntityType with MobCategory only in either expected exceptions or kill_all_mobs advancement: "
               + categoryDifference.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
         );
      }

      Set<EntityType<?>> entityTypeOverlap = Sets.intersection(
         EXCEPTIONS_BY_EXPECTED_CATEGORIES.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()), mobsToKill
      );
      if (!entityTypeOverlap.isEmpty()) {
         errors.add(
            "Found EntityType in both expected exceptions and kill_all_mobs advancement: "
               + entityTypeOverlap.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
         );
      }

      Map<MobCategory, Set<EntityType<?>>> doNotKillByCategory = this.output
         .listContextElements(Registries.ENTITY_TYPE)
         .map(Holder.Reference::value)
         .filter(Predicate.not(mobsToKill::contains))
         .collect(Collectors.groupingBy(EntityType::getCategory, Collectors.toSet()));
      EXCEPTIONS_BY_EXPECTED_CATEGORIES.forEach(
         (exceptedCategory, exceptedTypes) -> {
            Set<EntityType<?>> exceptedDiff = Sets.difference(doNotKillByCategory.getOrDefault(exceptedCategory, Set.of()), exceptedTypes);
            if (!exceptedDiff.isEmpty()) {
               errors.add(
                  String.format(
                     Locale.ROOT,
                     "Found (new?) EntityType with MobCategory %s which are in neither expected exceptions nor kill_all_mobs advancement: %s",
                     exceptedCategory,
                     exceptedDiff.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
                  )
               );
            }
         }
      );
      if (!errors.isEmpty()) {
         errors.forEach(LOGGER::error);
         throw new IllegalStateException("Found inconsistencies with kill_all_mobs advancement");
      } else {
         return data;
      }
   }
}
