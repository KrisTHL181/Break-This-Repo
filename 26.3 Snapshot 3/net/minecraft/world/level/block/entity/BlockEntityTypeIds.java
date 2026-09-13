package net.minecraft.world.level.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class BlockEntityTypeIds {
   public static final ResourceKey<BlockEntityType<?>> FURNACE = create("furnace");
   public static final ResourceKey<BlockEntityType<?>> CHEST = create("chest");
   public static final ResourceKey<BlockEntityType<?>> TRAPPED_CHEST = create("trapped_chest");
   public static final ResourceKey<BlockEntityType<?>> ENDER_CHEST = create("ender_chest");
   public static final ResourceKey<BlockEntityType<?>> JUKEBOX = create("jukebox");
   public static final ResourceKey<BlockEntityType<?>> DISPENSER = create("dispenser");
   public static final ResourceKey<BlockEntityType<?>> DROPPER = create("dropper");
   public static final ResourceKey<BlockEntityType<?>> SIGN = create("sign");
   public static final ResourceKey<BlockEntityType<?>> HANGING_SIGN = create("hanging_sign");
   public static final ResourceKey<BlockEntityType<?>> MOB_SPAWNER = create("mob_spawner");
   public static final ResourceKey<BlockEntityType<?>> CREAKING_HEART = create("creaking_heart");
   public static final ResourceKey<BlockEntityType<?>> PISTON = create("piston");
   public static final ResourceKey<BlockEntityType<?>> BREWING_STAND = create("brewing_stand");
   public static final ResourceKey<BlockEntityType<?>> ENCHANTING_TABLE = create("enchanting_table");
   public static final ResourceKey<BlockEntityType<?>> END_PORTAL = create("end_portal");
   public static final ResourceKey<BlockEntityType<?>> BEACON = create("beacon");
   public static final ResourceKey<BlockEntityType<?>> SKULL = create("skull");
   public static final ResourceKey<BlockEntityType<?>> DAYLIGHT_DETECTOR = create("daylight_detector");
   public static final ResourceKey<BlockEntityType<?>> HOPPER = create("hopper");
   public static final ResourceKey<BlockEntityType<?>> COMPARATOR = create("comparator");
   public static final ResourceKey<BlockEntityType<?>> BANNER = create("banner");
   public static final ResourceKey<BlockEntityType<?>> STRUCTURE_BLOCK = create("structure_block");
   public static final ResourceKey<BlockEntityType<?>> END_GATEWAY = create("end_gateway");
   public static final ResourceKey<BlockEntityType<?>> COMMAND_BLOCK = create("command_block");
   public static final ResourceKey<BlockEntityType<?>> SHULKER_BOX = create("shulker_box");
   public static final ResourceKey<BlockEntityType<?>> CONDUIT = create("conduit");
   public static final ResourceKey<BlockEntityType<?>> BARREL = create("barrel");
   public static final ResourceKey<BlockEntityType<?>> SMOKER = create("smoker");
   public static final ResourceKey<BlockEntityType<?>> BLAST_FURNACE = create("blast_furnace");
   public static final ResourceKey<BlockEntityType<?>> LECTERN = create("lectern");
   public static final ResourceKey<BlockEntityType<?>> BELL = create("bell");
   public static final ResourceKey<BlockEntityType<?>> JIGSAW = create("jigsaw");
   public static final ResourceKey<BlockEntityType<?>> CAMPFIRE = create("campfire");
   public static final ResourceKey<BlockEntityType<?>> BEEHIVE = create("beehive");
   public static final ResourceKey<BlockEntityType<?>> SCULK_SENSOR = create("sculk_sensor");
   public static final ResourceKey<BlockEntityType<?>> CALIBRATED_SCULK_SENSOR = create("calibrated_sculk_sensor");
   public static final ResourceKey<BlockEntityType<?>> SCULK_CATALYST = create("sculk_catalyst");
   public static final ResourceKey<BlockEntityType<?>> SCULK_SHRIEKER = create("sculk_shrieker");
   public static final ResourceKey<BlockEntityType<?>> CHISELED_BOOKSHELF = create("chiseled_bookshelf");
   public static final ResourceKey<BlockEntityType<?>> SHELF = create("shelf");
   public static final ResourceKey<BlockEntityType<?>> BRUSHABLE_BLOCK = create("brushable_block");
   public static final ResourceKey<BlockEntityType<?>> DECORATED_POT = create("decorated_pot");
   public static final ResourceKey<BlockEntityType<?>> CRAFTER = create("crafter");
   public static final ResourceKey<BlockEntityType<?>> TRIAL_SPAWNER = create("trial_spawner");
   public static final ResourceKey<BlockEntityType<?>> VAULT = create("vault");
   public static final ResourceKey<BlockEntityType<?>> TEST_BLOCK = create("test_block");
   public static final ResourceKey<BlockEntityType<?>> TEST_INSTANCE_BLOCK = create("test_instance_block");
   public static final ResourceKey<BlockEntityType<?>> COPPER_GOLEM_STATUE = create("copper_golem_statue");
   public static final ResourceKey<BlockEntityType<?>> POTENT_SULFUR = create("potent_sulfur");

   private static ResourceKey<BlockEntityType<?>> create(final String name) {
      return ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.withDefaultNamespace(name));
   }
}
