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
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.BeeNestDestroyedTrigger;
import net.minecraft.advancements.triggers.BredAnimalsTrigger;
import net.minecraft.advancements.triggers.ConsumeItemTrigger;
import net.minecraft.advancements.triggers.EffectsChangedTrigger;
import net.minecraft.advancements.triggers.FilledBucketTrigger;
import net.minecraft.advancements.triggers.FishingRodHookedTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.triggers.PickedUpItemTrigger;
import net.minecraft.advancements.triggers.PlayerInteractTrigger;
import net.minecraft.advancements.triggers.StartRidingTrigger;
import net.minecraft.advancements.triggers.TameAnimalTrigger;
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
import net.minecraft.world.entity.EntityTypes;
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
        EntityTypes.HORSE,
        EntityTypes.DONKEY,
        EntityTypes.MULE,
        EntityTypes.SHEEP,
        EntityTypes.COW,
        EntityTypes.MOOSHROOM,
        EntityTypes.PIG,
        EntityTypes.CHICKEN,
        EntityTypes.WOLF,
        EntityTypes.OCELOT,
        EntityTypes.RABBIT,
        EntityTypes.LLAMA,
        EntityTypes.CAT,
        EntityTypes.PANDA,
        EntityTypes.FOX,
        EntityTypes.BEE,
        EntityTypes.HOGLIN,
        EntityTypes.STRIDER,
        EntityTypes.GOAT,
        EntityTypes.AXOLOTL,
        EntityTypes.CAMEL,
        EntityTypes.ARMADILLO,
        EntityTypes.NAUTILUS
    );
    public static final List<EntityType<?>> INDIRECTLY_BREEDABLE_ANIMALS = List.of(EntityTypes.TURTLE, EntityTypes.FROG, EntityTypes.SNIFFER);
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
    private static final Comparator<Holder.Reference<?>> HOLDER_KEY_COMPARATOR = Comparator.comparing(e -> e.key().identifier());

    @Override
    public void generate(final HolderLookup.Provider registries, final Consumer<AdvancementHolder> output) {
        HolderGetter<EntityType<?>> entityTypes = registries.lookupOrThrow(Registries.ENTITY_TYPE);
        HolderGetter<Item> items = registries.lookupOrThrow(Registries.ITEM);
        HolderGetter<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        HolderLookup<FrogVariant> frogVariants = registries.lookupOrThrow(Registries.FROG_VARIANT);
        HolderLookup<CatVariant> catVariants = registries.lookupOrThrow(Registries.CAT_VARIANT);
        HolderLookup<WolfVariant> wolfVariants = registries.lookupOrThrow(Registries.WOLF_VARIANT);
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        AdvancementHolder root = Advancement.Builder.advancement()
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
            .save(output, "husbandry/root");
        AdvancementHolder plantSeed = Advancement.Builder.advancement()
            .parent(root)
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
            .save(output, "husbandry/plant_seed");
        Advancement.Builder.advancement()
            .parent(root)
            .display(
                Items.TNT,
                Component.translatable("advancements.husbandry.uh_oh.title"),
                Component.translatable("advancements.husbandry.uh_oh.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .requirements(AdvancementRequirements.Strategy.OR)
            .addCriterion(
                "pick_up_dropped_tnt",
                PickedUpItemTrigger.TriggerInstance.thrownItemPickedUpByEntity(
                    Optional.empty(),
                    Optional.of(ItemPredicate.Builder.item().of(items, ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE).build()),
                    Optional.of(
                        EntityPredicate.wrap(
                            EntityPredicate.Builder.entity()
                                .of(entityTypes, EntityTypes.SULFUR_CUBE)
                                .flags(EntityFlagsPredicate.Builder.flags().setIsBaby(false))
                        )
                    )
                )
            )
            .addCriterion(
                "give_tnt_directly",
                PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(
                    Optional.empty(),
                    ItemPredicate.Builder.item().of(items, ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE),
                    Optional.of(
                        EntityPredicate.wrap(
                            EntityPredicate.Builder.entity()
                                .of(entityTypes, EntityTypes.SULFUR_CUBE)
                                .flags(EntityFlagsPredicate.Builder.flags().setIsBaby(false))
                        )
                    )
                )
            )
            .save(output, "husbandry/uh_oh");
        AdvancementHolder breedAnAnimalAdvancement = Advancement.Builder.advancement()
            .parent(root)
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
            .save(output, "husbandry/breed_an_animal");
        createBreedAllAnimalsAdvancement(breedAnAnimalAdvancement, output, entityTypes, BREEDABLE_ANIMALS.stream(), INDIRECTLY_BREEDABLE_ANIMALS.stream());
        addFood(Advancement.Builder.advancement(), items)
            .parent(plantSeed)
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
            .save(output, "husbandry/balanced_diet");
        Advancement.Builder.advancement()
            .parent(plantSeed)
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
            .save(output, "husbandry/obtain_netherite_hoe");
        AdvancementHolder tameAnAnimal = Advancement.Builder.advancement()
            .parent(root)
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
            .save(output, "husbandry/tame_an_animal");
        AdvancementHolder fishyBusiness = addFish(Advancement.Builder.advancement(), items)
            .parent(root)
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
            .save(output, "husbandry/fishy_business");
        AdvancementHolder tacticalFishing = addFishBuckets(Advancement.Builder.advancement(), items)
            .parent(fishyBusiness)
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
            .save(output, "husbandry/tactical_fishing");
        AdvancementHolder theCutestPredetor = Advancement.Builder.advancement()
            .parent(tacticalFishing)
            .requirements(AdvancementRequirements.Strategy.OR)
            .addCriterion(
                BuiltInRegistries.ITEM.getKey(Items.AXOLOTL_BUCKET).getPath(),
                FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(items, Items.AXOLOTL_BUCKET))
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
            .save(output, "husbandry/axolotl_in_a_bucket");
        Advancement.Builder.advancement()
            .parent(theCutestPredetor)
            .addCriterion(
                "kill_axolotl_target",
                EffectsChangedTrigger.TriggerInstance.gotEffectsFrom(EntityPredicate.Builder.entity().of(entityTypes, EntityTypes.AXOLOTL))
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
            .save(output, "husbandry/kill_axolotl_target");
        addCatVariants(Advancement.Builder.advancement(), catVariants)
            .parent(tameAnAnimal)
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
            .save(output, "husbandry/complete_catalogue");
        addTamedWolfVariants(Advancement.Builder.advancement(), wolfVariants)
            .parent(tameAnAnimal)
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
            .save(output, "husbandry/whole_pack");
        AdvancementHolder safelyHarvestHoney = Advancement.Builder.advancement()
            .parent(root)
            .addCriterion(
                "safely_harvest_honey",
                ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(
                    LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, BlockTags.BEEHIVES)).setSmokey(true),
                    ItemPredicate.Builder.item().of(items, Items.GLASS_BOTTLE)
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
            .save(output, "husbandry/safely_harvest_honey");
        AdvancementHolder waxOn = Advancement.Builder.advancement()
            .parent(safelyHarvestHoney)
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
                    LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, HoneycombItem.WAXABLES.get().keySet())),
                    ItemPredicate.Builder.item().of(items, Items.HONEYCOMB)
                )
            )
            .save(output, "husbandry/wax_on");
        Advancement.Builder.advancement()
            .parent(waxOn)
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
                    LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, HoneycombItem.WAX_OFF_BY_BLOCK.get().keySet())),
                    ItemPredicate.Builder.item().of(items, WAX_SCRAPING_TOOLS)
                )
            )
            .save(output, "husbandry/wax_off");
        AdvancementHolder tadpoleInABucket = Advancement.Builder.advancement()
            .parent(root)
            .addCriterion(
                BuiltInRegistries.ITEM.getKey(Items.TADPOLE_BUCKET).getPath(),
                FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(items, Items.TADPOLE_BUCKET))
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
            .save(output, "husbandry/tadpole_in_a_bucket");
        AdvancementHolder allFrogsOnALeash = addLeashedFrogVariants(entityTypes, items, frogVariants, Advancement.Builder.advancement())
            .parent(tadpoleInABucket)
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
            .save(output, "husbandry/leash_all_frog_variants");
        Advancement.Builder.advancement()
            .parent(allFrogsOnALeash)
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
            .save(output, "husbandry/froglights");
        Advancement.Builder.advancement()
            .parent(root)
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
                                        List.of(new EnchantmentPredicate(enchantments.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))
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
            .save(output, "husbandry/silk_touch_nest");
        Advancement.Builder.advancement()
            .parent(root)
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
                                .of(entityTypes, EntityTypeTags.BOAT)
                                .passenger(EntityPredicate.Builder.entity().of(entityTypes, EntityTypes.GOAT))
                        )
                )
            )
            .save(output, "husbandry/ride_a_boat_with_a_goat");
        Advancement.Builder.advancement()
            .parent(root)
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
                    LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, BlockTags.ALL_SIGNS)),
                    ItemPredicate.Builder.item().of(items, Items.GLOW_INK_SAC)
                )
            )
            .save(output, "husbandry/make_a_sign_glow");
        AdvancementHolder itemDeliveredToPlayer = Advancement.Builder.advancement()
            .parent(root)
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
                    Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(entityTypes, EntityTypes.ALLAY)))
                )
            )
            .save(output, "husbandry/allay_deliver_item_to_player");
        Advancement.Builder.advancement()
            .parent(itemDeliveredToPlayer)
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
                    LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, Blocks.NOTE_BLOCK)),
                    ItemPredicate.Builder.item().of(items, Items.CAKE)
                )
            )
            .save(output, "husbandry/allay_deliver_cake_to_note_block");
        AdvancementHolder obtainSnifferEgg = Advancement.Builder.advancement()
            .parent(root)
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
            .save(output, "husbandry/obtain_sniffer_egg");
        AdvancementHolder feedSnifflet = Advancement.Builder.advancement()
            .parent(obtainSnifferEgg)
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
                    ItemPredicate.Builder.item().of(items, ItemTags.SNIFFER_FOOD),
                    Optional.of(
                        EntityPredicate.wrap(
                            EntityPredicate.Builder.entity().of(entityTypes, EntityTypes.SNIFFER).flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true))
                        )
                    )
                )
            )
            .save(output, "husbandry/feed_snifflet");
        Advancement.Builder.advancement()
            .parent(feedSnifflet)
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
            .save(output, "husbandry/plant_any_sniffer_seed");
        Advancement.Builder.advancement()
            .parent(tameAnAnimal)
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
                    ItemPredicate.Builder.item().of(items, Items.WOLF_ARMOR),
                    Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().of(entityTypes, EntityTypes.WOLF)))
                )
            )
            .save(output, "husbandry/remove_wolf_armor");
        Advancement.Builder.advancement()
            .parent(tameAnAnimal)
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
                    ItemPredicate.Builder.item().of(items, Items.ARMADILLO_SCUTE),
                    Optional.of(
                        EntityPredicate.wrap(
                            EntityPredicate.Builder.entity()
                                .of(entityTypes, EntityTypes.WOLF)
                                .equipment(
                                    EntityEquipmentPredicate.Builder.equipment()
                                        .body(
                                            ItemPredicate.Builder.item()
                                                .of(items, Items.WOLF_ARMOR)
                                                .withComponents(
                                                    DataComponentMatchers.Builder.components()
                                                        .exact(DataComponentExactPredicate.expect(DataComponents.DAMAGE, 0))
                                                        .build()
                                                )
                                        )
                                )
                        )
                    )
                )
            )
            .save(output, "husbandry/repair_wolf_armor");
        Advancement.Builder.advancement()
            .parent(root)
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
            .save(output, "husbandry/place_dried_ghast_in_water");
    }

    public static AdvancementHolder createBreedAllAnimalsAdvancement(
        final AdvancementHolder parent,
        final Consumer<AdvancementHolder> output,
        final HolderGetter<EntityType<?>> entityTypes,
        final Stream<EntityType<?>> breedable,
        final Stream<EntityType<?>> indirectlyBreedable
    ) {
        return addBreedable(Advancement.Builder.advancement(), breedable, entityTypes, indirectlyBreedable)
            .parent(parent)
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
            .save(output, "husbandry/bred_all_animals");
    }

    private static Advancement.Builder addLeashedFrogVariants(
        final HolderGetter<EntityType<?>> entityTypes,
        final HolderGetter<Item> items,
        final HolderLookup<FrogVariant> frogVariants,
        final Advancement.Builder advancement
    ) {
        sortedVariants(frogVariants)
            .forEach(
                frogVariant -> advancement.addCriterion(
                    frogVariant.key().identifier().toString(),
                    PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(
                        ItemPredicate.Builder.item().of(items, Items.LEAD),
                        Optional.of(
                            EntityPredicate.wrap(
                                EntityPredicate.Builder.entity()
                                    .of(entityTypes, EntityTypes.FROG)
                                    .components(
                                        DataComponentMatchers.Builder.components()
                                            .exact(DataComponentExactPredicate.expect(DataComponents.FROG_VARIANT, frogVariant))
                                            .build()
                                    )
                            )
                        )
                    )
                )
            );
        return advancement;
    }

    private static <T> Stream<Holder.Reference<T>> sortedVariants(final HolderLookup<T> variants) {
        return variants.listElements().sorted(HOLDER_KEY_COMPARATOR);
    }

    private static Advancement.Builder addFood(final Advancement.Builder advancement, final HolderGetter<Item> items) {
        for (Item food : EDIBLE_ITEMS) {
            advancement.addCriterion(BuiltInRegistries.ITEM.getKey(food).getPath(), ConsumeItemTrigger.TriggerInstance.usedItem(items, food));
        }

        return advancement;
    }

    private static Advancement.Builder addBreedable(
        final Advancement.Builder advancement,
        final Stream<EntityType<?>> breedable,
        final HolderGetter<EntityType<?>> entityTypes,
        final Stream<EntityType<?>> indirectlyBreedable
    ) {
        breedable.forEach(
            animal -> advancement.addCriterion(
                EntityType.getKey((EntityType<?>)animal).toString(),
                BredAnimalsTrigger.TriggerInstance.bredAnimals(EntityPredicate.Builder.entity().of(entityTypes, (EntityType<?>)animal))
            )
        );
        indirectlyBreedable.forEach(
            animal -> advancement.addCriterion(
                EntityType.getKey((EntityType<?>)animal).toString(),
                BredAnimalsTrigger.TriggerInstance.bredAnimals(
                    Optional.of(EntityPredicate.Builder.entity().of(entityTypes, (EntityType<?>)animal).build()),
                    Optional.of(EntityPredicate.Builder.entity().of(entityTypes, (EntityType<?>)animal).build()),
                    Optional.empty()
                )
            )
        );
        return advancement;
    }

    private static Advancement.Builder addFishBuckets(final Advancement.Builder advancement, final HolderGetter<Item> items) {
        for (Item bucket : FISH_BUCKETS) {
            advancement.addCriterion(
                BuiltInRegistries.ITEM.getKey(bucket).getPath(),
                FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(items, bucket))
            );
        }

        return advancement;
    }

    private static Advancement.Builder addFish(final Advancement.Builder advancement, final HolderGetter<Item> items) {
        for (Item fish : FISH) {
            advancement.addCriterion(
                BuiltInRegistries.ITEM.getKey(fish).getPath(),
                FishingRodHookedTrigger.TriggerInstance.fishedItem(
                    Optional.empty(), Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(items, fish).build())
                )
            );
        }

        return advancement;
    }

    private static Advancement.Builder addCatVariants(final Advancement.Builder advancement, final HolderLookup<CatVariant> catVariants) {
        sortedVariants(catVariants)
            .forEach(
                v -> advancement.addCriterion(
                    v.key().identifier().toString(),
                    TameAnimalTrigger.TriggerInstance.tamedAnimal(
                        EntityPredicate.Builder.entity().components(DataComponentExactPredicate.expect(DataComponents.CAT_VARIANT, v))
                    )
                )
            );
        return advancement;
    }

    private static Advancement.Builder addTamedWolfVariants(final Advancement.Builder advancement, final HolderLookup<WolfVariant> wolfVariants) {
        sortedVariants(wolfVariants)
            .forEach(
                v -> advancement.addCriterion(
                    v.key().identifier().toString(),
                    TameAnimalTrigger.TriggerInstance.tamedAnimal(
                        EntityPredicate.Builder.entity().components(DataComponentExactPredicate.expect(DataComponents.WOLF_VARIANT, v))
                    )
                )
            );
        return advancement;
    }
}