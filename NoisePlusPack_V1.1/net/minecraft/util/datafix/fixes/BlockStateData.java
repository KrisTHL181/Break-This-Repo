package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.nbt.NbtOps;
import org.jspecify.annotations.Nullable;

public class BlockStateData {
   private static final @Nullable Dynamic<?>[] MAP = new Dynamic[4096];
   private static final @Nullable Dynamic<?>[] BLOCK_DEFAULTS = new Dynamic[256];
   private static final Object2IntMap<Dynamic<?>> ID_BY_OLD = (Object2IntMap<Dynamic<?>>)DataFixUtils.make(
      new Object2IntOpenHashMap(), p_14955_ -> p_14955_.defaultReturnValue(-1)
   );
   private static final Object2IntMap<String> ID_BY_OLD_NAME = (Object2IntMap<String>)DataFixUtils.make(
      new Object2IntOpenHashMap(), p_14949_ -> p_14949_.defaultReturnValue(-1)
   );
   static final String FILTER_ME = "%%FILTER_ME%%";
   private static final String TAG_NAME = "Name";
   private static final String TAG_PROPERTIES = "Properties";
   private static final Map<String, String> AGE_0 = Map.of("age", "0");
   private static final Map<String, String> AGE_0_FACING_EAST = Map.of("age", "0", "facing", "east");
   private static final Map<String, String> AGE_0_FACING_NORTH = Map.of("age", "0", "facing", "north");
   private static final Map<String, String> AGE_0_FACING_SOUTH = Map.of("age", "0", "facing", "south");
   private static final Map<String, String> AGE_0_FACING_WEST = Map.of("age", "0", "facing", "west");
   private static final Map<String, String> AGE_1 = Map.of("age", "1");
   private static final Map<String, String> AGE_10 = Map.of("age", "10");
   private static final Map<String, String> AGE_11 = Map.of("age", "11");
   private static final Map<String, String> AGE_12 = Map.of("age", "12");
   private static final Map<String, String> AGE_13 = Map.of("age", "13");
   private static final Map<String, String> AGE_14 = Map.of("age", "14");
   private static final Map<String, String> AGE_15 = Map.of("age", "15");
   private static final Map<String, String> AGE_1_FACING_EAST = Map.of("age", "1", "facing", "east");
   private static final Map<String, String> AGE_1_FACING_NORTH = Map.of("age", "1", "facing", "north");
   private static final Map<String, String> AGE_1_FACING_SOUTH = Map.of("age", "1", "facing", "south");
   private static final Map<String, String> AGE_1_FACING_WEST = Map.of("age", "1", "facing", "west");
   private static final Map<String, String> AGE_2 = Map.of("age", "2");
   private static final Map<String, String> AGE_2_FACING_EAST = Map.of("age", "2", "facing", "east");
   private static final Map<String, String> AGE_2_FACING_NORTH = Map.of("age", "2", "facing", "north");
   private static final Map<String, String> AGE_2_FACING_SOUTH = Map.of("age", "2", "facing", "south");
   private static final Map<String, String> AGE_2_FACING_WEST = Map.of("age", "2", "facing", "west");
   private static final Map<String, String> AGE_3 = Map.of("age", "3");
   private static final Map<String, String> AGE_4 = Map.of("age", "4");
   private static final Map<String, String> AGE_5 = Map.of("age", "5");
   private static final Map<String, String> AGE_6 = Map.of("age", "6");
   private static final Map<String, String> AGE_7 = Map.of("age", "7");
   private static final Map<String, String> AGE_8 = Map.of("age", "8");
   private static final Map<String, String> AGE_9 = Map.of("age", "9");
   private static final Map<String, String> AXIS_X = Map.of("axis", "x");
   private static final Map<String, String> AXIS_Y = Map.of("axis", "y");
   private static final Map<String, String> AXIS_Z = Map.of("axis", "z");
   private static final Map<String, String> CHECK_DECAY_FALSE_DECAYABLE_FALSE = Map.of("check_decay", "false", "decayable", "false");
   private static final Map<String, String> CHECK_DECAY_FALSE_DECAYABLE_TRUE = Map.of("check_decay", "false", "decayable", "true");
   private static final Map<String, String> CHECK_DECAY_TRUE_DECAYABLE_FALSE = Map.of("check_decay", "true", "decayable", "false");
   private static final Map<String, String> CHECK_DECAY_TRUE_DECAYABLE_TRUE = Map.of("check_decay", "true", "decayable", "true");
   private static final Map<String, String> COLOR_BLACK = Map.of("color", "black");
   private static final Map<String, String> COLOR_BLUE = Map.of("color", "blue");
   private static final Map<String, String> COLOR_BROWN = Map.of("color", "brown");
   private static final Map<String, String> COLOR_CYAN = Map.of("color", "cyan");
   private static final Map<String, String> COLOR_GRAY = Map.of("color", "gray");
   private static final Map<String, String> COLOR_GREEN = Map.of("color", "green");
   private static final Map<String, String> COLOR_LIGHT_BLUE = Map.of("color", "light_blue");
   private static final Map<String, String> COLOR_LIME = Map.of("color", "lime");
   private static final Map<String, String> COLOR_MAGENTA = Map.of("color", "magenta");
   private static final Map<String, String> COLOR_ORANGE = Map.of("color", "orange");
   private static final Map<String, String> COLOR_PINK = Map.of("color", "pink");
   private static final Map<String, String> COLOR_PURPLE = Map.of("color", "purple");
   private static final Map<String, String> COLOR_RED = Map.of("color", "red");
   private static final Map<String, String> COLOR_SILVER = Map.of("color", "silver");
   private static final Map<String, String> COLOR_WHITE = Map.of("color", "white");
   private static final Map<String, String> COLOR_YELLOW = Map.of("color", "yellow");
   private static final Map<String, String> EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE = Map.of(
      "attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE = Map.of(
      "attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE = Map.of(
      "attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE = Map.of(
      "attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE = Map.of(
      "attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE = Map.of(
      "attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE = Map.of(
      "attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> FACE_CEILING_POWERED_FALSE_FACING_NORTH = Map.of("face", "ceiling", "facing", "north", "powered", "false");
   private static final Map<String, String> FACE_CEILING_POWERED_TRUE_FACING_NORTH = Map.of("face", "ceiling", "facing", "north", "powered", "true");
   private static final Map<String, String> FACE_FLOOR_POWERED_FALSE_FACING_NORTH = Map.of("face", "floor", "facing", "north", "powered", "false");
   private static final Map<String, String> FACE_FLOOR_POWERED_TRUE_FACING_NORTH = Map.of("face", "floor", "facing", "north", "powered", "true");
   private static final Map<String, String> FACE_WALL_POWERED_FALSE_FACING_EAST = Map.of("face", "wall", "facing", "east", "powered", "false");
   private static final Map<String, String> FACE_WALL_POWERED_FALSE_FACING_NORTH = Map.of("face", "wall", "facing", "north", "powered", "false");
   private static final Map<String, String> FACE_WALL_POWERED_FALSE_FACING_SOUTH = Map.of("face", "wall", "facing", "south", "powered", "false");
   private static final Map<String, String> FACE_WALL_POWERED_FALSE_FACING_WEST = Map.of("face", "wall", "facing", "west", "powered", "false");
   private static final Map<String, String> FACE_WALL_POWERED_TRUE_FACING_EAST = Map.of("face", "wall", "facing", "east", "powered", "true");
   private static final Map<String, String> FACE_WALL_POWERED_TRUE_FACING_NORTH = Map.of("face", "wall", "facing", "north", "powered", "true");
   private static final Map<String, String> FACE_WALL_POWERED_TRUE_FACING_SOUTH = Map.of("face", "wall", "facing", "south", "powered", "true");
   private static final Map<String, String> FACE_WALL_POWERED_TRUE_FACING_WEST = Map.of("face", "wall", "facing", "west", "powered", "true");
   private static final Map<String, String> FACING_DOWN = Map.of("facing", "down");
   private static final Map<String, String> FACING_DOWN_CONDITIONAL_FALSE = Map.of("conditional", "false", "facing", "down");
   private static final Map<String, String> FACING_DOWN_CONDITIONAL_TRUE = Map.of("conditional", "true", "facing", "down");
   private static final Map<String, String> FACING_DOWN_EXTENDED_FALSE = Map.of("extended", "false", "facing", "down");
   private static final Map<String, String> FACING_DOWN_EXTENDED_TRUE = Map.of("extended", "true", "facing", "down");
   private static final Map<String, String> FACING_DOWN_POWERED_FALSE = Map.of("facing", "down", "powered", "false");
   private static final Map<String, String> FACING_DOWN_POWERED_TRUE = Map.of("facing", "down", "powered", "true");
   private static final Map<String, String> FACING_EAST = Map.of("facing", "east");
   private static final Map<String, String> FACING_EAST_CONDITIONAL_FALSE = Map.of("conditional", "false", "facing", "east");
   private static final Map<String, String> FACING_EAST_CONDITIONAL_TRUE = Map.of("conditional", "true", "facing", "east");
   private static final Map<String, String> FACING_EAST_EXTENDED_FALSE = Map.of("extended", "false", "facing", "east");
   private static final Map<String, String> FACING_EAST_EXTENDED_TRUE = Map.of("extended", "true", "facing", "east");
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_EAST_POWERED_FALSE = Map.of("facing", "east", "powered", "false");
   private static final Map<String, String> FACING_EAST_POWERED_TRUE = Map.of("facing", "east", "powered", "true");
   private static final Map<String, String> FACING_NORTH = Map.of("facing", "north");
   private static final Map<String, String> FACING_NORTH_CONDITIONAL_FALSE = Map.of("conditional", "false", "facing", "north");
   private static final Map<String, String> FACING_NORTH_CONDITIONAL_TRUE = Map.of("conditional", "true", "facing", "north");
   private static final Map<String, String> FACING_NORTH_EXTENDED_FALSE = Map.of("extended", "false", "facing", "north");
   private static final Map<String, String> FACING_NORTH_EXTENDED_TRUE = Map.of("extended", "true", "facing", "north");
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_NORTH_POWERED_FALSE = Map.of("facing", "north", "powered", "false");
   private static final Map<String, String> FACING_NORTH_POWERED_TRUE = Map.of("facing", "north", "powered", "true");
   private static final Map<String, String> FACING_SOUTH = Map.of("facing", "south");
   private static final Map<String, String> FACING_SOUTH_CONDITIONAL_FALSE = Map.of("conditional", "false", "facing", "south");
   private static final Map<String, String> FACING_SOUTH_CONDITIONAL_TRUE = Map.of("conditional", "true", "facing", "south");
   private static final Map<String, String> FACING_SOUTH_EXTENDED_FALSE = Map.of("extended", "false", "facing", "south");
   private static final Map<String, String> FACING_SOUTH_EXTENDED_TRUE = Map.of("extended", "true", "facing", "south");
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_SOUTH_POWERED_FALSE = Map.of("facing", "south", "powered", "false");
   private static final Map<String, String> FACING_SOUTH_POWERED_TRUE = Map.of("facing", "south", "powered", "true");
   private static final Map<String, String> FACING_UP = Map.of("facing", "up");
   private static final Map<String, String> FACING_UP_CONDITIONAL_FALSE = Map.of("conditional", "false", "facing", "up");
   private static final Map<String, String> FACING_UP_CONDITIONAL_TRUE = Map.of("conditional", "true", "facing", "up");
   private static final Map<String, String> FACING_UP_EXTENDED_FALSE = Map.of("extended", "false", "facing", "up");
   private static final Map<String, String> FACING_UP_EXTENDED_TRUE = Map.of("extended", "true", "facing", "up");
   private static final Map<String, String> FACING_UP_POWERED_FALSE = Map.of("facing", "up", "powered", "false");
   private static final Map<String, String> FACING_UP_POWERED_TRUE = Map.of("facing", "up", "powered", "true");
   private static final Map<String, String> FACING_WEST = Map.of("facing", "west");
   private static final Map<String, String> FACING_WEST_CONDITIONAL_FALSE = Map.of("conditional", "false", "facing", "west");
   private static final Map<String, String> FACING_WEST_CONDITIONAL_TRUE = Map.of("conditional", "true", "facing", "west");
   private static final Map<String, String> FACING_WEST_EXTENDED_FALSE = Map.of("extended", "false", "facing", "west");
   private static final Map<String, String> FACING_WEST_EXTENDED_TRUE = Map.of("extended", "true", "facing", "west");
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "false"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER = Map.of(
      "facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER = Map.of(
      "facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "true"
   );
   private static final Map<String, String> FACING_WEST_POWERED_FALSE = Map.of("facing", "west", "powered", "false");
   private static final Map<String, String> FACING_WEST_POWERED_TRUE = Map.of("facing", "west", "powered", "true");
   private static final Map<String, String> HALF_BOTTOM_OPEN_FALSE_FACING_EAST = Map.of("facing", "east", "half", "bottom", "open", "false");
   private static final Map<String, String> HALF_BOTTOM_OPEN_FALSE_FACING_NORTH = Map.of("facing", "north", "half", "bottom", "open", "false");
   private static final Map<String, String> HALF_BOTTOM_OPEN_FALSE_FACING_SOUTH = Map.of("facing", "south", "half", "bottom", "open", "false");
   private static final Map<String, String> HALF_BOTTOM_OPEN_FALSE_FACING_WEST = Map.of("facing", "west", "half", "bottom", "open", "false");
   private static final Map<String, String> HALF_BOTTOM_OPEN_TRUE_FACING_EAST = Map.of("facing", "east", "half", "bottom", "open", "true");
   private static final Map<String, String> HALF_BOTTOM_OPEN_TRUE_FACING_NORTH = Map.of("facing", "north", "half", "bottom", "open", "true");
   private static final Map<String, String> HALF_BOTTOM_OPEN_TRUE_FACING_SOUTH = Map.of("facing", "south", "half", "bottom", "open", "true");
   private static final Map<String, String> HALF_BOTTOM_OPEN_TRUE_FACING_WEST = Map.of("facing", "west", "half", "bottom", "open", "true");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST = Map.of("facing", "east", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH = Map.of("facing", "north", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH = Map.of("facing", "south", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST = Map.of("facing", "west", "half", "bottom", "shape", "inner_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST = Map.of("facing", "east", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH = Map.of("facing", "north", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH = Map.of("facing", "south", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST = Map.of("facing", "west", "half", "bottom", "shape", "inner_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST = Map.of("facing", "east", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH = Map.of("facing", "north", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH = Map.of("facing", "south", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST = Map.of("facing", "west", "half", "bottom", "shape", "outer_left");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST = Map.of("facing", "east", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH = Map.of("facing", "north", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH = Map.of("facing", "south", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST = Map.of("facing", "west", "half", "bottom", "shape", "outer_right");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST = Map.of("facing", "east", "half", "bottom", "shape", "straight");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH = Map.of("facing", "north", "half", "bottom", "shape", "straight");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH = Map.of("facing", "south", "half", "bottom", "shape", "straight");
   private static final Map<String, String> HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST = Map.of("facing", "west", "half", "bottom", "shape", "straight");
   private static final Map<String, String> HALF_LOWER = Map.of("half", "lower");
   private static final Map<String, String> HALF_TOP_OPEN_FALSE_FACING_EAST = Map.of("facing", "east", "half", "top", "open", "false");
   private static final Map<String, String> HALF_TOP_OPEN_FALSE_FACING_NORTH = Map.of("facing", "north", "half", "top", "open", "false");
   private static final Map<String, String> HALF_TOP_OPEN_FALSE_FACING_SOUTH = Map.of("facing", "south", "half", "top", "open", "false");
   private static final Map<String, String> HALF_TOP_OPEN_FALSE_FACING_WEST = Map.of("facing", "west", "half", "top", "open", "false");
   private static final Map<String, String> HALF_TOP_OPEN_TRUE_FACING_EAST = Map.of("facing", "east", "half", "top", "open", "true");
   private static final Map<String, String> HALF_TOP_OPEN_TRUE_FACING_NORTH = Map.of("facing", "north", "half", "top", "open", "true");
   private static final Map<String, String> HALF_TOP_OPEN_TRUE_FACING_SOUTH = Map.of("facing", "south", "half", "top", "open", "true");
   private static final Map<String, String> HALF_TOP_OPEN_TRUE_FACING_WEST = Map.of("facing", "west", "half", "top", "open", "true");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST = Map.of("facing", "east", "half", "top", "shape", "inner_left");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH = Map.of("facing", "north", "half", "top", "shape", "inner_left");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH = Map.of("facing", "south", "half", "top", "shape", "inner_left");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST = Map.of("facing", "west", "half", "top", "shape", "inner_left");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST = Map.of("facing", "east", "half", "top", "shape", "inner_right");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH = Map.of("facing", "north", "half", "top", "shape", "inner_right");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH = Map.of("facing", "south", "half", "top", "shape", "inner_right");
   private static final Map<String, String> HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST = Map.of("facing", "west", "half", "top", "shape", "inner_right");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST = Map.of("facing", "east", "half", "top", "shape", "outer_left");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH = Map.of("facing", "north", "half", "top", "shape", "outer_left");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH = Map.of("facing", "south", "half", "top", "shape", "outer_left");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST = Map.of("facing", "west", "half", "top", "shape", "outer_left");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST = Map.of("facing", "east", "half", "top", "shape", "outer_right");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH = Map.of("facing", "north", "half", "top", "shape", "outer_right");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH = Map.of("facing", "south", "half", "top", "shape", "outer_right");
   private static final Map<String, String> HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST = Map.of("facing", "west", "half", "top", "shape", "outer_right");
   private static final Map<String, String> HALF_TOP_SHAPE_STRAIGHT_FACING_EAST = Map.of("facing", "east", "half", "top", "shape", "straight");
   private static final Map<String, String> HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH = Map.of("facing", "north", "half", "top", "shape", "straight");
   private static final Map<String, String> HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH = Map.of("facing", "south", "half", "top", "shape", "straight");
   private static final Map<String, String> HALF_TOP_SHAPE_STRAIGHT_FACING_WEST = Map.of("facing", "west", "half", "top", "shape", "straight");
   private static final Map<String, String> HALF_UPPER = Map.of("half", "upper");
   private static final Map<String, String> LEVEL_0 = Map.of("level", "0");
   private static final Map<String, String> LEVEL_1 = Map.of("level", "1");
   private static final Map<String, String> LEVEL_10 = Map.of("level", "10");
   private static final Map<String, String> LEVEL_11 = Map.of("level", "11");
   private static final Map<String, String> LEVEL_12 = Map.of("level", "12");
   private static final Map<String, String> LEVEL_13 = Map.of("level", "13");
   private static final Map<String, String> LEVEL_14 = Map.of("level", "14");
   private static final Map<String, String> LEVEL_15 = Map.of("level", "15");
   private static final Map<String, String> LEVEL_2 = Map.of("level", "2");
   private static final Map<String, String> LEVEL_3 = Map.of("level", "3");
   private static final Map<String, String> LEVEL_4 = Map.of("level", "4");
   private static final Map<String, String> LEVEL_5 = Map.of("level", "5");
   private static final Map<String, String> LEVEL_6 = Map.of("level", "6");
   private static final Map<String, String> LEVEL_7 = Map.of("level", "7");
   private static final Map<String, String> LEVEL_8 = Map.of("level", "8");
   private static final Map<String, String> LEVEL_9 = Map.of("level", "9");
   private static final Map<String, String> LIT_FALSE = Map.of("lit", "false");
   private static final Map<String, String> LIT_TRUE = Map.of("lit", "true");
   private static final Map<String, String> NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false"
   );
   private static final Map<String, String> NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false"
   );
   private static final Map<String, String> NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true"
   );
   private static final Map<String, String> NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE = Map.of(
      "down", "false", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true"
   );
   private static final Map<String, String> NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE = Map.of(
      "down", "false", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE = Map.of(
      "down", "false", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false"
   );
   private static final Map<String, String> NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE = Map.of(
      "down", "false", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE = Map.of(
      "down", "false", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true"
   );
   private static final Map<String, String> NORTH_TRUE_EAST_TRUE_UP_FALSE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE = Map.of(
      "down", "false", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true"
   );
   private static final Map<String, String> NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE = Map.of(
      "down", "false", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false"
   );
   private static final Map<String, String> NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_TRUE = Map.of(
      "down", "true", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"
   );
   private static final Map<String, String> POWERED_FALSE = Map.of("powered", "false");
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "false", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "false", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "true", "open", "false", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "true", "open", "true", "powered", "false"
   );
   private static final Map<String, String> POWERED_FALSE_MODE_COMPARE_FACING_EAST = Map.of("facing", "east", "mode", "compare", "powered", "false");
   private static final Map<String, String> POWERED_FALSE_MODE_COMPARE_FACING_NORTH = Map.of("facing", "north", "mode", "compare", "powered", "false");
   private static final Map<String, String> POWERED_FALSE_MODE_COMPARE_FACING_SOUTH = Map.of("facing", "south", "mode", "compare", "powered", "false");
   private static final Map<String, String> POWERED_FALSE_MODE_COMPARE_FACING_WEST = Map.of("facing", "west", "mode", "compare", "powered", "false");
   private static final Map<String, String> POWERED_FALSE_MODE_SUBTRACT_FACING_EAST = Map.of("facing", "east", "mode", "subtract", "powered", "false");
   private static final Map<String, String> POWERED_FALSE_MODE_SUBTRACT_FACING_NORTH = Map.of("facing", "north", "mode", "subtract", "powered", "false");
   private static final Map<String, String> POWERED_FALSE_MODE_SUBTRACT_FACING_SOUTH = Map.of("facing", "south", "mode", "subtract", "powered", "false");
   private static final Map<String, String> POWERED_FALSE_MODE_SUBTRACT_FACING_WEST = Map.of("facing", "west", "mode", "subtract", "powered", "false");
   private static final Map<String, String> POWERED_TRUE = Map.of("powered", "true");
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "false", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "false", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "true", "open", "false", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST = Map.of(
      "facing", "east", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH = Map.of(
      "facing", "north", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH = Map.of(
      "facing", "south", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST = Map.of(
      "facing", "west", "in_wall", "true", "open", "true", "powered", "true"
   );
   private static final Map<String, String> POWERED_TRUE_MODE_COMPARE_FACING_EAST = Map.of("facing", "east", "mode", "compare", "powered", "true");
   private static final Map<String, String> POWERED_TRUE_MODE_COMPARE_FACING_NORTH = Map.of("facing", "north", "mode", "compare", "powered", "true");
   private static final Map<String, String> POWERED_TRUE_MODE_COMPARE_FACING_SOUTH = Map.of("facing", "south", "mode", "compare", "powered", "true");
   private static final Map<String, String> POWERED_TRUE_MODE_COMPARE_FACING_WEST = Map.of("facing", "west", "mode", "compare", "powered", "true");
   private static final Map<String, String> POWERED_TRUE_MODE_SUBTRACT_FACING_EAST = Map.of("facing", "east", "mode", "subtract", "powered", "true");
   private static final Map<String, String> POWERED_TRUE_MODE_SUBTRACT_FACING_NORTH = Map.of("facing", "north", "mode", "subtract", "powered", "true");
   private static final Map<String, String> POWERED_TRUE_MODE_SUBTRACT_FACING_SOUTH = Map.of("facing", "south", "mode", "subtract", "powered", "true");
   private static final Map<String, String> POWERED_TRUE_MODE_SUBTRACT_FACING_WEST = Map.of("facing", "west", "mode", "subtract", "powered", "true");
   private static final Map<String, String> POWER_0 = Map.of("power", "0");
   private static final Map<String, String> POWER_1 = Map.of("power", "1");
   private static final Map<String, String> POWER_10 = Map.of("power", "10");
   private static final Map<String, String> POWER_11 = Map.of("power", "11");
   private static final Map<String, String> POWER_12 = Map.of("power", "12");
   private static final Map<String, String> POWER_13 = Map.of("power", "13");
   private static final Map<String, String> POWER_14 = Map.of("power", "14");
   private static final Map<String, String> POWER_15 = Map.of("power", "15");
   private static final Map<String, String> POWER_2 = Map.of("power", "2");
   private static final Map<String, String> POWER_3 = Map.of("power", "3");
   private static final Map<String, String> POWER_4 = Map.of("power", "4");
   private static final Map<String, String> POWER_5 = Map.of("power", "5");
   private static final Map<String, String> POWER_6 = Map.of("power", "6");
   private static final Map<String, String> POWER_7 = Map.of("power", "7");
   private static final Map<String, String> POWER_8 = Map.of("power", "8");
   private static final Map<String, String> POWER_9 = Map.of("power", "9");
   private static final Map<String, String> ROTATION_0 = Map.of("rotation", "0");
   private static final Map<String, String> ROTATION_1 = Map.of("rotation", "1");
   private static final Map<String, String> ROTATION_10 = Map.of("rotation", "10");
   private static final Map<String, String> ROTATION_11 = Map.of("rotation", "11");
   private static final Map<String, String> ROTATION_12 = Map.of("rotation", "12");
   private static final Map<String, String> ROTATION_13 = Map.of("rotation", "13");
   private static final Map<String, String> ROTATION_14 = Map.of("rotation", "14");
   private static final Map<String, String> ROTATION_15 = Map.of("rotation", "15");
   private static final Map<String, String> ROTATION_2 = Map.of("rotation", "2");
   private static final Map<String, String> ROTATION_3 = Map.of("rotation", "3");
   private static final Map<String, String> ROTATION_4 = Map.of("rotation", "4");
   private static final Map<String, String> ROTATION_5 = Map.of("rotation", "5");
   private static final Map<String, String> ROTATION_6 = Map.of("rotation", "6");
   private static final Map<String, String> ROTATION_7 = Map.of("rotation", "7");
   private static final Map<String, String> ROTATION_8 = Map.of("rotation", "8");
   private static final Map<String, String> ROTATION_9 = Map.of("rotation", "9");
   private static final Map<String, String> SHAPE_ASCENDING_EAST_POWERED_FALSE = Map.of("powered", "false", "shape", "ascending_east");
   private static final Map<String, String> SHAPE_ASCENDING_EAST_POWERED_TRUE = Map.of("powered", "true", "shape", "ascending_east");
   private static final Map<String, String> SHAPE_ASCENDING_NORTH_POWERED_FALSE = Map.of("powered", "false", "shape", "ascending_north");
   private static final Map<String, String> SHAPE_ASCENDING_NORTH_POWERED_TRUE = Map.of("powered", "true", "shape", "ascending_north");
   private static final Map<String, String> SHAPE_ASCENDING_SOUTH_POWERED_FALSE = Map.of("powered", "false", "shape", "ascending_south");
   private static final Map<String, String> SHAPE_ASCENDING_SOUTH_POWERED_TRUE = Map.of("powered", "true", "shape", "ascending_south");
   private static final Map<String, String> SHAPE_ASCENDING_WEST_POWERED_FALSE = Map.of("powered", "false", "shape", "ascending_west");
   private static final Map<String, String> SHAPE_ASCENDING_WEST_POWERED_TRUE = Map.of("powered", "true", "shape", "ascending_west");
   private static final Map<String, String> SHAPE_EAST_WEST_POWERED_FALSE = Map.of("powered", "false", "shape", "east_west");
   private static final Map<String, String> SHAPE_EAST_WEST_POWERED_TRUE = Map.of("powered", "true", "shape", "east_west");
   private static final Map<String, String> SHAPE_NORTH_SOUTH_POWERED_FALSE = Map.of("powered", "false", "shape", "north_south");
   private static final Map<String, String> SHAPE_NORTH_SOUTH_POWERED_TRUE = Map.of("powered", "true", "shape", "north_south");
   private static final Map<String, String> SNOWY_FALSE = Map.of("snowy", "false");
   private static final Map<String, String> STAGE_0 = Map.of("stage", "0");
   private static final Map<String, String> STAGE_1 = Map.of("stage", "1");
   private static final Map<String, String> TRIGGERED_FALSE_FACING_DOWN = Map.of("facing", "down", "triggered", "false");
   private static final Map<String, String> TRIGGERED_FALSE_FACING_EAST = Map.of("facing", "east", "triggered", "false");
   private static final Map<String, String> TRIGGERED_FALSE_FACING_NORTH = Map.of("facing", "north", "triggered", "false");
   private static final Map<String, String> TRIGGERED_FALSE_FACING_SOUTH = Map.of("facing", "south", "triggered", "false");
   private static final Map<String, String> TRIGGERED_FALSE_FACING_UP = Map.of("facing", "up", "triggered", "false");
   private static final Map<String, String> TRIGGERED_FALSE_FACING_WEST = Map.of("facing", "west", "triggered", "false");
   private static final Map<String, String> TRIGGERED_TRUE_FACING_DOWN = Map.of("facing", "down", "triggered", "true");
   private static final Map<String, String> TRIGGERED_TRUE_FACING_EAST = Map.of("facing", "east", "triggered", "true");
   private static final Map<String, String> TRIGGERED_TRUE_FACING_NORTH = Map.of("facing", "north", "triggered", "true");
   private static final Map<String, String> TRIGGERED_TRUE_FACING_SOUTH = Map.of("facing", "south", "triggered", "true");
   private static final Map<String, String> TRIGGERED_TRUE_FACING_UP = Map.of("facing", "up", "triggered", "true");
   private static final Map<String, String> TRIGGERED_TRUE_FACING_WEST = Map.of("facing", "west", "triggered", "true");
   private static final Map<String, String> TYPE_BOTTOM = Map.of("type", "bottom");
   private static final Map<String, String> TYPE_DOUBLE = Map.of("type", "double");
   private static final Map<String, String> TYPE_TOP = Map.of("type", "top");
   private static final Map<String, String> UP_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "false", "up", "false", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE = Map.of(
      "east", "true", "north", "false", "south", "false", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "true", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE = Map.of(
      "east", "true", "north", "false", "south", "true", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE = Map.of(
      "east", "false", "north", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE = Map.of(
      "east", "true", "north", "true", "south", "false", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE = Map.of(
      "east", "false", "north", "true", "south", "true", "west", "false"
   );
   private static final Map<String, String> WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE = Map.of(
      "east", "true", "north", "true", "south", "true", "west", "false"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "false", "west", "true"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE = Map.of(
      "east", "true", "north", "false", "south", "false", "west", "true"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE = Map.of(
      "east", "false", "north", "false", "south", "true", "west", "true"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE = Map.of(
      "east", "true", "north", "false", "south", "true", "west", "true"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE = Map.of(
      "east", "false", "north", "true", "south", "false", "west", "true"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE = Map.of(
      "east", "true", "north", "true", "south", "false", "west", "true"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE = Map.of(
      "east", "false", "north", "true", "south", "true", "west", "true"
   );
   private static final Map<String, String> WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE = Map.of("east", "true", "north", "true", "south", "true", "west", "true");

   private static Dynamic<?> create(String p_393491_) {
      return new Dynamic(JavaOps.INSTANCE, Map.of("Name", p_393491_)).convert(NbtOps.INSTANCE);
   }

   private static Dynamic<?> create(String p_396094_, Map<String, String> p_393702_) {
      return new Dynamic(JavaOps.INSTANCE, Map.of("Name", p_396094_, "Properties", p_393702_)).convert(NbtOps.INSTANCE);
   }

   private static void register(int p_14943_, Dynamic<?> p_391239_, Dynamic<?>... p_393258_) {
      MAP[p_14943_] = p_391239_;
      int i = p_14943_ >> 4;
      if (BLOCK_DEFAULTS[i] == null) {
         BLOCK_DEFAULTS[i] = p_391239_;
      }

      for (Dynamic<?> dynamic : p_393258_) {
         String s = dynamic.get("Name").asString("");
         ID_BY_OLD_NAME.putIfAbsent(s, p_14943_);
         ID_BY_OLD.put(dynamic, p_14943_);
      }
   }

   private static void finalizeMaps() {
      for (int i = 0; i < MAP.length; i++) {
         if (MAP[i] == null) {
            MAP[i] = BLOCK_DEFAULTS[i >> 4];
         }
      }
   }

   public static Dynamic<?> upgradeBlockStateTag(Dynamic<?> p_14947_) {
      int i = ID_BY_OLD.getInt(p_14947_);
      if (i >= 0 && i < MAP.length) {
         Dynamic<?> dynamic = MAP[i];
         return dynamic == null ? p_14947_ : dynamic;
      } else {
         return p_14947_;
      }
   }

   public static String upgradeBlock(String p_14951_) {
      int i = ID_BY_OLD_NAME.getInt(p_14951_);
      if (i >= 0 && i < MAP.length) {
         Dynamic<?> dynamic = MAP[i];
         return dynamic == null ? p_14951_ : dynamic.get("Name").asString("");
      } else {
         return p_14951_;
      }
   }

   public static String upgradeBlock(int p_14941_) {
      if (p_14941_ >= 0 && p_14941_ < MAP.length) {
         Dynamic<?> dynamic = MAP[p_14941_];
         return dynamic == null ? "minecraft:air" : dynamic.get("Name").asString("");
      } else {
         return "minecraft:air";
      }
   }

   public static Dynamic<?> getTag(int p_14953_) {
      Dynamic<?> dynamic = null;
      if (p_14953_ >= 0 && p_14953_ < MAP.length) {
         dynamic = MAP[p_14953_];
      }

      return dynamic == null ? MAP[0] : dynamic;
   }

   private static void bootstrap0() {
      register(0, create("minecraft:air"), create("minecraft:air"));
      register(16, create("minecraft:stone"), create("minecraft:stone", Map.of("variant", "stone")));
      register(17, create("minecraft:granite"), create("minecraft:stone", Map.of("variant", "granite")));
      register(18, create("minecraft:polished_granite"), create("minecraft:stone", Map.of("variant", "smooth_granite")));
      register(19, create("minecraft:diorite"), create("minecraft:stone", Map.of("variant", "diorite")));
      register(20, create("minecraft:polished_diorite"), create("minecraft:stone", Map.of("variant", "smooth_diorite")));
      register(21, create("minecraft:andesite"), create("minecraft:stone", Map.of("variant", "andesite")));
      register(22, create("minecraft:polished_andesite"), create("minecraft:stone", Map.of("variant", "smooth_andesite")));
      register(32, create("minecraft:grass_block", SNOWY_FALSE), create("minecraft:grass", SNOWY_FALSE), create("minecraft:grass", Map.of("snowy", "true")));
      register(
         48,
         create("minecraft:dirt"),
         create("minecraft:dirt", Map.of("snowy", "false", "variant", "dirt")),
         create("minecraft:dirt", Map.of("snowy", "true", "variant", "dirt"))
      );
      register(
         49,
         create("minecraft:coarse_dirt"),
         create("minecraft:dirt", Map.of("snowy", "false", "variant", "coarse_dirt")),
         create("minecraft:dirt", Map.of("snowy", "true", "variant", "coarse_dirt"))
      );
      register(
         50,
         create("minecraft:podzol", SNOWY_FALSE),
         create("minecraft:dirt", Map.of("snowy", "false", "variant", "podzol")),
         create("minecraft:dirt", Map.of("snowy", "true", "variant", "podzol"))
      );
      register(64, create("minecraft:cobblestone"), create("minecraft:cobblestone"));
      register(80, create("minecraft:oak_planks"), create("minecraft:planks", Map.of("variant", "oak")));
      register(81, create("minecraft:spruce_planks"), create("minecraft:planks", Map.of("variant", "spruce")));
      register(82, create("minecraft:birch_planks"), create("minecraft:planks", Map.of("variant", "birch")));
      register(83, create("minecraft:jungle_planks"), create("minecraft:planks", Map.of("variant", "jungle")));
      register(84, create("minecraft:acacia_planks"), create("minecraft:planks", Map.of("variant", "acacia")));
      register(85, create("minecraft:dark_oak_planks"), create("minecraft:planks", Map.of("variant", "dark_oak")));
      register(96, create("minecraft:oak_sapling", STAGE_0), create("minecraft:sapling", Map.of("stage", "0", "type", "oak")));
      register(97, create("minecraft:spruce_sapling", STAGE_0), create("minecraft:sapling", Map.of("stage", "0", "type", "spruce")));
      register(98, create("minecraft:birch_sapling", STAGE_0), create("minecraft:sapling", Map.of("stage", "0", "type", "birch")));
      register(99, create("minecraft:jungle_sapling", STAGE_0), create("minecraft:sapling", Map.of("stage", "0", "type", "jungle")));
      register(100, create("minecraft:acacia_sapling", STAGE_0), create("minecraft:sapling", Map.of("stage", "0", "type", "acacia")));
      register(101, create("minecraft:dark_oak_sapling", STAGE_0), create("minecraft:sapling", Map.of("stage", "0", "type", "dark_oak")));
      register(104, create("minecraft:oak_sapling", STAGE_1), create("minecraft:sapling", Map.of("stage", "1", "type", "oak")));
      register(105, create("minecraft:spruce_sapling", STAGE_1), create("minecraft:sapling", Map.of("stage", "1", "type", "spruce")));
      register(106, create("minecraft:birch_sapling", STAGE_1), create("minecraft:sapling", Map.of("stage", "1", "type", "birch")));
      register(107, create("minecraft:jungle_sapling", STAGE_1), create("minecraft:sapling", Map.of("stage", "1", "type", "jungle")));
      register(108, create("minecraft:acacia_sapling", STAGE_1), create("minecraft:sapling", Map.of("stage", "1", "type", "acacia")));
      register(109, create("minecraft:dark_oak_sapling", STAGE_1), create("minecraft:sapling", Map.of("stage", "1", "type", "dark_oak")));
      register(112, create("minecraft:bedrock"), create("minecraft:bedrock"));
      register(128, create("minecraft:water", LEVEL_0), create("minecraft:flowing_water", LEVEL_0));
      register(129, create("minecraft:water", LEVEL_1), create("minecraft:flowing_water", LEVEL_1));
      register(130, create("minecraft:water", LEVEL_2), create("minecraft:flowing_water", LEVEL_2));
      register(131, create("minecraft:water", LEVEL_3), create("minecraft:flowing_water", LEVEL_3));
      register(132, create("minecraft:water", LEVEL_4), create("minecraft:flowing_water", LEVEL_4));
      register(133, create("minecraft:water", LEVEL_5), create("minecraft:flowing_water", LEVEL_5));
      register(134, create("minecraft:water", LEVEL_6), create("minecraft:flowing_water", LEVEL_6));
      register(135, create("minecraft:water", LEVEL_7), create("minecraft:flowing_water", LEVEL_7));
      register(136, create("minecraft:water", LEVEL_8), create("minecraft:flowing_water", LEVEL_8));
      register(137, create("minecraft:water", LEVEL_9), create("minecraft:flowing_water", LEVEL_9));
      register(138, create("minecraft:water", LEVEL_10), create("minecraft:flowing_water", LEVEL_10));
      register(139, create("minecraft:water", LEVEL_11), create("minecraft:flowing_water", LEVEL_11));
      register(140, create("minecraft:water", LEVEL_12), create("minecraft:flowing_water", LEVEL_12));
      register(141, create("minecraft:water", LEVEL_13), create("minecraft:flowing_water", LEVEL_13));
      register(142, create("minecraft:water", LEVEL_14), create("minecraft:flowing_water", LEVEL_14));
      register(143, create("minecraft:water", LEVEL_15), create("minecraft:flowing_water", LEVEL_15));
      register(144, create("minecraft:water", LEVEL_0), create("minecraft:water", LEVEL_0));
      register(145, create("minecraft:water", LEVEL_1), create("minecraft:water", LEVEL_1));
      register(146, create("minecraft:water", LEVEL_2), create("minecraft:water", LEVEL_2));
      register(147, create("minecraft:water", LEVEL_3), create("minecraft:water", LEVEL_3));
      register(148, create("minecraft:water", LEVEL_4), create("minecraft:water", LEVEL_4));
      register(149, create("minecraft:water", LEVEL_5), create("minecraft:water", LEVEL_5));
      register(150, create("minecraft:water", LEVEL_6), create("minecraft:water", LEVEL_6));
      register(151, create("minecraft:water", LEVEL_7), create("minecraft:water", LEVEL_7));
      register(152, create("minecraft:water", LEVEL_8), create("minecraft:water", LEVEL_8));
      register(153, create("minecraft:water", LEVEL_9), create("minecraft:water", LEVEL_9));
      register(154, create("minecraft:water", LEVEL_10), create("minecraft:water", LEVEL_10));
      register(155, create("minecraft:water", LEVEL_11), create("minecraft:water", LEVEL_11));
      register(156, create("minecraft:water", LEVEL_12), create("minecraft:water", LEVEL_12));
      register(157, create("minecraft:water", LEVEL_13), create("minecraft:water", LEVEL_13));
      register(158, create("minecraft:water", LEVEL_14), create("minecraft:water", LEVEL_14));
      register(159, create("minecraft:water", LEVEL_15), create("minecraft:water", LEVEL_15));
      register(160, create("minecraft:lava", LEVEL_0), create("minecraft:flowing_lava", LEVEL_0));
      register(161, create("minecraft:lava", LEVEL_1), create("minecraft:flowing_lava", LEVEL_1));
      register(162, create("minecraft:lava", LEVEL_2), create("minecraft:flowing_lava", LEVEL_2));
      register(163, create("minecraft:lava", LEVEL_3), create("minecraft:flowing_lava", LEVEL_3));
      register(164, create("minecraft:lava", LEVEL_4), create("minecraft:flowing_lava", LEVEL_4));
      register(165, create("minecraft:lava", LEVEL_5), create("minecraft:flowing_lava", LEVEL_5));
      register(166, create("minecraft:lava", LEVEL_6), create("minecraft:flowing_lava", LEVEL_6));
      register(167, create("minecraft:lava", LEVEL_7), create("minecraft:flowing_lava", LEVEL_7));
      register(168, create("minecraft:lava", LEVEL_8), create("minecraft:flowing_lava", LEVEL_8));
      register(169, create("minecraft:lava", LEVEL_9), create("minecraft:flowing_lava", LEVEL_9));
      register(170, create("minecraft:lava", LEVEL_10), create("minecraft:flowing_lava", LEVEL_10));
      register(171, create("minecraft:lava", LEVEL_11), create("minecraft:flowing_lava", LEVEL_11));
      register(172, create("minecraft:lava", LEVEL_12), create("minecraft:flowing_lava", LEVEL_12));
      register(173, create("minecraft:lava", LEVEL_13), create("minecraft:flowing_lava", LEVEL_13));
      register(174, create("minecraft:lava", LEVEL_14), create("minecraft:flowing_lava", LEVEL_14));
      register(175, create("minecraft:lava", LEVEL_15), create("minecraft:flowing_lava", LEVEL_15));
      register(176, create("minecraft:lava", LEVEL_0), create("minecraft:lava", LEVEL_0));
      register(177, create("minecraft:lava", LEVEL_1), create("minecraft:lava", LEVEL_1));
      register(178, create("minecraft:lava", LEVEL_2), create("minecraft:lava", LEVEL_2));
      register(179, create("minecraft:lava", LEVEL_3), create("minecraft:lava", LEVEL_3));
      register(180, create("minecraft:lava", LEVEL_4), create("minecraft:lava", LEVEL_4));
      register(181, create("minecraft:lava", LEVEL_5), create("minecraft:lava", LEVEL_5));
      register(182, create("minecraft:lava", LEVEL_6), create("minecraft:lava", LEVEL_6));
      register(183, create("minecraft:lava", LEVEL_7), create("minecraft:lava", LEVEL_7));
      register(184, create("minecraft:lava", LEVEL_8), create("minecraft:lava", LEVEL_8));
      register(185, create("minecraft:lava", LEVEL_9), create("minecraft:lava", LEVEL_9));
      register(186, create("minecraft:lava", LEVEL_10), create("minecraft:lava", LEVEL_10));
      register(187, create("minecraft:lava", LEVEL_11), create("minecraft:lava", LEVEL_11));
      register(188, create("minecraft:lava", LEVEL_12), create("minecraft:lava", LEVEL_12));
      register(189, create("minecraft:lava", LEVEL_13), create("minecraft:lava", LEVEL_13));
      register(190, create("minecraft:lava", LEVEL_14), create("minecraft:lava", LEVEL_14));
      register(191, create("minecraft:lava", LEVEL_15), create("minecraft:lava", LEVEL_15));
      register(192, create("minecraft:sand"), create("minecraft:sand", Map.of("variant", "sand")));
      register(193, create("minecraft:red_sand"), create("minecraft:sand", Map.of("variant", "red_sand")));
      register(208, create("minecraft:gravel"), create("minecraft:gravel"));
      register(224, create("minecraft:gold_ore"), create("minecraft:gold_ore"));
      register(240, create("minecraft:iron_ore"), create("minecraft:iron_ore"));
   }

   private static void bootstrap1() {
      register(256, create("minecraft:coal_ore"), create("minecraft:coal_ore"));
      register(272, create("minecraft:oak_log", AXIS_Y), create("minecraft:log", Map.of("axis", "y", "variant", "oak")));
      register(273, create("minecraft:spruce_log", AXIS_Y), create("minecraft:log", Map.of("axis", "y", "variant", "spruce")));
      register(274, create("minecraft:birch_log", AXIS_Y), create("minecraft:log", Map.of("axis", "y", "variant", "birch")));
      register(275, create("minecraft:jungle_log", AXIS_Y), create("minecraft:log", Map.of("axis", "y", "variant", "jungle")));
      register(276, create("minecraft:oak_log", AXIS_X), create("minecraft:log", Map.of("axis", "x", "variant", "oak")));
      register(277, create("minecraft:spruce_log", AXIS_X), create("minecraft:log", Map.of("axis", "x", "variant", "spruce")));
      register(278, create("minecraft:birch_log", AXIS_X), create("minecraft:log", Map.of("axis", "x", "variant", "birch")));
      register(279, create("minecraft:jungle_log", AXIS_X), create("minecraft:log", Map.of("axis", "x", "variant", "jungle")));
      register(280, create("minecraft:oak_log", AXIS_Z), create("minecraft:log", Map.of("axis", "z", "variant", "oak")));
      register(281, create("minecraft:spruce_log", AXIS_Z), create("minecraft:log", Map.of("axis", "z", "variant", "spruce")));
      register(282, create("minecraft:birch_log", AXIS_Z), create("minecraft:log", Map.of("axis", "z", "variant", "birch")));
      register(283, create("minecraft:jungle_log", AXIS_Z), create("minecraft:log", Map.of("axis", "z", "variant", "jungle")));
      register(284, create("minecraft:oak_bark"), create("minecraft:log", Map.of("axis", "none", "variant", "oak")));
      register(285, create("minecraft:spruce_bark"), create("minecraft:log", Map.of("axis", "none", "variant", "spruce")));
      register(286, create("minecraft:birch_bark"), create("minecraft:log", Map.of("axis", "none", "variant", "birch")));
      register(287, create("minecraft:jungle_bark"), create("minecraft:log", Map.of("axis", "none", "variant", "jungle")));
      register(
         288,
         create("minecraft:oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "oak"))
      );
      register(
         289,
         create("minecraft:spruce_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "spruce"))
      );
      register(
         290,
         create("minecraft:birch_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "birch"))
      );
      register(
         291,
         create("minecraft:jungle_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "true", "variant", "jungle"))
      );
      register(
         292,
         create("minecraft:oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "oak"))
      );
      register(
         293,
         create("minecraft:spruce_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "spruce"))
      );
      register(
         294,
         create("minecraft:birch_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "birch"))
      );
      register(
         295,
         create("minecraft:jungle_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "false", "decayable", "false", "variant", "jungle"))
      );
      register(
         296,
         create("minecraft:oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "oak"))
      );
      register(
         297,
         create("minecraft:spruce_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "spruce"))
      );
      register(
         298,
         create("minecraft:birch_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "birch"))
      );
      register(
         299,
         create("minecraft:jungle_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "true", "variant", "jungle"))
      );
      register(
         300,
         create("minecraft:oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "oak"))
      );
      register(
         301,
         create("minecraft:spruce_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "spruce"))
      );
      register(
         302,
         create("minecraft:birch_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "birch"))
      );
      register(
         303,
         create("minecraft:jungle_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         create("minecraft:leaves", Map.of("check_decay", "true", "decayable", "false", "variant", "jungle"))
      );
      register(304, create("minecraft:sponge"), create("minecraft:sponge", Map.of("wet", "false")));
      register(305, create("minecraft:wet_sponge"), create("minecraft:sponge", Map.of("wet", "true")));
      register(320, create("minecraft:glass"), create("minecraft:glass"));
      register(336, create("minecraft:lapis_ore"), create("minecraft:lapis_ore"));
      register(352, create("minecraft:lapis_block"), create("minecraft:lapis_block"));
      register(368, create("minecraft:dispenser", TRIGGERED_FALSE_FACING_DOWN), create("minecraft:dispenser", TRIGGERED_FALSE_FACING_DOWN));
      register(369, create("minecraft:dispenser", TRIGGERED_FALSE_FACING_UP), create("minecraft:dispenser", TRIGGERED_FALSE_FACING_UP));
      register(370, create("minecraft:dispenser", TRIGGERED_FALSE_FACING_NORTH), create("minecraft:dispenser", TRIGGERED_FALSE_FACING_NORTH));
      register(371, create("minecraft:dispenser", TRIGGERED_FALSE_FACING_SOUTH), create("minecraft:dispenser", TRIGGERED_FALSE_FACING_SOUTH));
      register(372, create("minecraft:dispenser", TRIGGERED_FALSE_FACING_WEST), create("minecraft:dispenser", TRIGGERED_FALSE_FACING_WEST));
      register(373, create("minecraft:dispenser", TRIGGERED_FALSE_FACING_EAST), create("minecraft:dispenser", TRIGGERED_FALSE_FACING_EAST));
      register(376, create("minecraft:dispenser", TRIGGERED_TRUE_FACING_DOWN), create("minecraft:dispenser", TRIGGERED_TRUE_FACING_DOWN));
      register(377, create("minecraft:dispenser", TRIGGERED_TRUE_FACING_UP), create("minecraft:dispenser", TRIGGERED_TRUE_FACING_UP));
      register(378, create("minecraft:dispenser", TRIGGERED_TRUE_FACING_NORTH), create("minecraft:dispenser", TRIGGERED_TRUE_FACING_NORTH));
      register(379, create("minecraft:dispenser", TRIGGERED_TRUE_FACING_SOUTH), create("minecraft:dispenser", TRIGGERED_TRUE_FACING_SOUTH));
      register(380, create("minecraft:dispenser", TRIGGERED_TRUE_FACING_WEST), create("minecraft:dispenser", TRIGGERED_TRUE_FACING_WEST));
      register(381, create("minecraft:dispenser", TRIGGERED_TRUE_FACING_EAST), create("minecraft:dispenser", TRIGGERED_TRUE_FACING_EAST));
      register(384, create("minecraft:sandstone"), create("minecraft:sandstone", Map.of("type", "sandstone")));
      register(385, create("minecraft:chiseled_sandstone"), create("minecraft:sandstone", Map.of("type", "chiseled_sandstone")));
      register(386, create("minecraft:cut_sandstone"), create("minecraft:sandstone", Map.of("type", "smooth_sandstone")));
      register(400, create("minecraft:note_block"), create("minecraft:noteblock"));
      register(
         416,
         create("minecraft:red_bed", Map.of("facing", "south", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "south", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "south", "occupied", "true", "part", "foot"))
      );
      register(
         417,
         create("minecraft:red_bed", Map.of("facing", "west", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "west", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "west", "occupied", "true", "part", "foot"))
      );
      register(
         418,
         create("minecraft:red_bed", Map.of("facing", "north", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "north", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "north", "occupied", "true", "part", "foot"))
      );
      register(
         419,
         create("minecraft:red_bed", Map.of("facing", "east", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "east", "occupied", "false", "part", "foot")),
         create("minecraft:bed", Map.of("facing", "east", "occupied", "true", "part", "foot"))
      );
      register(
         424,
         create("minecraft:red_bed", Map.of("facing", "south", "occupied", "false", "part", "head")),
         create("minecraft:bed", Map.of("facing", "south", "occupied", "false", "part", "head"))
      );
      register(
         425,
         create("minecraft:red_bed", Map.of("facing", "west", "occupied", "false", "part", "head")),
         create("minecraft:bed", Map.of("facing", "west", "occupied", "false", "part", "head"))
      );
      register(
         426,
         create("minecraft:red_bed", Map.of("facing", "north", "occupied", "false", "part", "head")),
         create("minecraft:bed", Map.of("facing", "north", "occupied", "false", "part", "head"))
      );
      register(
         427,
         create("minecraft:red_bed", Map.of("facing", "east", "occupied", "false", "part", "head")),
         create("minecraft:bed", Map.of("facing", "east", "occupied", "false", "part", "head"))
      );
      register(
         428,
         create("minecraft:red_bed", Map.of("facing", "south", "occupied", "true", "part", "head")),
         create("minecraft:bed", Map.of("facing", "south", "occupied", "true", "part", "head"))
      );
      register(
         429,
         create("minecraft:red_bed", Map.of("facing", "west", "occupied", "true", "part", "head")),
         create("minecraft:bed", Map.of("facing", "west", "occupied", "true", "part", "head"))
      );
      register(
         430,
         create("minecraft:red_bed", Map.of("facing", "north", "occupied", "true", "part", "head")),
         create("minecraft:bed", Map.of("facing", "north", "occupied", "true", "part", "head"))
      );
      register(
         431,
         create("minecraft:red_bed", Map.of("facing", "east", "occupied", "true", "part", "head")),
         create("minecraft:bed", Map.of("facing", "east", "occupied", "true", "part", "head"))
      );
      register(432, create("minecraft:powered_rail", SHAPE_NORTH_SOUTH_POWERED_FALSE), create("minecraft:golden_rail", SHAPE_NORTH_SOUTH_POWERED_FALSE));
      register(433, create("minecraft:powered_rail", SHAPE_EAST_WEST_POWERED_FALSE), create("minecraft:golden_rail", SHAPE_EAST_WEST_POWERED_FALSE));
      register(434, create("minecraft:powered_rail", SHAPE_ASCENDING_EAST_POWERED_FALSE), create("minecraft:golden_rail", SHAPE_ASCENDING_EAST_POWERED_FALSE));
      register(435, create("minecraft:powered_rail", SHAPE_ASCENDING_WEST_POWERED_FALSE), create("minecraft:golden_rail", SHAPE_ASCENDING_WEST_POWERED_FALSE));
      register(436, create("minecraft:powered_rail", SHAPE_ASCENDING_NORTH_POWERED_FALSE), create("minecraft:golden_rail", SHAPE_ASCENDING_NORTH_POWERED_FALSE));
      register(437, create("minecraft:powered_rail", SHAPE_ASCENDING_SOUTH_POWERED_FALSE), create("minecraft:golden_rail", SHAPE_ASCENDING_SOUTH_POWERED_FALSE));
      register(440, create("minecraft:powered_rail", SHAPE_NORTH_SOUTH_POWERED_TRUE), create("minecraft:golden_rail", SHAPE_NORTH_SOUTH_POWERED_TRUE));
      register(441, create("minecraft:powered_rail", SHAPE_EAST_WEST_POWERED_TRUE), create("minecraft:golden_rail", SHAPE_EAST_WEST_POWERED_TRUE));
      register(442, create("minecraft:powered_rail", SHAPE_ASCENDING_EAST_POWERED_TRUE), create("minecraft:golden_rail", SHAPE_ASCENDING_EAST_POWERED_TRUE));
      register(443, create("minecraft:powered_rail", SHAPE_ASCENDING_WEST_POWERED_TRUE), create("minecraft:golden_rail", SHAPE_ASCENDING_WEST_POWERED_TRUE));
      register(444, create("minecraft:powered_rail", SHAPE_ASCENDING_NORTH_POWERED_TRUE), create("minecraft:golden_rail", SHAPE_ASCENDING_NORTH_POWERED_TRUE));
      register(445, create("minecraft:powered_rail", SHAPE_ASCENDING_SOUTH_POWERED_TRUE), create("minecraft:golden_rail", SHAPE_ASCENDING_SOUTH_POWERED_TRUE));
      register(448, create("minecraft:detector_rail", SHAPE_NORTH_SOUTH_POWERED_FALSE), create("minecraft:detector_rail", SHAPE_NORTH_SOUTH_POWERED_FALSE));
      register(449, create("minecraft:detector_rail", SHAPE_EAST_WEST_POWERED_FALSE), create("minecraft:detector_rail", SHAPE_EAST_WEST_POWERED_FALSE));
      register(
         450, create("minecraft:detector_rail", SHAPE_ASCENDING_EAST_POWERED_FALSE), create("minecraft:detector_rail", SHAPE_ASCENDING_EAST_POWERED_FALSE)
      );
      register(
         451, create("minecraft:detector_rail", SHAPE_ASCENDING_WEST_POWERED_FALSE), create("minecraft:detector_rail", SHAPE_ASCENDING_WEST_POWERED_FALSE)
      );
      register(
         452, create("minecraft:detector_rail", SHAPE_ASCENDING_NORTH_POWERED_FALSE), create("minecraft:detector_rail", SHAPE_ASCENDING_NORTH_POWERED_FALSE)
      );
      register(
         453, create("minecraft:detector_rail", SHAPE_ASCENDING_SOUTH_POWERED_FALSE), create("minecraft:detector_rail", SHAPE_ASCENDING_SOUTH_POWERED_FALSE)
      );
      register(456, create("minecraft:detector_rail", SHAPE_NORTH_SOUTH_POWERED_TRUE), create("minecraft:detector_rail", SHAPE_NORTH_SOUTH_POWERED_TRUE));
      register(457, create("minecraft:detector_rail", SHAPE_EAST_WEST_POWERED_TRUE), create("minecraft:detector_rail", SHAPE_EAST_WEST_POWERED_TRUE));
      register(458, create("minecraft:detector_rail", SHAPE_ASCENDING_EAST_POWERED_TRUE), create("minecraft:detector_rail", SHAPE_ASCENDING_EAST_POWERED_TRUE));
      register(459, create("minecraft:detector_rail", SHAPE_ASCENDING_WEST_POWERED_TRUE), create("minecraft:detector_rail", SHAPE_ASCENDING_WEST_POWERED_TRUE));
      register(
         460, create("minecraft:detector_rail", SHAPE_ASCENDING_NORTH_POWERED_TRUE), create("minecraft:detector_rail", SHAPE_ASCENDING_NORTH_POWERED_TRUE)
      );
      register(
         461, create("minecraft:detector_rail", SHAPE_ASCENDING_SOUTH_POWERED_TRUE), create("minecraft:detector_rail", SHAPE_ASCENDING_SOUTH_POWERED_TRUE)
      );
      register(464, create("minecraft:sticky_piston", FACING_DOWN_EXTENDED_FALSE), create("minecraft:sticky_piston", FACING_DOWN_EXTENDED_FALSE));
      register(465, create("minecraft:sticky_piston", FACING_UP_EXTENDED_FALSE), create("minecraft:sticky_piston", FACING_UP_EXTENDED_FALSE));
      register(466, create("minecraft:sticky_piston", FACING_NORTH_EXTENDED_FALSE), create("minecraft:sticky_piston", FACING_NORTH_EXTENDED_FALSE));
      register(467, create("minecraft:sticky_piston", FACING_SOUTH_EXTENDED_FALSE), create("minecraft:sticky_piston", FACING_SOUTH_EXTENDED_FALSE));
      register(468, create("minecraft:sticky_piston", FACING_WEST_EXTENDED_FALSE), create("minecraft:sticky_piston", FACING_WEST_EXTENDED_FALSE));
      register(469, create("minecraft:sticky_piston", FACING_EAST_EXTENDED_FALSE), create("minecraft:sticky_piston", FACING_EAST_EXTENDED_FALSE));
      register(472, create("minecraft:sticky_piston", FACING_DOWN_EXTENDED_TRUE), create("minecraft:sticky_piston", FACING_DOWN_EXTENDED_TRUE));
      register(473, create("minecraft:sticky_piston", FACING_UP_EXTENDED_TRUE), create("minecraft:sticky_piston", FACING_UP_EXTENDED_TRUE));
      register(474, create("minecraft:sticky_piston", FACING_NORTH_EXTENDED_TRUE), create("minecraft:sticky_piston", FACING_NORTH_EXTENDED_TRUE));
      register(475, create("minecraft:sticky_piston", FACING_SOUTH_EXTENDED_TRUE), create("minecraft:sticky_piston", FACING_SOUTH_EXTENDED_TRUE));
      register(476, create("minecraft:sticky_piston", FACING_WEST_EXTENDED_TRUE), create("minecraft:sticky_piston", FACING_WEST_EXTENDED_TRUE));
      register(477, create("minecraft:sticky_piston", FACING_EAST_EXTENDED_TRUE), create("minecraft:sticky_piston", FACING_EAST_EXTENDED_TRUE));
      register(480, create("minecraft:cobweb"), create("minecraft:web"));
      register(496, create("minecraft:dead_bush"), create("minecraft:tallgrass", Map.of("type", "dead_bush")));
      register(497, create("minecraft:grass"), create("minecraft:tallgrass", Map.of("type", "tall_grass")));
      register(498, create("minecraft:fern"), create("minecraft:tallgrass", Map.of("type", "fern")));
   }

   private static void bootstrap2() {
      register(512, create("minecraft:dead_bush"), create("minecraft:deadbush"));
      register(528, create("minecraft:piston", FACING_DOWN_EXTENDED_FALSE), create("minecraft:piston", FACING_DOWN_EXTENDED_FALSE));
      register(529, create("minecraft:piston", FACING_UP_EXTENDED_FALSE), create("minecraft:piston", FACING_UP_EXTENDED_FALSE));
      register(530, create("minecraft:piston", FACING_NORTH_EXTENDED_FALSE), create("minecraft:piston", FACING_NORTH_EXTENDED_FALSE));
      register(531, create("minecraft:piston", FACING_SOUTH_EXTENDED_FALSE), create("minecraft:piston", FACING_SOUTH_EXTENDED_FALSE));
      register(532, create("minecraft:piston", FACING_WEST_EXTENDED_FALSE), create("minecraft:piston", FACING_WEST_EXTENDED_FALSE));
      register(533, create("minecraft:piston", FACING_EAST_EXTENDED_FALSE), create("minecraft:piston", FACING_EAST_EXTENDED_FALSE));
      register(536, create("minecraft:piston", FACING_DOWN_EXTENDED_TRUE), create("minecraft:piston", FACING_DOWN_EXTENDED_TRUE));
      register(537, create("minecraft:piston", FACING_UP_EXTENDED_TRUE), create("minecraft:piston", FACING_UP_EXTENDED_TRUE));
      register(538, create("minecraft:piston", FACING_NORTH_EXTENDED_TRUE), create("minecraft:piston", FACING_NORTH_EXTENDED_TRUE));
      register(539, create("minecraft:piston", FACING_SOUTH_EXTENDED_TRUE), create("minecraft:piston", FACING_SOUTH_EXTENDED_TRUE));
      register(540, create("minecraft:piston", FACING_WEST_EXTENDED_TRUE), create("minecraft:piston", FACING_WEST_EXTENDED_TRUE));
      register(541, create("minecraft:piston", FACING_EAST_EXTENDED_TRUE), create("minecraft:piston", FACING_EAST_EXTENDED_TRUE));
      register(
         544,
         create("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "down", "short", "true", "type", "normal"))
      );
      register(
         545,
         create("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "up", "short", "true", "type", "normal"))
      );
      register(
         546,
         create("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "north", "short", "true", "type", "normal"))
      );
      register(
         547,
         create("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "south", "short", "true", "type", "normal"))
      );
      register(
         548,
         create("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "west", "short", "true", "type", "normal"))
      );
      register(
         549,
         create("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "normal")),
         create("minecraft:piston_head", Map.of("facing", "east", "short", "true", "type", "normal"))
      );
      register(
         552,
         create("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "down", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "down", "short", "true", "type", "sticky"))
      );
      register(
         553,
         create("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "up", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "up", "short", "true", "type", "sticky"))
      );
      register(
         554,
         create("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "north", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "north", "short", "true", "type", "sticky"))
      );
      register(
         555,
         create("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "south", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "south", "short", "true", "type", "sticky"))
      );
      register(
         556,
         create("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "west", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "west", "short", "true", "type", "sticky"))
      );
      register(
         557,
         create("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "east", "short", "false", "type", "sticky")),
         create("minecraft:piston_head", Map.of("facing", "east", "short", "true", "type", "sticky"))
      );
      register(560, create("minecraft:white_wool"), create("minecraft:wool", COLOR_WHITE));
      register(561, create("minecraft:orange_wool"), create("minecraft:wool", COLOR_ORANGE));
      register(562, create("minecraft:magenta_wool"), create("minecraft:wool", COLOR_MAGENTA));
      register(563, create("minecraft:light_blue_wool"), create("minecraft:wool", COLOR_LIGHT_BLUE));
      register(564, create("minecraft:yellow_wool"), create("minecraft:wool", COLOR_YELLOW));
      register(565, create("minecraft:lime_wool"), create("minecraft:wool", COLOR_LIME));
      register(566, create("minecraft:pink_wool"), create("minecraft:wool", COLOR_PINK));
      register(567, create("minecraft:gray_wool"), create("minecraft:wool", COLOR_GRAY));
      register(568, create("minecraft:light_gray_wool"), create("minecraft:wool", COLOR_SILVER));
      register(569, create("minecraft:cyan_wool"), create("minecraft:wool", COLOR_CYAN));
      register(570, create("minecraft:purple_wool"), create("minecraft:wool", COLOR_PURPLE));
      register(571, create("minecraft:blue_wool"), create("minecraft:wool", COLOR_BLUE));
      register(572, create("minecraft:brown_wool"), create("minecraft:wool", COLOR_BROWN));
      register(573, create("minecraft:green_wool"), create("minecraft:wool", COLOR_GREEN));
      register(574, create("minecraft:red_wool"), create("minecraft:wool", COLOR_RED));
      register(575, create("minecraft:black_wool"), create("minecraft:wool", COLOR_BLACK));
      register(
         576,
         create("minecraft:moving_piston", Map.of("facing", "down", "type", "normal")),
         create("minecraft:piston_extension", Map.of("facing", "down", "type", "normal"))
      );
      register(
         577,
         create("minecraft:moving_piston", Map.of("facing", "up", "type", "normal")),
         create("minecraft:piston_extension", Map.of("facing", "up", "type", "normal"))
      );
      register(
         578,
         create("minecraft:moving_piston", Map.of("facing", "north", "type", "normal")),
         create("minecraft:piston_extension", Map.of("facing", "north", "type", "normal"))
      );
      register(
         579,
         create("minecraft:moving_piston", Map.of("facing", "south", "type", "normal")),
         create("minecraft:piston_extension", Map.of("facing", "south", "type", "normal"))
      );
      register(
         580,
         create("minecraft:moving_piston", Map.of("facing", "west", "type", "normal")),
         create("minecraft:piston_extension", Map.of("facing", "west", "type", "normal"))
      );
      register(
         581,
         create("minecraft:moving_piston", Map.of("facing", "east", "type", "normal")),
         create("minecraft:piston_extension", Map.of("facing", "east", "type", "normal"))
      );
      register(
         584,
         create("minecraft:moving_piston", Map.of("facing", "down", "type", "sticky")),
         create("minecraft:piston_extension", Map.of("facing", "down", "type", "sticky"))
      );
      register(
         585,
         create("minecraft:moving_piston", Map.of("facing", "up", "type", "sticky")),
         create("minecraft:piston_extension", Map.of("facing", "up", "type", "sticky"))
      );
      register(
         586,
         create("minecraft:moving_piston", Map.of("facing", "north", "type", "sticky")),
         create("minecraft:piston_extension", Map.of("facing", "north", "type", "sticky"))
      );
      register(
         587,
         create("minecraft:moving_piston", Map.of("facing", "south", "type", "sticky")),
         create("minecraft:piston_extension", Map.of("facing", "south", "type", "sticky"))
      );
      register(
         588,
         create("minecraft:moving_piston", Map.of("facing", "west", "type", "sticky")),
         create("minecraft:piston_extension", Map.of("facing", "west", "type", "sticky"))
      );
      register(
         589,
         create("minecraft:moving_piston", Map.of("facing", "east", "type", "sticky")),
         create("minecraft:piston_extension", Map.of("facing", "east", "type", "sticky"))
      );
      register(592, create("minecraft:dandelion"), create("minecraft:yellow_flower", Map.of("type", "dandelion")));
      register(608, create("minecraft:poppy"), create("minecraft:red_flower", Map.of("type", "poppy")));
      register(609, create("minecraft:blue_orchid"), create("minecraft:red_flower", Map.of("type", "blue_orchid")));
      register(610, create("minecraft:allium"), create("minecraft:red_flower", Map.of("type", "allium")));
      register(611, create("minecraft:azure_bluet"), create("minecraft:red_flower", Map.of("type", "houstonia")));
      register(612, create("minecraft:red_tulip"), create("minecraft:red_flower", Map.of("type", "red_tulip")));
      register(613, create("minecraft:orange_tulip"), create("minecraft:red_flower", Map.of("type", "orange_tulip")));
      register(614, create("minecraft:white_tulip"), create("minecraft:red_flower", Map.of("type", "white_tulip")));
      register(615, create("minecraft:pink_tulip"), create("minecraft:red_flower", Map.of("type", "pink_tulip")));
      register(616, create("minecraft:oxeye_daisy"), create("minecraft:red_flower", Map.of("type", "oxeye_daisy")));
      register(624, create("minecraft:brown_mushroom"), create("minecraft:brown_mushroom"));
      register(640, create("minecraft:red_mushroom"), create("minecraft:red_mushroom"));
      register(656, create("minecraft:gold_block"), create("minecraft:gold_block"));
      register(672, create("minecraft:iron_block"), create("minecraft:iron_block"));
      register(688, create("minecraft:stone_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "stone")));
      register(689, create("minecraft:sandstone_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "sandstone")));
      register(
         690, create("minecraft:petrified_oak_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "wood_old"))
      );
      register(
         691, create("minecraft:cobblestone_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "cobblestone"))
      );
      register(692, create("minecraft:brick_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "brick")));
      register(
         693, create("minecraft:stone_brick_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "stone_brick"))
      );
      register(
         694, create("minecraft:nether_brick_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "nether_brick"))
      );
      register(695, create("minecraft:quartz_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "false", "variant", "quartz")));
      register(696, create("minecraft:smooth_stone"), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "stone")));
      register(697, create("minecraft:smooth_sandstone"), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "sandstone")));
      register(
         698, create("minecraft:petrified_oak_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "wood_old"))
      );
      register(
         699, create("minecraft:cobblestone_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "cobblestone"))
      );
      register(700, create("minecraft:brick_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "brick")));
      register(
         701, create("minecraft:stone_brick_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "stone_brick"))
      );
      register(
         702, create("minecraft:nether_brick_slab", TYPE_DOUBLE), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "nether_brick"))
      );
      register(703, create("minecraft:smooth_quartz"), create("minecraft:double_stone_slab", Map.of("seamless", "true", "variant", "quartz")));
      register(704, create("minecraft:stone_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "stone")));
      register(705, create("minecraft:sandstone_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "sandstone")));
      register(706, create("minecraft:petrified_oak_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "wood_old")));
      register(707, create("minecraft:cobblestone_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "cobblestone")));
      register(708, create("minecraft:brick_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "brick")));
      register(709, create("minecraft:stone_brick_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "stone_brick")));
      register(710, create("minecraft:nether_brick_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "nether_brick")));
      register(711, create("minecraft:quartz_slab", TYPE_BOTTOM), create("minecraft:stone_slab", Map.of("half", "bottom", "variant", "quartz")));
      register(712, create("minecraft:stone_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "stone")));
      register(713, create("minecraft:sandstone_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "sandstone")));
      register(714, create("minecraft:petrified_oak_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "wood_old")));
      register(715, create("minecraft:cobblestone_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "cobblestone")));
      register(716, create("minecraft:brick_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "brick")));
      register(717, create("minecraft:stone_brick_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "stone_brick")));
      register(718, create("minecraft:nether_brick_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "nether_brick")));
      register(719, create("minecraft:quartz_slab", TYPE_TOP), create("minecraft:stone_slab", Map.of("half", "top", "variant", "quartz")));
      register(720, create("minecraft:bricks"), create("minecraft:brick_block"));
      register(736, create("minecraft:tnt", Map.of("unstable", "false")), create("minecraft:tnt", Map.of("explode", "false")));
      register(737, create("minecraft:tnt", Map.of("unstable", "true")), create("minecraft:tnt", Map.of("explode", "true")));
      register(752, create("minecraft:bookshelf"), create("minecraft:bookshelf"));
   }

   private static void bootstrap3_1() {
      register(768, create("minecraft:mossy_cobblestone"), create("minecraft:mossy_cobblestone"));
      register(784, create("minecraft:obsidian"), create("minecraft:obsidian"));
      register(801, create("minecraft:wall_torch", FACING_EAST), create("minecraft:torch", FACING_EAST));
      register(802, create("minecraft:wall_torch", FACING_WEST), create("minecraft:torch", FACING_WEST));
      register(803, create("minecraft:wall_torch", FACING_SOUTH), create("minecraft:torch", FACING_SOUTH));
      register(804, create("minecraft:wall_torch", FACING_NORTH), create("minecraft:torch", FACING_NORTH));
      register(805, create("minecraft:torch"), create("minecraft:torch", FACING_UP));
      register(
         816,
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "0", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         817,
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "1", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         818,
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "2", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         819,
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "3", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         820,
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "4", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         821,
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "5", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         822,
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "6", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         823,
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "7", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         824,
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "8", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         825,
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "9", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         826,
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "10", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         827,
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "11", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         828,
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "12", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         829,
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "13", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         830,
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "14", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         831,
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:fire", Map.of("age", "15", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
   }

   private static void bootstrap3_2() {
      register(832, create("minecraft:mob_spawner"), create("minecraft:mob_spawner"));
      register(
         848,
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         849,
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         850,
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         851,
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         852,
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         853,
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         854,
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         855,
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(866, create("minecraft:chest", Map.of("facing", "north", "type", "single")), create("minecraft:chest", FACING_NORTH));
      register(867, create("minecraft:chest", Map.of("facing", "south", "type", "single")), create("minecraft:chest", FACING_SOUTH));
      register(868, create("minecraft:chest", Map.of("facing", "west", "type", "single")), create("minecraft:chest", FACING_WEST));
      register(869, create("minecraft:chest", Map.of("facing", "east", "type", "single")), create("minecraft:chest", FACING_EAST));
      register(
         880,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "0", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "0", "south", "up", "west", "up"))
      );
      register(
         881,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "1", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "1", "south", "up", "west", "up"))
      );
      register(
         882,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "2", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "2", "south", "up", "west", "up"))
      );
      register(
         883,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "3", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "3", "south", "up", "west", "up"))
      );
      register(
         884,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "4", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "4", "south", "up", "west", "up"))
      );
      register(
         885,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "5", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "5", "south", "up", "west", "up"))
      );
      register(
         886,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "6", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "6", "south", "up", "west", "up"))
      );
      register(
         887,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "7", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "7", "south", "up", "west", "up"))
      );
      register(
         888,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "8", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "8", "south", "up", "west", "up"))
      );
      register(
         889,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "9", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "9", "south", "up", "west", "up"))
      );
      register(
         890,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "10", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "10", "south", "up", "west", "up"))
      );
      register(
         891,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "11", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "11", "south", "up", "west", "up"))
      );
      register(
         892,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "12", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "12", "south", "up", "west", "up"))
      );
      register(
         893,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "13", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "13", "south", "up", "west", "up"))
      );
      register(
         894,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "14", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "14", "south", "up", "west", "up"))
      );
      register(
         895,
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "none", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "side", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "none", "north", "up", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "none", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "side", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "side", "north", "up", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "none", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "side", "power", "15", "south", "up", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "none", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "none", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "none", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "side", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "side", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "side", "west", "up")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "up", "west", "none")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "up", "west", "side")),
         create("minecraft:redstone_wire", Map.of("east", "up", "north", "up", "power", "15", "south", "up", "west", "up"))
      );
      register(896, create("minecraft:diamond_ore"), create("minecraft:diamond_ore"));
      register(912, create("minecraft:diamond_block"), create("minecraft:diamond_block"));
      register(928, create("minecraft:crafting_table"), create("minecraft:crafting_table"));
      register(944, create("minecraft:wheat", AGE_0), create("minecraft:wheat", AGE_0));
      register(945, create("minecraft:wheat", AGE_1), create("minecraft:wheat", AGE_1));
      register(946, create("minecraft:wheat", AGE_2), create("minecraft:wheat", AGE_2));
      register(947, create("minecraft:wheat", AGE_3), create("minecraft:wheat", AGE_3));
      register(948, create("minecraft:wheat", AGE_4), create("minecraft:wheat", AGE_4));
      register(949, create("minecraft:wheat", AGE_5), create("minecraft:wheat", AGE_5));
      register(950, create("minecraft:wheat", AGE_6), create("minecraft:wheat", AGE_6));
      register(951, create("minecraft:wheat", AGE_7), create("minecraft:wheat", AGE_7));
      register(960, create("minecraft:farmland", Map.of("moisture", "0")), create("minecraft:farmland", Map.of("moisture", "0")));
      register(961, create("minecraft:farmland", Map.of("moisture", "1")), create("minecraft:farmland", Map.of("moisture", "1")));
      register(962, create("minecraft:farmland", Map.of("moisture", "2")), create("minecraft:farmland", Map.of("moisture", "2")));
      register(963, create("minecraft:farmland", Map.of("moisture", "3")), create("minecraft:farmland", Map.of("moisture", "3")));
      register(964, create("minecraft:farmland", Map.of("moisture", "4")), create("minecraft:farmland", Map.of("moisture", "4")));
      register(965, create("minecraft:farmland", Map.of("moisture", "5")), create("minecraft:farmland", Map.of("moisture", "5")));
      register(966, create("minecraft:farmland", Map.of("moisture", "6")), create("minecraft:farmland", Map.of("moisture", "6")));
      register(967, create("minecraft:farmland", Map.of("moisture", "7")), create("minecraft:farmland", Map.of("moisture", "7")));
      register(978, create("minecraft:furnace", Map.of("facing", "north", "lit", "false")), create("minecraft:furnace", FACING_NORTH));
      register(979, create("minecraft:furnace", Map.of("facing", "south", "lit", "false")), create("minecraft:furnace", FACING_SOUTH));
      register(980, create("minecraft:furnace", Map.of("facing", "west", "lit", "false")), create("minecraft:furnace", FACING_WEST));
      register(981, create("minecraft:furnace", Map.of("facing", "east", "lit", "false")), create("minecraft:furnace", FACING_EAST));
      register(994, create("minecraft:furnace", Map.of("facing", "north", "lit", "true")), create("minecraft:lit_furnace", FACING_NORTH));
      register(995, create("minecraft:furnace", Map.of("facing", "south", "lit", "true")), create("minecraft:lit_furnace", FACING_SOUTH));
      register(996, create("minecraft:furnace", Map.of("facing", "west", "lit", "true")), create("minecraft:lit_furnace", FACING_WEST));
      register(997, create("minecraft:furnace", Map.of("facing", "east", "lit", "true")), create("minecraft:lit_furnace", FACING_EAST));
      register(1008, create("minecraft:sign", ROTATION_0), create("minecraft:standing_sign", ROTATION_0));
      register(1009, create("minecraft:sign", ROTATION_1), create("minecraft:standing_sign", ROTATION_1));
      register(1010, create("minecraft:sign", ROTATION_2), create("minecraft:standing_sign", ROTATION_2));
      register(1011, create("minecraft:sign", ROTATION_3), create("minecraft:standing_sign", ROTATION_3));
      register(1012, create("minecraft:sign", ROTATION_4), create("minecraft:standing_sign", ROTATION_4));
      register(1013, create("minecraft:sign", ROTATION_5), create("minecraft:standing_sign", ROTATION_5));
      register(1014, create("minecraft:sign", ROTATION_6), create("minecraft:standing_sign", ROTATION_6));
      register(1015, create("minecraft:sign", ROTATION_7), create("minecraft:standing_sign", ROTATION_7));
      register(1016, create("minecraft:sign", ROTATION_8), create("minecraft:standing_sign", ROTATION_8));
      register(1017, create("minecraft:sign", ROTATION_9), create("minecraft:standing_sign", ROTATION_9));
      register(1018, create("minecraft:sign", ROTATION_10), create("minecraft:standing_sign", ROTATION_10));
      register(1019, create("minecraft:sign", ROTATION_11), create("minecraft:standing_sign", ROTATION_11));
      register(1020, create("minecraft:sign", ROTATION_12), create("minecraft:standing_sign", ROTATION_12));
      register(1021, create("minecraft:sign", ROTATION_13), create("minecraft:standing_sign", ROTATION_13));
      register(1022, create("minecraft:sign", ROTATION_14), create("minecraft:standing_sign", ROTATION_14));
      register(1023, create("minecraft:sign", ROTATION_15), create("minecraft:standing_sign", ROTATION_15));
   }

   private static void bootstrap4() {
      register(
         1024,
         create("minecraft:oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1025,
         create("minecraft:oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1026,
         create("minecraft:oak_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1027,
         create("minecraft:oak_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1028,
         create("minecraft:oak_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1029,
         create("minecraft:oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1030,
         create("minecraft:oak_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1031,
         create("minecraft:oak_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1032,
         create("minecraft:oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         1033,
         create("minecraft:oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         1034,
         create("minecraft:oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         1035,
         create("minecraft:oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:wooden_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER)
      );
      register(1036, create("minecraft:oak_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1037, create("minecraft:oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1038, create("minecraft:oak_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1039, create("minecraft:oak_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1042, create("minecraft:ladder", FACING_NORTH), create("minecraft:ladder", FACING_NORTH));
      register(1043, create("minecraft:ladder", FACING_SOUTH), create("minecraft:ladder", FACING_SOUTH));
      register(1044, create("minecraft:ladder", FACING_WEST), create("minecraft:ladder", FACING_WEST));
      register(1045, create("minecraft:ladder", FACING_EAST), create("minecraft:ladder", FACING_EAST));
      register(1056, create("minecraft:rail", Map.of("shape", "north_south")), create("minecraft:rail", Map.of("shape", "north_south")));
      register(1057, create("minecraft:rail", Map.of("shape", "east_west")), create("minecraft:rail", Map.of("shape", "east_west")));
      register(1058, create("minecraft:rail", Map.of("shape", "ascending_east")), create("minecraft:rail", Map.of("shape", "ascending_east")));
      register(1059, create("minecraft:rail", Map.of("shape", "ascending_west")), create("minecraft:rail", Map.of("shape", "ascending_west")));
      register(1060, create("minecraft:rail", Map.of("shape", "ascending_north")), create("minecraft:rail", Map.of("shape", "ascending_north")));
      register(1061, create("minecraft:rail", Map.of("shape", "ascending_south")), create("minecraft:rail", Map.of("shape", "ascending_south")));
      register(1062, create("minecraft:rail", Map.of("shape", "south_east")), create("minecraft:rail", Map.of("shape", "south_east")));
      register(1063, create("minecraft:rail", Map.of("shape", "south_west")), create("minecraft:rail", Map.of("shape", "south_west")));
      register(1064, create("minecraft:rail", Map.of("shape", "north_west")), create("minecraft:rail", Map.of("shape", "north_west")));
      register(1065, create("minecraft:rail", Map.of("shape", "north_east")), create("minecraft:rail", Map.of("shape", "north_east")));
      register(
         1072,
         create("minecraft:cobblestone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1073,
         create("minecraft:cobblestone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1074,
         create("minecraft:cobblestone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1075,
         create("minecraft:cobblestone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         1076,
         create("minecraft:cobblestone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1077,
         create("minecraft:cobblestone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1078,
         create("minecraft:cobblestone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1079,
         create("minecraft:cobblestone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:stone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(1090, create("minecraft:wall_sign", FACING_NORTH), create("minecraft:wall_sign", FACING_NORTH));
      register(1091, create("minecraft:wall_sign", FACING_SOUTH), create("minecraft:wall_sign", FACING_SOUTH));
      register(1092, create("minecraft:wall_sign", FACING_WEST), create("minecraft:wall_sign", FACING_WEST));
      register(1093, create("minecraft:wall_sign", FACING_EAST), create("minecraft:wall_sign", FACING_EAST));
      register(
         1104,
         create("minecraft:lever", Map.of("face", "ceiling", "facing", "west", "powered", "false")),
         create("minecraft:lever", Map.of("facing", "down_x", "powered", "false"))
      );
      register(1105, create("minecraft:lever", FACE_WALL_POWERED_FALSE_FACING_EAST), create("minecraft:lever", FACING_EAST_POWERED_FALSE));
      register(1106, create("minecraft:lever", FACE_WALL_POWERED_FALSE_FACING_WEST), create("minecraft:lever", FACING_WEST_POWERED_FALSE));
      register(1107, create("minecraft:lever", FACE_WALL_POWERED_FALSE_FACING_SOUTH), create("minecraft:lever", FACING_SOUTH_POWERED_FALSE));
      register(1108, create("minecraft:lever", FACE_WALL_POWERED_FALSE_FACING_NORTH), create("minecraft:lever", FACING_NORTH_POWERED_FALSE));
      register(1109, create("minecraft:lever", FACE_FLOOR_POWERED_FALSE_FACING_NORTH), create("minecraft:lever", Map.of("facing", "up_z", "powered", "false")));
      register(
         1110,
         create("minecraft:lever", Map.of("face", "floor", "facing", "west", "powered", "false")),
         create("minecraft:lever", Map.of("facing", "up_x", "powered", "false"))
      );
      register(
         1111, create("minecraft:lever", FACE_CEILING_POWERED_FALSE_FACING_NORTH), create("minecraft:lever", Map.of("facing", "down_z", "powered", "false"))
      );
      register(
         1112,
         create("minecraft:lever", Map.of("face", "ceiling", "facing", "west", "powered", "true")),
         create("minecraft:lever", Map.of("facing", "down_x", "powered", "true"))
      );
      register(1113, create("minecraft:lever", FACE_WALL_POWERED_TRUE_FACING_EAST), create("minecraft:lever", FACING_EAST_POWERED_TRUE));
      register(1114, create("minecraft:lever", FACE_WALL_POWERED_TRUE_FACING_WEST), create("minecraft:lever", FACING_WEST_POWERED_TRUE));
      register(1115, create("minecraft:lever", FACE_WALL_POWERED_TRUE_FACING_SOUTH), create("minecraft:lever", FACING_SOUTH_POWERED_TRUE));
      register(1116, create("minecraft:lever", FACE_WALL_POWERED_TRUE_FACING_NORTH), create("minecraft:lever", FACING_NORTH_POWERED_TRUE));
      register(1117, create("minecraft:lever", FACE_FLOOR_POWERED_TRUE_FACING_NORTH), create("minecraft:lever", Map.of("facing", "up_z", "powered", "true")));
      register(
         1118,
         create("minecraft:lever", Map.of("face", "floor", "facing", "west", "powered", "true")),
         create("minecraft:lever", Map.of("facing", "up_x", "powered", "true"))
      );
      register(
         1119, create("minecraft:lever", FACE_CEILING_POWERED_TRUE_FACING_NORTH), create("minecraft:lever", Map.of("facing", "down_z", "powered", "true"))
      );
      register(1120, create("minecraft:stone_pressure_plate", POWERED_FALSE), create("minecraft:stone_pressure_plate", POWERED_FALSE));
      register(1121, create("minecraft:stone_pressure_plate", POWERED_TRUE), create("minecraft:stone_pressure_plate", POWERED_TRUE));
      register(
         1136,
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1137,
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1138,
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1139,
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1140,
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1141,
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1142,
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1143,
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         1144,
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         1145,
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         1146,
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         1147,
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER)
      );
      register(1148, create("minecraft:iron_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1149, create("minecraft:iron_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1150, create("minecraft:iron_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1151, create("minecraft:iron_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER));
      register(1152, create("minecraft:oak_pressure_plate", POWERED_FALSE), create("minecraft:wooden_pressure_plate", POWERED_FALSE));
      register(1153, create("minecraft:oak_pressure_plate", POWERED_TRUE), create("minecraft:wooden_pressure_plate", POWERED_TRUE));
      register(1168, create("minecraft:redstone_ore", LIT_FALSE), create("minecraft:redstone_ore"));
      register(1184, create("minecraft:redstone_ore", LIT_TRUE), create("minecraft:lit_redstone_ore"));
      register(1201, create("minecraft:redstone_wall_torch", Map.of("facing", "east", "lit", "false")), create("minecraft:unlit_redstone_torch", FACING_EAST));
      register(1202, create("minecraft:redstone_wall_torch", Map.of("facing", "west", "lit", "false")), create("minecraft:unlit_redstone_torch", FACING_WEST));
      register(1203, create("minecraft:redstone_wall_torch", Map.of("facing", "south", "lit", "false")), create("minecraft:unlit_redstone_torch", FACING_SOUTH));
      register(1204, create("minecraft:redstone_wall_torch", Map.of("facing", "north", "lit", "false")), create("minecraft:unlit_redstone_torch", FACING_NORTH));
      register(1205, create("minecraft:redstone_torch", LIT_FALSE), create("minecraft:unlit_redstone_torch", FACING_UP));
      register(1217, create("minecraft:redstone_wall_torch", Map.of("facing", "east", "lit", "true")), create("minecraft:redstone_torch", FACING_EAST));
      register(1218, create("minecraft:redstone_wall_torch", Map.of("facing", "west", "lit", "true")), create("minecraft:redstone_torch", FACING_WEST));
      register(1219, create("minecraft:redstone_wall_torch", Map.of("facing", "south", "lit", "true")), create("minecraft:redstone_torch", FACING_SOUTH));
      register(1220, create("minecraft:redstone_wall_torch", Map.of("facing", "north", "lit", "true")), create("minecraft:redstone_torch", FACING_NORTH));
      register(1221, create("minecraft:redstone_torch", LIT_TRUE), create("minecraft:redstone_torch", FACING_UP));
      register(1232, create("minecraft:stone_button", FACE_CEILING_POWERED_FALSE_FACING_NORTH), create("minecraft:stone_button", FACING_DOWN_POWERED_FALSE));
      register(1233, create("minecraft:stone_button", FACE_WALL_POWERED_FALSE_FACING_EAST), create("minecraft:stone_button", FACING_EAST_POWERED_FALSE));
      register(1234, create("minecraft:stone_button", FACE_WALL_POWERED_FALSE_FACING_WEST), create("minecraft:stone_button", FACING_WEST_POWERED_FALSE));
      register(1235, create("minecraft:stone_button", FACE_WALL_POWERED_FALSE_FACING_SOUTH), create("minecraft:stone_button", FACING_SOUTH_POWERED_FALSE));
      register(1236, create("minecraft:stone_button", FACE_WALL_POWERED_FALSE_FACING_NORTH), create("minecraft:stone_button", FACING_NORTH_POWERED_FALSE));
      register(1237, create("minecraft:stone_button", FACE_FLOOR_POWERED_FALSE_FACING_NORTH), create("minecraft:stone_button", FACING_UP_POWERED_FALSE));
      register(1240, create("minecraft:stone_button", FACE_CEILING_POWERED_TRUE_FACING_NORTH), create("minecraft:stone_button", FACING_DOWN_POWERED_TRUE));
      register(1241, create("minecraft:stone_button", FACE_WALL_POWERED_TRUE_FACING_EAST), create("minecraft:stone_button", FACING_EAST_POWERED_TRUE));
      register(1242, create("minecraft:stone_button", FACE_WALL_POWERED_TRUE_FACING_WEST), create("minecraft:stone_button", FACING_WEST_POWERED_TRUE));
      register(1243, create("minecraft:stone_button", FACE_WALL_POWERED_TRUE_FACING_SOUTH), create("minecraft:stone_button", FACING_SOUTH_POWERED_TRUE));
      register(1244, create("minecraft:stone_button", FACE_WALL_POWERED_TRUE_FACING_NORTH), create("minecraft:stone_button", FACING_NORTH_POWERED_TRUE));
      register(1245, create("minecraft:stone_button", FACE_FLOOR_POWERED_TRUE_FACING_NORTH), create("minecraft:stone_button", FACING_UP_POWERED_TRUE));
      register(1248, create("minecraft:snow", Map.of("layers", "1")), create("minecraft:snow_layer", Map.of("layers", "1")));
      register(1249, create("minecraft:snow", Map.of("layers", "2")), create("minecraft:snow_layer", Map.of("layers", "2")));
      register(1250, create("minecraft:snow", Map.of("layers", "3")), create("minecraft:snow_layer", Map.of("layers", "3")));
      register(1251, create("minecraft:snow", Map.of("layers", "4")), create("minecraft:snow_layer", Map.of("layers", "4")));
      register(1252, create("minecraft:snow", Map.of("layers", "5")), create("minecraft:snow_layer", Map.of("layers", "5")));
      register(1253, create("minecraft:snow", Map.of("layers", "6")), create("minecraft:snow_layer", Map.of("layers", "6")));
      register(1254, create("minecraft:snow", Map.of("layers", "7")), create("minecraft:snow_layer", Map.of("layers", "7")));
      register(1255, create("minecraft:snow", Map.of("layers", "8")), create("minecraft:snow_layer", Map.of("layers", "8")));
      register(1264, create("minecraft:ice"), create("minecraft:ice"));
   }

   private static void bootstrap5() {
      register(1280, create("minecraft:snow_block"), create("minecraft:snow"));
      register(1296, create("minecraft:cactus", AGE_0), create("minecraft:cactus", AGE_0));
      register(1297, create("minecraft:cactus", AGE_1), create("minecraft:cactus", AGE_1));
      register(1298, create("minecraft:cactus", AGE_2), create("minecraft:cactus", AGE_2));
      register(1299, create("minecraft:cactus", AGE_3), create("minecraft:cactus", AGE_3));
      register(1300, create("minecraft:cactus", AGE_4), create("minecraft:cactus", AGE_4));
      register(1301, create("minecraft:cactus", AGE_5), create("minecraft:cactus", AGE_5));
      register(1302, create("minecraft:cactus", AGE_6), create("minecraft:cactus", AGE_6));
      register(1303, create("minecraft:cactus", AGE_7), create("minecraft:cactus", AGE_7));
      register(1304, create("minecraft:cactus", AGE_8), create("minecraft:cactus", AGE_8));
      register(1305, create("minecraft:cactus", AGE_9), create("minecraft:cactus", AGE_9));
      register(1306, create("minecraft:cactus", AGE_10), create("minecraft:cactus", AGE_10));
      register(1307, create("minecraft:cactus", AGE_11), create("minecraft:cactus", AGE_11));
      register(1308, create("minecraft:cactus", AGE_12), create("minecraft:cactus", AGE_12));
      register(1309, create("minecraft:cactus", AGE_13), create("minecraft:cactus", AGE_13));
      register(1310, create("minecraft:cactus", AGE_14), create("minecraft:cactus", AGE_14));
      register(1311, create("minecraft:cactus", AGE_15), create("minecraft:cactus", AGE_15));
      register(1312, create("minecraft:clay"), create("minecraft:clay"));
      register(1328, create("minecraft:sugar_cane", AGE_0), create("minecraft:reeds", AGE_0));
      register(1329, create("minecraft:sugar_cane", AGE_1), create("minecraft:reeds", AGE_1));
      register(1330, create("minecraft:sugar_cane", AGE_2), create("minecraft:reeds", AGE_2));
      register(1331, create("minecraft:sugar_cane", AGE_3), create("minecraft:reeds", AGE_3));
      register(1332, create("minecraft:sugar_cane", AGE_4), create("minecraft:reeds", AGE_4));
      register(1333, create("minecraft:sugar_cane", AGE_5), create("minecraft:reeds", AGE_5));
      register(1334, create("minecraft:sugar_cane", AGE_6), create("minecraft:reeds", AGE_6));
      register(1335, create("minecraft:sugar_cane", AGE_7), create("minecraft:reeds", AGE_7));
      register(1336, create("minecraft:sugar_cane", AGE_8), create("minecraft:reeds", AGE_8));
      register(1337, create("minecraft:sugar_cane", AGE_9), create("minecraft:reeds", AGE_9));
      register(1338, create("minecraft:sugar_cane", AGE_10), create("minecraft:reeds", AGE_10));
      register(1339, create("minecraft:sugar_cane", AGE_11), create("minecraft:reeds", AGE_11));
      register(1340, create("minecraft:sugar_cane", AGE_12), create("minecraft:reeds", AGE_12));
      register(1341, create("minecraft:sugar_cane", AGE_13), create("minecraft:reeds", AGE_13));
      register(1342, create("minecraft:sugar_cane", AGE_14), create("minecraft:reeds", AGE_14));
      register(1343, create("minecraft:sugar_cane", AGE_15), create("minecraft:reeds", AGE_15));
      register(1344, create("minecraft:jukebox", Map.of("has_record", "false")), create("minecraft:jukebox", Map.of("has_record", "false")));
      register(1345, create("minecraft:jukebox", Map.of("has_record", "true")), create("minecraft:jukebox", Map.of("has_record", "true")));
      register(
         1360,
         create("minecraft:oak_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(1376, create("minecraft:carved_pumpkin", FACING_SOUTH), create("minecraft:pumpkin", FACING_SOUTH));
      register(1377, create("minecraft:carved_pumpkin", FACING_WEST), create("minecraft:pumpkin", FACING_WEST));
      register(1378, create("minecraft:carved_pumpkin", FACING_NORTH), create("minecraft:pumpkin", FACING_NORTH));
      register(1379, create("minecraft:carved_pumpkin", FACING_EAST), create("minecraft:pumpkin", FACING_EAST));
      register(1392, create("minecraft:netherrack"), create("minecraft:netherrack"));
      register(1408, create("minecraft:soul_sand"), create("minecraft:soul_sand"));
      register(1424, create("minecraft:glowstone"), create("minecraft:glowstone"));
      register(1441, create("minecraft:portal", AXIS_X), create("minecraft:portal", AXIS_X));
      register(1442, create("minecraft:portal", AXIS_Z), create("minecraft:portal", AXIS_Z));
      register(1456, create("minecraft:jack_o_lantern", FACING_SOUTH), create("minecraft:lit_pumpkin", FACING_SOUTH));
      register(1457, create("minecraft:jack_o_lantern", FACING_WEST), create("minecraft:lit_pumpkin", FACING_WEST));
      register(1458, create("minecraft:jack_o_lantern", FACING_NORTH), create("minecraft:lit_pumpkin", FACING_NORTH));
      register(1459, create("minecraft:jack_o_lantern", FACING_EAST), create("minecraft:lit_pumpkin", FACING_EAST));
      register(1472, create("minecraft:cake", Map.of("bites", "0")), create("minecraft:cake", Map.of("bites", "0")));
      register(1473, create("minecraft:cake", Map.of("bites", "1")), create("minecraft:cake", Map.of("bites", "1")));
      register(1474, create("minecraft:cake", Map.of("bites", "2")), create("minecraft:cake", Map.of("bites", "2")));
      register(1475, create("minecraft:cake", Map.of("bites", "3")), create("minecraft:cake", Map.of("bites", "3")));
      register(1476, create("minecraft:cake", Map.of("bites", "4")), create("minecraft:cake", Map.of("bites", "4")));
      register(1477, create("minecraft:cake", Map.of("bites", "5")), create("minecraft:cake", Map.of("bites", "5")));
      register(1478, create("minecraft:cake", Map.of("bites", "6")), create("minecraft:cake", Map.of("bites", "6")));
      register(
         1488,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "south", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "south", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "south", "locked", "true"))
      );
      register(
         1489,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "west", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "west", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "west", "locked", "true"))
      );
      register(
         1490,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "north", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "north", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "north", "locked", "true"))
      );
      register(
         1491,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "east", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "east", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "1", "facing", "east", "locked", "true"))
      );
      register(
         1492,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "south", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "south", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "south", "locked", "true"))
      );
      register(
         1493,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "west", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "west", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "west", "locked", "true"))
      );
      register(
         1494,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "north", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "north", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "north", "locked", "true"))
      );
      register(
         1495,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "east", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "east", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "2", "facing", "east", "locked", "true"))
      );
      register(
         1496,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "south", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "south", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "south", "locked", "true"))
      );
      register(
         1497,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "west", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "west", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "west", "locked", "true"))
      );
      register(
         1498,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "north", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "north", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "north", "locked", "true"))
      );
      register(
         1499,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "east", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "east", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "3", "facing", "east", "locked", "true"))
      );
      register(
         1500,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "south", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "south", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "south", "locked", "true"))
      );
      register(
         1501,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "west", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "west", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "west", "locked", "true"))
      );
      register(
         1502,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "north", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "north", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "north", "locked", "true"))
      );
      register(
         1503,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "east", "locked", "false", "powered", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "east", "locked", "false")),
         create("minecraft:unpowered_repeater", Map.of("delay", "4", "facing", "east", "locked", "true"))
      );
      register(
         1504,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "south", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "south", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "south", "locked", "true"))
      );
      register(
         1505,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "west", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "west", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "west", "locked", "true"))
      );
      register(
         1506,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "north", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "north", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "north", "locked", "true"))
      );
      register(
         1507,
         create("minecraft:repeater", Map.of("delay", "1", "facing", "east", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "east", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "1", "facing", "east", "locked", "true"))
      );
      register(
         1508,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "south", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "south", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "south", "locked", "true"))
      );
      register(
         1509,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "west", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "west", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "west", "locked", "true"))
      );
      register(
         1510,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "north", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "north", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "north", "locked", "true"))
      );
      register(
         1511,
         create("minecraft:repeater", Map.of("delay", "2", "facing", "east", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "east", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "2", "facing", "east", "locked", "true"))
      );
      register(
         1512,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "south", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "south", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "south", "locked", "true"))
      );
      register(
         1513,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "west", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "west", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "west", "locked", "true"))
      );
      register(
         1514,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "north", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "north", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "north", "locked", "true"))
      );
      register(
         1515,
         create("minecraft:repeater", Map.of("delay", "3", "facing", "east", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "east", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "3", "facing", "east", "locked", "true"))
      );
      register(
         1516,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "south", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "south", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "south", "locked", "true"))
      );
      register(
         1517,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "west", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "west", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "west", "locked", "true"))
      );
      register(
         1518,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "north", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "north", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "north", "locked", "true"))
      );
      register(
         1519,
         create("minecraft:repeater", Map.of("delay", "4", "facing", "east", "locked", "false", "powered", "true")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "east", "locked", "false")),
         create("minecraft:powered_repeater", Map.of("delay", "4", "facing", "east", "locked", "true"))
      );
      register(1520, create("minecraft:white_stained_glass"), create("minecraft:stained_glass", COLOR_WHITE));
      register(1521, create("minecraft:orange_stained_glass"), create("minecraft:stained_glass", COLOR_ORANGE));
      register(1522, create("minecraft:magenta_stained_glass"), create("minecraft:stained_glass", COLOR_MAGENTA));
      register(1523, create("minecraft:light_blue_stained_glass"), create("minecraft:stained_glass", COLOR_LIGHT_BLUE));
      register(1524, create("minecraft:yellow_stained_glass"), create("minecraft:stained_glass", COLOR_YELLOW));
      register(1525, create("minecraft:lime_stained_glass"), create("minecraft:stained_glass", COLOR_LIME));
      register(1526, create("minecraft:pink_stained_glass"), create("minecraft:stained_glass", COLOR_PINK));
      register(1527, create("minecraft:gray_stained_glass"), create("minecraft:stained_glass", COLOR_GRAY));
      register(1528, create("minecraft:light_gray_stained_glass"), create("minecraft:stained_glass", COLOR_SILVER));
      register(1529, create("minecraft:cyan_stained_glass"), create("minecraft:stained_glass", COLOR_CYAN));
      register(1530, create("minecraft:purple_stained_glass"), create("minecraft:stained_glass", COLOR_PURPLE));
      register(1531, create("minecraft:blue_stained_glass"), create("minecraft:stained_glass", COLOR_BLUE));
      register(1532, create("minecraft:brown_stained_glass"), create("minecraft:stained_glass", COLOR_BROWN));
      register(1533, create("minecraft:green_stained_glass"), create("minecraft:stained_glass", COLOR_GREEN));
      register(1534, create("minecraft:red_stained_glass"), create("minecraft:stained_glass", COLOR_RED));
      register(1535, create("minecraft:black_stained_glass"), create("minecraft:stained_glass", COLOR_BLACK));
   }

   private static void bootstrap6() {
      register(1536, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_NORTH), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_NORTH));
      register(1537, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_SOUTH), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_SOUTH));
      register(1538, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_WEST), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_WEST));
      register(1539, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_EAST), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_EAST));
      register(1540, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_NORTH), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_NORTH));
      register(1541, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_SOUTH), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_SOUTH));
      register(1542, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_WEST), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_WEST));
      register(1543, create("minecraft:oak_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_EAST), create("minecraft:trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_EAST));
      register(1544, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_FALSE_FACING_NORTH), create("minecraft:trapdoor", HALF_TOP_OPEN_FALSE_FACING_NORTH));
      register(1545, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_FALSE_FACING_SOUTH), create("minecraft:trapdoor", HALF_TOP_OPEN_FALSE_FACING_SOUTH));
      register(1546, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_FALSE_FACING_WEST), create("minecraft:trapdoor", HALF_TOP_OPEN_FALSE_FACING_WEST));
      register(1547, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_FALSE_FACING_EAST), create("minecraft:trapdoor", HALF_TOP_OPEN_FALSE_FACING_EAST));
      register(1548, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_TRUE_FACING_NORTH), create("minecraft:trapdoor", HALF_TOP_OPEN_TRUE_FACING_NORTH));
      register(1549, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_TRUE_FACING_SOUTH), create("minecraft:trapdoor", HALF_TOP_OPEN_TRUE_FACING_SOUTH));
      register(1550, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_TRUE_FACING_WEST), create("minecraft:trapdoor", HALF_TOP_OPEN_TRUE_FACING_WEST));
      register(1551, create("minecraft:oak_trapdoor", HALF_TOP_OPEN_TRUE_FACING_EAST), create("minecraft:trapdoor", HALF_TOP_OPEN_TRUE_FACING_EAST));
      register(1552, create("minecraft:infested_stone"), create("minecraft:monster_egg", Map.of("variant", "stone")));
      register(1553, create("minecraft:infested_cobblestone"), create("minecraft:monster_egg", Map.of("variant", "cobblestone")));
      register(1554, create("minecraft:infested_stone_bricks"), create("minecraft:monster_egg", Map.of("variant", "stone_brick")));
      register(1555, create("minecraft:infested_mossy_stone_bricks"), create("minecraft:monster_egg", Map.of("variant", "mossy_brick")));
      register(1556, create("minecraft:infested_cracked_stone_bricks"), create("minecraft:monster_egg", Map.of("variant", "cracked_brick")));
      register(1557, create("minecraft:infested_chiseled_stone_bricks"), create("minecraft:monster_egg", Map.of("variant", "chiseled_brick")));
      register(1568, create("minecraft:stone_bricks"), create("minecraft:stonebrick", Map.of("variant", "stonebrick")));
      register(1569, create("minecraft:mossy_stone_bricks"), create("minecraft:stonebrick", Map.of("variant", "mossy_stonebrick")));
      register(1570, create("minecraft:cracked_stone_bricks"), create("minecraft:stonebrick", Map.of("variant", "cracked_stonebrick")));
      register(1571, create("minecraft:chiseled_stone_bricks"), create("minecraft:stonebrick", Map.of("variant", "chiseled_stonebrick")));
      register(
         1584,
         create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "all_inside"))
      );
      register(
         1585,
         create("minecraft:brown_mushroom_block", NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "north_west"))
      );
      register(
         1586,
         create("minecraft:brown_mushroom_block", NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "north"))
      );
      register(
         1587,
         create("minecraft:brown_mushroom_block", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "north_east"))
      );
      register(
         1588,
         create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "west"))
      );
      register(
         1589,
         create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "center"))
      );
      register(
         1590,
         create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "east"))
      );
      register(
         1591,
         create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "south_west"))
      );
      register(
         1592,
         create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "south"))
      );
      register(
         1593,
         create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "south_east"))
      );
      register(
         1594,
         create("minecraft:mushroom_stem", NORTH_TRUE_EAST_TRUE_UP_FALSE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "stem"))
      );
      register(1595, create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE));
      register(1596, create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE));
      register(1597, create("minecraft:brown_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE));
      register(
         1598,
         create("minecraft:brown_mushroom_block", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_TRUE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "all_outside"))
      );
      register(
         1599,
         create("minecraft:mushroom_stem", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_TRUE),
         create("minecraft:brown_mushroom_block", Map.of("variant", "all_stem"))
      );
      register(
         1600,
         create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "all_inside"))
      );
      register(
         1601,
         create("minecraft:red_mushroom_block", NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "north_west"))
      );
      register(
         1602,
         create("minecraft:red_mushroom_block", NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "north"))
      );
      register(
         1603,
         create("minecraft:red_mushroom_block", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "north_east"))
      );
      register(
         1604,
         create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "west"))
      );
      register(
         1605,
         create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "center"))
      );
      register(
         1606,
         create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "east"))
      );
      register(
         1607,
         create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "south_west"))
      );
      register(
         1608,
         create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "south"))
      );
      register(
         1609,
         create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "south_east"))
      );
      register(
         1610,
         create("minecraft:mushroom_stem", NORTH_TRUE_EAST_TRUE_UP_FALSE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:red_mushroom_block", Map.of("variant", "stem"))
      );
      register(1611, create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE));
      register(1612, create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE));
      register(1613, create("minecraft:red_mushroom_block", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE));
      register(
         1614,
         create("minecraft:red_mushroom_block", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_TRUE),
         create("minecraft:red_mushroom_block", Map.of("variant", "all_outside"))
      );
      register(
         1615,
         create("minecraft:mushroom_stem", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_TRUE),
         create("minecraft:red_mushroom_block", Map.of("variant", "all_stem"))
      );
      register(
         1616,
         create("minecraft:iron_bars", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:iron_bars", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:iron_bars", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(
         1632,
         create("minecraft:glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:glass_pane", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:glass_pane", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(1648, create("minecraft:melon_block"), create("minecraft:melon_block"));
      register(
         1664,
         create("minecraft:pumpkin_stem", AGE_0),
         create("minecraft:pumpkin_stem", AGE_0_FACING_EAST),
         create("minecraft:pumpkin_stem", AGE_0_FACING_NORTH),
         create("minecraft:pumpkin_stem", AGE_0_FACING_SOUTH),
         create("minecraft:pumpkin_stem", Map.of("age", "0", "facing", "up")),
         create("minecraft:pumpkin_stem", AGE_0_FACING_WEST)
      );
      register(
         1665,
         create("minecraft:pumpkin_stem", AGE_1),
         create("minecraft:pumpkin_stem", AGE_1_FACING_EAST),
         create("minecraft:pumpkin_stem", AGE_1_FACING_NORTH),
         create("minecraft:pumpkin_stem", AGE_1_FACING_SOUTH),
         create("minecraft:pumpkin_stem", Map.of("age", "1", "facing", "up")),
         create("minecraft:pumpkin_stem", AGE_1_FACING_WEST)
      );
      register(
         1666,
         create("minecraft:pumpkin_stem", AGE_2),
         create("minecraft:pumpkin_stem", AGE_2_FACING_EAST),
         create("minecraft:pumpkin_stem", AGE_2_FACING_NORTH),
         create("minecraft:pumpkin_stem", AGE_2_FACING_SOUTH),
         create("minecraft:pumpkin_stem", Map.of("age", "2", "facing", "up")),
         create("minecraft:pumpkin_stem", AGE_2_FACING_WEST)
      );
      register(
         1667,
         create("minecraft:pumpkin_stem", AGE_3),
         create("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "east")),
         create("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "north")),
         create("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "south")),
         create("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "up")),
         create("minecraft:pumpkin_stem", Map.of("age", "3", "facing", "west"))
      );
      register(
         1668,
         create("minecraft:pumpkin_stem", AGE_4),
         create("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "east")),
         create("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "north")),
         create("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "south")),
         create("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "up")),
         create("minecraft:pumpkin_stem", Map.of("age", "4", "facing", "west"))
      );
      register(
         1669,
         create("minecraft:pumpkin_stem", AGE_5),
         create("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "east")),
         create("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "north")),
         create("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "south")),
         create("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "up")),
         create("minecraft:pumpkin_stem", Map.of("age", "5", "facing", "west"))
      );
      register(
         1670,
         create("minecraft:pumpkin_stem", AGE_6),
         create("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "east")),
         create("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "north")),
         create("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "south")),
         create("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "up")),
         create("minecraft:pumpkin_stem", Map.of("age", "6", "facing", "west"))
      );
      register(
         1671,
         create("minecraft:pumpkin_stem", AGE_7),
         create("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "east")),
         create("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "north")),
         create("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "south")),
         create("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "up")),
         create("minecraft:pumpkin_stem", Map.of("age", "7", "facing", "west"))
      );
      register(
         1680,
         create("minecraft:melon_stem", AGE_0),
         create("minecraft:melon_stem", AGE_0_FACING_EAST),
         create("minecraft:melon_stem", AGE_0_FACING_NORTH),
         create("minecraft:melon_stem", AGE_0_FACING_SOUTH),
         create("minecraft:melon_stem", Map.of("age", "0", "facing", "up")),
         create("minecraft:melon_stem", AGE_0_FACING_WEST)
      );
      register(
         1681,
         create("minecraft:melon_stem", AGE_1),
         create("minecraft:melon_stem", AGE_1_FACING_EAST),
         create("minecraft:melon_stem", AGE_1_FACING_NORTH),
         create("minecraft:melon_stem", AGE_1_FACING_SOUTH),
         create("minecraft:melon_stem", Map.of("age", "1", "facing", "up")),
         create("minecraft:melon_stem", AGE_1_FACING_WEST)
      );
      register(
         1682,
         create("minecraft:melon_stem", AGE_2),
         create("minecraft:melon_stem", AGE_2_FACING_EAST),
         create("minecraft:melon_stem", AGE_2_FACING_NORTH),
         create("minecraft:melon_stem", AGE_2_FACING_SOUTH),
         create("minecraft:melon_stem", Map.of("age", "2", "facing", "up")),
         create("minecraft:melon_stem", AGE_2_FACING_WEST)
      );
      register(
         1683,
         create("minecraft:melon_stem", AGE_3),
         create("minecraft:melon_stem", Map.of("age", "3", "facing", "east")),
         create("minecraft:melon_stem", Map.of("age", "3", "facing", "north")),
         create("minecraft:melon_stem", Map.of("age", "3", "facing", "south")),
         create("minecraft:melon_stem", Map.of("age", "3", "facing", "up")),
         create("minecraft:melon_stem", Map.of("age", "3", "facing", "west"))
      );
      register(
         1684,
         create("minecraft:melon_stem", AGE_4),
         create("minecraft:melon_stem", Map.of("age", "4", "facing", "east")),
         create("minecraft:melon_stem", Map.of("age", "4", "facing", "north")),
         create("minecraft:melon_stem", Map.of("age", "4", "facing", "south")),
         create("minecraft:melon_stem", Map.of("age", "4", "facing", "up")),
         create("minecraft:melon_stem", Map.of("age", "4", "facing", "west"))
      );
      register(
         1685,
         create("minecraft:melon_stem", AGE_5),
         create("minecraft:melon_stem", Map.of("age", "5", "facing", "east")),
         create("minecraft:melon_stem", Map.of("age", "5", "facing", "north")),
         create("minecraft:melon_stem", Map.of("age", "5", "facing", "south")),
         create("minecraft:melon_stem", Map.of("age", "5", "facing", "up")),
         create("minecraft:melon_stem", Map.of("age", "5", "facing", "west"))
      );
      register(
         1686,
         create("minecraft:melon_stem", AGE_6),
         create("minecraft:melon_stem", Map.of("age", "6", "facing", "east")),
         create("minecraft:melon_stem", Map.of("age", "6", "facing", "north")),
         create("minecraft:melon_stem", Map.of("age", "6", "facing", "south")),
         create("minecraft:melon_stem", Map.of("age", "6", "facing", "up")),
         create("minecraft:melon_stem", Map.of("age", "6", "facing", "west"))
      );
      register(
         1687,
         create("minecraft:melon_stem", AGE_7),
         create("minecraft:melon_stem", Map.of("age", "7", "facing", "east")),
         create("minecraft:melon_stem", Map.of("age", "7", "facing", "north")),
         create("minecraft:melon_stem", Map.of("age", "7", "facing", "south")),
         create("minecraft:melon_stem", Map.of("age", "7", "facing", "up")),
         create("minecraft:melon_stem", Map.of("age", "7", "facing", "west"))
      );
      register(
         1696,
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:vine", UP_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "false"))
      );
      register(
         1697,
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "false"))
      );
      register(
         1698,
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "west", "true"))
      );
      register(
         1699,
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "west", "true"))
      );
      register(
         1700,
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "false"))
      );
      register(
         1701,
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "false"))
      );
      register(
         1702,
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "west", "true"))
      );
      register(
         1703,
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         1704,
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "false"))
      );
      register(
         1705,
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "false"))
      );
      register(
         1706,
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "west", "true"))
      );
      register(
         1707,
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "west", "true"))
      );
      register(
         1708,
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "false"))
      );
      register(
         1709,
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "false"))
      );
      register(
         1710,
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "west", "true"))
      );
      register(
         1711,
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:vine", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "west", "true"))
      );
      register(
         1712,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         1713,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         1714,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         1715,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         1716,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         1717,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         1718,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         1719,
         create("minecraft:oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         1720,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         1721,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         1722,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         1723,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         1724,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         1725,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         1726,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         1727,
         create("minecraft:oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         1728,
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1729,
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1730,
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1731,
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         1732,
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1733,
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1734,
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1735,
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         1744,
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1745,
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1746,
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1747,
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         1748,
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1749,
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1750,
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1751,
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:stone_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         1760, create("minecraft:mycelium", SNOWY_FALSE), create("minecraft:mycelium", SNOWY_FALSE), create("minecraft:mycelium", Map.of("snowy", "true"))
      );
      register(1776, create("minecraft:lily_pad"), create("minecraft:waterlily"));
   }

   private static void bootstrap7() {
      register(1792, create("minecraft:nether_bricks"), create("minecraft:nether_brick"));
      register(
         1808,
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:nether_brick_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:nether_brick_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(
         1824,
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1825,
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1826,
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1827,
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         1828,
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         1829,
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         1830,
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         1831,
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:nether_brick_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(1840, create("minecraft:nether_wart", AGE_0), create("minecraft:nether_wart", AGE_0));
      register(1841, create("minecraft:nether_wart", AGE_1), create("minecraft:nether_wart", AGE_1));
      register(1842, create("minecraft:nether_wart", AGE_2), create("minecraft:nether_wart", AGE_2));
      register(1843, create("minecraft:nether_wart", AGE_3), create("minecraft:nether_wart", AGE_3));
      register(1856, create("minecraft:enchanting_table"), create("minecraft:enchanting_table"));
      register(
         1872,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "false")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "false"))
      );
      register(
         1873,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "false")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "false"))
      );
      register(
         1874,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "false")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "false"))
      );
      register(
         1875,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "false")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "false"))
      );
      register(
         1876,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "true")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "false", "has_bottle_2", "true"))
      );
      register(
         1877,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "true")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "false", "has_bottle_2", "true"))
      );
      register(
         1878,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "true")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "false", "has_bottle_1", "true", "has_bottle_2", "true"))
      );
      register(
         1879,
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "true")),
         create("minecraft:brewing_stand", Map.of("has_bottle_0", "true", "has_bottle_1", "true", "has_bottle_2", "true"))
      );
      register(1888, create("minecraft:cauldron", LEVEL_0), create("minecraft:cauldron", LEVEL_0));
      register(1889, create("minecraft:cauldron", LEVEL_1), create("minecraft:cauldron", LEVEL_1));
      register(1890, create("minecraft:cauldron", LEVEL_2), create("minecraft:cauldron", LEVEL_2));
      register(1891, create("minecraft:cauldron", LEVEL_3), create("minecraft:cauldron", LEVEL_3));
      register(1904, create("minecraft:end_portal"), create("minecraft:end_portal"));
      register(
         1920,
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "south")),
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "south"))
      );
      register(
         1921,
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "west")),
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "west"))
      );
      register(
         1922,
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "north")),
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "north"))
      );
      register(
         1923,
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "east")),
         create("minecraft:end_portal_frame", Map.of("eye", "false", "facing", "east"))
      );
      register(
         1924,
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "south")),
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "south"))
      );
      register(
         1925,
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "west")),
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "west"))
      );
      register(
         1926,
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "north")),
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "north"))
      );
      register(
         1927,
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "east")),
         create("minecraft:end_portal_frame", Map.of("eye", "true", "facing", "east"))
      );
      register(1936, create("minecraft:end_stone"), create("minecraft:end_stone"));
      register(1952, create("minecraft:dragon_egg"), create("minecraft:dragon_egg"));
      register(1968, create("minecraft:redstone_lamp", LIT_FALSE), create("minecraft:redstone_lamp"));
      register(1984, create("minecraft:redstone_lamp", LIT_TRUE), create("minecraft:lit_redstone_lamp"));
      register(2000, create("minecraft:oak_slab", TYPE_DOUBLE), create("minecraft:double_wooden_slab", Map.of("variant", "oak")));
      register(2001, create("minecraft:spruce_slab", TYPE_DOUBLE), create("minecraft:double_wooden_slab", Map.of("variant", "spruce")));
      register(2002, create("minecraft:birch_slab", TYPE_DOUBLE), create("minecraft:double_wooden_slab", Map.of("variant", "birch")));
      register(2003, create("minecraft:jungle_slab", TYPE_DOUBLE), create("minecraft:double_wooden_slab", Map.of("variant", "jungle")));
      register(2004, create("minecraft:acacia_slab", TYPE_DOUBLE), create("minecraft:double_wooden_slab", Map.of("variant", "acacia")));
      register(2005, create("minecraft:dark_oak_slab", TYPE_DOUBLE), create("minecraft:double_wooden_slab", Map.of("variant", "dark_oak")));
      register(2016, create("minecraft:oak_slab", TYPE_BOTTOM), create("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "oak")));
      register(2017, create("minecraft:spruce_slab", TYPE_BOTTOM), create("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "spruce")));
      register(2018, create("minecraft:birch_slab", TYPE_BOTTOM), create("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "birch")));
      register(2019, create("minecraft:jungle_slab", TYPE_BOTTOM), create("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "jungle")));
      register(2020, create("minecraft:acacia_slab", TYPE_BOTTOM), create("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "acacia")));
      register(2021, create("minecraft:dark_oak_slab", TYPE_BOTTOM), create("minecraft:wooden_slab", Map.of("half", "bottom", "variant", "dark_oak")));
      register(2024, create("minecraft:oak_slab", TYPE_TOP), create("minecraft:wooden_slab", Map.of("half", "top", "variant", "oak")));
      register(2025, create("minecraft:spruce_slab", TYPE_TOP), create("minecraft:wooden_slab", Map.of("half", "top", "variant", "spruce")));
      register(2026, create("minecraft:birch_slab", TYPE_TOP), create("minecraft:wooden_slab", Map.of("half", "top", "variant", "birch")));
      register(2027, create("minecraft:jungle_slab", TYPE_TOP), create("minecraft:wooden_slab", Map.of("half", "top", "variant", "jungle")));
      register(2028, create("minecraft:acacia_slab", TYPE_TOP), create("minecraft:wooden_slab", Map.of("half", "top", "variant", "acacia")));
      register(2029, create("minecraft:dark_oak_slab", TYPE_TOP), create("minecraft:wooden_slab", Map.of("half", "top", "variant", "dark_oak")));
      register(2032, create("minecraft:cocoa", AGE_0_FACING_SOUTH), create("minecraft:cocoa", AGE_0_FACING_SOUTH));
      register(2033, create("minecraft:cocoa", AGE_0_FACING_WEST), create("minecraft:cocoa", AGE_0_FACING_WEST));
      register(2034, create("minecraft:cocoa", AGE_0_FACING_NORTH), create("minecraft:cocoa", AGE_0_FACING_NORTH));
      register(2035, create("minecraft:cocoa", AGE_0_FACING_EAST), create("minecraft:cocoa", AGE_0_FACING_EAST));
      register(2036, create("minecraft:cocoa", AGE_1_FACING_SOUTH), create("minecraft:cocoa", AGE_1_FACING_SOUTH));
      register(2037, create("minecraft:cocoa", AGE_1_FACING_WEST), create("minecraft:cocoa", AGE_1_FACING_WEST));
      register(2038, create("minecraft:cocoa", AGE_1_FACING_NORTH), create("minecraft:cocoa", AGE_1_FACING_NORTH));
      register(2039, create("minecraft:cocoa", AGE_1_FACING_EAST), create("minecraft:cocoa", AGE_1_FACING_EAST));
      register(2040, create("minecraft:cocoa", AGE_2_FACING_SOUTH), create("minecraft:cocoa", AGE_2_FACING_SOUTH));
      register(2041, create("minecraft:cocoa", AGE_2_FACING_WEST), create("minecraft:cocoa", AGE_2_FACING_WEST));
      register(2042, create("minecraft:cocoa", AGE_2_FACING_NORTH), create("minecraft:cocoa", AGE_2_FACING_NORTH));
      register(2043, create("minecraft:cocoa", AGE_2_FACING_EAST), create("minecraft:cocoa", AGE_2_FACING_EAST));
   }

   private static void bootstrap8() {
      register(
         2048,
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2049,
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2050,
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2051,
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2052,
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2053,
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2054,
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2055,
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(2064, create("minecraft:emerald_ore"), create("minecraft:emerald_ore"));
      register(2082, create("minecraft:ender_chest", FACING_NORTH), create("minecraft:ender_chest", FACING_NORTH));
      register(2083, create("minecraft:ender_chest", FACING_SOUTH), create("minecraft:ender_chest", FACING_SOUTH));
      register(2084, create("minecraft:ender_chest", FACING_WEST), create("minecraft:ender_chest", FACING_WEST));
      register(2085, create("minecraft:ender_chest", FACING_EAST), create("minecraft:ender_chest", FACING_EAST));
      register(
         2096,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "false"))
      );
      register(
         2097,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "false"))
      );
      register(
         2098,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "false"))
      );
      register(
         2099,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "false"))
      );
      register(
         2100,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "false"))
      );
      register(
         2101,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "false"))
      );
      register(
         2102,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "false"))
      );
      register(
         2103,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "false")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "false"))
      );
      register(
         2104,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "south", "powered", "true"))
      );
      register(
         2105,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "west", "powered", "true"))
      );
      register(
         2106,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "north", "powered", "true"))
      );
      register(
         2107,
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "false", "facing", "east", "powered", "true"))
      );
      register(
         2108,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "south", "powered", "true"))
      );
      register(
         2109,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "west", "powered", "true"))
      );
      register(
         2110,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "north", "powered", "true"))
      );
      register(
         2111,
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "true")),
         create("minecraft:tripwire_hook", Map.of("attached", "true", "facing", "east", "powered", "true"))
      );
      register(
         2112,
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      register(
         2113,
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE),
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      register(2114, create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE));
      register(2115, create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE));
      register(
         2116,
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      register(
         2117,
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE),
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "false", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      register(2118, create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE));
      register(2119, create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_FALSE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE));
      register(
         2120,
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      register(
         2121,
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE),
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "false", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      register(2122, create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE));
      register(2123, create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_FALSE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_TRUE));
      register(
         2124,
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "false", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "false", "south", "true", "west", "true")
         )
      );
      register(
         2125,
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "false", "north", "true", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "false", "powered", "true", "south", "true", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "false", "west", "true")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "false")
         ),
         create(
            "minecraft:tripwire",
            Map.of("attached", "true", "disarmed", "true", "east", "true", "north", "true", "powered", "true", "south", "true", "west", "true")
         )
      );
      register(2126, create("minecraft:tripwire", EAST_FALSE_SOUTH_FALSE_ATTACHED_TRUE_DISARMED_TRUE_WEST_FALSE_NORTH_FALSE_POWERED_FALSE));
      register(2128, create("minecraft:emerald_block"), create("minecraft:emerald_block"));
      register(
         2144,
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2145,
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2146,
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2147,
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2148,
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2149,
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2150,
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2151,
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:spruce_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2160,
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2161,
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2162,
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2163,
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2164,
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2165,
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2166,
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2167,
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:birch_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2176,
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2177,
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2178,
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2179,
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2180,
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2181,
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2182,
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2183,
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:jungle_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(2192, create("minecraft:command_block", FACING_DOWN_CONDITIONAL_FALSE), create("minecraft:command_block", FACING_DOWN_CONDITIONAL_FALSE));
      register(2193, create("minecraft:command_block", FACING_UP_CONDITIONAL_FALSE), create("minecraft:command_block", FACING_UP_CONDITIONAL_FALSE));
      register(2194, create("minecraft:command_block", FACING_NORTH_CONDITIONAL_FALSE), create("minecraft:command_block", FACING_NORTH_CONDITIONAL_FALSE));
      register(2195, create("minecraft:command_block", FACING_SOUTH_CONDITIONAL_FALSE), create("minecraft:command_block", FACING_SOUTH_CONDITIONAL_FALSE));
      register(2196, create("minecraft:command_block", FACING_WEST_CONDITIONAL_FALSE), create("minecraft:command_block", FACING_WEST_CONDITIONAL_FALSE));
      register(2197, create("minecraft:command_block", FACING_EAST_CONDITIONAL_FALSE), create("minecraft:command_block", FACING_EAST_CONDITIONAL_FALSE));
      register(2200, create("minecraft:command_block", FACING_DOWN_CONDITIONAL_TRUE), create("minecraft:command_block", FACING_DOWN_CONDITIONAL_TRUE));
      register(2201, create("minecraft:command_block", FACING_UP_CONDITIONAL_TRUE), create("minecraft:command_block", FACING_UP_CONDITIONAL_TRUE));
      register(2202, create("minecraft:command_block", FACING_NORTH_CONDITIONAL_TRUE), create("minecraft:command_block", FACING_NORTH_CONDITIONAL_TRUE));
      register(2203, create("minecraft:command_block", FACING_SOUTH_CONDITIONAL_TRUE), create("minecraft:command_block", FACING_SOUTH_CONDITIONAL_TRUE));
      register(2204, create("minecraft:command_block", FACING_WEST_CONDITIONAL_TRUE), create("minecraft:command_block", FACING_WEST_CONDITIONAL_TRUE));
      register(2205, create("minecraft:command_block", FACING_EAST_CONDITIONAL_TRUE), create("minecraft:command_block", FACING_EAST_CONDITIONAL_TRUE));
      register(2208, create("minecraft:beacon"), create("minecraft:beacon"));
      register(
         2224,
         create("minecraft:cobblestone_wall", UP_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         create("minecraft:cobblestone_wall", Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "true")),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         create("minecraft:cobblestone_wall", Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "cobblestone", "west", "true")),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "false")
         ),
         create("minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "cobblestone", "west", "true")),
         create(
            "minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "false")
         ),
         create("minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "cobblestone", "west", "true")),
         create("minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "false")),
         create("minecraft:cobblestone_wall", Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "cobblestone", "west", "true"))
      );
      register(
         2225,
         create("minecraft:mossy_cobblestone_wall", UP_FALSE_EAST_FALSE_NORTH_FALSE_SOUTH_FALSE_WEST_FALSE),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "false", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "false", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "false", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "false", "variant", "mossy_cobblestone", "west", "true")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "false")
         ),
         create(
            "minecraft:cobblestone_wall",
            Map.of("east", "true", "north", "true", "south", "true", "up", "true", "variant", "mossy_cobblestone", "west", "true")
         )
      );
      register(
         2240,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "0")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "0"))
      );
      register(
         2241,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "1")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "1"))
      );
      register(
         2242,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "2")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "2"))
      );
      register(
         2243,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "3")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "3"))
      );
      register(
         2244,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "4")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "4"))
      );
      register(
         2245,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "5")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "5"))
      );
      register(
         2246,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "6")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "6"))
      );
      register(
         2247,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "7")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "7"))
      );
      register(
         2248,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "8")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "8"))
      );
      register(
         2249,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "9")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "9"))
      );
      register(
         2250,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "10")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "10"))
      );
      register(
         2251,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "11")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "11"))
      );
      register(
         2252,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "12")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "12"))
      );
      register(
         2253,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "13")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "13"))
      );
      register(
         2254,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "14")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "14"))
      );
      register(
         2255,
         create("minecraft:potted_cactus"),
         create("minecraft:flower_pot", Map.of("contents", "acacia_sapling", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "allium", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "birch_sapling", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "blue_orchid", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "cactus", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "dandelion", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "dark_oak_sapling", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "dead_bush", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "empty", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "fern", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "houstonia", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "jungle_sapling", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_brown", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "mushroom_red", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "oak_sapling", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "orange_tulip", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "oxeye_daisy", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "pink_tulip", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "red_tulip", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "rose", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "spruce_sapling", "legacy_data", "15")),
         create("minecraft:flower_pot", Map.of("contents", "white_tulip", "legacy_data", "15"))
      );
      register(2256, create("minecraft:carrots", AGE_0), create("minecraft:carrots", AGE_0));
      register(2257, create("minecraft:carrots", AGE_1), create("minecraft:carrots", AGE_1));
      register(2258, create("minecraft:carrots", AGE_2), create("minecraft:carrots", AGE_2));
      register(2259, create("minecraft:carrots", AGE_3), create("minecraft:carrots", AGE_3));
      register(2260, create("minecraft:carrots", AGE_4), create("minecraft:carrots", AGE_4));
      register(2261, create("minecraft:carrots", AGE_5), create("minecraft:carrots", AGE_5));
      register(2262, create("minecraft:carrots", AGE_6), create("minecraft:carrots", AGE_6));
      register(2263, create("minecraft:carrots", AGE_7), create("minecraft:carrots", AGE_7));
      register(2272, create("minecraft:potatoes", AGE_0), create("minecraft:potatoes", AGE_0));
      register(2273, create("minecraft:potatoes", AGE_1), create("minecraft:potatoes", AGE_1));
      register(2274, create("minecraft:potatoes", AGE_2), create("minecraft:potatoes", AGE_2));
      register(2275, create("minecraft:potatoes", AGE_3), create("minecraft:potatoes", AGE_3));
      register(2276, create("minecraft:potatoes", AGE_4), create("minecraft:potatoes", AGE_4));
      register(2277, create("minecraft:potatoes", AGE_5), create("minecraft:potatoes", AGE_5));
      register(2278, create("minecraft:potatoes", AGE_6), create("minecraft:potatoes", AGE_6));
      register(2279, create("minecraft:potatoes", AGE_7), create("minecraft:potatoes", AGE_7));
      register(2288, create("minecraft:oak_button", FACE_CEILING_POWERED_FALSE_FACING_NORTH), create("minecraft:wooden_button", FACING_DOWN_POWERED_FALSE));
      register(2289, create("minecraft:oak_button", FACE_WALL_POWERED_FALSE_FACING_EAST), create("minecraft:wooden_button", FACING_EAST_POWERED_FALSE));
      register(2290, create("minecraft:oak_button", FACE_WALL_POWERED_FALSE_FACING_WEST), create("minecraft:wooden_button", FACING_WEST_POWERED_FALSE));
      register(2291, create("minecraft:oak_button", FACE_WALL_POWERED_FALSE_FACING_SOUTH), create("minecraft:wooden_button", FACING_SOUTH_POWERED_FALSE));
      register(2292, create("minecraft:oak_button", FACE_WALL_POWERED_FALSE_FACING_NORTH), create("minecraft:wooden_button", FACING_NORTH_POWERED_FALSE));
      register(2293, create("minecraft:oak_button", FACE_FLOOR_POWERED_FALSE_FACING_NORTH), create("minecraft:wooden_button", FACING_UP_POWERED_FALSE));
      register(2296, create("minecraft:oak_button", FACE_CEILING_POWERED_TRUE_FACING_NORTH), create("minecraft:wooden_button", FACING_DOWN_POWERED_TRUE));
      register(2297, create("minecraft:oak_button", FACE_WALL_POWERED_TRUE_FACING_EAST), create("minecraft:wooden_button", FACING_EAST_POWERED_TRUE));
      register(2298, create("minecraft:oak_button", FACE_WALL_POWERED_TRUE_FACING_WEST), create("minecraft:wooden_button", FACING_WEST_POWERED_TRUE));
      register(2299, create("minecraft:oak_button", FACE_WALL_POWERED_TRUE_FACING_SOUTH), create("minecraft:wooden_button", FACING_SOUTH_POWERED_TRUE));
      register(2300, create("minecraft:oak_button", FACE_WALL_POWERED_TRUE_FACING_NORTH), create("minecraft:wooden_button", FACING_NORTH_POWERED_TRUE));
      register(2301, create("minecraft:oak_button", FACE_FLOOR_POWERED_TRUE_FACING_NORTH), create("minecraft:wooden_button", FACING_UP_POWERED_TRUE));
   }

   private static void bootstrap9() {
      register(
         2304, create("%%FILTER_ME%%", Map.of("facing", "down", "nodrop", "false")), create("minecraft:skull", Map.of("facing", "down", "nodrop", "false"))
      );
      register(2305, create("%%FILTER_ME%%", Map.of("facing", "up", "nodrop", "false")), create("minecraft:skull", Map.of("facing", "up", "nodrop", "false")));
      register(
         2306, create("%%FILTER_ME%%", Map.of("facing", "north", "nodrop", "false")), create("minecraft:skull", Map.of("facing", "north", "nodrop", "false"))
      );
      register(
         2307, create("%%FILTER_ME%%", Map.of("facing", "south", "nodrop", "false")), create("minecraft:skull", Map.of("facing", "south", "nodrop", "false"))
      );
      register(
         2308, create("%%FILTER_ME%%", Map.of("facing", "west", "nodrop", "false")), create("minecraft:skull", Map.of("facing", "west", "nodrop", "false"))
      );
      register(
         2309, create("%%FILTER_ME%%", Map.of("facing", "east", "nodrop", "false")), create("minecraft:skull", Map.of("facing", "east", "nodrop", "false"))
      );
      register(2312, create("%%FILTER_ME%%", Map.of("facing", "down", "nodrop", "true")), create("minecraft:skull", Map.of("facing", "down", "nodrop", "true")));
      register(2313, create("%%FILTER_ME%%", Map.of("facing", "up", "nodrop", "true")), create("minecraft:skull", Map.of("facing", "up", "nodrop", "true")));
      register(
         2314, create("%%FILTER_ME%%", Map.of("facing", "north", "nodrop", "true")), create("minecraft:skull", Map.of("facing", "north", "nodrop", "true"))
      );
      register(
         2315, create("%%FILTER_ME%%", Map.of("facing", "south", "nodrop", "true")), create("minecraft:skull", Map.of("facing", "south", "nodrop", "true"))
      );
      register(2316, create("%%FILTER_ME%%", Map.of("facing", "west", "nodrop", "true")), create("minecraft:skull", Map.of("facing", "west", "nodrop", "true")));
      register(2317, create("%%FILTER_ME%%", Map.of("facing", "east", "nodrop", "true")), create("minecraft:skull", Map.of("facing", "east", "nodrop", "true")));
      register(2320, create("minecraft:anvil", FACING_SOUTH), create("minecraft:anvil", Map.of("damage", "0", "facing", "south")));
      register(2321, create("minecraft:anvil", FACING_WEST), create("minecraft:anvil", Map.of("damage", "0", "facing", "west")));
      register(2322, create("minecraft:anvil", FACING_NORTH), create("minecraft:anvil", Map.of("damage", "0", "facing", "north")));
      register(2323, create("minecraft:anvil", FACING_EAST), create("minecraft:anvil", Map.of("damage", "0", "facing", "east")));
      register(2324, create("minecraft:chipped_anvil", FACING_SOUTH), create("minecraft:anvil", Map.of("damage", "1", "facing", "south")));
      register(2325, create("minecraft:chipped_anvil", FACING_WEST), create("minecraft:anvil", Map.of("damage", "1", "facing", "west")));
      register(2326, create("minecraft:chipped_anvil", FACING_NORTH), create("minecraft:anvil", Map.of("damage", "1", "facing", "north")));
      register(2327, create("minecraft:chipped_anvil", FACING_EAST), create("minecraft:anvil", Map.of("damage", "1", "facing", "east")));
      register(2328, create("minecraft:damaged_anvil", FACING_SOUTH), create("minecraft:anvil", Map.of("damage", "2", "facing", "south")));
      register(2329, create("minecraft:damaged_anvil", FACING_WEST), create("minecraft:anvil", Map.of("damage", "2", "facing", "west")));
      register(2330, create("minecraft:damaged_anvil", FACING_NORTH), create("minecraft:anvil", Map.of("damage", "2", "facing", "north")));
      register(2331, create("minecraft:damaged_anvil", FACING_EAST), create("minecraft:anvil", Map.of("damage", "2", "facing", "east")));
      register(2338, create("minecraft:trapped_chest", Map.of("facing", "north", "type", "single")), create("minecraft:trapped_chest", FACING_NORTH));
      register(2339, create("minecraft:trapped_chest", Map.of("facing", "south", "type", "single")), create("minecraft:trapped_chest", FACING_SOUTH));
      register(2340, create("minecraft:trapped_chest", Map.of("facing", "west", "type", "single")), create("minecraft:trapped_chest", FACING_WEST));
      register(2341, create("minecraft:trapped_chest", Map.of("facing", "east", "type", "single")), create("minecraft:trapped_chest", FACING_EAST));
      register(2352, create("minecraft:light_weighted_pressure_plate", POWER_0), create("minecraft:light_weighted_pressure_plate", POWER_0));
      register(2353, create("minecraft:light_weighted_pressure_plate", POWER_1), create("minecraft:light_weighted_pressure_plate", POWER_1));
      register(2354, create("minecraft:light_weighted_pressure_plate", POWER_2), create("minecraft:light_weighted_pressure_plate", POWER_2));
      register(2355, create("minecraft:light_weighted_pressure_plate", POWER_3), create("minecraft:light_weighted_pressure_plate", POWER_3));
      register(2356, create("minecraft:light_weighted_pressure_plate", POWER_4), create("minecraft:light_weighted_pressure_plate", POWER_4));
      register(2357, create("minecraft:light_weighted_pressure_plate", POWER_5), create("minecraft:light_weighted_pressure_plate", POWER_5));
      register(2358, create("minecraft:light_weighted_pressure_plate", POWER_6), create("minecraft:light_weighted_pressure_plate", POWER_6));
      register(2359, create("minecraft:light_weighted_pressure_plate", POWER_7), create("minecraft:light_weighted_pressure_plate", POWER_7));
      register(2360, create("minecraft:light_weighted_pressure_plate", POWER_8), create("minecraft:light_weighted_pressure_plate", POWER_8));
      register(2361, create("minecraft:light_weighted_pressure_plate", POWER_9), create("minecraft:light_weighted_pressure_plate", POWER_9));
      register(2362, create("minecraft:light_weighted_pressure_plate", POWER_10), create("minecraft:light_weighted_pressure_plate", POWER_10));
      register(2363, create("minecraft:light_weighted_pressure_plate", POWER_11), create("minecraft:light_weighted_pressure_plate", POWER_11));
      register(2364, create("minecraft:light_weighted_pressure_plate", POWER_12), create("minecraft:light_weighted_pressure_plate", POWER_12));
      register(2365, create("minecraft:light_weighted_pressure_plate", POWER_13), create("minecraft:light_weighted_pressure_plate", POWER_13));
      register(2366, create("minecraft:light_weighted_pressure_plate", POWER_14), create("minecraft:light_weighted_pressure_plate", POWER_14));
      register(2367, create("minecraft:light_weighted_pressure_plate", POWER_15), create("minecraft:light_weighted_pressure_plate", POWER_15));
      register(2368, create("minecraft:heavy_weighted_pressure_plate", POWER_0), create("minecraft:heavy_weighted_pressure_plate", POWER_0));
      register(2369, create("minecraft:heavy_weighted_pressure_plate", POWER_1), create("minecraft:heavy_weighted_pressure_plate", POWER_1));
      register(2370, create("minecraft:heavy_weighted_pressure_plate", POWER_2), create("minecraft:heavy_weighted_pressure_plate", POWER_2));
      register(2371, create("minecraft:heavy_weighted_pressure_plate", POWER_3), create("minecraft:heavy_weighted_pressure_plate", POWER_3));
      register(2372, create("minecraft:heavy_weighted_pressure_plate", POWER_4), create("minecraft:heavy_weighted_pressure_plate", POWER_4));
      register(2373, create("minecraft:heavy_weighted_pressure_plate", POWER_5), create("minecraft:heavy_weighted_pressure_plate", POWER_5));
      register(2374, create("minecraft:heavy_weighted_pressure_plate", POWER_6), create("minecraft:heavy_weighted_pressure_plate", POWER_6));
      register(2375, create("minecraft:heavy_weighted_pressure_plate", POWER_7), create("minecraft:heavy_weighted_pressure_plate", POWER_7));
      register(2376, create("minecraft:heavy_weighted_pressure_plate", POWER_8), create("minecraft:heavy_weighted_pressure_plate", POWER_8));
      register(2377, create("minecraft:heavy_weighted_pressure_plate", POWER_9), create("minecraft:heavy_weighted_pressure_plate", POWER_9));
      register(2378, create("minecraft:heavy_weighted_pressure_plate", POWER_10), create("minecraft:heavy_weighted_pressure_plate", POWER_10));
      register(2379, create("minecraft:heavy_weighted_pressure_plate", POWER_11), create("minecraft:heavy_weighted_pressure_plate", POWER_11));
      register(2380, create("minecraft:heavy_weighted_pressure_plate", POWER_12), create("minecraft:heavy_weighted_pressure_plate", POWER_12));
      register(2381, create("minecraft:heavy_weighted_pressure_plate", POWER_13), create("minecraft:heavy_weighted_pressure_plate", POWER_13));
      register(2382, create("minecraft:heavy_weighted_pressure_plate", POWER_14), create("minecraft:heavy_weighted_pressure_plate", POWER_14));
      register(2383, create("minecraft:heavy_weighted_pressure_plate", POWER_15), create("minecraft:heavy_weighted_pressure_plate", POWER_15));
      register(
         2384,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_SOUTH),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_SOUTH)
      );
      register(
         2385,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_WEST),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_WEST)
      );
      register(
         2386,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_NORTH),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_NORTH)
      );
      register(
         2387,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_EAST),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_EAST)
      );
      register(
         2388,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_SOUTH),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_SOUTH)
      );
      register(
         2389,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_WEST),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_WEST)
      );
      register(
         2390,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_NORTH),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_NORTH)
      );
      register(
         2391,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_EAST),
         create("minecraft:unpowered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_EAST)
      );
      register(
         2392,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_SOUTH),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_SOUTH)
      );
      register(
         2393,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_WEST),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_WEST)
      );
      register(
         2394,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_NORTH),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_NORTH)
      );
      register(
         2395,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_EAST),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_EAST)
      );
      register(
         2396,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_SOUTH),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_SOUTH)
      );
      register(
         2397,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_WEST),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_WEST)
      );
      register(
         2398,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_NORTH),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_NORTH)
      );
      register(
         2399,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_EAST),
         create("minecraft:unpowered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_EAST)
      );
      register(
         2400,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_SOUTH),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_SOUTH)
      );
      register(
         2401,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_WEST),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_WEST)
      );
      register(
         2402,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_NORTH),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_NORTH)
      );
      register(
         2403,
         create("minecraft:comparator", POWERED_FALSE_MODE_COMPARE_FACING_EAST),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_COMPARE_FACING_EAST)
      );
      register(
         2404,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_SOUTH),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_SOUTH)
      );
      register(
         2405,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_WEST),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_WEST)
      );
      register(
         2406,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_NORTH),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_NORTH)
      );
      register(
         2407,
         create("minecraft:comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_EAST),
         create("minecraft:powered_comparator", POWERED_FALSE_MODE_SUBTRACT_FACING_EAST)
      );
      register(
         2408,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_SOUTH),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_SOUTH)
      );
      register(
         2409,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_WEST),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_WEST)
      );
      register(
         2410,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_NORTH),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_NORTH)
      );
      register(
         2411,
         create("minecraft:comparator", POWERED_TRUE_MODE_COMPARE_FACING_EAST),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_COMPARE_FACING_EAST)
      );
      register(
         2412,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_SOUTH),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_SOUTH)
      );
      register(
         2413,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_WEST),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_WEST)
      );
      register(
         2414,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_NORTH),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_NORTH)
      );
      register(
         2415,
         create("minecraft:comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_EAST),
         create("minecraft:powered_comparator", POWERED_TRUE_MODE_SUBTRACT_FACING_EAST)
      );
      register(2416, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "0")), create("minecraft:daylight_detector", POWER_0));
      register(2417, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "1")), create("minecraft:daylight_detector", POWER_1));
      register(2418, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "2")), create("minecraft:daylight_detector", POWER_2));
      register(2419, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "3")), create("minecraft:daylight_detector", POWER_3));
      register(2420, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "4")), create("minecraft:daylight_detector", POWER_4));
      register(2421, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "5")), create("minecraft:daylight_detector", POWER_5));
      register(2422, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "6")), create("minecraft:daylight_detector", POWER_6));
      register(2423, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "7")), create("minecraft:daylight_detector", POWER_7));
      register(2424, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "8")), create("minecraft:daylight_detector", POWER_8));
      register(2425, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "9")), create("minecraft:daylight_detector", POWER_9));
      register(2426, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "10")), create("minecraft:daylight_detector", POWER_10));
      register(2427, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "11")), create("minecraft:daylight_detector", POWER_11));
      register(2428, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "12")), create("minecraft:daylight_detector", POWER_12));
      register(2429, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "13")), create("minecraft:daylight_detector", POWER_13));
      register(2430, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "14")), create("minecraft:daylight_detector", POWER_14));
      register(2431, create("minecraft:daylight_detector", Map.of("inverted", "false", "power", "15")), create("minecraft:daylight_detector", POWER_15));
      register(2432, create("minecraft:redstone_block"), create("minecraft:redstone_block"));
      register(2448, create("minecraft:nether_quartz_ore"), create("minecraft:quartz_ore"));
      register(
         2464, create("minecraft:hopper", Map.of("enabled", "true", "facing", "down")), create("minecraft:hopper", Map.of("enabled", "true", "facing", "down"))
      );
      register(
         2466,
         create("minecraft:hopper", Map.of("enabled", "true", "facing", "north")),
         create("minecraft:hopper", Map.of("enabled", "true", "facing", "north"))
      );
      register(
         2467,
         create("minecraft:hopper", Map.of("enabled", "true", "facing", "south")),
         create("minecraft:hopper", Map.of("enabled", "true", "facing", "south"))
      );
      register(
         2468, create("minecraft:hopper", Map.of("enabled", "true", "facing", "west")), create("minecraft:hopper", Map.of("enabled", "true", "facing", "west"))
      );
      register(
         2469, create("minecraft:hopper", Map.of("enabled", "true", "facing", "east")), create("minecraft:hopper", Map.of("enabled", "true", "facing", "east"))
      );
      register(
         2472,
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "down")),
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "down"))
      );
      register(
         2474,
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "north")),
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "north"))
      );
      register(
         2475,
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "south")),
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "south"))
      );
      register(
         2476,
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "west")),
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "west"))
      );
      register(
         2477,
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "east")),
         create("minecraft:hopper", Map.of("enabled", "false", "facing", "east"))
      );
      register(2480, create("minecraft:quartz_block"), create("minecraft:quartz_block", Map.of("variant", "default")));
      register(2481, create("minecraft:chiseled_quartz_block"), create("minecraft:quartz_block", Map.of("variant", "chiseled")));
      register(2482, create("minecraft:quartz_pillar", AXIS_Y), create("minecraft:quartz_block", Map.of("variant", "lines_y")));
      register(2483, create("minecraft:quartz_pillar", AXIS_X), create("minecraft:quartz_block", Map.of("variant", "lines_x")));
      register(2484, create("minecraft:quartz_pillar", AXIS_Z), create("minecraft:quartz_block", Map.of("variant", "lines_z")));
      register(
         2496,
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2497,
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2498,
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2499,
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2500,
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2501,
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2502,
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2503,
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:quartz_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(2512, create("minecraft:activator_rail", SHAPE_NORTH_SOUTH_POWERED_FALSE), create("minecraft:activator_rail", SHAPE_NORTH_SOUTH_POWERED_FALSE));
      register(2513, create("minecraft:activator_rail", SHAPE_EAST_WEST_POWERED_FALSE), create("minecraft:activator_rail", SHAPE_EAST_WEST_POWERED_FALSE));
      register(
         2514, create("minecraft:activator_rail", SHAPE_ASCENDING_EAST_POWERED_FALSE), create("minecraft:activator_rail", SHAPE_ASCENDING_EAST_POWERED_FALSE)
      );
      register(
         2515, create("minecraft:activator_rail", SHAPE_ASCENDING_WEST_POWERED_FALSE), create("minecraft:activator_rail", SHAPE_ASCENDING_WEST_POWERED_FALSE)
      );
      register(
         2516, create("minecraft:activator_rail", SHAPE_ASCENDING_NORTH_POWERED_FALSE), create("minecraft:activator_rail", SHAPE_ASCENDING_NORTH_POWERED_FALSE)
      );
      register(
         2517, create("minecraft:activator_rail", SHAPE_ASCENDING_SOUTH_POWERED_FALSE), create("minecraft:activator_rail", SHAPE_ASCENDING_SOUTH_POWERED_FALSE)
      );
      register(2520, create("minecraft:activator_rail", SHAPE_NORTH_SOUTH_POWERED_TRUE), create("minecraft:activator_rail", SHAPE_NORTH_SOUTH_POWERED_TRUE));
      register(2521, create("minecraft:activator_rail", SHAPE_EAST_WEST_POWERED_TRUE), create("minecraft:activator_rail", SHAPE_EAST_WEST_POWERED_TRUE));
      register(
         2522, create("minecraft:activator_rail", SHAPE_ASCENDING_EAST_POWERED_TRUE), create("minecraft:activator_rail", SHAPE_ASCENDING_EAST_POWERED_TRUE)
      );
      register(
         2523, create("minecraft:activator_rail", SHAPE_ASCENDING_WEST_POWERED_TRUE), create("minecraft:activator_rail", SHAPE_ASCENDING_WEST_POWERED_TRUE)
      );
      register(
         2524, create("minecraft:activator_rail", SHAPE_ASCENDING_NORTH_POWERED_TRUE), create("minecraft:activator_rail", SHAPE_ASCENDING_NORTH_POWERED_TRUE)
      );
      register(
         2525, create("minecraft:activator_rail", SHAPE_ASCENDING_SOUTH_POWERED_TRUE), create("minecraft:activator_rail", SHAPE_ASCENDING_SOUTH_POWERED_TRUE)
      );
      register(2528, create("minecraft:dropper", TRIGGERED_FALSE_FACING_DOWN), create("minecraft:dropper", TRIGGERED_FALSE_FACING_DOWN));
      register(2529, create("minecraft:dropper", TRIGGERED_FALSE_FACING_UP), create("minecraft:dropper", TRIGGERED_FALSE_FACING_UP));
      register(2530, create("minecraft:dropper", TRIGGERED_FALSE_FACING_NORTH), create("minecraft:dropper", TRIGGERED_FALSE_FACING_NORTH));
      register(2531, create("minecraft:dropper", TRIGGERED_FALSE_FACING_SOUTH), create("minecraft:dropper", TRIGGERED_FALSE_FACING_SOUTH));
      register(2532, create("minecraft:dropper", TRIGGERED_FALSE_FACING_WEST), create("minecraft:dropper", TRIGGERED_FALSE_FACING_WEST));
      register(2533, create("minecraft:dropper", TRIGGERED_FALSE_FACING_EAST), create("minecraft:dropper", TRIGGERED_FALSE_FACING_EAST));
      register(2536, create("minecraft:dropper", TRIGGERED_TRUE_FACING_DOWN), create("minecraft:dropper", TRIGGERED_TRUE_FACING_DOWN));
      register(2537, create("minecraft:dropper", TRIGGERED_TRUE_FACING_UP), create("minecraft:dropper", TRIGGERED_TRUE_FACING_UP));
      register(2538, create("minecraft:dropper", TRIGGERED_TRUE_FACING_NORTH), create("minecraft:dropper", TRIGGERED_TRUE_FACING_NORTH));
      register(2539, create("minecraft:dropper", TRIGGERED_TRUE_FACING_SOUTH), create("minecraft:dropper", TRIGGERED_TRUE_FACING_SOUTH));
      register(2540, create("minecraft:dropper", TRIGGERED_TRUE_FACING_WEST), create("minecraft:dropper", TRIGGERED_TRUE_FACING_WEST));
      register(2541, create("minecraft:dropper", TRIGGERED_TRUE_FACING_EAST), create("minecraft:dropper", TRIGGERED_TRUE_FACING_EAST));
      register(2544, create("minecraft:white_terracotta"), create("minecraft:stained_hardened_clay", COLOR_WHITE));
      register(2545, create("minecraft:orange_terracotta"), create("minecraft:stained_hardened_clay", COLOR_ORANGE));
      register(2546, create("minecraft:magenta_terracotta"), create("minecraft:stained_hardened_clay", COLOR_MAGENTA));
      register(2547, create("minecraft:light_blue_terracotta"), create("minecraft:stained_hardened_clay", COLOR_LIGHT_BLUE));
      register(2548, create("minecraft:yellow_terracotta"), create("minecraft:stained_hardened_clay", COLOR_YELLOW));
      register(2549, create("minecraft:lime_terracotta"), create("minecraft:stained_hardened_clay", COLOR_LIME));
      register(2550, create("minecraft:pink_terracotta"), create("minecraft:stained_hardened_clay", COLOR_PINK));
      register(2551, create("minecraft:gray_terracotta"), create("minecraft:stained_hardened_clay", COLOR_GRAY));
      register(2552, create("minecraft:light_gray_terracotta"), create("minecraft:stained_hardened_clay", COLOR_SILVER));
      register(2553, create("minecraft:cyan_terracotta"), create("minecraft:stained_hardened_clay", COLOR_CYAN));
      register(2554, create("minecraft:purple_terracotta"), create("minecraft:stained_hardened_clay", COLOR_PURPLE));
      register(2555, create("minecraft:blue_terracotta"), create("minecraft:stained_hardened_clay", COLOR_BLUE));
      register(2556, create("minecraft:brown_terracotta"), create("minecraft:stained_hardened_clay", COLOR_BROWN));
      register(2557, create("minecraft:green_terracotta"), create("minecraft:stained_hardened_clay", COLOR_GREEN));
      register(2558, create("minecraft:red_terracotta"), create("minecraft:stained_hardened_clay", COLOR_RED));
      register(2559, create("minecraft:black_terracotta"), create("minecraft:stained_hardened_clay", COLOR_BLACK));
   }

   private static void bootstrapA() {
      register(
         2560,
         create("minecraft:white_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "white", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2561,
         create("minecraft:orange_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "orange", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2562,
         create("minecraft:magenta_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "magenta", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2563,
         create("minecraft:light_blue_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "light_blue", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2564,
         create("minecraft:yellow_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "yellow", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2565,
         create("minecraft:lime_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "lime", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2566,
         create("minecraft:pink_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "pink", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2567,
         create("minecraft:gray_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "gray", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2568,
         create("minecraft:light_gray_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "silver", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2569,
         create("minecraft:cyan_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "cyan", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2570,
         create("minecraft:purple_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "purple", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2571,
         create("minecraft:blue_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "blue", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2572,
         create("minecraft:brown_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "brown", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2573,
         create("minecraft:green_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "green", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2574,
         create("minecraft:red_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "red", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2575,
         create("minecraft:black_stained_glass_pane", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "false", "north", "true", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "false", "south", "true", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "false", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "false", "west", "true")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "true", "west", "false")),
         create("minecraft:stained_glass_pane", Map.of("color", "black", "east", "true", "north", "true", "south", "true", "west", "true"))
      );
      register(
         2576,
         create("minecraft:acacia_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         create("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "true", "variant", "acacia"))
      );
      register(
         2577,
         create("minecraft:dark_oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_TRUE),
         create("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "true", "variant", "dark_oak"))
      );
      register(
         2580,
         create("minecraft:acacia_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         create("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "false", "variant", "acacia"))
      );
      register(
         2581,
         create("minecraft:dark_oak_leaves", CHECK_DECAY_FALSE_DECAYABLE_FALSE),
         create("minecraft:leaves2", Map.of("check_decay", "false", "decayable", "false", "variant", "dark_oak"))
      );
      register(
         2584,
         create("minecraft:acacia_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         create("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "true", "variant", "acacia"))
      );
      register(
         2585,
         create("minecraft:dark_oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_TRUE),
         create("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "true", "variant", "dark_oak"))
      );
      register(
         2588,
         create("minecraft:acacia_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         create("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "false", "variant", "acacia"))
      );
      register(
         2589,
         create("minecraft:dark_oak_leaves", CHECK_DECAY_TRUE_DECAYABLE_FALSE),
         create("minecraft:leaves2", Map.of("check_decay", "true", "decayable", "false", "variant", "dark_oak"))
      );
      register(2592, create("minecraft:acacia_log", AXIS_Y), create("minecraft:log2", Map.of("axis", "y", "variant", "acacia")));
      register(2593, create("minecraft:dark_oak_log", AXIS_Y), create("minecraft:log2", Map.of("axis", "y", "variant", "dark_oak")));
      register(2596, create("minecraft:acacia_log", AXIS_X), create("minecraft:log2", Map.of("axis", "x", "variant", "acacia")));
      register(2597, create("minecraft:dark_oak_log", AXIS_X), create("minecraft:log2", Map.of("axis", "x", "variant", "dark_oak")));
      register(2600, create("minecraft:acacia_log", AXIS_Z), create("minecraft:log2", Map.of("axis", "z", "variant", "acacia")));
      register(2601, create("minecraft:dark_oak_log", AXIS_Z), create("minecraft:log2", Map.of("axis", "z", "variant", "dark_oak")));
      register(2604, create("minecraft:acacia_bark"), create("minecraft:log2", Map.of("axis", "none", "variant", "acacia")));
      register(2605, create("minecraft:dark_oak_bark"), create("minecraft:log2", Map.of("axis", "none", "variant", "dark_oak")));
      register(
         2608,
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2609,
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2610,
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2611,
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2612,
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2613,
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2614,
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2615,
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:acacia_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2624,
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2625,
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2626,
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2627,
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2628,
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2629,
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2630,
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2631,
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:dark_oak_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(2640, create("minecraft:slime_block"), create("minecraft:slime"));
      register(2656, create("minecraft:barrier"), create("minecraft:barrier"));
      register(
         2672, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_NORTH), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2673, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_SOUTH), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2674, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_WEST), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_WEST)
      );
      register(
         2675, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_EAST), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_FALSE_FACING_EAST)
      );
      register(
         2676, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_NORTH), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2677, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_SOUTH), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_SOUTH)
      );
      register(2678, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_WEST), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_WEST));
      register(2679, create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_EAST), create("minecraft:iron_trapdoor", HALF_BOTTOM_OPEN_TRUE_FACING_EAST));
      register(2680, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_NORTH), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_NORTH));
      register(2681, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_SOUTH), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_SOUTH));
      register(2682, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_WEST), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_WEST));
      register(2683, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_EAST), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_FALSE_FACING_EAST));
      register(2684, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_NORTH), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_NORTH));
      register(2685, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_SOUTH), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_SOUTH));
      register(2686, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_WEST), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_WEST));
      register(2687, create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_EAST), create("minecraft:iron_trapdoor", HALF_TOP_OPEN_TRUE_FACING_EAST));
      register(2688, create("minecraft:prismarine"), create("minecraft:prismarine", Map.of("variant", "prismarine")));
      register(2689, create("minecraft:prismarine_bricks"), create("minecraft:prismarine", Map.of("variant", "prismarine_bricks")));
      register(2690, create("minecraft:dark_prismarine"), create("minecraft:prismarine", Map.of("variant", "dark_prismarine")));
      register(2704, create("minecraft:sea_lantern"), create("minecraft:sea_lantern"));
      register(2720, create("minecraft:hay_block", AXIS_Y), create("minecraft:hay_block", AXIS_Y));
      register(2724, create("minecraft:hay_block", AXIS_X), create("minecraft:hay_block", AXIS_X));
      register(2728, create("minecraft:hay_block", AXIS_Z), create("minecraft:hay_block", AXIS_Z));
      register(2736, create("minecraft:white_carpet"), create("minecraft:carpet", COLOR_WHITE));
      register(2737, create("minecraft:orange_carpet"), create("minecraft:carpet", COLOR_ORANGE));
      register(2738, create("minecraft:magenta_carpet"), create("minecraft:carpet", COLOR_MAGENTA));
      register(2739, create("minecraft:light_blue_carpet"), create("minecraft:carpet", COLOR_LIGHT_BLUE));
      register(2740, create("minecraft:yellow_carpet"), create("minecraft:carpet", COLOR_YELLOW));
      register(2741, create("minecraft:lime_carpet"), create("minecraft:carpet", COLOR_LIME));
      register(2742, create("minecraft:pink_carpet"), create("minecraft:carpet", COLOR_PINK));
      register(2743, create("minecraft:gray_carpet"), create("minecraft:carpet", COLOR_GRAY));
      register(2744, create("minecraft:light_gray_carpet"), create("minecraft:carpet", COLOR_SILVER));
      register(2745, create("minecraft:cyan_carpet"), create("minecraft:carpet", COLOR_CYAN));
      register(2746, create("minecraft:purple_carpet"), create("minecraft:carpet", COLOR_PURPLE));
      register(2747, create("minecraft:blue_carpet"), create("minecraft:carpet", COLOR_BLUE));
      register(2748, create("minecraft:brown_carpet"), create("minecraft:carpet", COLOR_BROWN));
      register(2749, create("minecraft:green_carpet"), create("minecraft:carpet", COLOR_GREEN));
      register(2750, create("minecraft:red_carpet"), create("minecraft:carpet", COLOR_RED));
      register(2751, create("minecraft:black_carpet"), create("minecraft:carpet", COLOR_BLACK));
      register(2752, create("minecraft:terracotta"), create("minecraft:hardened_clay"));
      register(2768, create("minecraft:coal_block"), create("minecraft:coal_block"));
      register(2784, create("minecraft:packed_ice"), create("minecraft:packed_ice"));
      register(
         2800,
         create("minecraft:sunflower", HALF_LOWER),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "sunflower")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "sunflower")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "sunflower")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "sunflower"))
      );
      register(
         2801,
         create("minecraft:lilac", HALF_LOWER),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "syringa")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "syringa")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "syringa")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "syringa"))
      );
      register(
         2802,
         create("minecraft:tall_grass", HALF_LOWER),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "double_grass")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "double_grass")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "double_grass")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "double_grass"))
      );
      register(
         2803,
         create("minecraft:large_fern", HALF_LOWER),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "double_fern")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "double_fern")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "double_fern")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "double_fern"))
      );
      register(
         2804,
         create("minecraft:rose_bush", HALF_LOWER),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "double_rose")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "double_rose")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "double_rose")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "double_rose"))
      );
      register(
         2805,
         create("minecraft:peony", HALF_LOWER),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "lower", "variant", "paeonia")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "lower", "variant", "paeonia")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "lower", "variant", "paeonia")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "lower", "variant", "paeonia"))
      );
      register(
         2808,
         create("minecraft:peony", HALF_UPPER),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "double_fern")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "double_grass")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "double_rose")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "paeonia")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "sunflower")),
         create("minecraft:double_plant", Map.of("facing", "south", "half", "upper", "variant", "syringa"))
      );
      register(
         2809,
         create("minecraft:peony", HALF_UPPER),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "double_fern")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "double_grass")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "double_rose")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "paeonia")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "sunflower")),
         create("minecraft:double_plant", Map.of("facing", "west", "half", "upper", "variant", "syringa"))
      );
      register(
         2810,
         create("minecraft:peony", HALF_UPPER),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "double_fern")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "double_grass")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "double_rose")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "paeonia")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "sunflower")),
         create("minecraft:double_plant", Map.of("facing", "north", "half", "upper", "variant", "syringa"))
      );
      register(
         2811,
         create("minecraft:peony", HALF_UPPER),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "double_fern")),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "double_grass")),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "double_rose")),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "paeonia")),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "sunflower")),
         create("minecraft:double_plant", Map.of("facing", "east", "half", "upper", "variant", "syringa"))
      );
   }

   private static void bootstrapB() {
      register(2816, create("minecraft:white_banner", ROTATION_0), create("minecraft:standing_banner", ROTATION_0));
      register(2817, create("minecraft:white_banner", ROTATION_1), create("minecraft:standing_banner", ROTATION_1));
      register(2818, create("minecraft:white_banner", ROTATION_2), create("minecraft:standing_banner", ROTATION_2));
      register(2819, create("minecraft:white_banner", ROTATION_3), create("minecraft:standing_banner", ROTATION_3));
      register(2820, create("minecraft:white_banner", ROTATION_4), create("minecraft:standing_banner", ROTATION_4));
      register(2821, create("minecraft:white_banner", ROTATION_5), create("minecraft:standing_banner", ROTATION_5));
      register(2822, create("minecraft:white_banner", ROTATION_6), create("minecraft:standing_banner", ROTATION_6));
      register(2823, create("minecraft:white_banner", ROTATION_7), create("minecraft:standing_banner", ROTATION_7));
      register(2824, create("minecraft:white_banner", ROTATION_8), create("minecraft:standing_banner", ROTATION_8));
      register(2825, create("minecraft:white_banner", ROTATION_9), create("minecraft:standing_banner", ROTATION_9));
      register(2826, create("minecraft:white_banner", ROTATION_10), create("minecraft:standing_banner", ROTATION_10));
      register(2827, create("minecraft:white_banner", ROTATION_11), create("minecraft:standing_banner", ROTATION_11));
      register(2828, create("minecraft:white_banner", ROTATION_12), create("minecraft:standing_banner", ROTATION_12));
      register(2829, create("minecraft:white_banner", ROTATION_13), create("minecraft:standing_banner", ROTATION_13));
      register(2830, create("minecraft:white_banner", ROTATION_14), create("minecraft:standing_banner", ROTATION_14));
      register(2831, create("minecraft:white_banner", ROTATION_15), create("minecraft:standing_banner", ROTATION_15));
      register(2834, create("minecraft:white_wall_banner", FACING_NORTH), create("minecraft:wall_banner", FACING_NORTH));
      register(2835, create("minecraft:white_wall_banner", FACING_SOUTH), create("minecraft:wall_banner", FACING_SOUTH));
      register(2836, create("minecraft:white_wall_banner", FACING_WEST), create("minecraft:wall_banner", FACING_WEST));
      register(2837, create("minecraft:white_wall_banner", FACING_EAST), create("minecraft:wall_banner", FACING_EAST));
      register(2848, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "0")), create("minecraft:daylight_detector_inverted", POWER_0));
      register(2849, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "1")), create("minecraft:daylight_detector_inverted", POWER_1));
      register(2850, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "2")), create("minecraft:daylight_detector_inverted", POWER_2));
      register(2851, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "3")), create("minecraft:daylight_detector_inverted", POWER_3));
      register(2852, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "4")), create("minecraft:daylight_detector_inverted", POWER_4));
      register(2853, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "5")), create("minecraft:daylight_detector_inverted", POWER_5));
      register(2854, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "6")), create("minecraft:daylight_detector_inverted", POWER_6));
      register(2855, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "7")), create("minecraft:daylight_detector_inverted", POWER_7));
      register(2856, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "8")), create("minecraft:daylight_detector_inverted", POWER_8));
      register(2857, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "9")), create("minecraft:daylight_detector_inverted", POWER_9));
      register(2858, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "10")), create("minecraft:daylight_detector_inverted", POWER_10));
      register(2859, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "11")), create("minecraft:daylight_detector_inverted", POWER_11));
      register(2860, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "12")), create("minecraft:daylight_detector_inverted", POWER_12));
      register(2861, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "13")), create("minecraft:daylight_detector_inverted", POWER_13));
      register(2862, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "14")), create("minecraft:daylight_detector_inverted", POWER_14));
      register(2863, create("minecraft:daylight_detector", Map.of("inverted", "true", "power", "15")), create("minecraft:daylight_detector_inverted", POWER_15));
      register(2864, create("minecraft:red_sandstone"), create("minecraft:red_sandstone", Map.of("type", "red_sandstone")));
      register(2865, create("minecraft:chiseled_red_sandstone"), create("minecraft:red_sandstone", Map.of("type", "chiseled_red_sandstone")));
      register(2866, create("minecraft:cut_red_sandstone"), create("minecraft:red_sandstone", Map.of("type", "smooth_red_sandstone")));
      register(
         2880,
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2881,
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2882,
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2883,
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2884,
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         2885,
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         2886,
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         2887,
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:red_sandstone_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         2896,
         create("minecraft:red_sandstone_slab", TYPE_DOUBLE),
         create("minecraft:double_stone_slab2", Map.of("seamless", "false", "variant", "red_sandstone"))
      );
      register(2904, create("minecraft:smooth_red_sandstone"), create("minecraft:double_stone_slab2", Map.of("seamless", "true", "variant", "red_sandstone")));
      register(2912, create("minecraft:red_sandstone_slab", TYPE_BOTTOM), create("minecraft:stone_slab2", Map.of("half", "bottom", "variant", "red_sandstone")));
      register(2920, create("minecraft:red_sandstone_slab", TYPE_TOP), create("minecraft:stone_slab2", Map.of("half", "top", "variant", "red_sandstone")));
      register(
         2928,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2929,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2930,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2931,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2932,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2933,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2934,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2935,
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2936,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2937,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2938,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2939,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2940,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2941,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2942,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2943,
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:spruce_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2944,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2945,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2946,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2947,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2948,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2949,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2950,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2951,
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2952,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2953,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2954,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2955,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2956,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2957,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2958,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2959,
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:birch_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2960,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2961,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2962,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2963,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2964,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2965,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2966,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2967,
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2968,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2969,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2970,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2971,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2972,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2973,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2974,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2975,
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:jungle_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2976,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2977,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2978,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2979,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2980,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2981,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2982,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2983,
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2984,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2985,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2986,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2987,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2988,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2989,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2990,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2991,
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:dark_oak_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         2992,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         2993,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         2994,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         2995,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         2996,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         2997,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         2998,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         2999,
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_FALSE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         3000,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_SOUTH)
      );
      register(
         3001,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_WEST)
      );
      register(
         3002,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_NORTH)
      );
      register(
         3003,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_FALSE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_FALSE_FACING_EAST)
      );
      register(
         3004,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_SOUTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_SOUTH)
      );
      register(
         3005,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_WEST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_WEST)
      );
      register(
         3006,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_NORTH),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_NORTH)
      );
      register(
         3007,
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_FALSE_OPEN_TRUE_FACING_EAST),
         create("minecraft:acacia_fence_gate", POWERED_TRUE_IN_WALL_TRUE_OPEN_TRUE_FACING_EAST)
      );
      register(
         3008,
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:spruce_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:spruce_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(
         3024,
         create("minecraft:birch_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:birch_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:birch_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(
         3040,
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:jungle_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:jungle_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(
         3056,
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:dark_oak_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:dark_oak_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
   }

   private static void bootstrapC() {
      register(
         3072,
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_FALSE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_FALSE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_FALSE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_TRUE_SOUTH_FALSE_EAST_TRUE),
         create("minecraft:acacia_fence", WEST_FALSE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE),
         create("minecraft:acacia_fence", WEST_TRUE_NORTH_TRUE_SOUTH_TRUE_EAST_TRUE)
      );
      register(
         3088,
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3089,
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3090,
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3091,
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3092,
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3093,
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3094,
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3095,
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3096,
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3097,
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3098,
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3099,
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:spruce_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3104,
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3105,
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3106,
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3107,
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3108,
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3109,
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3110,
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3111,
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3112,
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3113,
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3114,
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3115,
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:birch_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3120,
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3121,
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3122,
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3123,
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3124,
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3125,
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3126,
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3127,
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3128,
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3129,
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3130,
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3131,
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:jungle_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3136,
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3137,
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3138,
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3139,
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3140,
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3141,
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3142,
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3143,
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3144,
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3145,
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3146,
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3147,
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:acacia_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3152,
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3153,
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3154,
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3155,
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3156,
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3157,
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3158,
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3159,
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_LOWER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_LOWER)
      );
      register(
         3160,
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3161,
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_FALSE_HINGE_RIGHT_HALF_UPPER)
      );
      register(
         3162,
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_LEFT_HALF_UPPER)
      );
      register(
         3163,
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_EAST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_NORTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_SOUTH_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_FALSE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER),
         create("minecraft:dark_oak_door", FACING_WEST_OPEN_TRUE_POWERED_TRUE_HINGE_RIGHT_HALF_UPPER)
      );
      register(3168, create("minecraft:end_rod", FACING_DOWN), create("minecraft:end_rod", FACING_DOWN));
      register(3169, create("minecraft:end_rod", FACING_UP), create("minecraft:end_rod", FACING_UP));
      register(3170, create("minecraft:end_rod", FACING_NORTH), create("minecraft:end_rod", FACING_NORTH));
      register(3171, create("minecraft:end_rod", FACING_SOUTH), create("minecraft:end_rod", FACING_SOUTH));
      register(3172, create("minecraft:end_rod", FACING_WEST), create("minecraft:end_rod", FACING_WEST));
      register(3173, create("minecraft:end_rod", FACING_EAST), create("minecraft:end_rod", FACING_EAST));
      register(
         3184,
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_FALSE_UP_FALSE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", NORTH_TRUE_EAST_FALSE_UP_TRUE_WEST_TRUE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", NORTH_FALSE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_FALSE_SOUTH_FALSE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", NORTH_TRUE_EAST_TRUE_UP_FALSE_WEST_TRUE_SOUTH_TRUE_DOWN_FALSE),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "false", "east", "true", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "false", "north", "true", "south", "true", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "false", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "false", "south", "true", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "false", "up", "true", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "true", "up", "false", "west", "false")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "true", "up", "false", "west", "true")),
         create("minecraft:chorus_plant", Map.of("down", "true", "east", "true", "north", "true", "south", "true", "up", "true", "west", "false")),
         create("minecraft:chorus_plant", NORTH_TRUE_EAST_TRUE_UP_TRUE_WEST_TRUE_SOUTH_TRUE_DOWN_TRUE)
      );
      register(3200, create("minecraft:chorus_flower", AGE_0), create("minecraft:chorus_flower", AGE_0));
      register(3201, create("minecraft:chorus_flower", AGE_1), create("minecraft:chorus_flower", AGE_1));
      register(3202, create("minecraft:chorus_flower", AGE_2), create("minecraft:chorus_flower", AGE_2));
      register(3203, create("minecraft:chorus_flower", AGE_3), create("minecraft:chorus_flower", AGE_3));
      register(3204, create("minecraft:chorus_flower", AGE_4), create("minecraft:chorus_flower", AGE_4));
      register(3205, create("minecraft:chorus_flower", AGE_5), create("minecraft:chorus_flower", AGE_5));
      register(3216, create("minecraft:purpur_block"), create("minecraft:purpur_block"));
      register(3232, create("minecraft:purpur_pillar", AXIS_Y), create("minecraft:purpur_pillar", AXIS_Y));
      register(3236, create("minecraft:purpur_pillar", AXIS_X), create("minecraft:purpur_pillar", AXIS_X));
      register(3240, create("minecraft:purpur_pillar", AXIS_Z), create("minecraft:purpur_pillar", AXIS_Z));
      register(
         3248,
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         3249,
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         3250,
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         3251,
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_BOTTOM_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(
         3252,
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_EAST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_EAST)
      );
      register(
         3253,
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_WEST),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_WEST)
      );
      register(
         3254,
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_SOUTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_SOUTH)
      );
      register(
         3255,
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_LEFT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_INNER_RIGHT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_LEFT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_OUTER_RIGHT_FACING_NORTH),
         create("minecraft:purpur_stairs", HALF_TOP_SHAPE_STRAIGHT_FACING_NORTH)
      );
      register(3264, create("minecraft:purpur_slab", TYPE_DOUBLE), create("minecraft:purpur_double_slab", Map.of("variant", "default")));
      register(3280, create("minecraft:purpur_slab", TYPE_BOTTOM), create("minecraft:purpur_slab", Map.of("half", "bottom", "variant", "default")));
      register(3288, create("minecraft:purpur_slab", TYPE_TOP), create("minecraft:purpur_slab", Map.of("half", "top", "variant", "default")));
      register(3296, create("minecraft:end_stone_bricks"), create("minecraft:end_bricks"));
      register(3312, create("minecraft:beetroots", AGE_0), create("minecraft:beetroots", AGE_0));
      register(3313, create("minecraft:beetroots", AGE_1), create("minecraft:beetroots", AGE_1));
      register(3314, create("minecraft:beetroots", AGE_2), create("minecraft:beetroots", AGE_2));
      register(3315, create("minecraft:beetroots", AGE_3), create("minecraft:beetroots", AGE_3));
   }

   private static void bootstrapD() {
      register(3328, create("minecraft:grass_path"), create("minecraft:grass_path"));
      register(3344, create("minecraft:end_gateway"), create("minecraft:end_gateway"));
      register(
         3360,
         create("minecraft:repeating_command_block", FACING_DOWN_CONDITIONAL_FALSE),
         create("minecraft:repeating_command_block", FACING_DOWN_CONDITIONAL_FALSE)
      );
      register(
         3361,
         create("minecraft:repeating_command_block", FACING_UP_CONDITIONAL_FALSE),
         create("minecraft:repeating_command_block", FACING_UP_CONDITIONAL_FALSE)
      );
      register(
         3362,
         create("minecraft:repeating_command_block", FACING_NORTH_CONDITIONAL_FALSE),
         create("minecraft:repeating_command_block", FACING_NORTH_CONDITIONAL_FALSE)
      );
      register(
         3363,
         create("minecraft:repeating_command_block", FACING_SOUTH_CONDITIONAL_FALSE),
         create("minecraft:repeating_command_block", FACING_SOUTH_CONDITIONAL_FALSE)
      );
      register(
         3364,
         create("minecraft:repeating_command_block", FACING_WEST_CONDITIONAL_FALSE),
         create("minecraft:repeating_command_block", FACING_WEST_CONDITIONAL_FALSE)
      );
      register(
         3365,
         create("minecraft:repeating_command_block", FACING_EAST_CONDITIONAL_FALSE),
         create("minecraft:repeating_command_block", FACING_EAST_CONDITIONAL_FALSE)
      );
      register(
         3368,
         create("minecraft:repeating_command_block", FACING_DOWN_CONDITIONAL_TRUE),
         create("minecraft:repeating_command_block", FACING_DOWN_CONDITIONAL_TRUE)
      );
      register(
         3369, create("minecraft:repeating_command_block", FACING_UP_CONDITIONAL_TRUE), create("minecraft:repeating_command_block", FACING_UP_CONDITIONAL_TRUE)
      );
      register(
         3370,
         create("minecraft:repeating_command_block", FACING_NORTH_CONDITIONAL_TRUE),
         create("minecraft:repeating_command_block", FACING_NORTH_CONDITIONAL_TRUE)
      );
      register(
         3371,
         create("minecraft:repeating_command_block", FACING_SOUTH_CONDITIONAL_TRUE),
         create("minecraft:repeating_command_block", FACING_SOUTH_CONDITIONAL_TRUE)
      );
      register(
         3372,
         create("minecraft:repeating_command_block", FACING_WEST_CONDITIONAL_TRUE),
         create("minecraft:repeating_command_block", FACING_WEST_CONDITIONAL_TRUE)
      );
      register(
         3373,
         create("minecraft:repeating_command_block", FACING_EAST_CONDITIONAL_TRUE),
         create("minecraft:repeating_command_block", FACING_EAST_CONDITIONAL_TRUE)
      );
      register(
         3376, create("minecraft:chain_command_block", FACING_DOWN_CONDITIONAL_FALSE), create("minecraft:chain_command_block", FACING_DOWN_CONDITIONAL_FALSE)
      );
      register(3377, create("minecraft:chain_command_block", FACING_UP_CONDITIONAL_FALSE), create("minecraft:chain_command_block", FACING_UP_CONDITIONAL_FALSE));
      register(
         3378, create("minecraft:chain_command_block", FACING_NORTH_CONDITIONAL_FALSE), create("minecraft:chain_command_block", FACING_NORTH_CONDITIONAL_FALSE)
      );
      register(
         3379, create("minecraft:chain_command_block", FACING_SOUTH_CONDITIONAL_FALSE), create("minecraft:chain_command_block", FACING_SOUTH_CONDITIONAL_FALSE)
      );
      register(
         3380, create("minecraft:chain_command_block", FACING_WEST_CONDITIONAL_FALSE), create("minecraft:chain_command_block", FACING_WEST_CONDITIONAL_FALSE)
      );
      register(
         3381, create("minecraft:chain_command_block", FACING_EAST_CONDITIONAL_FALSE), create("minecraft:chain_command_block", FACING_EAST_CONDITIONAL_FALSE)
      );
      register(
         3384, create("minecraft:chain_command_block", FACING_DOWN_CONDITIONAL_TRUE), create("minecraft:chain_command_block", FACING_DOWN_CONDITIONAL_TRUE)
      );
      register(3385, create("minecraft:chain_command_block", FACING_UP_CONDITIONAL_TRUE), create("minecraft:chain_command_block", FACING_UP_CONDITIONAL_TRUE));
      register(
         3386, create("minecraft:chain_command_block", FACING_NORTH_CONDITIONAL_TRUE), create("minecraft:chain_command_block", FACING_NORTH_CONDITIONAL_TRUE)
      );
      register(
         3387, create("minecraft:chain_command_block", FACING_SOUTH_CONDITIONAL_TRUE), create("minecraft:chain_command_block", FACING_SOUTH_CONDITIONAL_TRUE)
      );
      register(
         3388, create("minecraft:chain_command_block", FACING_WEST_CONDITIONAL_TRUE), create("minecraft:chain_command_block", FACING_WEST_CONDITIONAL_TRUE)
      );
      register(
         3389, create("minecraft:chain_command_block", FACING_EAST_CONDITIONAL_TRUE), create("minecraft:chain_command_block", FACING_EAST_CONDITIONAL_TRUE)
      );
      register(3392, create("minecraft:frosted_ice", AGE_0), create("minecraft:frosted_ice", AGE_0));
      register(3393, create("minecraft:frosted_ice", AGE_1), create("minecraft:frosted_ice", AGE_1));
      register(3394, create("minecraft:frosted_ice", AGE_2), create("minecraft:frosted_ice", AGE_2));
      register(3395, create("minecraft:frosted_ice", AGE_3), create("minecraft:frosted_ice", AGE_3));
      register(3408, create("minecraft:magma_block"), create("minecraft:magma"));
      register(3424, create("minecraft:nether_wart_block"), create("minecraft:nether_wart_block"));
      register(3440, create("minecraft:red_nether_bricks"), create("minecraft:red_nether_brick"));
      register(3456, create("minecraft:bone_block", AXIS_Y), create("minecraft:bone_block", AXIS_Y));
      register(3460, create("minecraft:bone_block", AXIS_X), create("minecraft:bone_block", AXIS_X));
      register(3464, create("minecraft:bone_block", AXIS_Z), create("minecraft:bone_block", AXIS_Z));
      register(3472, create("minecraft:structure_void"), create("minecraft:structure_void"));
      register(3488, create("minecraft:observer", FACING_DOWN_POWERED_FALSE), create("minecraft:observer", FACING_DOWN_POWERED_FALSE));
      register(3489, create("minecraft:observer", FACING_UP_POWERED_FALSE), create("minecraft:observer", FACING_UP_POWERED_FALSE));
      register(3490, create("minecraft:observer", FACING_NORTH_POWERED_FALSE), create("minecraft:observer", FACING_NORTH_POWERED_FALSE));
      register(3491, create("minecraft:observer", FACING_SOUTH_POWERED_FALSE), create("minecraft:observer", FACING_SOUTH_POWERED_FALSE));
      register(3492, create("minecraft:observer", FACING_WEST_POWERED_FALSE), create("minecraft:observer", FACING_WEST_POWERED_FALSE));
      register(3493, create("minecraft:observer", FACING_EAST_POWERED_FALSE), create("minecraft:observer", FACING_EAST_POWERED_FALSE));
      register(3496, create("minecraft:observer", FACING_DOWN_POWERED_TRUE), create("minecraft:observer", FACING_DOWN_POWERED_TRUE));
      register(3497, create("minecraft:observer", FACING_UP_POWERED_TRUE), create("minecraft:observer", FACING_UP_POWERED_TRUE));
      register(3498, create("minecraft:observer", FACING_NORTH_POWERED_TRUE), create("minecraft:observer", FACING_NORTH_POWERED_TRUE));
      register(3499, create("minecraft:observer", FACING_SOUTH_POWERED_TRUE), create("minecraft:observer", FACING_SOUTH_POWERED_TRUE));
      register(3500, create("minecraft:observer", FACING_WEST_POWERED_TRUE), create("minecraft:observer", FACING_WEST_POWERED_TRUE));
      register(3501, create("minecraft:observer", FACING_EAST_POWERED_TRUE), create("minecraft:observer", FACING_EAST_POWERED_TRUE));
      register(3504, create("minecraft:white_shulker_box", FACING_DOWN), create("minecraft:white_shulker_box", FACING_DOWN));
      register(3505, create("minecraft:white_shulker_box", FACING_UP), create("minecraft:white_shulker_box", FACING_UP));
      register(3506, create("minecraft:white_shulker_box", FACING_NORTH), create("minecraft:white_shulker_box", FACING_NORTH));
      register(3507, create("minecraft:white_shulker_box", FACING_SOUTH), create("minecraft:white_shulker_box", FACING_SOUTH));
      register(3508, create("minecraft:white_shulker_box", FACING_WEST), create("minecraft:white_shulker_box", FACING_WEST));
      register(3509, create("minecraft:white_shulker_box", FACING_EAST), create("minecraft:white_shulker_box", FACING_EAST));
      register(3520, create("minecraft:orange_shulker_box", FACING_DOWN), create("minecraft:orange_shulker_box", FACING_DOWN));
      register(3521, create("minecraft:orange_shulker_box", FACING_UP), create("minecraft:orange_shulker_box", FACING_UP));
      register(3522, create("minecraft:orange_shulker_box", FACING_NORTH), create("minecraft:orange_shulker_box", FACING_NORTH));
      register(3523, create("minecraft:orange_shulker_box", FACING_SOUTH), create("minecraft:orange_shulker_box", FACING_SOUTH));
      register(3524, create("minecraft:orange_shulker_box", FACING_WEST), create("minecraft:orange_shulker_box", FACING_WEST));
      register(3525, create("minecraft:orange_shulker_box", FACING_EAST), create("minecraft:orange_shulker_box", FACING_EAST));
      register(3536, create("minecraft:magenta_shulker_box", FACING_DOWN), create("minecraft:magenta_shulker_box", FACING_DOWN));
      register(3537, create("minecraft:magenta_shulker_box", FACING_UP), create("minecraft:magenta_shulker_box", FACING_UP));
      register(3538, create("minecraft:magenta_shulker_box", FACING_NORTH), create("minecraft:magenta_shulker_box", FACING_NORTH));
      register(3539, create("minecraft:magenta_shulker_box", FACING_SOUTH), create("minecraft:magenta_shulker_box", FACING_SOUTH));
      register(3540, create("minecraft:magenta_shulker_box", FACING_WEST), create("minecraft:magenta_shulker_box", FACING_WEST));
      register(3541, create("minecraft:magenta_shulker_box", FACING_EAST), create("minecraft:magenta_shulker_box", FACING_EAST));
      register(3552, create("minecraft:light_blue_shulker_box", FACING_DOWN), create("minecraft:light_blue_shulker_box", FACING_DOWN));
      register(3553, create("minecraft:light_blue_shulker_box", FACING_UP), create("minecraft:light_blue_shulker_box", FACING_UP));
      register(3554, create("minecraft:light_blue_shulker_box", FACING_NORTH), create("minecraft:light_blue_shulker_box", FACING_NORTH));
      register(3555, create("minecraft:light_blue_shulker_box", FACING_SOUTH), create("minecraft:light_blue_shulker_box", FACING_SOUTH));
      register(3556, create("minecraft:light_blue_shulker_box", FACING_WEST), create("minecraft:light_blue_shulker_box", FACING_WEST));
      register(3557, create("minecraft:light_blue_shulker_box", FACING_EAST), create("minecraft:light_blue_shulker_box", FACING_EAST));
      register(3568, create("minecraft:yellow_shulker_box", FACING_DOWN), create("minecraft:yellow_shulker_box", FACING_DOWN));
      register(3569, create("minecraft:yellow_shulker_box", FACING_UP), create("minecraft:yellow_shulker_box", FACING_UP));
      register(3570, create("minecraft:yellow_shulker_box", FACING_NORTH), create("minecraft:yellow_shulker_box", FACING_NORTH));
      register(3571, create("minecraft:yellow_shulker_box", FACING_SOUTH), create("minecraft:yellow_shulker_box", FACING_SOUTH));
      register(3572, create("minecraft:yellow_shulker_box", FACING_WEST), create("minecraft:yellow_shulker_box", FACING_WEST));
      register(3573, create("minecraft:yellow_shulker_box", FACING_EAST), create("minecraft:yellow_shulker_box", FACING_EAST));
   }

   private static void bootstrapE() {
      register(3584, create("minecraft:lime_shulker_box", FACING_DOWN), create("minecraft:lime_shulker_box", FACING_DOWN));
      register(3585, create("minecraft:lime_shulker_box", FACING_UP), create("minecraft:lime_shulker_box", FACING_UP));
      register(3586, create("minecraft:lime_shulker_box", FACING_NORTH), create("minecraft:lime_shulker_box", FACING_NORTH));
      register(3587, create("minecraft:lime_shulker_box", FACING_SOUTH), create("minecraft:lime_shulker_box", FACING_SOUTH));
      register(3588, create("minecraft:lime_shulker_box", FACING_WEST), create("minecraft:lime_shulker_box", FACING_WEST));
      register(3589, create("minecraft:lime_shulker_box", FACING_EAST), create("minecraft:lime_shulker_box", FACING_EAST));
      register(3600, create("minecraft:pink_shulker_box", FACING_DOWN), create("minecraft:pink_shulker_box", FACING_DOWN));
      register(3601, create("minecraft:pink_shulker_box", FACING_UP), create("minecraft:pink_shulker_box", FACING_UP));
      register(3602, create("minecraft:pink_shulker_box", FACING_NORTH), create("minecraft:pink_shulker_box", FACING_NORTH));
      register(3603, create("minecraft:pink_shulker_box", FACING_SOUTH), create("minecraft:pink_shulker_box", FACING_SOUTH));
      register(3604, create("minecraft:pink_shulker_box", FACING_WEST), create("minecraft:pink_shulker_box", FACING_WEST));
      register(3605, create("minecraft:pink_shulker_box", FACING_EAST), create("minecraft:pink_shulker_box", FACING_EAST));
      register(3616, create("minecraft:gray_shulker_box", FACING_DOWN), create("minecraft:gray_shulker_box", FACING_DOWN));
      register(3617, create("minecraft:gray_shulker_box", FACING_UP), create("minecraft:gray_shulker_box", FACING_UP));
      register(3618, create("minecraft:gray_shulker_box", FACING_NORTH), create("minecraft:gray_shulker_box", FACING_NORTH));
      register(3619, create("minecraft:gray_shulker_box", FACING_SOUTH), create("minecraft:gray_shulker_box", FACING_SOUTH));
      register(3620, create("minecraft:gray_shulker_box", FACING_WEST), create("minecraft:gray_shulker_box", FACING_WEST));
      register(3621, create("minecraft:gray_shulker_box", FACING_EAST), create("minecraft:gray_shulker_box", FACING_EAST));
      register(3632, create("minecraft:light_gray_shulker_box", FACING_DOWN), create("minecraft:silver_shulker_box", FACING_DOWN));
      register(3633, create("minecraft:light_gray_shulker_box", FACING_UP), create("minecraft:silver_shulker_box", FACING_UP));
      register(3634, create("minecraft:light_gray_shulker_box", FACING_NORTH), create("minecraft:silver_shulker_box", FACING_NORTH));
      register(3635, create("minecraft:light_gray_shulker_box", FACING_SOUTH), create("minecraft:silver_shulker_box", FACING_SOUTH));
      register(3636, create("minecraft:light_gray_shulker_box", FACING_WEST), create("minecraft:silver_shulker_box", FACING_WEST));
      register(3637, create("minecraft:light_gray_shulker_box", FACING_EAST), create("minecraft:silver_shulker_box", FACING_EAST));
      register(3648, create("minecraft:cyan_shulker_box", FACING_DOWN), create("minecraft:cyan_shulker_box", FACING_DOWN));
      register(3649, create("minecraft:cyan_shulker_box", FACING_UP), create("minecraft:cyan_shulker_box", FACING_UP));
      register(3650, create("minecraft:cyan_shulker_box", FACING_NORTH), create("minecraft:cyan_shulker_box", FACING_NORTH));
      register(3651, create("minecraft:cyan_shulker_box", FACING_SOUTH), create("minecraft:cyan_shulker_box", FACING_SOUTH));
      register(3652, create("minecraft:cyan_shulker_box", FACING_WEST), create("minecraft:cyan_shulker_box", FACING_WEST));
      register(3653, create("minecraft:cyan_shulker_box", FACING_EAST), create("minecraft:cyan_shulker_box", FACING_EAST));
      register(3664, create("minecraft:purple_shulker_box", FACING_DOWN), create("minecraft:purple_shulker_box", FACING_DOWN));
      register(3665, create("minecraft:purple_shulker_box", FACING_UP), create("minecraft:purple_shulker_box", FACING_UP));
      register(3666, create("minecraft:purple_shulker_box", FACING_NORTH), create("minecraft:purple_shulker_box", FACING_NORTH));
      register(3667, create("minecraft:purple_shulker_box", FACING_SOUTH), create("minecraft:purple_shulker_box", FACING_SOUTH));
      register(3668, create("minecraft:purple_shulker_box", FACING_WEST), create("minecraft:purple_shulker_box", FACING_WEST));
      register(3669, create("minecraft:purple_shulker_box", FACING_EAST), create("minecraft:purple_shulker_box", FACING_EAST));
      register(3680, create("minecraft:blue_shulker_box", FACING_DOWN), create("minecraft:blue_shulker_box", FACING_DOWN));
      register(3681, create("minecraft:blue_shulker_box", FACING_UP), create("minecraft:blue_shulker_box", FACING_UP));
      register(3682, create("minecraft:blue_shulker_box", FACING_NORTH), create("minecraft:blue_shulker_box", FACING_NORTH));
      register(3683, create("minecraft:blue_shulker_box", FACING_SOUTH), create("minecraft:blue_shulker_box", FACING_SOUTH));
      register(3684, create("minecraft:blue_shulker_box", FACING_WEST), create("minecraft:blue_shulker_box", FACING_WEST));
      register(3685, create("minecraft:blue_shulker_box", FACING_EAST), create("minecraft:blue_shulker_box", FACING_EAST));
      register(3696, create("minecraft:brown_shulker_box", FACING_DOWN), create("minecraft:brown_shulker_box", FACING_DOWN));
      register(3697, create("minecraft:brown_shulker_box", FACING_UP), create("minecraft:brown_shulker_box", FACING_UP));
      register(3698, create("minecraft:brown_shulker_box", FACING_NORTH), create("minecraft:brown_shulker_box", FACING_NORTH));
      register(3699, create("minecraft:brown_shulker_box", FACING_SOUTH), create("minecraft:brown_shulker_box", FACING_SOUTH));
      register(3700, create("minecraft:brown_shulker_box", FACING_WEST), create("minecraft:brown_shulker_box", FACING_WEST));
      register(3701, create("minecraft:brown_shulker_box", FACING_EAST), create("minecraft:brown_shulker_box", FACING_EAST));
      register(3712, create("minecraft:green_shulker_box", FACING_DOWN), create("minecraft:green_shulker_box", FACING_DOWN));
      register(3713, create("minecraft:green_shulker_box", FACING_UP), create("minecraft:green_shulker_box", FACING_UP));
      register(3714, create("minecraft:green_shulker_box", FACING_NORTH), create("minecraft:green_shulker_box", FACING_NORTH));
      register(3715, create("minecraft:green_shulker_box", FACING_SOUTH), create("minecraft:green_shulker_box", FACING_SOUTH));
      register(3716, create("minecraft:green_shulker_box", FACING_WEST), create("minecraft:green_shulker_box", FACING_WEST));
      register(3717, create("minecraft:green_shulker_box", FACING_EAST), create("minecraft:green_shulker_box", FACING_EAST));
      register(3728, create("minecraft:red_shulker_box", FACING_DOWN), create("minecraft:red_shulker_box", FACING_DOWN));
      register(3729, create("minecraft:red_shulker_box", FACING_UP), create("minecraft:red_shulker_box", FACING_UP));
      register(3730, create("minecraft:red_shulker_box", FACING_NORTH), create("minecraft:red_shulker_box", FACING_NORTH));
      register(3731, create("minecraft:red_shulker_box", FACING_SOUTH), create("minecraft:red_shulker_box", FACING_SOUTH));
      register(3732, create("minecraft:red_shulker_box", FACING_WEST), create("minecraft:red_shulker_box", FACING_WEST));
      register(3733, create("minecraft:red_shulker_box", FACING_EAST), create("minecraft:red_shulker_box", FACING_EAST));
      register(3744, create("minecraft:black_shulker_box", FACING_DOWN), create("minecraft:black_shulker_box", FACING_DOWN));
      register(3745, create("minecraft:black_shulker_box", FACING_UP), create("minecraft:black_shulker_box", FACING_UP));
      register(3746, create("minecraft:black_shulker_box", FACING_NORTH), create("minecraft:black_shulker_box", FACING_NORTH));
      register(3747, create("minecraft:black_shulker_box", FACING_SOUTH), create("minecraft:black_shulker_box", FACING_SOUTH));
      register(3748, create("minecraft:black_shulker_box", FACING_WEST), create("minecraft:black_shulker_box", FACING_WEST));
      register(3749, create("minecraft:black_shulker_box", FACING_EAST), create("minecraft:black_shulker_box", FACING_EAST));
      register(3760, create("minecraft:white_glazed_terracotta", FACING_SOUTH), create("minecraft:white_glazed_terracotta", FACING_SOUTH));
      register(3761, create("minecraft:white_glazed_terracotta", FACING_WEST), create("minecraft:white_glazed_terracotta", FACING_WEST));
      register(3762, create("minecraft:white_glazed_terracotta", FACING_NORTH), create("minecraft:white_glazed_terracotta", FACING_NORTH));
      register(3763, create("minecraft:white_glazed_terracotta", FACING_EAST), create("minecraft:white_glazed_terracotta", FACING_EAST));
      register(3776, create("minecraft:orange_glazed_terracotta", FACING_SOUTH), create("minecraft:orange_glazed_terracotta", FACING_SOUTH));
      register(3777, create("minecraft:orange_glazed_terracotta", FACING_WEST), create("minecraft:orange_glazed_terracotta", FACING_WEST));
      register(3778, create("minecraft:orange_glazed_terracotta", FACING_NORTH), create("minecraft:orange_glazed_terracotta", FACING_NORTH));
      register(3779, create("minecraft:orange_glazed_terracotta", FACING_EAST), create("minecraft:orange_glazed_terracotta", FACING_EAST));
      register(3792, create("minecraft:magenta_glazed_terracotta", FACING_SOUTH), create("minecraft:magenta_glazed_terracotta", FACING_SOUTH));
      register(3793, create("minecraft:magenta_glazed_terracotta", FACING_WEST), create("minecraft:magenta_glazed_terracotta", FACING_WEST));
      register(3794, create("minecraft:magenta_glazed_terracotta", FACING_NORTH), create("minecraft:magenta_glazed_terracotta", FACING_NORTH));
      register(3795, create("minecraft:magenta_glazed_terracotta", FACING_EAST), create("minecraft:magenta_glazed_terracotta", FACING_EAST));
      register(3808, create("minecraft:light_blue_glazed_terracotta", FACING_SOUTH), create("minecraft:light_blue_glazed_terracotta", FACING_SOUTH));
      register(3809, create("minecraft:light_blue_glazed_terracotta", FACING_WEST), create("minecraft:light_blue_glazed_terracotta", FACING_WEST));
      register(3810, create("minecraft:light_blue_glazed_terracotta", FACING_NORTH), create("minecraft:light_blue_glazed_terracotta", FACING_NORTH));
      register(3811, create("minecraft:light_blue_glazed_terracotta", FACING_EAST), create("minecraft:light_blue_glazed_terracotta", FACING_EAST));
      register(3824, create("minecraft:yellow_glazed_terracotta", FACING_SOUTH), create("minecraft:yellow_glazed_terracotta", FACING_SOUTH));
      register(3825, create("minecraft:yellow_glazed_terracotta", FACING_WEST), create("minecraft:yellow_glazed_terracotta", FACING_WEST));
      register(3826, create("minecraft:yellow_glazed_terracotta", FACING_NORTH), create("minecraft:yellow_glazed_terracotta", FACING_NORTH));
      register(3827, create("minecraft:yellow_glazed_terracotta", FACING_EAST), create("minecraft:yellow_glazed_terracotta", FACING_EAST));
   }

   private static void bootstrapF() {
      register(3840, create("minecraft:lime_glazed_terracotta", FACING_SOUTH), create("minecraft:lime_glazed_terracotta", FACING_SOUTH));
      register(3841, create("minecraft:lime_glazed_terracotta", FACING_WEST), create("minecraft:lime_glazed_terracotta", FACING_WEST));
      register(3842, create("minecraft:lime_glazed_terracotta", FACING_NORTH), create("minecraft:lime_glazed_terracotta", FACING_NORTH));
      register(3843, create("minecraft:lime_glazed_terracotta", FACING_EAST), create("minecraft:lime_glazed_terracotta", FACING_EAST));
      register(3856, create("minecraft:pink_glazed_terracotta", FACING_SOUTH), create("minecraft:pink_glazed_terracotta", FACING_SOUTH));
      register(3857, create("minecraft:pink_glazed_terracotta", FACING_WEST), create("minecraft:pink_glazed_terracotta", FACING_WEST));
      register(3858, create("minecraft:pink_glazed_terracotta", FACING_NORTH), create("minecraft:pink_glazed_terracotta", FACING_NORTH));
      register(3859, create("minecraft:pink_glazed_terracotta", FACING_EAST), create("minecraft:pink_glazed_terracotta", FACING_EAST));
      register(3872, create("minecraft:gray_glazed_terracotta", FACING_SOUTH), create("minecraft:gray_glazed_terracotta", FACING_SOUTH));
      register(3873, create("minecraft:gray_glazed_terracotta", FACING_WEST), create("minecraft:gray_glazed_terracotta", FACING_WEST));
      register(3874, create("minecraft:gray_glazed_terracotta", FACING_NORTH), create("minecraft:gray_glazed_terracotta", FACING_NORTH));
      register(3875, create("minecraft:gray_glazed_terracotta", FACING_EAST), create("minecraft:gray_glazed_terracotta", FACING_EAST));
      register(3888, create("minecraft:light_gray_glazed_terracotta", FACING_SOUTH), create("minecraft:silver_glazed_terracotta", FACING_SOUTH));
      register(3889, create("minecraft:light_gray_glazed_terracotta", FACING_WEST), create("minecraft:silver_glazed_terracotta", FACING_WEST));
      register(3890, create("minecraft:light_gray_glazed_terracotta", FACING_NORTH), create("minecraft:silver_glazed_terracotta", FACING_NORTH));
      register(3891, create("minecraft:light_gray_glazed_terracotta", FACING_EAST), create("minecraft:silver_glazed_terracotta", FACING_EAST));
      register(3904, create("minecraft:cyan_glazed_terracotta", FACING_SOUTH), create("minecraft:cyan_glazed_terracotta", FACING_SOUTH));
      register(3905, create("minecraft:cyan_glazed_terracotta", FACING_WEST), create("minecraft:cyan_glazed_terracotta", FACING_WEST));
      register(3906, create("minecraft:cyan_glazed_terracotta", FACING_NORTH), create("minecraft:cyan_glazed_terracotta", FACING_NORTH));
      register(3907, create("minecraft:cyan_glazed_terracotta", FACING_EAST), create("minecraft:cyan_glazed_terracotta", FACING_EAST));
      register(3920, create("minecraft:purple_glazed_terracotta", FACING_SOUTH), create("minecraft:purple_glazed_terracotta", FACING_SOUTH));
      register(3921, create("minecraft:purple_glazed_terracotta", FACING_WEST), create("minecraft:purple_glazed_terracotta", FACING_WEST));
      register(3922, create("minecraft:purple_glazed_terracotta", FACING_NORTH), create("minecraft:purple_glazed_terracotta", FACING_NORTH));
      register(3923, create("minecraft:purple_glazed_terracotta", FACING_EAST), create("minecraft:purple_glazed_terracotta", FACING_EAST));
      register(3936, create("minecraft:blue_glazed_terracotta", FACING_SOUTH), create("minecraft:blue_glazed_terracotta", FACING_SOUTH));
      register(3937, create("minecraft:blue_glazed_terracotta", FACING_WEST), create("minecraft:blue_glazed_terracotta", FACING_WEST));
      register(3938, create("minecraft:blue_glazed_terracotta", FACING_NORTH), create("minecraft:blue_glazed_terracotta", FACING_NORTH));
      register(3939, create("minecraft:blue_glazed_terracotta", FACING_EAST), create("minecraft:blue_glazed_terracotta", FACING_EAST));
      register(3952, create("minecraft:brown_glazed_terracotta", FACING_SOUTH), create("minecraft:brown_glazed_terracotta", FACING_SOUTH));
      register(3953, create("minecraft:brown_glazed_terracotta", FACING_WEST), create("minecraft:brown_glazed_terracotta", FACING_WEST));
      register(3954, create("minecraft:brown_glazed_terracotta", FACING_NORTH), create("minecraft:brown_glazed_terracotta", FACING_NORTH));
      register(3955, create("minecraft:brown_glazed_terracotta", FACING_EAST), create("minecraft:brown_glazed_terracotta", FACING_EAST));
      register(3968, create("minecraft:green_glazed_terracotta", FACING_SOUTH), create("minecraft:green_glazed_terracotta", FACING_SOUTH));
      register(3969, create("minecraft:green_glazed_terracotta", FACING_WEST), create("minecraft:green_glazed_terracotta", FACING_WEST));
      register(3970, create("minecraft:green_glazed_terracotta", FACING_NORTH), create("minecraft:green_glazed_terracotta", FACING_NORTH));
      register(3971, create("minecraft:green_glazed_terracotta", FACING_EAST), create("minecraft:green_glazed_terracotta", FACING_EAST));
      register(3984, create("minecraft:red_glazed_terracotta", FACING_SOUTH), create("minecraft:red_glazed_terracotta", FACING_SOUTH));
      register(3985, create("minecraft:red_glazed_terracotta", FACING_WEST), create("minecraft:red_glazed_terracotta", FACING_WEST));
      register(3986, create("minecraft:red_glazed_terracotta", FACING_NORTH), create("minecraft:red_glazed_terracotta", FACING_NORTH));
      register(3987, create("minecraft:red_glazed_terracotta", FACING_EAST), create("minecraft:red_glazed_terracotta", FACING_EAST));
      register(4000, create("minecraft:black_glazed_terracotta", FACING_SOUTH), create("minecraft:black_glazed_terracotta", FACING_SOUTH));
      register(4001, create("minecraft:black_glazed_terracotta", FACING_WEST), create("minecraft:black_glazed_terracotta", FACING_WEST));
      register(4002, create("minecraft:black_glazed_terracotta", FACING_NORTH), create("minecraft:black_glazed_terracotta", FACING_NORTH));
      register(4003, create("minecraft:black_glazed_terracotta", FACING_EAST), create("minecraft:black_glazed_terracotta", FACING_EAST));
      register(4016, create("minecraft:white_concrete"), create("minecraft:concrete", COLOR_WHITE));
      register(4017, create("minecraft:orange_concrete"), create("minecraft:concrete", COLOR_ORANGE));
      register(4018, create("minecraft:magenta_concrete"), create("minecraft:concrete", COLOR_MAGENTA));
      register(4019, create("minecraft:light_blue_concrete"), create("minecraft:concrete", COLOR_LIGHT_BLUE));
      register(4020, create("minecraft:yellow_concrete"), create("minecraft:concrete", COLOR_YELLOW));
      register(4021, create("minecraft:lime_concrete"), create("minecraft:concrete", COLOR_LIME));
      register(4022, create("minecraft:pink_concrete"), create("minecraft:concrete", COLOR_PINK));
      register(4023, create("minecraft:gray_concrete"), create("minecraft:concrete", COLOR_GRAY));
      register(4024, create("minecraft:light_gray_concrete"), create("minecraft:concrete", COLOR_SILVER));
      register(4025, create("minecraft:cyan_concrete"), create("minecraft:concrete", COLOR_CYAN));
      register(4026, create("minecraft:purple_concrete"), create("minecraft:concrete", COLOR_PURPLE));
      register(4027, create("minecraft:blue_concrete"), create("minecraft:concrete", COLOR_BLUE));
      register(4028, create("minecraft:brown_concrete"), create("minecraft:concrete", COLOR_BROWN));
      register(4029, create("minecraft:green_concrete"), create("minecraft:concrete", COLOR_GREEN));
      register(4030, create("minecraft:red_concrete"), create("minecraft:concrete", COLOR_RED));
      register(4031, create("minecraft:black_concrete"), create("minecraft:concrete", COLOR_BLACK));
      register(4032, create("minecraft:white_concrete_powder"), create("minecraft:concrete_powder", COLOR_WHITE));
      register(4033, create("minecraft:orange_concrete_powder"), create("minecraft:concrete_powder", COLOR_ORANGE));
      register(4034, create("minecraft:magenta_concrete_powder"), create("minecraft:concrete_powder", COLOR_MAGENTA));
      register(4035, create("minecraft:light_blue_concrete_powder"), create("minecraft:concrete_powder", COLOR_LIGHT_BLUE));
      register(4036, create("minecraft:yellow_concrete_powder"), create("minecraft:concrete_powder", COLOR_YELLOW));
      register(4037, create("minecraft:lime_concrete_powder"), create("minecraft:concrete_powder", COLOR_LIME));
      register(4038, create("minecraft:pink_concrete_powder"), create("minecraft:concrete_powder", COLOR_PINK));
      register(4039, create("minecraft:gray_concrete_powder"), create("minecraft:concrete_powder", COLOR_GRAY));
      register(4040, create("minecraft:light_gray_concrete_powder"), create("minecraft:concrete_powder", COLOR_SILVER));
      register(4041, create("minecraft:cyan_concrete_powder"), create("minecraft:concrete_powder", COLOR_CYAN));
      register(4042, create("minecraft:purple_concrete_powder"), create("minecraft:concrete_powder", COLOR_PURPLE));
      register(4043, create("minecraft:blue_concrete_powder"), create("minecraft:concrete_powder", COLOR_BLUE));
      register(4044, create("minecraft:brown_concrete_powder"), create("minecraft:concrete_powder", COLOR_BROWN));
      register(4045, create("minecraft:green_concrete_powder"), create("minecraft:concrete_powder", COLOR_GREEN));
      register(4046, create("minecraft:red_concrete_powder"), create("minecraft:concrete_powder", COLOR_RED));
      register(4047, create("minecraft:black_concrete_powder"), create("minecraft:concrete_powder", COLOR_BLACK));
      register(4080, create("minecraft:structure_block", Map.of("mode", "save")), create("minecraft:structure_block", Map.of("mode", "save")));
      register(4081, create("minecraft:structure_block", Map.of("mode", "load")), create("minecraft:structure_block", Map.of("mode", "load")));
      register(4082, create("minecraft:structure_block", Map.of("mode", "corner")), create("minecraft:structure_block", Map.of("mode", "corner")));
      register(4083, create("minecraft:structure_block", Map.of("mode", "data")), create("minecraft:structure_block", Map.of("mode", "data")));
   }

   static {
      ID_BY_OLD.defaultReturnValue(-1);
      bootstrap0();
      bootstrap1();
      bootstrap2();
      bootstrap3_1();
      bootstrap3_2();
      bootstrap4();
      bootstrap5();
      bootstrap6();
      bootstrap7();
      bootstrap8();
      bootstrap9();
      bootstrapA();
      bootstrapB();
      bootstrapC();
      bootstrapD();
      bootstrapE();
      bootstrapF();
      finalizeMaps();
   }
}
