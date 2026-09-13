package net.minecraft.client.data.models.model;

import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelTemplates {
   public static final ModelTemplate CUBE = create(
      "cube", TextureSlot.PARTICLE, TextureSlot.NORTH, TextureSlot.SOUTH, TextureSlot.EAST, TextureSlot.WEST, TextureSlot.UP, TextureSlot.DOWN
   );
   public static final ModelTemplate CUBE_DIRECTIONAL = create(
      "cube_directional", TextureSlot.PARTICLE, TextureSlot.NORTH, TextureSlot.SOUTH, TextureSlot.EAST, TextureSlot.WEST, TextureSlot.UP, TextureSlot.DOWN
   );
   public static final ModelTemplate CUBE_ALL = create("cube_all", TextureSlot.ALL);
   public static final ModelTemplate CUBE_ALL_INNER_FACES = create("cube_all_inner_faces", TextureSlot.ALL);
   public static final ModelTemplate CUBE_MIRRORED_ALL = create("cube_mirrored_all", "_mirrored", TextureSlot.ALL);
   public static final ModelTemplate CUBE_NORTH_WEST_MIRRORED_ALL = create("cube_north_west_mirrored_all", "_north_west_mirrored", TextureSlot.ALL);
   public static final ModelTemplate CUBE_COLUMN_UV_LOCKED_X = create("cube_column_uv_locked_x", "_x", TextureSlot.END, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_COLUMN_UV_LOCKED_Y = create("cube_column_uv_locked_y", "_y", TextureSlot.END, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_COLUMN_UV_LOCKED_Z = create("cube_column_uv_locked_z", "_z", TextureSlot.END, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_COLUMN = create("cube_column", TextureSlot.END, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_COLUMN_HORIZONTAL = create("cube_column_horizontal", "_horizontal", TextureSlot.END, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_COLUMN_MIRRORED = create("cube_column_mirrored", "_mirrored", TextureSlot.END, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_TOP = create("cube_top", TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_BOTTOM_TOP = create("cube_bottom_top", TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_BOTTOM_TOP_INNER_FACES = create("cube_bottom_top_inner_faces", TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_ORIENTABLE = create("orientable", TextureSlot.TOP, TextureSlot.FRONT, TextureSlot.SIDE);
   public static final ModelTemplate CUBE_ORIENTABLE_TOP_BOTTOM = create(
      "orientable_with_bottom", TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.FRONT
   );
   public static final ModelTemplate CUBE_ORIENTABLE_VERTICAL = create("orientable_vertical", "_vertical", TextureSlot.FRONT, TextureSlot.SIDE);
   public static final ModelTemplate BUTTON = create("button", TextureSlot.TEXTURE);
   public static final ModelTemplate BUTTON_PRESSED = create("button_pressed", "_pressed", TextureSlot.TEXTURE);
   public static final ModelTemplate BUTTON_INVENTORY = create("button_inventory", "_inventory", TextureSlot.TEXTURE);
   public static final ModelTemplate DOOR_BOTTOM_LEFT = create("door_bottom_left", "_bottom_left", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate DOOR_BOTTOM_LEFT_OPEN = create("door_bottom_left_open", "_bottom_left_open", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate DOOR_BOTTOM_RIGHT = create("door_bottom_right", "_bottom_right", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate DOOR_BOTTOM_RIGHT_OPEN = create("door_bottom_right_open", "_bottom_right_open", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate DOOR_TOP_LEFT = create("door_top_left", "_top_left", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate DOOR_TOP_LEFT_OPEN = create("door_top_left_open", "_top_left_open", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate DOOR_TOP_RIGHT = create("door_top_right", "_top_right", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate DOOR_TOP_RIGHT_OPEN = create("door_top_right_open", "_top_right_open", TextureSlot.TOP, TextureSlot.BOTTOM);
   public static final ModelTemplate CUSTOM_FENCE_POST = create("custom_fence_post", "_post", TextureSlot.TEXTURE, TextureSlot.PARTICLE);
   public static final ModelTemplate CUSTOM_FENCE_SIDE_NORTH = create("custom_fence_side_north", "_side_north", TextureSlot.TEXTURE);
   public static final ModelTemplate CUSTOM_FENCE_SIDE_EAST = create("custom_fence_side_east", "_side_east", TextureSlot.TEXTURE);
   public static final ModelTemplate CUSTOM_FENCE_SIDE_SOUTH = create("custom_fence_side_south", "_side_south", TextureSlot.TEXTURE);
   public static final ModelTemplate CUSTOM_FENCE_SIDE_WEST = create("custom_fence_side_west", "_side_west", TextureSlot.TEXTURE);
   public static final ModelTemplate CUSTOM_FENCE_INVENTORY = create("custom_fence_inventory", "_inventory", TextureSlot.TEXTURE);
   public static final ModelTemplate FENCE_POST = create("fence_post", "_post", TextureSlot.TEXTURE);
   public static final ModelTemplate FENCE_SIDE = create("fence_side", "_side", TextureSlot.TEXTURE);
   public static final ModelTemplate FENCE_INVENTORY = create("fence_inventory", "_inventory", TextureSlot.TEXTURE);
   public static final ModelTemplate WALL_POST = create("template_wall_post", "_post", TextureSlot.WALL);
   public static final ModelTemplate WALL_LOW_SIDE = create("template_wall_side", "_side", TextureSlot.WALL);
   public static final ModelTemplate WALL_TALL_SIDE = create("template_wall_side_tall", "_side_tall", TextureSlot.WALL);
   public static final ModelTemplate WALL_INVENTORY = create("wall_inventory", "_inventory", TextureSlot.WALL);
   public static final ModelTemplate CUSTOM_FENCE_GATE_CLOSED = create("template_custom_fence_gate", TextureSlot.TEXTURE, TextureSlot.PARTICLE);
   public static final ModelTemplate CUSTOM_FENCE_GATE_OPEN = create("template_custom_fence_gate_open", "_open", TextureSlot.TEXTURE, TextureSlot.PARTICLE);
   public static final ModelTemplate CUSTOM_FENCE_GATE_WALL_CLOSED = create(
      "template_custom_fence_gate_wall", "_wall", TextureSlot.TEXTURE, TextureSlot.PARTICLE
   );
   public static final ModelTemplate CUSTOM_FENCE_GATE_WALL_OPEN = create(
      "template_custom_fence_gate_wall_open", "_wall_open", TextureSlot.TEXTURE, TextureSlot.PARTICLE
   );
   public static final ModelTemplate FENCE_GATE_CLOSED = create("template_fence_gate", TextureSlot.TEXTURE);
   public static final ModelTemplate FENCE_GATE_OPEN = create("template_fence_gate_open", "_open", TextureSlot.TEXTURE);
   public static final ModelTemplate FENCE_GATE_WALL_CLOSED = create("template_fence_gate_wall", "_wall", TextureSlot.TEXTURE);
   public static final ModelTemplate FENCE_GATE_WALL_OPEN = create("template_fence_gate_wall_open", "_wall_open", TextureSlot.TEXTURE);
   public static final ModelTemplate PRESSURE_PLATE_UP = create("pressure_plate_up", TextureSlot.TEXTURE);
   public static final ModelTemplate PRESSURE_PLATE_DOWN = create("pressure_plate_down", "_down", TextureSlot.TEXTURE);
   public static final ModelTemplate PARTICLE_ONLY = create(TextureSlot.PARTICLE);
   public static final ModelTemplate SLAB_BOTTOM = create("slab", TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate SLAB_TOP = create("slab_top", "_top", TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate LEAVES = create("leaves", TextureSlot.ALL);
   public static final ModelTemplate STAIRS_STRAIGHT = create("stairs", TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate STAIRS_INNER = create("inner_stairs", "_inner", TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate STAIRS_OUTER = create("outer_stairs", "_outer", TextureSlot.BOTTOM, TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate TRAPDOOR_TOP = create("template_trapdoor_top", "_top", TextureSlot.TEXTURE);
   public static final ModelTemplate TRAPDOOR_BOTTOM = create("template_trapdoor_bottom", "_bottom", TextureSlot.TEXTURE);
   public static final ModelTemplate TRAPDOOR_OPEN = create("template_trapdoor_open", "_open", TextureSlot.TEXTURE);
   public static final ModelTemplate ORIENTABLE_TRAPDOOR_TOP = create("template_orientable_trapdoor_top", "_top", TextureSlot.TEXTURE);
   public static final ModelTemplate ORIENTABLE_TRAPDOOR_BOTTOM = create("template_orientable_trapdoor_bottom", "_bottom", TextureSlot.TEXTURE);
   public static final ModelTemplate ORIENTABLE_TRAPDOOR_OPEN = create("template_orientable_trapdoor_open", "_open", TextureSlot.TEXTURE);
   public static final ModelTemplate POINTED_DRIPSTONE = create("pointed_dripstone", TextureSlot.CROSS);
   public static final ModelTemplate CROSS = create("cross", TextureSlot.CROSS);
   public static final ModelTemplate TINTED_CROSS = create("tinted_cross", TextureSlot.CROSS);
   public static final ModelTemplate CROSS_EMISSIVE = create("cross_emissive", TextureSlot.CROSS, TextureSlot.CROSS_EMISSIVE);
   public static final ModelTemplate FLOWER_POT_CROSS = create("flower_pot_cross", TextureSlot.PLANT);
   public static final ModelTemplate TINTED_FLOWER_POT_CROSS = create("tinted_flower_pot_cross", TextureSlot.PLANT);
   public static final ModelTemplate FLOWER_POT_CROSS_EMISSIVE = create("flower_pot_cross_emissive", TextureSlot.PLANT, TextureSlot.CROSS_EMISSIVE);
   public static final ModelTemplate RAIL_FLAT = create("rail_flat", TextureSlot.RAIL);
   public static final ModelTemplate RAIL_CURVED = create("rail_curved", "_corner", TextureSlot.RAIL);
   public static final ModelTemplate RAIL_RAISED_NE = create("template_rail_raised_ne", "_raised_ne", TextureSlot.RAIL);
   public static final ModelTemplate RAIL_RAISED_SW = create("template_rail_raised_sw", "_raised_sw", TextureSlot.RAIL);
   public static final ModelTemplate CARPET = create("carpet", TextureSlot.WOOL);
   public static final ModelTemplate MOSSY_CARPET_SIDE = create("mossy_carpet_side", TextureSlot.SIDE);
   public static final ModelTemplate FLOWERBED_1 = create("flowerbed_1", "_1", TextureSlot.FLOWERBED, TextureSlot.STEM);
   public static final ModelTemplate FLOWERBED_2 = create("flowerbed_2", "_2", TextureSlot.FLOWERBED, TextureSlot.STEM);
   public static final ModelTemplate FLOWERBED_3 = create("flowerbed_3", "_3", TextureSlot.FLOWERBED, TextureSlot.STEM);
   public static final ModelTemplate FLOWERBED_4 = create("flowerbed_4", "_4", TextureSlot.FLOWERBED, TextureSlot.STEM);
   public static final ModelTemplate LEAF_LITTER_1 = create("template_leaf_litter_1", "_1", TextureSlot.TEXTURE);
   public static final ModelTemplate LEAF_LITTER_2 = create("template_leaf_litter_2", "_2", TextureSlot.TEXTURE);
   public static final ModelTemplate LEAF_LITTER_3 = create("template_leaf_litter_3", "_3", TextureSlot.TEXTURE);
   public static final ModelTemplate LEAF_LITTER_4 = create("template_leaf_litter_4", "_4", TextureSlot.TEXTURE);
   public static final ModelTemplate CORAL_FAN = create("coral_fan", TextureSlot.FAN);
   public static final ModelTemplate CORAL_WALL_FAN = create("coral_wall_fan", TextureSlot.FAN);
   public static final ModelTemplate GLAZED_TERRACOTTA = create("template_glazed_terracotta", TextureSlot.PATTERN);
   public static final ModelTemplate CHORUS_FLOWER = create("template_chorus_flower", TextureSlot.TEXTURE);
   public static final ModelTemplate DAYLIGHT_DETECTOR = create("template_daylight_detector", TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate STAINED_GLASS_PANE_NOSIDE = create("template_glass_pane_noside", "_noside", TextureSlot.PANE);
   public static final ModelTemplate STAINED_GLASS_PANE_NOSIDE_ALT = create("template_glass_pane_noside_alt", "_noside_alt", TextureSlot.PANE);
   public static final ModelTemplate STAINED_GLASS_PANE_POST = create("template_glass_pane_post", "_post", TextureSlot.PANE, TextureSlot.EDGE);
   public static final ModelTemplate STAINED_GLASS_PANE_SIDE = create("template_glass_pane_side", "_side", TextureSlot.PANE, TextureSlot.EDGE);
   public static final ModelTemplate STAINED_GLASS_PANE_SIDE_ALT = create("template_glass_pane_side_alt", "_side_alt", TextureSlot.PANE, TextureSlot.EDGE);
   public static final ModelTemplate COMMAND_BLOCK = create("template_command_block", TextureSlot.FRONT, TextureSlot.BACK, TextureSlot.SIDE);
   public static final ModelTemplate CHISELED_BOOKSHELF_SLOT_TOP_LEFT = create(
      "template_chiseled_bookshelf_slot_top_left", "_slot_top_left", TextureSlot.TEXTURE
   );
   public static final ModelTemplate CHISELED_BOOKSHELF_SLOT_TOP_MID = create("template_chiseled_bookshelf_slot_top_mid", "_slot_top_mid", TextureSlot.TEXTURE);
   public static final ModelTemplate CHISELED_BOOKSHELF_SLOT_TOP_RIGHT = create(
      "template_chiseled_bookshelf_slot_top_right", "_slot_top_right", TextureSlot.TEXTURE
   );
   public static final ModelTemplate CHISELED_BOOKSHELF_SLOT_BOTTOM_LEFT = create(
      "template_chiseled_bookshelf_slot_bottom_left", "_slot_bottom_left", TextureSlot.TEXTURE
   );
   public static final ModelTemplate CHISELED_BOOKSHELF_SLOT_BOTTOM_MID = create(
      "template_chiseled_bookshelf_slot_bottom_mid", "_slot_bottom_mid", TextureSlot.TEXTURE
   );
   public static final ModelTemplate CHISELED_BOOKSHELF_SLOT_BOTTOM_RIGHT = create(
      "template_chiseled_bookshelf_slot_bottom_right", "_slot_bottom_right", TextureSlot.TEXTURE
   );
   public static final ModelTemplate SHELF_BODY = create("template_shelf_body", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate SHELF_INVENTORY = create("template_shelf_inventory", "_inventory", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate SHELF_UNPOWERED = create("template_shelf_unpowered", "_unpowered", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate SHELF_UNCONNECTED = create("template_shelf_unconnected", "_unconnected", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate SHELF_LEFT = create("template_shelf_left", "_left", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate SHELF_CENTER = create("template_shelf_center", "_center", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate SHELF_RIGHT = create("template_shelf_right", "_right", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate ANVIL = create("template_anvil", TextureSlot.TOP);
   public static final ModelTemplate[] STEMS = IntStream.range(0, 8)
      .mapToObj(p_377726_ -> create("stem_growth" + p_377726_, "_stage" + p_377726_, TextureSlot.STEM))
      .toArray(ModelTemplate[]::new);
   public static final ModelTemplate ATTACHED_STEM = create("stem_fruit", TextureSlot.STEM, TextureSlot.UPPER_STEM);
   public static final ModelTemplate CROP = create("crop", TextureSlot.CROP);
   public static final ModelTemplate FARMLAND = create("template_farmland", TextureSlot.DIRT, TextureSlot.TOP);
   public static final ModelTemplate FIRE_FLOOR = create("template_fire_floor", TextureSlot.FIRE);
   public static final ModelTemplate FIRE_SIDE = create("template_fire_side", TextureSlot.FIRE);
   public static final ModelTemplate FIRE_SIDE_ALT = create("template_fire_side_alt", TextureSlot.FIRE);
   public static final ModelTemplate FIRE_UP = create("template_fire_up", TextureSlot.FIRE);
   public static final ModelTemplate FIRE_UP_ALT = create("template_fire_up_alt", TextureSlot.FIRE);
   public static final ModelTemplate CAMPFIRE = create("template_campfire", TextureSlot.FIRE, TextureSlot.LIT_LOG);
   public static final ModelTemplate LANTERN = create("template_lantern", TextureSlot.LANTERN);
   public static final ModelTemplate HANGING_LANTERN = create("template_hanging_lantern", "_hanging", TextureSlot.LANTERN);
   public static final ModelTemplate CHAIN = create("template_chain", TextureSlot.TEXTURE);
   public static final ModelTemplate BARS_CAP = create("template_bars_cap", "_cap", TextureSlot.BARS, TextureSlot.EDGE);
   public static final ModelTemplate BARS_CAP_ALT = create("template_bars_cap_alt", "_cap_alt", TextureSlot.BARS, TextureSlot.EDGE);
   public static final ModelTemplate BARS_POST = create("template_bars_post", "_post", TextureSlot.BARS, TextureSlot.EDGE);
   public static final ModelTemplate BARS_POST_ENDS = create("template_bars_post_ends", "_post_ends", TextureSlot.BARS, TextureSlot.EDGE);
   public static final ModelTemplate BARS_POST_SIDE = create("template_bars_side", "_side", TextureSlot.BARS, TextureSlot.EDGE);
   public static final ModelTemplate BARS_POST_SIDE_ALT = create("template_bars_side_alt", "_side_alt", TextureSlot.BARS, TextureSlot.EDGE);
   public static final ModelTemplate TORCH = create("template_torch", TextureSlot.TORCH);
   public static final ModelTemplate TORCH_UNLIT = create("template_torch_unlit", TextureSlot.TORCH);
   public static final ModelTemplate WALL_TORCH = create("template_torch_wall", TextureSlot.TORCH);
   public static final ModelTemplate WALL_TORCH_UNLIT = create("template_torch_wall_unlit", TextureSlot.TORCH);
   public static final ModelTemplate REDSTONE_TORCH = create("template_redstone_torch", TextureSlot.TORCH);
   public static final ModelTemplate REDSTONE_WALL_TORCH = create("template_redstone_torch_wall", TextureSlot.TORCH);
   public static final ModelTemplate PISTON = create("template_piston", TextureSlot.PLATFORM, TextureSlot.BOTTOM, TextureSlot.SIDE);
   public static final ModelTemplate PISTON_HEAD = create("template_piston_head", TextureSlot.PLATFORM, TextureSlot.SIDE, TextureSlot.UNSTICKY);
   public static final ModelTemplate PISTON_HEAD_SHORT = create("template_piston_head_short", TextureSlot.PLATFORM, TextureSlot.SIDE, TextureSlot.UNSTICKY);
   public static final ModelTemplate SEAGRASS = create("template_seagrass", TextureSlot.TEXTURE);
   public static final ModelTemplate TURTLE_EGG = create("template_turtle_egg", TextureSlot.ALL);
   public static final ModelTemplate DRIED_GHAST = create(
      "dried_ghast",
      TextureSlot.PARTICLE,
      TextureSlot.TOP,
      TextureSlot.BOTTOM,
      TextureSlot.NORTH,
      TextureSlot.SOUTH,
      TextureSlot.EAST,
      TextureSlot.WEST,
      TextureSlot.TENTACLES
   );
   public static final ModelTemplate TWO_TURTLE_EGGS = create("template_two_turtle_eggs", TextureSlot.ALL);
   public static final ModelTemplate THREE_TURTLE_EGGS = create("template_three_turtle_eggs", TextureSlot.ALL);
   public static final ModelTemplate FOUR_TURTLE_EGGS = create("template_four_turtle_eggs", TextureSlot.ALL);
   public static final ModelTemplate SINGLE_FACE = create("template_single_face", TextureSlot.TEXTURE);
   public static final ModelTemplate CAULDRON_LEVEL1 = create(
      "template_cauldron_level1", TextureSlot.CONTENT, TextureSlot.INSIDE, TextureSlot.PARTICLE, TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE
   );
   public static final ModelTemplate CAULDRON_LEVEL2 = create(
      "template_cauldron_level2", TextureSlot.CONTENT, TextureSlot.INSIDE, TextureSlot.PARTICLE, TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE
   );
   public static final ModelTemplate CAULDRON_FULL = create(
      "template_cauldron_full", TextureSlot.CONTENT, TextureSlot.INSIDE, TextureSlot.PARTICLE, TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE
   );
   public static final ModelTemplate AZALEA = create("template_azalea", TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate POTTED_AZALEA = create("template_potted_azalea_bush", TextureSlot.PLANT, TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate POTTED_FLOWERING_AZALEA = create("template_potted_azalea_bush", TextureSlot.PLANT, TextureSlot.TOP, TextureSlot.SIDE);
   public static final ModelTemplate SNIFFER_EGG = create(
      "sniffer_egg", TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.NORTH, TextureSlot.SOUTH, TextureSlot.EAST, TextureSlot.WEST
   );
   public static final ModelTemplate FLAT_ITEM = createItem("generated", TextureSlot.LAYER0);
   public static final ModelTemplate MUSIC_DISC = createItem("template_music_disc", TextureSlot.LAYER0);
   public static final ModelTemplate FLAT_HANDHELD_ITEM = createItem("handheld", TextureSlot.LAYER0);
   public static final ModelTemplate FLAT_HANDHELD_ROD_ITEM = createItem("handheld_rod", TextureSlot.LAYER0);
   public static final ModelTemplate TWO_LAYERED_ITEM = createItem("generated", TextureSlot.LAYER0, TextureSlot.LAYER1);
   public static final ModelTemplate THREE_LAYERED_ITEM = createItem("generated", TextureSlot.LAYER0, TextureSlot.LAYER1, TextureSlot.LAYER2);
   public static final ModelTemplate SHULKER_BOX_INVENTORY = createItem("template_shulker_box", TextureSlot.PARTICLE);
   public static final ModelTemplate BED_INVENTORY = createItem("template_bed", TextureSlot.PARTICLE);
   public static final ModelTemplate CHEST_INVENTORY = createItem("template_chest", TextureSlot.PARTICLE);
   public static final ModelTemplate BUNDLE_OPEN_FRONT_INVENTORY = createItem("template_bundle_open_front", "_open_front", TextureSlot.LAYER0);
   public static final ModelTemplate BUNDLE_OPEN_BACK_INVENTORY = createItem("template_bundle_open_back", "_open_back", TextureSlot.LAYER0);
   public static final ModelTemplate BOW = createItem("bow", TextureSlot.LAYER0);
   public static final ModelTemplate CROSSBOW = createItem("crossbow", TextureSlot.LAYER0);
   public static final ModelTemplate SPEAR_IN_HAND = createItem("spear_in_hand", "_in_hand", TextureSlot.LAYER0);
   public static final ModelTemplate CANDLE = create("template_candle", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate TWO_CANDLES = create("template_two_candles", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate THREE_CANDLES = create("template_three_candles", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate FOUR_CANDLES = create("template_four_candles", TextureSlot.ALL, TextureSlot.PARTICLE);
   public static final ModelTemplate CANDLE_CAKE = create(
      "template_cake_with_candle", TextureSlot.CANDLE, TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.PARTICLE
   );
   public static final ModelTemplate SCULK_SHRIEKER = create(
      "template_sculk_shrieker", TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.PARTICLE, TextureSlot.INNER_TOP
   );
   public static final ModelTemplate VAULT = create("template_vault", TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.FRONT);
   public static final ModelTemplate FLAT_HANDHELD_MACE_ITEM = createItem("handheld_mace", TextureSlot.LAYER0);
   public static final ModelTemplate LIGHTNING_ROD = create("template_lightning_rod", TextureSlot.TEXTURE);

   private static ModelTemplate create(TextureSlot... p_375982_) {
      return new ModelTemplate(Optional.empty(), Optional.empty(), p_375982_);
   }

   private static ModelTemplate create(String p_378022_, TextureSlot... p_377561_) {
      return new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/" + p_378022_)), Optional.empty(), p_377561_);
   }

   private static ModelTemplate createItem(String p_378751_, TextureSlot... p_377825_) {
      return new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("item/" + p_378751_)), Optional.empty(), p_377825_);
   }

   private static ModelTemplate createItem(String p_378296_, String p_376935_, TextureSlot... p_378333_) {
      return new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("item/" + p_378296_)), Optional.of(p_376935_), p_378333_);
   }

   private static ModelTemplate create(String p_376051_, String p_377115_, TextureSlot... p_376137_) {
      return new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/" + p_376051_)), Optional.of(p_377115_), p_376137_);
   }
}
