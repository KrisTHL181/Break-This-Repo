package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.references.BlockItemId;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.WeatheringCopperCollection;

public class VanillaItemTagsProvider extends TagsProvider<Item> {
    public VanillaItemTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider);
    }

    protected BlockItemTagAppender<Item> tag(final TagKey<Item> tag) {
        return new BlockItemTagAppender<Item>(super.tag(tag)) {
            @Override
            protected ResourceKey<Item> convertElement(final BlockItemId element) {
                return element.item();
            }
        };
    }

    @Override
    protected void addTags(final HolderLookup.Provider registries) {
        new VanillaBlockItemTagsProvider(tagId -> BlockItemTagsProvider.wrapForItems(this.tag(tagId.item()))).run();
        this.tag(ItemTags.BANNERS).addAll(toIds(BlockItemIds.BANNER));
        this.tag(ItemTags.BOATS)
            .add(
                ItemIds.OAK_BOAT,
                ItemIds.SPRUCE_BOAT,
                ItemIds.BIRCH_BOAT,
                ItemIds.JUNGLE_BOAT,
                ItemIds.ACACIA_BOAT,
                ItemIds.DARK_OAK_BOAT,
                ItemIds.PALE_OAK_BOAT,
                ItemIds.MANGROVE_BOAT,
                ItemIds.BAMBOO_RAFT,
                ItemIds.CHERRY_BOAT
            )
            .addTag(ItemTags.CHEST_BOATS);
        this.tag(ItemTags.BUNDLES).add(ItemIds.BUNDLE).addAll(ItemIds.DYED_BUNDLE);
        this.tag(ItemTags.CHEST_BOATS)
            .add(
                ItemIds.OAK_CHEST_BOAT,
                ItemIds.SPRUCE_CHEST_BOAT,
                ItemIds.BIRCH_CHEST_BOAT,
                ItemIds.JUNGLE_CHEST_BOAT,
                ItemIds.ACACIA_CHEST_BOAT,
                ItemIds.DARK_OAK_CHEST_BOAT,
                ItemIds.PALE_OAK_CHEST_BOAT,
                ItemIds.MANGROVE_CHEST_BOAT,
                ItemIds.BAMBOO_CHEST_RAFT,
                ItemIds.CHERRY_CHEST_BOAT
            );
        this.tag(ItemTags.EGGS).add(ItemIds.EGG, ItemIds.BLUE_EGG, ItemIds.BROWN_EGG);
        this.tag(ItemTags.FISHES).add(ItemIds.COD, ItemIds.COOKED_COD, ItemIds.SALMON, ItemIds.COOKED_SALMON, ItemIds.PUFFERFISH, ItemIds.TROPICAL_FISH);
        this.tag(ItemTags.CREEPER_DROP_MUSIC_DISCS)
            .add(
                ItemIds.MUSIC_DISC_13,
                ItemIds.MUSIC_DISC_CAT,
                ItemIds.MUSIC_DISC_BLOCKS,
                ItemIds.MUSIC_DISC_CHIRP,
                ItemIds.MUSIC_DISC_FAR,
                ItemIds.MUSIC_DISC_MALL,
                ItemIds.MUSIC_DISC_MELLOHI,
                ItemIds.MUSIC_DISC_STAL,
                ItemIds.MUSIC_DISC_STRAD,
                ItemIds.MUSIC_DISC_WARD,
                ItemIds.MUSIC_DISC_11,
                ItemIds.MUSIC_DISC_WAIT
            );
        this.tag(ItemTags.COALS).add(ItemIds.COAL, ItemIds.CHARCOAL);
        this.tag(ItemTags.ARROWS).add(ItemIds.ARROW, ItemIds.TIPPED_ARROW, ItemIds.SPECTRAL_ARROW);
        this.tag(ItemTags.LECTERN_BOOKS).add(ItemIds.WRITTEN_BOOK, ItemIds.WRITABLE_BOOK);
        this.tag(ItemTags.BEACON_PAYMENT_ITEMS).add(ItemIds.NETHERITE_INGOT, ItemIds.EMERALD, ItemIds.DIAMOND, ItemIds.GOLD_INGOT, ItemIds.IRON_INGOT);
        this.tag(ItemTags.PIGLIN_REPELLENTS)
            .add(new BlockItemId[]{BlockItemIds.SOUL_TORCH})
            .add(new BlockItemId[]{BlockItemIds.SOUL_LANTERN})
            .add(new BlockItemId[]{BlockItemIds.SOUL_CAMPFIRE});
        this.tag(ItemTags.PIGLIN_LOVED)
            .addTag(ItemTags.GOLD_ORES)
            .add(BlockItemIds.GOLD_BLOCK, BlockItemIds.GILDED_BLACKSTONE, BlockItemIds.LIGHT_WEIGHTED_PRESSURE_PLATE)
            .add(ItemIds.GOLD_INGOT)
            .add(new BlockItemId[]{BlockItemIds.BELL})
            .add(
                ItemIds.CLOCK,
                ItemIds.GOLDEN_CARROT,
                ItemIds.GLISTERING_MELON_SLICE,
                ItemIds.GOLDEN_APPLE,
                ItemIds.ENCHANTED_GOLDEN_APPLE,
                ItemIds.GOLDEN_HELMET,
                ItemIds.GOLDEN_CHESTPLATE,
                ItemIds.GOLDEN_LEGGINGS,
                ItemIds.GOLDEN_BOOTS,
                ItemIds.GOLDEN_HORSE_ARMOR,
                ItemIds.GOLDEN_NAUTILUS_ARMOR,
                ItemIds.GOLDEN_SWORD,
                ItemIds.GOLDEN_SPEAR,
                ItemIds.GOLDEN_PICKAXE,
                ItemIds.GOLDEN_SHOVEL,
                ItemIds.GOLDEN_AXE,
                ItemIds.GOLDEN_HOE,
                ItemIds.RAW_GOLD
            )
            .add(BlockItemIds.RAW_GOLD_BLOCK, BlockItemIds.GOLDEN_DANDELION);
        this.tag(ItemTags.IGNORED_BY_PIGLIN_BABIES).add(ItemIds.LEATHER);
        this.tag(ItemTags.PIGLIN_FOOD).add(ItemIds.PORKCHOP, ItemIds.COOKED_PORKCHOP);
        this.tag(ItemTags.PIGLIN_SAFE_ARMOR).add(ItemIds.GOLDEN_HELMET, ItemIds.GOLDEN_CHESTPLATE, ItemIds.GOLDEN_LEGGINGS, ItemIds.GOLDEN_BOOTS);
        this.tag(ItemTags.FOX_FOOD).add(BlockItemIds.SWEET_BERRY_CROP, BlockItemIds.GLOW_BERRY_CROP);
        this.tag(ItemTags.DUPLICATES_ALLAYS).add(ItemIds.AMETHYST_SHARD);
        this.tag(ItemTags.BREWING_FUEL).add(ItemIds.BLAZE_POWDER);
        this.tag(ItemTags.NON_FLAMMABLE_WOOD)
            .add(
                BlockItemIds.WARPED_STEM,
                BlockItemIds.STRIPPED_WARPED_STEM,
                BlockItemIds.WARPED_HYPHAE,
                BlockItemIds.STRIPPED_WARPED_HYPHAE,
                BlockItemIds.CRIMSON_STEM,
                BlockItemIds.STRIPPED_CRIMSON_STEM,
                BlockItemIds.CRIMSON_HYPHAE,
                BlockItemIds.STRIPPED_CRIMSON_HYPHAE,
                BlockItemIds.CRIMSON_PLANKS,
                BlockItemIds.WARPED_PLANKS,
                BlockItemIds.CRIMSON_SLAB,
                BlockItemIds.WARPED_SLAB,
                BlockItemIds.CRIMSON_PRESSURE_PLATE,
                BlockItemIds.WARPED_PRESSURE_PLATE,
                BlockItemIds.CRIMSON_FENCE,
                BlockItemIds.WARPED_FENCE,
                BlockItemIds.CRIMSON_TRAPDOOR,
                BlockItemIds.WARPED_TRAPDOOR,
                BlockItemIds.CRIMSON_FENCE_GATE,
                BlockItemIds.WARPED_FENCE_GATE,
                BlockItemIds.CRIMSON_STAIRS,
                BlockItemIds.WARPED_STAIRS,
                BlockItemIds.CRIMSON_BUTTON,
                BlockItemIds.WARPED_BUTTON,
                BlockItemIds.CRIMSON_DOOR,
                BlockItemIds.WARPED_DOOR,
                BlockItemIds.CRIMSON_SIGN,
                BlockItemIds.WARPED_SIGN,
                BlockItemIds.WARPED_HANGING_SIGN,
                BlockItemIds.CRIMSON_HANGING_SIGN,
                BlockItemIds.WARPED_SHELF,
                BlockItemIds.CRIMSON_SHELF
            );
        this.tag(ItemTags.WOODEN_TOOL_MATERIALS).addTag(ItemTags.PLANKS);
        this.tag(ItemTags.STONE_TOOL_MATERIALS).add(BlockItemIds.COBBLESTONE, BlockItemIds.BLACKSTONE, BlockItemIds.COBBLED_DEEPSLATE);
        this.tag(ItemTags.COPPER_TOOL_MATERIALS).add(ItemIds.COPPER_INGOT);
        this.tag(ItemTags.IRON_TOOL_MATERIALS).add(ItemIds.IRON_INGOT);
        this.tag(ItemTags.GOLD_TOOL_MATERIALS).add(ItemIds.GOLD_INGOT);
        this.tag(ItemTags.DIAMOND_TOOL_MATERIALS).add(ItemIds.DIAMOND);
        this.tag(ItemTags.NETHERITE_TOOL_MATERIALS).add(ItemIds.NETHERITE_INGOT);
        this.tag(ItemTags.REPAIRS_LEATHER_ARMOR).add(ItemIds.LEATHER);
        this.tag(ItemTags.REPAIRS_COPPER_ARMOR).add(ItemIds.COPPER_INGOT);
        this.tag(ItemTags.REPAIRS_CHAIN_ARMOR).add(ItemIds.IRON_INGOT);
        this.tag(ItemTags.REPAIRS_IRON_ARMOR).add(ItemIds.IRON_INGOT);
        this.tag(ItemTags.REPAIRS_GOLD_ARMOR).add(ItemIds.GOLD_INGOT);
        this.tag(ItemTags.REPAIRS_DIAMOND_ARMOR).add(ItemIds.DIAMOND);
        this.tag(ItemTags.REPAIRS_NETHERITE_ARMOR).add(ItemIds.NETHERITE_INGOT);
        this.tag(ItemTags.REPAIRS_TURTLE_HELMET).add(ItemIds.TURTLE_SCUTE);
        this.tag(ItemTags.REPAIRS_WOLF_ARMOR).add(ItemIds.ARMADILLO_SCUTE);
        this.tag(ItemTags.STONE_CRAFTING_MATERIALS).add(BlockItemIds.COBBLESTONE, BlockItemIds.BLACKSTONE, BlockItemIds.COBBLED_DEEPSLATE);
        this.tag(ItemTags.FREEZE_IMMUNE_WEARABLES)
            .add(ItemIds.LEATHER_BOOTS, ItemIds.LEATHER_LEGGINGS, ItemIds.LEATHER_CHESTPLATE, ItemIds.LEATHER_HELMET, ItemIds.LEATHER_HORSE_ARMOR);
        this.tag(ItemTags.AXOLOTL_FOOD).add(ItemIds.TROPICAL_FISH_BUCKET);
        this.tag(ItemTags.CLUSTER_MAX_HARVESTABLES)
            .add(
                ItemIds.DIAMOND_PICKAXE,
                ItemIds.GOLDEN_PICKAXE,
                ItemIds.IRON_PICKAXE,
                ItemIds.NETHERITE_PICKAXE,
                ItemIds.STONE_PICKAXE,
                ItemIds.WOODEN_PICKAXE,
                ItemIds.COPPER_PICKAXE
            );
        this.tag(ItemTags.COMPASSES).add(ItemIds.COMPASS).add(ItemIds.RECOVERY_COMPASS);
        this.tag(ItemTags.CREEPER_IGNITERS).add(ItemIds.FLINT_AND_STEEL).add(ItemIds.FIRE_CHARGE);
        this.tag(ItemTags.SWORDS)
            .add(ItemIds.DIAMOND_SWORD)
            .add(ItemIds.STONE_SWORD)
            .add(ItemIds.GOLDEN_SWORD)
            .add(ItemIds.NETHERITE_SWORD)
            .add(ItemIds.WOODEN_SWORD)
            .add(ItemIds.IRON_SWORD)
            .add(ItemIds.COPPER_SWORD);
        this.tag(ItemTags.AXES)
            .add(ItemIds.DIAMOND_AXE)
            .add(ItemIds.STONE_AXE)
            .add(ItemIds.GOLDEN_AXE)
            .add(ItemIds.NETHERITE_AXE)
            .add(ItemIds.WOODEN_AXE)
            .add(ItemIds.IRON_AXE)
            .add(ItemIds.COPPER_AXE);
        this.tag(ItemTags.PICKAXES)
            .add(ItemIds.DIAMOND_PICKAXE)
            .add(ItemIds.STONE_PICKAXE)
            .add(ItemIds.GOLDEN_PICKAXE)
            .add(ItemIds.NETHERITE_PICKAXE)
            .add(ItemIds.WOODEN_PICKAXE)
            .add(ItemIds.IRON_PICKAXE)
            .add(ItemIds.COPPER_PICKAXE);
        this.tag(ItemTags.SHOVELS)
            .add(ItemIds.DIAMOND_SHOVEL)
            .add(ItemIds.STONE_SHOVEL)
            .add(ItemIds.GOLDEN_SHOVEL)
            .add(ItemIds.NETHERITE_SHOVEL)
            .add(ItemIds.WOODEN_SHOVEL)
            .add(ItemIds.IRON_SHOVEL)
            .add(ItemIds.COPPER_SHOVEL);
        this.tag(ItemTags.HOES)
            .add(ItemIds.DIAMOND_HOE)
            .add(ItemIds.STONE_HOE)
            .add(ItemIds.GOLDEN_HOE)
            .add(ItemIds.NETHERITE_HOE)
            .add(ItemIds.WOODEN_HOE)
            .add(ItemIds.IRON_HOE)
            .add(ItemIds.COPPER_HOE);
        this.tag(ItemTags.SPEARS)
            .add(
                ItemIds.DIAMOND_SPEAR,
                ItemIds.STONE_SPEAR,
                ItemIds.GOLDEN_SPEAR,
                ItemIds.NETHERITE_SPEAR,
                ItemIds.WOODEN_SPEAR,
                ItemIds.IRON_SPEAR,
                ItemIds.COPPER_SPEAR
            );
        this.tag(ItemTags.BREAKS_DECORATED_POTS)
            .addTag(ItemTags.SWORDS)
            .addTag(ItemTags.AXES)
            .addTag(ItemTags.PICKAXES)
            .addTag(ItemTags.SHOVELS)
            .addTag(ItemTags.HOES)
            .add(ItemIds.TRIDENT)
            .add(ItemIds.MACE);
        this.tag(ItemTags.SKELETON_PREFERRED_WEAPONS).add(ItemIds.BOW);
        this.tag(ItemTags.DROWNED_PREFERRED_WEAPONS).add(ItemIds.TRIDENT);
        this.tag(ItemTags.PIGLIN_PREFERRED_WEAPONS).add(ItemIds.CROSSBOW, ItemIds.GOLDEN_SPEAR);
        this.tag(ItemTags.PILLAGER_PREFERRED_WEAPONS).add(ItemIds.CROSSBOW);
        this.tag(ItemTags.WITHER_SKELETON_DISLIKED_WEAPONS).add(ItemIds.BOW).add(ItemIds.CROSSBOW);
        this.tag(ItemTags.DECORATED_POT_SHERDS)
            .add(
                ItemIds.ANGLER_POTTERY_SHERD,
                ItemIds.ARCHER_POTTERY_SHERD,
                ItemIds.ARMS_UP_POTTERY_SHERD,
                ItemIds.BLADE_POTTERY_SHERD,
                ItemIds.BREWER_POTTERY_SHERD,
                ItemIds.BURN_POTTERY_SHERD,
                ItemIds.DANGER_POTTERY_SHERD,
                ItemIds.EXPLORER_POTTERY_SHERD,
                ItemIds.FRIEND_POTTERY_SHERD,
                ItemIds.HEART_POTTERY_SHERD,
                ItemIds.HEARTBREAK_POTTERY_SHERD,
                ItemIds.HOWL_POTTERY_SHERD,
                ItemIds.MINER_POTTERY_SHERD,
                ItemIds.MOURNER_POTTERY_SHERD,
                ItemIds.PLENTY_POTTERY_SHERD,
                ItemIds.PRIZE_POTTERY_SHERD,
                ItemIds.SHEAF_POTTERY_SHERD,
                ItemIds.SHELTER_POTTERY_SHERD,
                ItemIds.SKULL_POTTERY_SHERD,
                ItemIds.SNORT_POTTERY_SHERD,
                ItemIds.FLOW_POTTERY_SHERD,
                ItemIds.GUSTER_POTTERY_SHERD,
                ItemIds.SCRAPE_POTTERY_SHERD
            );
        this.tag(ItemTags.DECORATED_POT_INGREDIENTS).add(ItemIds.BRICK).addTag(ItemTags.DECORATED_POT_SHERDS);
        this.tag(ItemTags.FOOT_ARMOR)
            .add(
                ItemIds.LEATHER_BOOTS,
                ItemIds.COPPER_BOOTS,
                ItemIds.CHAINMAIL_BOOTS,
                ItemIds.GOLDEN_BOOTS,
                ItemIds.IRON_BOOTS,
                ItemIds.DIAMOND_BOOTS,
                ItemIds.NETHERITE_BOOTS
            );
        this.tag(ItemTags.LEG_ARMOR)
            .add(
                ItemIds.LEATHER_LEGGINGS,
                ItemIds.COPPER_LEGGINGS,
                ItemIds.CHAINMAIL_LEGGINGS,
                ItemIds.GOLDEN_LEGGINGS,
                ItemIds.IRON_LEGGINGS,
                ItemIds.DIAMOND_LEGGINGS,
                ItemIds.NETHERITE_LEGGINGS
            );
        this.tag(ItemTags.CHEST_ARMOR)
            .add(
                ItemIds.LEATHER_CHESTPLATE,
                ItemIds.COPPER_CHESTPLATE,
                ItemIds.CHAINMAIL_CHESTPLATE,
                ItemIds.GOLDEN_CHESTPLATE,
                ItemIds.IRON_CHESTPLATE,
                ItemIds.DIAMOND_CHESTPLATE,
                ItemIds.NETHERITE_CHESTPLATE
            );
        this.tag(ItemTags.HEAD_ARMOR)
            .add(
                ItemIds.LEATHER_HELMET,
                ItemIds.COPPER_HELMET,
                ItemIds.CHAINMAIL_HELMET,
                ItemIds.GOLDEN_HELMET,
                ItemIds.IRON_HELMET,
                ItemIds.DIAMOND_HELMET,
                ItemIds.NETHERITE_HELMET,
                ItemIds.TURTLE_HELMET
            );
        this.tag(ItemTags.SKULLS)
            .add(
                BlockItemIds.PLAYER_HEAD,
                BlockItemIds.CREEPER_HEAD,
                BlockItemIds.ZOMBIE_HEAD,
                BlockItemIds.SKELETON_SKULL,
                BlockItemIds.WITHER_SKELETON_SKULL,
                BlockItemIds.DRAGON_HEAD,
                BlockItemIds.PIGLIN_HEAD
            );
        this.tag(ItemTags.TRIMMABLE_ARMOR).addTag(ItemTags.FOOT_ARMOR).addTag(ItemTags.LEG_ARMOR).addTag(ItemTags.CHEST_ARMOR).addTag(ItemTags.HEAD_ARMOR);
        this.tag(ItemTags.TRIM_MATERIALS)
            .add(
                ItemIds.AMETHYST_SHARD,
                ItemIds.COPPER_INGOT,
                ItemIds.DIAMOND,
                ItemIds.EMERALD,
                ItemIds.GOLD_INGOT,
                ItemIds.IRON_INGOT,
                ItemIds.LAPIS_LAZULI,
                ItemIds.NETHERITE_INGOT,
                ItemIds.QUARTZ
            )
            .add(new BlockItemId[]{BlockItemIds.REDSTONE_DUST})
            .add(ItemIds.RESIN_BRICK);
        this.tag(ItemTags.BOOKSHELF_BOOKS).add(ItemIds.BOOK, ItemIds.WRITTEN_BOOK, ItemIds.ENCHANTED_BOOK, ItemIds.WRITABLE_BOOK, ItemIds.KNOWLEDGE_BOOK);
        this.tag(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS)
            .add(
                BlockItemIds.ZOMBIE_HEAD,
                BlockItemIds.SKELETON_SKULL,
                BlockItemIds.CREEPER_HEAD,
                BlockItemIds.DRAGON_HEAD,
                BlockItemIds.WITHER_SKELETON_SKULL,
                BlockItemIds.PIGLIN_HEAD,
                BlockItemIds.PLAYER_HEAD
            );
        this.tag(ItemTags.SNIFFER_FOOD).add(new BlockItemId[]{BlockItemIds.TORCHFLOWER_CROP});
        this.tag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
            .add(
                BlockItemIds.WHEAT_CROP,
                BlockItemIds.POTATO_CROP,
                BlockItemIds.CARROT_CROP,
                BlockItemIds.BEETROOT_CROP,
                BlockItemIds.TORCHFLOWER_CROP,
                BlockItemIds.PITCHER_CROP
            );
        this.tag(ItemTags.VILLAGER_PICKS_UP).addTag(ItemTags.VILLAGER_PLANTABLE_SEEDS).add(ItemIds.BREAD, ItemIds.WHEAT, ItemIds.BEETROOT);
        this.tag(ItemTags.BOOK_CLONING_TARGET).add(ItemIds.WRITABLE_BOOK);
        this.tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).addTag(ItemTags.FOOT_ARMOR);
        this.tag(ItemTags.LEG_ARMOR_ENCHANTABLE).addTag(ItemTags.LEG_ARMOR);
        this.tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).addTag(ItemTags.CHEST_ARMOR);
        this.tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).addTag(ItemTags.HEAD_ARMOR);
        this.tag(ItemTags.ARMOR_ENCHANTABLE)
            .addTag(ItemTags.FOOT_ARMOR_ENCHANTABLE)
            .addTag(ItemTags.LEG_ARMOR_ENCHANTABLE)
            .addTag(ItemTags.CHEST_ARMOR_ENCHANTABLE)
            .addTag(ItemTags.HEAD_ARMOR_ENCHANTABLE);
        this.tag(ItemTags.SWEEPING_ENCHANTABLE).addTag(ItemTags.SWORDS);
        this.tag(ItemTags.MELEE_WEAPON_ENCHANTABLE).addTag(ItemTags.SWORDS).addTag(ItemTags.SPEARS);
        this.tag(ItemTags.FIRE_ASPECT_ENCHANTABLE).addTag(ItemTags.MELEE_WEAPON_ENCHANTABLE).add(ItemIds.MACE);
        this.tag(ItemTags.SHARP_WEAPON_ENCHANTABLE).addTag(ItemTags.MELEE_WEAPON_ENCHANTABLE).addTag(ItemTags.AXES);
        this.tag(ItemTags.WEAPON_ENCHANTABLE).addTag(ItemTags.SHARP_WEAPON_ENCHANTABLE).add(ItemIds.MACE);
        this.tag(ItemTags.MACE_ENCHANTABLE).add(ItemIds.MACE);
        this.tag(ItemTags.MINING_ENCHANTABLE)
            .addTag(ItemTags.AXES)
            .addTag(ItemTags.PICKAXES)
            .addTag(ItemTags.SHOVELS)
            .addTag(ItemTags.HOES)
            .add(ItemIds.SHEARS);
        this.tag(ItemTags.MINING_LOOT_ENCHANTABLE).addTag(ItemTags.AXES).addTag(ItemTags.PICKAXES).addTag(ItemTags.SHOVELS).addTag(ItemTags.HOES);
        this.tag(ItemTags.FISHING_ENCHANTABLE).add(ItemIds.FISHING_ROD);
        this.tag(ItemTags.TRIDENT_ENCHANTABLE).add(ItemIds.TRIDENT);
        this.tag(ItemTags.LUNGE_ENCHANTABLE).addTag(ItemTags.SPEARS);
        this.tag(ItemTags.DURABILITY_ENCHANTABLE)
            .addTag(ItemTags.FOOT_ARMOR)
            .addTag(ItemTags.LEG_ARMOR)
            .addTag(ItemTags.CHEST_ARMOR)
            .addTag(ItemTags.HEAD_ARMOR)
            .add(ItemIds.ELYTRA)
            .add(ItemIds.SHIELD)
            .addTag(ItemTags.SWORDS)
            .addTag(ItemTags.AXES)
            .addTag(ItemTags.PICKAXES)
            .addTag(ItemTags.SHOVELS)
            .addTag(ItemTags.HOES)
            .add(ItemIds.BOW)
            .add(ItemIds.CROSSBOW)
            .add(ItemIds.TRIDENT)
            .add(ItemIds.FLINT_AND_STEEL)
            .add(ItemIds.SHEARS)
            .add(ItemIds.BRUSH)
            .add(ItemIds.FISHING_ROD)
            .add(ItemIds.CARROT_ON_A_STICK, ItemIds.WARPED_FUNGUS_ON_A_STICK)
            .add(ItemIds.MACE)
            .addTag(ItemTags.SPEARS);
        this.tag(ItemTags.BOW_ENCHANTABLE).add(ItemIds.BOW);
        this.tag(ItemTags.EQUIPPABLE_ENCHANTABLE)
            .addTag(ItemTags.FOOT_ARMOR)
            .addTag(ItemTags.LEG_ARMOR)
            .addTag(ItemTags.CHEST_ARMOR)
            .addTag(ItemTags.HEAD_ARMOR)
            .add(ItemIds.ELYTRA)
            .addTag(ItemTags.SKULLS)
            .add(new BlockItemId[]{BlockItemIds.CARVED_PUMPKIN});
        this.tag(ItemTags.CROSSBOW_ENCHANTABLE).add(ItemIds.CROSSBOW);
        this.tag(ItemTags.VANISHING_ENCHANTABLE)
            .addTag(ItemTags.DURABILITY_ENCHANTABLE)
            .add(ItemIds.COMPASS)
            .add(new BlockItemId[]{BlockItemIds.CARVED_PUMPKIN})
            .addTag(ItemTags.SKULLS);
        this.tag(ItemTags.DYES).addAll(ItemIds.DYE);
        this.tag(ItemTags.CAULDRON_CAN_REMOVE_DYE)
            .add(
                ItemIds.LEATHER_HELMET,
                ItemIds.LEATHER_CHESTPLATE,
                ItemIds.LEATHER_LEGGINGS,
                ItemIds.LEATHER_BOOTS,
                ItemIds.LEATHER_HORSE_ARMOR,
                ItemIds.WOLF_ARMOR
            );
        this.tag(ItemTags.FURNACE_MINECART_FUEL).add(ItemIds.COAL, ItemIds.CHARCOAL);
        this.tag(ItemTags.MEAT)
            .add(
                ItemIds.BEEF,
                ItemIds.CHICKEN,
                ItemIds.COOKED_BEEF,
                ItemIds.COOKED_CHICKEN,
                ItemIds.COOKED_MUTTON,
                ItemIds.COOKED_PORKCHOP,
                ItemIds.COOKED_RABBIT,
                ItemIds.MUTTON,
                ItemIds.PORKCHOP,
                ItemIds.RABBIT,
                ItemIds.ROTTEN_FLESH
            );
        this.tag(ItemTags.WOLF_FOOD)
            .addTag(ItemTags.MEAT)
            .add(ItemIds.COD, ItemIds.COOKED_COD, ItemIds.SALMON, ItemIds.COOKED_SALMON, ItemIds.TROPICAL_FISH, ItemIds.PUFFERFISH, ItemIds.RABBIT_STEW);
        this.tag(ItemTags.OCELOT_FOOD).add(ItemIds.COD, ItemIds.SALMON);
        this.tag(ItemTags.CAT_FOOD).add(ItemIds.COD, ItemIds.SALMON);
        this.tag(ItemTags.HORSE_FOOD)
            .add(ItemIds.WHEAT, ItemIds.SUGAR)
            .add(new BlockItemId[]{BlockItemIds.HAY_BLOCK})
            .add(ItemIds.APPLE)
            .add(new BlockItemId[]{BlockItemIds.CARROT_CROP})
            .add(ItemIds.GOLDEN_CARROT, ItemIds.GOLDEN_APPLE, ItemIds.ENCHANTED_GOLDEN_APPLE);
        this.tag(ItemTags.ZOMBIE_HORSE_FOOD).add(new BlockItemId[]{BlockItemIds.RED_MUSHROOM});
        this.tag(ItemTags.HORSE_TEMPT_ITEMS).add(ItemIds.GOLDEN_CARROT, ItemIds.GOLDEN_APPLE, ItemIds.ENCHANTED_GOLDEN_APPLE);
        this.tag(ItemTags.HARNESSES).addAll(ItemIds.HARNESS);
        this.tag(ItemTags.HAPPY_GHAST_FOOD).add(ItemIds.SNOWBALL);
        this.tag(ItemTags.HAPPY_GHAST_TEMPT_ITEMS).addTag(ItemTags.HAPPY_GHAST_FOOD).addTag(ItemTags.HARNESSES);
        this.tag(ItemTags.CAMEL_FOOD).add(new BlockItemId[]{BlockItemIds.CACTUS});
        this.tag(ItemTags.CAMEL_HUSK_FOOD).add(ItemIds.RABBIT_FOOT);
        this.tag(ItemTags.ARMADILLO_FOOD).add(ItemIds.SPIDER_EYE);
        this.tag(ItemTags.CHICKEN_FOOD)
            .add(
                BlockItemIds.WHEAT_CROP,
                BlockItemIds.MELON_CROP,
                BlockItemIds.PUMPKIN_CROP,
                BlockItemIds.BEETROOT_CROP,
                BlockItemIds.TORCHFLOWER_CROP,
                BlockItemIds.PITCHER_CROP
            );
        this.tag(ItemTags.FROG_FOOD).add(ItemIds.SLIME_BALL);
        this.tag(ItemTags.HOGLIN_FOOD).add(new BlockItemId[]{BlockItemIds.CRIMSON_FUNGUS});
        this.tag(ItemTags.LLAMA_FOOD).add(ItemIds.WHEAT).add(new BlockItemId[]{BlockItemIds.HAY_BLOCK});
        this.tag(ItemTags.LLAMA_TEMPT_ITEMS).add(new BlockItemId[]{BlockItemIds.HAY_BLOCK});
        this.tag(ItemTags.NAUTILUS_TAMING_ITEMS).add(ItemIds.PUFFERFISH_BUCKET, ItemIds.PUFFERFISH);
        this.tag(ItemTags.NAUTILUS_BUCKET_FOOD).add(ItemIds.PUFFERFISH_BUCKET, ItemIds.COD_BUCKET, ItemIds.SALMON_BUCKET, ItemIds.TROPICAL_FISH_BUCKET);
        this.tag(ItemTags.NAUTILUS_FOOD).addTag(ItemTags.FISHES).addTag(ItemTags.NAUTILUS_BUCKET_FOOD);
        this.tag(ItemTags.PANDA_FOOD).add(new BlockItemId[]{BlockItemIds.BAMBOO});
        this.tag(ItemTags.PANDA_EATS_FROM_GROUND).addTag(ItemTags.PANDA_FOOD).add(new BlockItemId[]{BlockItemIds.CAKE});
        this.tag(ItemTags.PIG_FOOD).add(BlockItemIds.CARROT_CROP, BlockItemIds.POTATO_CROP).add(ItemIds.BEETROOT);
        this.tag(ItemTags.RABBIT_FOOD)
            .add(new BlockItemId[]{BlockItemIds.CARROT_CROP})
            .add(ItemIds.GOLDEN_CARROT)
            .add(new BlockItemId[]{BlockItemIds.DANDELION});
        this.tag(ItemTags.STRIDER_FOOD).add(new BlockItemId[]{BlockItemIds.WARPED_FUNGUS});
        this.tag(ItemTags.STRIDER_TEMPT_ITEMS).addTag(ItemTags.STRIDER_FOOD).add(ItemIds.WARPED_FUNGUS_ON_A_STICK);
        this.tag(ItemTags.TURTLE_FOOD).add(new BlockItemId[]{BlockItemIds.SEAGRASS});
        this.tag(ItemTags.PARROT_FOOD)
            .add(new BlockItemId[]{BlockItemIds.WHEAT_CROP})
            .add(BlockItemIds.MELON_CROP, BlockItemIds.PUMPKIN_CROP, BlockItemIds.BEETROOT_CROP, BlockItemIds.TORCHFLOWER_CROP, BlockItemIds.PITCHER_CROP);
        this.tag(ItemTags.PARROT_POISONOUS_FOOD).add(ItemIds.COOKIE);
        this.tag(ItemTags.COW_FOOD).add(ItemIds.WHEAT);
        this.tag(ItemTags.SHEEP_FOOD).add(ItemIds.WHEAT);
        this.tag(ItemTags.SULFUR_CUBE_FOOD).add(ItemIds.SLIME_BALL);
        this.tag(ItemTags.GOAT_FOOD).add(ItemIds.WHEAT);
        this.tag(ItemTags.MAP_INVISIBILITY_EQUIPMENT).add(new BlockItemId[]{BlockItemIds.CARVED_PUMPKIN});
        this.tag(ItemTags.GAZE_DISGUISE_EQUIPMENT).add(new BlockItemId[]{BlockItemIds.CARVED_PUMPKIN});
        this.tag(ItemTags.SHEARABLE_FROM_COPPER_GOLEM).add(new BlockItemId[]{BlockItemIds.POPPY});
        this.tag(ItemTags.METAL_NUGGETS).add(ItemIds.COPPER_NUGGET, ItemIds.IRON_NUGGET, ItemIds.GOLD_NUGGET);
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_BOUNCY)
            .addTag(ItemTags.PLANKS)
            .add(new BlockItemId[]{BlockItemIds.BAMBOO_MOSAIC})
            .addTag(ItemTags.LOGS)
            .addTag(ItemTags.BAMBOO_BLOCKS);
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY)
            .add(BlockItemIds.AMETHYST_BLOCK, BlockItemIds.ANDESITE, BlockItemIds.BASALT, BlockItemIds.BLACKSTONE, BlockItemIds.BRICKS, BlockItemIds.CALCITE)
            .add(
                BlockItemIds.CHISELED_CINNABAR,
                BlockItemIds.CHISELED_DEEPSLATE,
                BlockItemIds.CHISELED_NETHER_BRICKS,
                BlockItemIds.CHISELED_POLISHED_BLACKSTONE,
                BlockItemIds.CHISELED_QUARTZ_BLOCK,
                BlockItemIds.CHISELED_RED_SANDSTONE,
                BlockItemIds.CHISELED_SANDSTONE,
                BlockItemIds.CHISELED_STONE_BRICKS,
                BlockItemIds.CHISELED_SULFUR,
                BlockItemIds.CHISELED_TUFF,
                BlockItemIds.CHISELED_TUFF_BRICKS
            )
            .add(BlockItemIds.CINNABAR, BlockItemIds.CINNABAR_BRICKS, BlockItemIds.COBBLED_DEEPSLATE, BlockItemIds.COBBLESTONE)
            .add(
                BlockItemIds.CRACKED_DEEPSLATE_BRICKS,
                BlockItemIds.CRACKED_DEEPSLATE_TILES,
                BlockItemIds.CRACKED_NETHER_BRICKS,
                BlockItemIds.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                BlockItemIds.CRACKED_STONE_BRICKS
            )
            .add(
                BlockItemIds.CRIMSON_NYLIUM,
                BlockItemIds.CRYING_OBSIDIAN,
                BlockItemIds.CUT_RED_SANDSTONE,
                BlockItemIds.CUT_SANDSTONE,
                BlockItemIds.DARK_PRISMARINE
            )
            .add(BlockItemIds.DEEPSLATE, BlockItemIds.DEEPSLATE_BRICKS, BlockItemIds.DEEPSLATE_TILES)
            .add(
                BlockItemIds.DIAMOND_BLOCK,
                BlockItemIds.DIORITE,
                BlockItemIds.DRIPSTONE_BLOCK,
                BlockItemIds.EMERALD_BLOCK,
                BlockItemIds.END_STONE,
                BlockItemIds.END_STONE_BRICKS,
                BlockItemIds.GILDED_BLACKSTONE,
                BlockItemIds.GLOWSTONE,
                BlockItemIds.GRANITE,
                BlockItemIds.LAPIS_BLOCK
            )
            .add(
                BlockItemIds.MOSSY_COBBLESTONE,
                BlockItemIds.MOSSY_STONE_BRICKS,
                BlockItemIds.MUD_BRICKS,
                BlockItemIds.NETHER_BRICKS,
                BlockItemIds.NETHERRACK,
                BlockItemIds.OBSERVER,
                BlockItemIds.OBSIDIAN
            )
            .add(
                BlockItemIds.POLISHED_ANDESITE,
                BlockItemIds.POLISHED_BASALT,
                BlockItemIds.POLISHED_BLACKSTONE,
                BlockItemIds.POLISHED_BLACKSTONE_BRICKS,
                BlockItemIds.POLISHED_CINNABAR,
                BlockItemIds.POLISHED_DEEPSLATE,
                BlockItemIds.POLISHED_DIORITE,
                BlockItemIds.POLISHED_GRANITE,
                BlockItemIds.POLISHED_SULFUR,
                BlockItemIds.POLISHED_TUFF
            )
            .add(
                BlockItemIds.PRISMARINE,
                BlockItemIds.PRISMARINE_BRICKS,
                BlockItemIds.PURPUR_BLOCK,
                BlockItemIds.PURPUR_PILLAR,
                BlockItemIds.QUARTZ_BLOCK,
                BlockItemIds.QUARTZ_BRICKS,
                BlockItemIds.NETHER_QUARTZ_ORE,
                BlockItemIds.QUARTZ_PILLAR
            )
            .add(BlockItemIds.RED_NETHER_BRICKS, BlockItemIds.RED_SANDSTONE, BlockItemIds.REDSTONE_LAMP, BlockItemIds.SANDSTONE, BlockItemIds.SEA_LANTERN)
            .add(
                BlockItemIds.SMOOTH_BASALT,
                BlockItemIds.SMOOTH_QUARTZ,
                BlockItemIds.SMOOTH_RED_SANDSTONE,
                BlockItemIds.SMOOTH_SANDSTONE,
                BlockItemIds.SMOOTH_STONE
            )
            .add(
                BlockItemIds.STONE,
                BlockItemIds.STONE_BRICKS,
                BlockItemIds.SULFUR,
                BlockItemIds.SULFUR_BRICKS,
                BlockItemIds.TUFF,
                BlockItemIds.TUFF_BRICKS,
                BlockItemIds.WARPED_NYLIUM
            )
            .addTag(ItemTags.CONCRETE)
            .addTag(ItemTags.COAL_ORES)
            .addTag(ItemTags.LAPIS_ORES)
            .addTag(ItemTags.REDSTONE_ORES)
            .addTag(ItemTags.DIAMOND_ORES)
            .addTag(ItemTags.EMERALD_ORES)
            .addTag(ItemTags.TERRACOTTA)
            .addTag(ItemTags.GLAZED_TERRACOTTA);
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_REGULAR)
            .addTag(ItemTags.CONCRETE_POWDERS)
            .add(BlockItemIds.MUD, BlockItemIds.MUDDY_MANGROVE_ROOTS, BlockItemIds.PACKED_MUD)
            .add(new BlockItemId[]{BlockItemIds.COAL_BLOCK})
            .add(BlockItemIds.DIRT, BlockItemIds.COARSE_DIRT, BlockItemIds.ROOTED_DIRT, BlockItemIds.PODZOL, BlockItemIds.GRASS_BLOCK, BlockItemIds.CLAY)
            .add(new BlockItemId[]{BlockItemIds.BONE_BLOCK});
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT)
            .add(BlockItemIds.IRON_BLOCK, BlockItemIds.GOLD_BLOCK, BlockItemIds.RAW_COPPER_BLOCK, BlockItemIds.RAW_GOLD_BLOCK, BlockItemIds.RAW_IRON_BLOCK)
            .addTag(ItemTags.GOLD_ORES)
            .addTag(ItemTags.IRON_ORES)
            .addTag(ItemTags.COPPER_ORES)
            .add(BlockItemIds.NETHERITE_BLOCK, BlockItemIds.ANCIENT_DEBRIS)
            .addAll(toIds(BlockItemIds.COPPER_BLOCK))
            .addAll(toIds(BlockItemIds.COPPER_BULB))
            .addAll(toIds(BlockItemIds.CUT_COPPER))
            .addAll(toIds(BlockItemIds.CHISELED_COPPER));
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_FLAT)
            .add(
                BlockItemIds.TUBE_CORAL_BLOCK,
                BlockItemIds.BRAIN_CORAL_BLOCK,
                BlockItemIds.BUBBLE_CORAL_BLOCK,
                BlockItemIds.FIRE_CORAL_BLOCK,
                BlockItemIds.HORN_CORAL_BLOCK
            )
            .add(
                BlockItemIds.DEAD_TUBE_CORAL_BLOCK,
                BlockItemIds.DEAD_BRAIN_CORAL_BLOCK,
                BlockItemIds.DEAD_BUBBLE_CORAL_BLOCK,
                BlockItemIds.DEAD_FIRE_CORAL_BLOCK,
                BlockItemIds.DEAD_HORN_CORAL_BLOCK
            )
            .add(BlockItemIds.SPONGE, BlockItemIds.WET_SPONGE, BlockItemIds.DRIED_KELP_BLOCK)
            .addTag(ItemTags.MOSS_BLOCKS)
            .add(BlockItemIds.RESIN_BLOCK, BlockItemIds.RESIN_BRICKS, BlockItemIds.CHISELED_RESIN_BRICKS)
            .add(BlockItemIds.MELON, BlockItemIds.HAY_BLOCK, BlockItemIds.PUMPKIN, BlockItemIds.CARVED_PUMPKIN, BlockItemIds.JACK_O_LANTERN)
            .add(BlockItemIds.OCHRE_FROGLIGHT, BlockItemIds.PEARLESCENT_FROGLIGHT, BlockItemIds.VERDANT_FROGLIGHT);
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_LIGHT).addTag(ItemTags.WOOL);
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING).add(BlockItemIds.BLUE_ICE, BlockItemIds.PACKED_ICE, BlockItemIds.SNOW_BLOCK);
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING)
            .add(BlockItemIds.BROWN_MUSHROOM_BLOCK, BlockItemIds.RED_MUSHROOM_BLOCK, BlockItemIds.MUSHROOM_STEM, BlockItemIds.MYCELIUM)
            .addTag(ItemTags.WART_BLOCKS)
            .add(new BlockItemId[]{BlockItemIds.SHROOMLIGHT});
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_STICKY).add(new BlockItemId[]{BlockItemIds.HONEYCOMB_BLOCK});
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE).add(BlockItemIds.SOUL_SAND, BlockItemIds.SOUL_SOIL);
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE).add(new BlockItemId[]{BlockItemIds.TNT});
        this.tag(ItemTags.SULFUR_CUBE_ARCHETYPE_HOT).add(new BlockItemId[]{BlockItemIds.MAGMA_BLOCK});
        this.tag(ItemTags.SULFUR_CUBE_SWALLOWABLE)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_BOUNCY)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_REGULAR)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_FLAT)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_LIGHT)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_STICKY)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_HOT)
            .addTag(ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY);
        this.tag(ItemTags.LOOM_DYES).addTag(ItemTags.DYES);
        this.tag(ItemTags.LOOM_PATTERNS)
            .add(
                ItemIds.FLOWER_BANNER_PATTERN,
                ItemIds.CREEPER_BANNER_PATTERN,
                ItemIds.SKULL_BANNER_PATTERN,
                ItemIds.MOJANG_BANNER_PATTERN,
                ItemIds.GLOBE_BANNER_PATTERN,
                ItemIds.PIGLIN_BANNER_PATTERN,
                ItemIds.FLOW_BANNER_PATTERN,
                ItemIds.GUSTER_BANNER_PATTERN,
                ItemIds.FIELD_MASONED_BANNER_PATTERN,
                ItemIds.BORDURE_INDENTED_BANNER_PATTERN
            );
        this.tag(ItemTags.CAT_COLLAR_DYES).addTag(ItemTags.DYES);
        this.tag(ItemTags.WOLF_COLLAR_DYES).addTag(ItemTags.DYES);
    }

    private static ColorCollection<ResourceKey<Item>> toIds(final ColorCollection<BlockItemId> ids) {
        return ids.map(BlockItemId::item);
    }

    private static WeatheringCopperCollection<ResourceKey<Item>> toIds(final WeatheringCopperCollection<BlockItemId> ids) {
        return ids.map(BlockItemId::item);
    }
}