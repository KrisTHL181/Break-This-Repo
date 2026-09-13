package net.minecraft.data.advancements.packs;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.criterion.BeeNestDestroyedTrigger;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.BredAnimalsTrigger;
import net.minecraft.advancements.criterion.ConsumeItemTrigger;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EffectsChangedTrigger;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.EntityEquipmentPredicate;
import net.minecraft.advancements.criterion.EntityFlagsPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.FilledBucketTrigger;
import net.minecraft.advancements.criterion.FishingRodHookedTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.PickedUpItemTrigger;
import net.minecraft.advancements.criterion.PlayerInteractTrigger;
import net.minecraft.advancements.criterion.StartRidingTrigger;
import net.minecraft.advancements.criterion.TameAnimalTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class VanillaHusbandryAdvancements implements AdvancementSubProvider {
   public static final List<EntityType<?>> BREEDABLE_ANIMALS = List.of(
      EntityType.HORSE,
      EntityType.DONKEY,
      EntityType.MULE,
      EntityType.SHEEP,
      EntityType.COW,
      EntityType.MOOSHROOM,
      EntityType.PIG,
      EntityType.CHICKEN,
      EntityType.WOLF,
      EntityType.OCELOT,
      EntityType.RABBIT,
      EntityType.LLAMA,
      EntityType.CAT,
      EntityType.PANDA,
      EntityType.FOX,
      EntityType.BEE,
      EntityType.HOGLIN,
      EntityType.STRIDER,
      EntityType.GOAT,
      EntityType.AXOLOTL,
      EntityType.CAMEL,
      EntityType.ARMADILLO,
      EntityType.NAUTILUS
   );
   public static final List<EntityType<?>> INDIRECTLY_BREEDABLE_ANIMALS = List.of(EntityType.TURTLE, EntityType.FROG, EntityType.SNIFFER);
   private static final Item[] FISH = new Item[]{Items.COD, Items.TROPICAL_FISH, Items.PUFFERFISH, Items.SALMON};
   private static final Item[] FISH_BUCKETS = new Item[]{Items.COD_BUCKET, Items.TROPICAL_FISH_BUCKET, Items.PUFFERFISH_BUCKET, Items.SALMON_BUCKET};
   private static final Item[] EDIBLE_ITEMS = new Item[]{
      Items.APPLE,
      Items.MUSHROOM_STEW,
      Items.BREAD,
      Items.PORKCHOP,
      Items.COOKED_PORKCHOP,
      Items.GOLDEN_APPLE,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.COD,
      Items.SALMON,
      Items.TROPICAL_FISH,
      Items.PUFFERFISH,
      Items.COOKED_COD,
      Items.COOKED_SALMON,
      Items.COOKIE,
      Items.MELON_SLICE,
      Items.BEEF,
      Items.COOKED_BEEF,
      Items.CHICKEN,
      Items.COOKED_CHICKEN,
      Items.ROTTEN_FLESH,
      Items.SPIDER_EYE,
      Items.CARROT,
      Items.POTATO,
      Items.BAKED_POTATO,
      Items.POISONOUS_POTATO,
      Items.GOLDEN_CARROT,
      Items.PUMPKIN_PIE,
      Items.RABBIT,
      Items.COOKED_RABBIT,
      Items.RABBIT_STEW,
      Items.MUTTON,
      Items.COOKED_MUTTON,
      Items.CHORUS_FRUIT,
      Items.BEETROOT,
      Items.BEETROOT_SOUP,
      Items.DRIED_KELP,
      Items.SUSPICIOUS_STEW,
      Items.SWEET_BERRIES,
      Items.HONEY_BOTTLE,
      Items.GLOW_BERRIES
   };
   public static final Item[] WAX_SCRAPING_TOOLS = new Item[]{
      Items.WOODEN_AXE, Items.GOLDEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE
   };
   private static final Comparator<Holder.Reference<?>> HOLDER_KEY_COMPARATOR = Comparator.comparing(p_448657_ -> p_448657_.key().identifier());

   @Override
   public void generate(HolderLookup.Provider p_255680_, Consumer<AdvancementHolder> p_251389_) {
      HolderGetter<EntityType<?>> holdergetter = p_255680_.lookupOrThrow(Registries.ENTITY_TYPE);
      HolderGetter<Item> holdergetter1 = p_255680_.lookupOrThrow(Registries.ITEM);
      HolderGetter<Block> holdergetter2 = p_255680_.lookupOrThrow(Registries.BLOCK);
      HolderLookup<FrogVariant> holderlookup = p_255680_.lookupOrThrow(Registries.FROG_VARIANT);
      HolderLookup<CatVariant> holderlookup1 = p_255680_.lookupOrThrow(Registries.CAT_VARIANT);
      HolderLookup<WolfVariant> holderlookup2 = p_255680_.lookupOrThrow(Registries.WOLF_VARIANT);
      HolderLookup.RegistryLookup<Enchantment> registrylookup = p_255680_.lookupOrThrow(Registries.ENCHANTMENT);
      AdvancementHolder advancementholder = Advancement.Builder.advancement()
         .display(
            Blocks.HAY_BLOCK,
            Component.translatable("advancements.husbandry.root.title"),
            Component.translatable("advancements.husbandry.root.description"),
            Identifier.withDefaultNamespace("gui/advancements/backgrounds/husbandry"),
            AdvancementType.TASK,
            false,
            false,
            false
         )
         .addCriterion("consumed_item", ConsumeItemTrigger.TriggerInstance.usedItem())
         .save(p_251389_, "husbandry/root");
      AdvancementHolder advancementholder1 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.WHEAT,
            Component.translatable("advancements.husbandry.plant_seed.title"),
            Component.translatable("advancements.husbandry.plant_seed.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .requirements(AdvancementRequirements.Strategy.OR)
         .addCriterion("wheat", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.WHEAT))
         .addCriterion("pumpkin_stem", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.PUMPKIN_STEM))
         .addCriterion("melon_stem", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.MELON_STEM))
         .addCriterion("beetroots", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.BEETROOTS))
         .addCriterion("nether_wart", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.NETHER_WART))
         .addCriterion("torchflower", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.TORCHFLOWER_CROP))
         .addCriterion("pitcher_pod", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.PITCHER_CROP))
         .save(p_251389_, "husbandry/plant_seed");
      AdvancementHolder advancementholder2 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.WHEAT,
            Component.translatable("advancements.husbandry.breed_an_animal.title"),
            Component.translatable("advancements.husbandry.breed_an_animal.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .requirements(AdvancementRequirements.Strategy.OR)
         .addCriterion("bred", BredAnimalsTrigger.TriggerInstance.bredAnimals())
         .save(p_251389_, "husbandry/breed_an_animal");
      createBreedAllAnimalsAdvancement(advancementholder2, p_251389_, holdergetter, BREEDABLE_ANIMALS.stream(), INDIRECTLY_BREEDABLE_ANIMALS.stream());
      addFood(Advancement.Builder.advancement(), holdergetter1)
         .parent(advancementholder1)
         .display(
            Items.APPLE,
            Component.translatable("advancements.husbandry.balanced_diet.title"),
            Component.translatable("advancements.husbandry.balanced_diet.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(100))
         .save(p_251389_, "husbandry/balanced_diet");
      Advancement.Builder.advancement()
         .parent(advancementholder1)
         .display(
            Items.NETHERITE_HOE,
            Component.translatable("advancements.husbandry.netherite_hoe.title"),
            Component.translatable("advancements.husbandry.netherite_hoe.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(100))
         .addCriterion("netherite_hoe", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HOE))
         .save(p_251389_, "husbandry/obtain_netherite_hoe");
      AdvancementHolder advancementholder3 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.LEAD,
            Component.translatable("advancements.husbandry.tame_an_animal.title"),
            Component.translatable("advancements.husbandry.tame_an_animal.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("tamed_animal", TameAnimalTrigger.TriggerInstance.tamedAnimal())
         .save(p_251389_, "husbandry/tame_an_animal");
      AdvancementHolder advancementholder4 = addFish(Advancement.Builder.advancement(), holdergetter1)
         .parent(advancementholder)
         .requirements(AdvancementRequirements.Strategy.OR)
         .display(
            Items.FISHING_ROD,
            Component.translatable("advancements.husbandry.fishy_business.title"),
            Component.translatable("advancements.husbandry.fishy_business.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/fishy_business");
      AdvancementHolder advancementholder5 = addFishBuckets(Advancement.Builder.advancement(), holdergetter1)
         .parent(advancementholder4)
         .requirements(AdvancementRequirements.Strategy.OR)
         .display(
            Items.PUFFERFISH_BUCKET,
            Component.translatable("advancements.husbandry.tactical_fishing.title"),
            Component.translatable("advancements.husbandry.tactical_fishing.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/tactical_fishing");
      AdvancementHolder advancementholder6 = Advancement.Builder.advancement()
         .parent(advancementholder5)
         .requirements(AdvancementRequirements.Strategy.OR)
         .addCriterion(
            BuiltInRegistries.ITEM.getKey(Items.AXOLOTL_BUCKET).getPath(),
            FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(holdergetter1, Items.AXOLOTL_BUCKET))
         )
         .display(
            Items.AXOLOTL_BUCKET,
            Component.translatable("advancements.husbandry.axolotl_in_a_bucket.title"),
            Component.translatable("advancements.husbandry.axolotl_in_a_bucket.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/axolotl_in_a_bucket");
      Advancement.Builder.advancement()
         .parent(advancementholder6)
         .addCriterion(
            "kill_axolotl_target", EffectsChangedTrigger.TriggerInstance.gotEffectsFrom(EntityPredicate.Builder.entity().of(holdergetter, EntityType.AXOLOTL))
         )
         .display(
            Items.TROPICAL_FISH_BUCKET,
            Component.translatable("advancements.husbandry.kill_axolotl_target.title"),
            Component.translatable("advancements.husbandry.kill_axolotl_target.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/kill_axolotl_target");
      addCatVariants(Advancement.Builder.advancement(), holderlookup1)
         .parent(advancementholder3)
         .display(
            Items.COD,
            Component.translatable("advancements.husbandry.complete_catalogue.title"),
            Component.translatable("advancements.husbandry.complete_catalogue.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(50))
         .save(p_251389_, "husbandry/complete_catalogue");
      addTamedWolfVariants(Advancement.Builder.advancement(), holderlookup2)
         .parent(advancementholder3)
         .display(
            Items.BONE,
            Component.translatable("advancements.husbandry.whole_pack.title"),
            Component.translatable("advancements.husbandry.whole_pack.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(50))
         .save(p_251389_, "husbandry/whole_pack");
      AdvancementHolder advancementholder7 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .addCriterion(
            "safely_harvest_honey",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(holdergetter2, BlockTags.BEEHIVES)).setSmokey(true),
               ItemPredicate.Builder.item().of(holdergetter1, Items.GLASS_BOTTLE)
            )
         )
         .display(
            Items.HONEY_BOTTLE,
            Component.translatable("advancements.husbandry.safely_harvest_honey.title"),
            Component.translatable("advancements.husbandry.safely_harvest_honey.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/safely_harvest_honey");
      AdvancementHolder advancementholder8 = Advancement.Builder.advancement()
         .parent(advancementholder7)
         .display(
            Items.HONEYCOMB,
            Component.translatable("advancements.husbandry.wax_on.title"),
            Component.translatable("advancements.husbandry.wax_on.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "wax_on",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(holdergetter2, HoneycombItem.WAXABLES.get().keySet())),
               ItemPredicate.Builder.item().of(holdergetter1, Items.HONEYCOMB)
            )
         )
         .save(p_251389_, "husbandry/wax_on");
      Advancement.Builder.advancement()
         .parent(advancementholder8)
         .display(
            Items.STONE_AXE,
            Component.translatable("advancements.husbandry.wax_off.title"),
            Component.translatable("advancements.husbandry.wax_off.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "wax_off",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(holdergetter2, HoneycombItem.WAX_OFF_BY_BLOCK.get().keySet())),
               ItemPredicate.Builder.item().of(holdergetter1, WAX_SCRAPING_TOOLS)
            )
         )
         .save(p_251389_, "husbandry/wax_off");
      AdvancementHolder advancementholder9 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .addCriterion(
            BuiltInRegistries.ITEM.getKey(Items.TADPOLE_BUCKET).getPath(),
            FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(holdergetter1, Items.TADPOLE_BUCKET))
         )
         .display(
            Items.TADPOLE_BUCKET,
            Component.translatable("advancements.husbandry.tadpole_in_a_bucket.title"),
            Component.translatable("advancements.husbandry.tadpole_in_a_bucket.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/tadpole_in_a_bucket");
      AdvancementHolder advancementholder10 = addLeashedFrogVariants(holdergetter, holdergetter1, holderlookup, Advancement.Builder.advancement())
         .parent(advancementholder9)
         .display(
            Items.LEAD,
            Component.translatable("advancements.husbandry.leash_all_frog_variants.title"),
            Component.translatable("advancements.husbandry.leash_all_frog_variants.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/leash_all_frog_variants");
      Advancement.Builder.advancement()
         .parent(advancementholder10)
         .display(
            Items.VERDANT_FROGLIGHT,
            Component.translatable("advancements.husbandry.froglights.title"),
            Component.translatable("advancements.husbandry.froglights.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .addCriterion(
            "froglights", InventoryChangeTrigger.TriggerInstance.hasItems(Items.OCHRE_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, Items.VERDANT_FROGLIGHT)
         )
         .save(p_251389_, "husbandry/froglights");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .addCriterion(
            "silk_touch_nest",
            BeeNestDestroyedTrigger.TriggerInstance.destroyedBeeNest(
               Blocks.BEE_NEST,
               ItemPredicate.Builder.item()
                  .withComponents(
                     DataComponentMatchers.Builder.components()
                        .partial(
                           DataComponentPredicates.ENCHANTMENTS,
                           EnchantmentsPredicate.enchantments(
                              List.of(new EnchantmentPredicate(registrylookup.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))
                           )
                        )
                        .build()
                  ),
               MinMaxBounds.Ints.exactly(3)
            )
         )
         .display(
            Blocks.BEE_NEST,
            Component.translatable("advancements.husbandry.silk_touch_nest.title"),
            Component.translatable("advancements.husbandry.silk_touch_nest.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .save(p_251389_, "husbandry/silk_touch_nest");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.OAK_BOAT,
            Component.translatable("advancements.husbandry.ride_a_boat_with_a_goat.title"),
            Component.translatable("advancements.husbandry.ride_a_boat_with_a_goat.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "ride_a_boat_with_a_goat",
            StartRidingTrigger.TriggerInstance.playerStartsRiding(
               EntityPredicate.Builder.entity()
                  .vehicle(
                     EntityPredicate.Builder.entity()
                        .of(holdergetter, EntityTypeTags.BOAT)
                        .passenger(EntityPredicate.Builder.entity().of(holdergetter, EntityType.GOAT))
                  )
            )
         )
         .save(p_251389_, "husbandry/ride_a_boat_with_a_goat");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.GLOW_INK_SAC,
            Component.translatable("advancements.husbandry.make_a_sign_glow.title"),
            Component.translatable("advancements.husbandry.make_a_sign_glow.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "make_a_sign_glow",
            ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
               LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(holdergetter2, BlockTags.ALL_SIGNS)),
               ItemPredicate.Builder.item().of(holdergetter1, Items.GLOW_INK_SAC)
            )
         )
         .save(p_251389_, "husbandry/make_a_sign_glow");
      AdvancementHolder advancementholder11 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.COOKIE,
            Component.translatable("advancements.husbandry.allay_deliver_item_to_player.title"),
            Component.translatable("advancements.husbandry.allay_deliver_item_to_player.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            true
         )
         .addCriterion(
            "allay_deliver_item_to_player",
            PickedUpItemTrigger.TriggerInstance.thrownItemPickedUpByPlayer(
               Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(holdergetter, EntityType.ALLAY)))
            )
         )
         .save(p_251389_, "husbandry/allay_deliver_item_to_player");
      Advancement.Builder.advancement()
         .parent(advancementholder11)
         .display(
            Items.NOTE_BLOCK,
            Component.translatable("advancements.husbandry.allay_deliver_cake_to_note_block.title"),
            Component.translatable("advancements.husbandry.allay_deliver_cake_to_note_block.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            true
         )
         .addCriterion(
            "allay_deliver_cake_to_note_block",
            ItemUsedOnLocationTrigger.TriggerInstance.allayDropItemOnBlock(
               LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(holdergetter2, Blocks.NOTE_BLOCK)),
               ItemPredicate.Builder.item().of(holdergetter1, Items.CAKE)
            )
         )
         .save(p_251389_, "husbandry/allay_deliver_cake_to_note_block");
      AdvancementHolder advancementholder12 = Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.SNIFFER_EGG,
            Component.translatable("advancements.husbandry.obtain_sniffer_egg.title"),
            Component.translatable("advancements.husbandry.obtain_sniffer_egg.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            true
         )
         .addCriterion("obtain_sniffer_egg", InventoryChangeTrigger.TriggerInstance.hasItems(Items.SNIFFER_EGG))
         .save(p_251389_, "husbandry/obtain_sniffer_egg");
      AdvancementHolder advancementholder13 = Advancement.Builder.advancement()
         .parent(advancementholder12)
         .display(
            Items.TORCHFLOWER_SEEDS,
            Component.translatable("advancements.husbandry.feed_snifflet.title"),
            Component.translatable("advancements.husbandry.feed_snifflet.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            true
         )
         .addCriterion(
            "feed_snifflet",
            PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(
               ItemPredicate.Builder.item().of(holdergetter1, ItemTags.SNIFFER_FOOD),
               Optional.of(
                  EntityPredicate.wrap(
                     EntityPredicate.Builder.entity().of(holdergetter, EntityType.SNIFFER).flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true))
                  )
               )
            )
         )
         .save(p_251389_, "husbandry/feed_snifflet");
      Advancement.Builder.advancement()
         .parent(advancementholder13)
         .display(
            Items.PITCHER_POD,
            Component.translatable("advancements.husbandry.plant_any_sniffer_seed.title"),
            Component.translatable("advancements.husbandry.plant_any_sniffer_seed.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            true
         )
         .requirements(AdvancementRequirements.Strategy.OR)
         .addCriterion("torchflower", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.TORCHFLOWER_CROP))
         .addCriterion("pitcher_pod", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.PITCHER_CROP))
         .save(p_251389_, "husbandry/plant_any_sniffer_seed");
      Advancement.Builder.advancement()
         .parent(advancementholder3)
         .display(
            Items.SHEARS,
            Component.translatable("advancements.husbandry.remove_wolf_armor.title"),
            Component.translatable("advancements.husbandry.remove_wolf_armor.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "remove_wolf_armor",
            PlayerInteractTrigger.TriggerInstance.equipmentSheared(
               ItemPredicate.Builder.item().of(holdergetter1, Items.WOLF_ARMOR),
               Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(holdergetter, EntityType.WOLF)))
            )
         )
         .save(p_251389_, "husbandry/remove_wolf_armor");
      Advancement.Builder.advancement()
         .parent(advancementholder3)
         .display(
            Items.WOLF_ARMOR,
            Component.translatable("advancements.husbandry.repair_wolf_armor.title"),
            Component.translatable("advancements.husbandry.repair_wolf_armor.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "repair_wolf_armor",
            PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(
               ItemPredicate.Builder.item().of(holdergetter1, Items.ARMADILLO_SCUTE),
               Optional.of(
                  EntityPredicate.wrap(
                     EntityPredicate.Builder.entity()
                        .of(holdergetter, EntityType.WOLF)
                        .equipment(
                           EntityEquipmentPredicate.Builder.equipment()
                              .body(
                                 ItemPredicate.Builder.item()
                                    .of(holdergetter1, Items.WOLF_ARMOR)
                                    .withComponents(
                                       DataComponentMatchers.Builder.components().exact(DataComponentExactPredicate.expect(DataComponents.DAMAGE, 0)).build()
                                    )
                              )
                        )
                  )
               )
            )
         )
         .save(p_251389_, "husbandry/repair_wolf_armor");
      Advancement.Builder.advancement()
         .parent(advancementholder)
         .display(
            Items.DRIED_GHAST,
            Component.translatable("advancements.husbandry.place_dried_ghast_in_water.title"),
            Component.translatable("advancements.husbandry.place_dried_ghast_in_water.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "place_dried_ghast_in_water",
            ItemUsedOnLocationTrigger.TriggerInstance.placedBlockWithProperties(Blocks.DRIED_GHAST, BlockStateProperties.WATERLOGGED, true)
         )
         .save(p_251389_, "husbandry/place_dried_ghast_in_water");
   }

   public static AdvancementHolder createBreedAllAnimalsAdvancement(
      AdvancementHolder p_301269_,
      Consumer<AdvancementHolder> p_266923_,
      HolderGetter<EntityType<?>> p_367933_,
      Stream<EntityType<?>> p_266961_,
      Stream<EntityType<?>> p_266751_
   ) {
      return addBreedable(Advancement.Builder.advancement(), p_266961_, p_367933_, p_266751_)
         .parent(p_301269_)
         .display(
            Items.GOLDEN_CARROT,
            Component.translatable("advancements.husbandry.breed_all_animals.title"),
            Component.translatable("advancements.husbandry.breed_all_animals.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(100))
         .save(p_266923_, "husbandry/bred_all_animals");
   }

   private static Advancement.Builder addLeashedFrogVariants(
      HolderGetter<EntityType<?>> p_362943_, HolderGetter<Item> p_369066_, HolderLookup<FrogVariant> p_391881_, Advancement.Builder p_249739_
   ) {
      sortedVariants(p_391881_)
         .forEach(
            p_448656_ -> p_249739_.addCriterion(
               p_448656_.key().identifier().toString(),
               PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(
                  ItemPredicate.Builder.item().of(p_369066_, Items.LEAD),
                  Optional.of(
                     EntityPredicate.wrap(
                        EntityPredicate.Builder.entity()
                           .of(p_362943_, EntityType.FROG)
                           .components(
                              DataComponentMatchers.Builder.components()
                                 .exact(DataComponentExactPredicate.expect(DataComponents.FROG_VARIANT, p_448656_))
                                 .build()
                           )
                     )
                  )
               )
            )
         );
      return p_249739_;
   }

   private static <T> Stream<Holder.Reference<T>> sortedVariants(HolderLookup<T> p_397961_) {
      return p_397961_.listElements().sorted(HOLDER_KEY_COMPARATOR);
   }

   private static Advancement.Builder addFood(Advancement.Builder p_248532_, HolderGetter<Item> p_364143_) {
      for (Item item : EDIBLE_ITEMS) {
         p_248532_.addCriterion(BuiltInRegistries.ITEM.getKey(item).getPath(), ConsumeItemTrigger.TriggerInstance.usedItem(p_364143_, item));
      }

      return p_248532_;
   }

   private static Advancement.Builder addBreedable(
      Advancement.Builder p_266978_, Stream<EntityType<?>> p_267147_, HolderGetter<EntityType<?>> p_364973_, Stream<EntityType<?>> p_267091_
   ) {
      p_267147_.forEach(
         p_448660_ -> p_266978_.addCriterion(
            EntityType.getKey((EntityType<?>)p_448660_).toString(),
            BredAnimalsTrigger.TriggerInstance.bredAnimals(EntityPredicate.Builder.entity().of(p_364973_, (EntityType<?>)p_448660_))
         )
      );
      p_267091_.forEach(
         p_448652_ -> p_266978_.addCriterion(
            EntityType.getKey((EntityType<?>)p_448652_).toString(),
            BredAnimalsTrigger.TriggerInstance.bredAnimals(
               Optional.of(EntityPredicate.Builder.entity().of(p_364973_, (EntityType<?>)p_448652_).build()),
               Optional.of(EntityPredicate.Builder.entity().of(p_364973_, (EntityType<?>)p_448652_).build()),
               Optional.empty()
            )
         )
      );
      return p_266978_;
   }

   private static Advancement.Builder addFishBuckets(Advancement.Builder p_249285_, HolderGetter<Item> p_365002_) {
      for (Item item : FISH_BUCKETS) {
         p_249285_.addCriterion(
            BuiltInRegistries.ITEM.getKey(item).getPath(), FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(p_365002_, item))
         );
      }

      return p_249285_;
   }

   private static Advancement.Builder addFish(Advancement.Builder p_248725_, HolderGetter<Item> p_367279_) {
      for (Item item : FISH) {
         p_248725_.addCriterion(
            BuiltInRegistries.ITEM.getKey(item).getPath(),
            FishingRodHookedTrigger.TriggerInstance.fishedItem(
               Optional.empty(), Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(p_367279_, item).build())
            )
         );
      }

      return p_248725_;
   }

   private static Advancement.Builder addCatVariants(Advancement.Builder p_249232_, HolderLookup<CatVariant> p_396377_) {
      sortedVariants(p_396377_)
         .forEach(
            p_448649_ -> p_249232_.addCriterion(
               p_448649_.key().identifier().toString(),
               TameAnimalTrigger.TriggerInstance.tamedAnimal(
                  EntityPredicate.Builder.entity()
                     .components(
                        DataComponentMatchers.Builder.components().exact(DataComponentExactPredicate.expect(DataComponents.CAT_VARIANT, p_448649_)).build()
                     )
               )
            )
         );
      return p_249232_;
   }

   private static Advancement.Builder addTamedWolfVariants(Advancement.Builder p_336151_, HolderLookup<WolfVariant> p_393228_) {
      sortedVariants(p_393228_)
         .forEach(
            p_448662_ -> p_336151_.addCriterion(
               p_448662_.key().identifier().toString(),
               TameAnimalTrigger.TriggerInstance.tamedAnimal(
                  EntityPredicate.Builder.entity()
                     .components(
                        DataComponentMatchers.Builder.components().exact(DataComponentExactPredicate.expect(DataComponents.WOLF_VARIANT, p_448662_)).build()
                     )
               )
            )
         );
      return p_336151_;
   }
}
