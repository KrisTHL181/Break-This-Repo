package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.PackedBitStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChunkPalettedStorageFix extends DataFix {
   private static final int NORTH_WEST_MASK = 128;
   private static final int WEST_MASK = 64;
   private static final int SOUTH_WEST_MASK = 32;
   private static final int SOUTH_MASK = 16;
   private static final int SOUTH_EAST_MASK = 8;
   private static final int EAST_MASK = 4;
   private static final int NORTH_EAST_MASK = 2;
   private static final int NORTH_MASK = 1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SIZE = 4096;

   public ChunkPalettedStorageFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public static String getName(final Dynamic<?> state) {
      return state.get("Name").asString("");
   }

   public static String getProperty(final Dynamic<?> state, final String property) {
      return state.get("Properties").get(property).asString("");
   }

   public static int idFor(final CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> states, final Dynamic<?> state) {
      int id = states.getId(state);
      if (id == -1) {
         id = states.add(state);
      }

      return id;
   }

   private Dynamic<?> fix(final Dynamic<?> input) {
      Optional<? extends Dynamic<?>> level = input.get("Level").result();
      return level.isPresent() && level.get().get("Sections").asStreamOpt().result().isPresent()
         ? input.set("Level", new ChunkPalettedStorageFix.UpgradeChunk((Dynamic<?>)level.get()).write())
         : input;
   }

   public TypeRewriteRule makeRule() {
      Type<?> oldType = this.getInputSchema().getType(References.CHUNK);
      Type<?> newType = this.getOutputSchema().getType(References.CHUNK);
      return this.writeFixAndRead("ChunkPalettedStorageFix", oldType, newType, this::fix);
   }

   public static int getSideMask(final boolean west, final boolean east, final boolean north, final boolean south) {
      int s = 0;
      if (north) {
         if (east) {
            s |= 2;
         } else if (west) {
            s |= 128;
         } else {
            s |= 1;
         }
      } else if (south) {
         if (west) {
            s |= 32;
         } else if (east) {
            s |= 8;
         } else {
            s |= 16;
         }
      } else if (east) {
         s |= 4;
      } else if (west) {
         s |= 64;
      }

      return s;
   }

   private static class DataLayer {
      private static final int SIZE = 2048;
      private static final int NIBBLE_SIZE = 4;
      private final byte[] data;

      public DataLayer() {
         this.data = new byte[2048];
      }

      public DataLayer(final byte[] data) {
         this.data = data;
         if (data.length != 2048) {
            throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + data.length);
         }
      }

      public int get(final int x, final int y, final int z) {
         int position = this.getPosition(y << 8 | z << 4 | x);
         return this.isFirst(y << 8 | z << 4 | x) ? this.data[position] & 15 : this.data[position] >> 4 & 15;
      }

      private boolean isFirst(final int position) {
         return (position & 1) == 0;
      }

      private int getPosition(final int position) {
         return position >> 1;
      }
   }

   public enum Direction {
      DOWN(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
      UP(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
      NORTH(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
      SOUTH(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
      WEST(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.X),
      EAST(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.X);

      private final ChunkPalettedStorageFix.Direction.Axis axis;
      private final ChunkPalettedStorageFix.Direction.AxisDirection axisDirection;

      Direction(final ChunkPalettedStorageFix.Direction.AxisDirection axisDirection, final ChunkPalettedStorageFix.Direction.Axis axis) {
         this.axis = axis;
         this.axisDirection = axisDirection;
      }

      public ChunkPalettedStorageFix.Direction.AxisDirection getAxisDirection() {
         return this.axisDirection;
      }

      public ChunkPalettedStorageFix.Direction.Axis getAxis() {
         return this.axis;
      }

      public enum Axis {
         X,
         Y,
         Z;
      }

      public enum AxisDirection {
         POSITIVE(1),
         NEGATIVE(-1);

         private final int step;

         AxisDirection(final int step) {
            this.step = step;
         }

         public int getStep() {
            return this.step;
         }
      }
   }

   private static class MappingConstants {
      private static final BitSet VIRTUAL = new BitSet(256);
      private static final BitSet FIX = new BitSet(256);
      private static final Dynamic<?> PUMPKIN = ExtraDataFixUtils.blockState("minecraft:pumpkin");
      private static final Dynamic<?> SNOWY_PODZOL = ExtraDataFixUtils.blockState("minecraft:podzol", Map.of("snowy", "true"));
      private static final Dynamic<?> SNOWY_GRASS = ExtraDataFixUtils.blockState("minecraft:grass_block", Map.of("snowy", "true"));
      private static final Dynamic<?> SNOWY_MYCELIUM = ExtraDataFixUtils.blockState("minecraft:mycelium", Map.of("snowy", "true"));
      private static final Dynamic<?> UPPER_SUNFLOWER = ExtraDataFixUtils.blockState("minecraft:sunflower", Map.of("half", "upper"));
      private static final Dynamic<?> UPPER_LILAC = ExtraDataFixUtils.blockState("minecraft:lilac", Map.of("half", "upper"));
      private static final Dynamic<?> UPPER_TALL_GRASS = ExtraDataFixUtils.blockState("minecraft:tall_grass", Map.of("half", "upper"));
      private static final Dynamic<?> UPPER_LARGE_FERN = ExtraDataFixUtils.blockState("minecraft:large_fern", Map.of("half", "upper"));
      private static final Dynamic<?> UPPER_ROSE_BUSH = ExtraDataFixUtils.blockState("minecraft:rose_bush", Map.of("half", "upper"));
      private static final Dynamic<?> UPPER_PEONY = ExtraDataFixUtils.blockState("minecraft:peony", Map.of("half", "upper"));
      private static final Map<String, Dynamic<?>> FLOWER_POT_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), map -> {
         map.put("minecraft:air0", ExtraDataFixUtils.blockState("minecraft:flower_pot"));
         map.put("minecraft:red_flower0", ExtraDataFixUtils.blockState("minecraft:potted_poppy"));
         map.put("minecraft:red_flower1", ExtraDataFixUtils.blockState("minecraft:potted_blue_orchid"));
         map.put("minecraft:red_flower2", ExtraDataFixUtils.blockState("minecraft:potted_allium"));
         map.put("minecraft:red_flower3", ExtraDataFixUtils.blockState("minecraft:potted_azure_bluet"));
         map.put("minecraft:red_flower4", ExtraDataFixUtils.blockState("minecraft:potted_red_tulip"));
         map.put("minecraft:red_flower5", ExtraDataFixUtils.blockState("minecraft:potted_orange_tulip"));
         map.put("minecraft:red_flower6", ExtraDataFixUtils.blockState("minecraft:potted_white_tulip"));
         map.put("minecraft:red_flower7", ExtraDataFixUtils.blockState("minecraft:potted_pink_tulip"));
         map.put("minecraft:red_flower8", ExtraDataFixUtils.blockState("minecraft:potted_oxeye_daisy"));
         map.put("minecraft:yellow_flower0", ExtraDataFixUtils.blockState("minecraft:potted_dandelion"));
         map.put("minecraft:sapling0", ExtraDataFixUtils.blockState("minecraft:potted_oak_sapling"));
         map.put("minecraft:sapling1", ExtraDataFixUtils.blockState("minecraft:potted_spruce_sapling"));
         map.put("minecraft:sapling2", ExtraDataFixUtils.blockState("minecraft:potted_birch_sapling"));
         map.put("minecraft:sapling3", ExtraDataFixUtils.blockState("minecraft:potted_jungle_sapling"));
         map.put("minecraft:sapling4", ExtraDataFixUtils.blockState("minecraft:potted_acacia_sapling"));
         map.put("minecraft:sapling5", ExtraDataFixUtils.blockState("minecraft:potted_dark_oak_sapling"));
         map.put("minecraft:red_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_red_mushroom"));
         map.put("minecraft:brown_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_brown_mushroom"));
         map.put("minecraft:deadbush0", ExtraDataFixUtils.blockState("minecraft:potted_dead_bush"));
         map.put("minecraft:tallgrass2", ExtraDataFixUtils.blockState("minecraft:potted_fern"));
         map.put("minecraft:cactus0", ExtraDataFixUtils.blockState("minecraft:potted_cactus"));
      });
      private static final Map<String, Dynamic<?>> SKULL_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), map -> {
         mapSkull(map, 0, "skeleton", "skull");
         mapSkull(map, 1, "wither_skeleton", "skull");
         mapSkull(map, 2, "zombie", "head");
         mapSkull(map, 3, "player", "head");
         mapSkull(map, 4, "creeper", "head");
         mapSkull(map, 5, "dragon", "head");
      });
      private static final Map<String, Dynamic<?>> DOOR_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), map -> {
         mapDoor(map, "oak_door");
         mapDoor(map, "iron_door");
         mapDoor(map, "spruce_door");
         mapDoor(map, "birch_door");
         mapDoor(map, "jungle_door");
         mapDoor(map, "acacia_door");
         mapDoor(map, "dark_oak_door");
      });
      private static final Map<String, Dynamic<?>> NOTE_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), map -> {
         for (int i = 0; i < 26; i++) {
            map.put("true" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "true", "note", String.valueOf(i))));
            map.put("false" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "false", "note", String.valueOf(i))));
         }
      });
      private static final Int2ObjectMap<String> DYE_COLOR_MAP = (Int2ObjectMap<String>)DataFixUtils.make(new Int2ObjectOpenHashMap(), map -> {
         map.put(0, "white");
         map.put(1, "orange");
         map.put(2, "magenta");
         map.put(3, "light_blue");
         map.put(4, "yellow");
         map.put(5, "lime");
         map.put(6, "pink");
         map.put(7, "gray");
         map.put(8, "light_gray");
         map.put(9, "cyan");
         map.put(10, "purple");
         map.put(11, "blue");
         map.put(12, "brown");
         map.put(13, "green");
         map.put(14, "red");
         map.put(15, "black");
      });
      private static final Map<String, Dynamic<?>> BED_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), map -> {
         Iterator i$ = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

         while (i$.hasNext()) {
            Entry<String> entry = (Entry<String>)i$.next();
            if (!Objects.equals(entry.getValue(), "red")) {
               addBeds(map, entry.getIntKey(), (String)entry.getValue());
            }
         }
      });
      private static final Map<String, Dynamic<?>> BANNER_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), map -> {
         Iterator i$ = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

         while (i$.hasNext()) {
            Entry<String> entry = (Entry<String>)i$.next();
            if (!Objects.equals(entry.getValue(), "white")) {
               addBanners(map, 15 - entry.getIntKey(), (String)entry.getValue());
            }
         }
      });
      private static final Dynamic<?> AIR = ExtraDataFixUtils.blockState("minecraft:air");

      private static void mapSkull(final Map<String, Dynamic<?>> map, final int i, final String name, final String type) {
         map.put(i + "north", ExtraDataFixUtils.blockState("minecraft:" + name + "_wall_" + type, Map.of("facing", "north")));
         map.put(i + "east", ExtraDataFixUtils.blockState("minecraft:" + name + "_wall_" + type, Map.of("facing", "east")));
         map.put(i + "south", ExtraDataFixUtils.blockState("minecraft:" + name + "_wall_" + type, Map.of("facing", "south")));
         map.put(i + "west", ExtraDataFixUtils.blockState("minecraft:" + name + "_wall_" + type, Map.of("facing", "west")));

         for (int rot = 0; rot < 16; rot++) {
            map.put("" + i + rot, ExtraDataFixUtils.blockState("minecraft:" + name + "_" + type, Map.of("rotation", String.valueOf(rot))));
         }
      }

      private static void mapDoor(final Map<String, Dynamic<?>> map, final String type) {
         String id = "minecraft:" + type;
         map.put(
            "minecraft:" + type + "eastlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "eastlowerlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastlowerlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "eastlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "eastlowerrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastlowerrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "eastupperleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastupperleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "eastupperlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastupperlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "eastupperrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastupperrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "eastupperrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "eastupperrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northlowerlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northlowerlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northlowerrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northlowerrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northupperleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northupperleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northupperlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northupperlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northupperrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northupperrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "northupperrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "northupperrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southlowerlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southlowerlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southlowerrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southlowerrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southupperleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southupperleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southupperlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southupperlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southupperrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southupperrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "southupperrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "southupperrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westlowerlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westlowerlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westlowerrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westlowerrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westupperleftfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westupperleftfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westupperlefttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westupperlefttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westupperrightfalsefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westupperrightfalsetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + type + "westupperrighttruefalse",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + type + "westupperrighttruetrue",
            ExtraDataFixUtils.blockState(id, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
      }

      private static void addBeds(final Map<String, Dynamic<?>> map, final int colorId, final String color) {
         map.put(
            "southfalsefoot" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "south", "occupied", "false", "part", "foot"))
         );
         map.put(
            "westfalsefoot" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "west", "occupied", "false", "part", "foot"))
         );
         map.put(
            "northfalsefoot" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "north", "occupied", "false", "part", "foot"))
         );
         map.put(
            "eastfalsefoot" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "east", "occupied", "false", "part", "foot"))
         );
         map.put(
            "southfalsehead" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "south", "occupied", "false", "part", "head"))
         );
         map.put(
            "westfalsehead" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "west", "occupied", "false", "part", "head"))
         );
         map.put(
            "northfalsehead" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "north", "occupied", "false", "part", "head"))
         );
         map.put(
            "eastfalsehead" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "east", "occupied", "false", "part", "head"))
         );
         map.put(
            "southtruehead" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "south", "occupied", "true", "part", "head"))
         );
         map.put(
            "westtruehead" + colorId, ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "west", "occupied", "true", "part", "head"))
         );
         map.put(
            "northtruehead" + colorId,
            ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "north", "occupied", "true", "part", "head"))
         );
         map.put(
            "easttruehead" + colorId, ExtraDataFixUtils.blockState("minecraft:" + color + "_bed", Map.of("facing", "east", "occupied", "true", "part", "head"))
         );
      }

      private static void addBanners(final Map<String, Dynamic<?>> map, final int colorId, final String color) {
         for (int i = 0; i < 16; i++) {
            map.put(i + "_" + colorId, ExtraDataFixUtils.blockState("minecraft:" + color + "_banner", Map.of("rotation", String.valueOf(i))));
         }

         map.put("north_" + colorId, ExtraDataFixUtils.blockState("minecraft:" + color + "_wall_banner", Map.of("facing", "north")));
         map.put("south_" + colorId, ExtraDataFixUtils.blockState("minecraft:" + color + "_wall_banner", Map.of("facing", "south")));
         map.put("west_" + colorId, ExtraDataFixUtils.blockState("minecraft:" + color + "_wall_banner", Map.of("facing", "west")));
         map.put("east_" + colorId, ExtraDataFixUtils.blockState("minecraft:" + color + "_wall_banner", Map.of("facing", "east")));
      }

      static {
         FIX.set(2);
         FIX.set(3);
         FIX.set(110);
         FIX.set(140);
         FIX.set(144);
         FIX.set(25);
         FIX.set(86);
         FIX.set(26);
         FIX.set(176);
         FIX.set(177);
         FIX.set(175);
         FIX.set(64);
         FIX.set(71);
         FIX.set(193);
         FIX.set(194);
         FIX.set(195);
         FIX.set(196);
         FIX.set(197);
         VIRTUAL.set(54);
         VIRTUAL.set(146);
         VIRTUAL.set(25);
         VIRTUAL.set(26);
         VIRTUAL.set(51);
         VIRTUAL.set(53);
         VIRTUAL.set(67);
         VIRTUAL.set(108);
         VIRTUAL.set(109);
         VIRTUAL.set(114);
         VIRTUAL.set(128);
         VIRTUAL.set(134);
         VIRTUAL.set(135);
         VIRTUAL.set(136);
         VIRTUAL.set(156);
         VIRTUAL.set(163);
         VIRTUAL.set(164);
         VIRTUAL.set(180);
         VIRTUAL.set(203);
         VIRTUAL.set(55);
         VIRTUAL.set(85);
         VIRTUAL.set(113);
         VIRTUAL.set(188);
         VIRTUAL.set(189);
         VIRTUAL.set(190);
         VIRTUAL.set(191);
         VIRTUAL.set(192);
         VIRTUAL.set(93);
         VIRTUAL.set(94);
         VIRTUAL.set(101);
         VIRTUAL.set(102);
         VIRTUAL.set(160);
         VIRTUAL.set(106);
         VIRTUAL.set(107);
         VIRTUAL.set(183);
         VIRTUAL.set(184);
         VIRTUAL.set(185);
         VIRTUAL.set(186);
         VIRTUAL.set(187);
         VIRTUAL.set(132);
         VIRTUAL.set(139);
         VIRTUAL.set(199);
      }
   }

   private static class Section {
      private final CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette = CrudeIncrementalIntIdentityHashBiMap.create(32);
      private final List<Dynamic<?>> listTag;
      private final Dynamic<?> section;
      private final boolean hasData;
      private final Int2ObjectMap<IntList> toFix = new Int2ObjectLinkedOpenHashMap();
      private final IntList update = new IntArrayList();
      public final int y;
      private final Set<Dynamic<?>> seen = Sets.newIdentityHashSet();
      private final int[] buffer = new int[4096];

      public Section(final Dynamic<?> section) {
         this.listTag = Lists.newArrayList();
         this.section = section;
         this.y = section.get("Y").asInt(0);
         this.hasData = section.get("Blocks").result().isPresent();
      }

      public Dynamic<?> getBlock(final int pos) {
         if (pos >= 0 && pos <= 4095) {
            Dynamic<?> tag = this.palette.byId(this.buffer[pos]);
            return tag == null ? ChunkPalettedStorageFix.MappingConstants.AIR : tag;
         } else {
            return ChunkPalettedStorageFix.MappingConstants.AIR;
         }
      }

      public void setBlock(final int idx, final Dynamic<?> blockState) {
         if (this.seen.add(blockState)) {
            this.listTag.add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(blockState)) ? ChunkPalettedStorageFix.MappingConstants.AIR : blockState);
         }

         this.buffer[idx] = ChunkPalettedStorageFix.idFor(this.palette, blockState);
      }

      public int upgrade(int sides) {
         if (!this.hasData) {
            return sides;
         }

         ByteBuffer blocks = (ByteBuffer)this.section.get("Blocks").asByteBufferOpt().result().get();
         ChunkPalettedStorageFix.DataLayer data = this.section
            .get("Data")
            .asByteBufferOpt()
            .map(buffer -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(buffer)))
            .result()
            .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
         ChunkPalettedStorageFix.DataLayer addBlocks = this.section
            .get("Add")
            .asByteBufferOpt()
            .map(buffer -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(buffer)))
            .result()
            .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
         this.seen.add(ChunkPalettedStorageFix.MappingConstants.AIR);
         ChunkPalettedStorageFix.idFor(this.palette, ChunkPalettedStorageFix.MappingConstants.AIR);
         this.listTag.add(ChunkPalettedStorageFix.MappingConstants.AIR);

         for (int idx = 0; idx < 4096; idx++) {
            int xx = idx & 15;
            int yy = idx >> 8 & 15;
            int zz = idx >> 4 & 15;
            int id = addBlocks.get(xx, yy, zz) << 12 | (blocks.get(idx) & 255) << 4 | data.get(xx, yy, zz);
            if (ChunkPalettedStorageFix.MappingConstants.FIX.get(id >> 4)) {
               this.addFix(id >> 4, idx);
            }

            if (ChunkPalettedStorageFix.MappingConstants.VIRTUAL.get(id >> 4)) {
               int s = ChunkPalettedStorageFix.getSideMask(xx == 0, xx == 15, zz == 0, zz == 15);
               if (s == 0) {
                  this.update.add(idx);
               } else {
                  sides |= s;
               }
            }

            this.setBlock(idx, BlockStateData.getTag(id));
         }

         return sides;
      }

      private void addFix(final int id, final int position) {
         IntList list = (IntList)this.toFix.get(id);
         if (list == null) {
            list = new IntArrayList();
            this.toFix.put(id, list);
         }

         list.add(position);
      }

      public Dynamic<?> write() {
         Dynamic<?> section = this.section;
         if (!this.hasData) {
            return section;
         }

         section = section.set("Palette", section.createList(this.listTag.stream()));
         int size = Math.max(4, DataFixUtils.ceillog2(this.seen.size()));
         PackedBitStorage storage = new PackedBitStorage(size, 4096);

         for (int j = 0; j < this.buffer.length; j++) {
            storage.set(j, this.buffer[j]);
         }

         section = section.set("BlockStates", section.createLongList(Arrays.stream(storage.getRaw())));
         section = section.remove("Blocks");
         section = section.remove("Data");
         return section.remove("Add");
      }
   }

   private static final class UpgradeChunk {
      private int sides;
      private final ChunkPalettedStorageFix.@Nullable Section[] sections = new ChunkPalettedStorageFix.Section[16];
      private final Dynamic<?> level;
      private final int x;
      private final int z;
      private final Int2ObjectMap<Dynamic<?>> blockEntities = new Int2ObjectLinkedOpenHashMap(16);

      public UpgradeChunk(final Dynamic<?> level) {
         this.level = level;
         this.x = level.get("xPos").asInt(0) << 4;
         this.z = level.get("zPos").asInt(0) << 4;
         level.get("TileEntities")
            .asStreamOpt()
            .ifSuccess(
               s -> s.forEach(
                  entity -> {
                     int x = entity.get("x").asInt(0) - this.x & 15;
                     int y = entity.get("y").asInt(0);
                     int z = entity.get("z").asInt(0) - this.z & 15;
                     int key = y << 8 | z << 4 | x;
                     if (this.blockEntities.put(key, entity) != null) {
                        ChunkPalettedStorageFix.LOGGER
                           .warn("In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]", new Object[]{this.x, this.z, x, y, z});
                     }
                  }
               )
            );
         boolean convertedFromAlphaFormat = level.get("convertedFromAlphaFormat").asBoolean(false);
         level.get("Sections").asStreamOpt().ifSuccess(s -> s.forEach(sec -> {
            ChunkPalettedStorageFix.Section sectionx = new ChunkPalettedStorageFix.Section((Dynamic<?>)sec);
            this.sides = sectionx.upgrade(this.sides);
            this.sections[sectionx.y] = sectionx;
         }));

         for (ChunkPalettedStorageFix.Section section : this.sections) {
            if (section != null) {
               ObjectIterator var7 = section.toFix.int2ObjectEntrySet().iterator();

               while (var7.hasNext()) {
                  Entry<IntList> entry = (Entry<IntList>)var7.next();
                  int dy = section.y << 12;
                  switch (entry.getIntKey()) {
                     case 2:
                        IntListIterator var30 = ((IntList)entry.getValue()).iterator();

                        while (var30.hasNext()) {
                           int pos = (Integer)var30.next();
                           pos |= dy;
                           Dynamic<?> state = this.getBlock(pos);
                           if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName(state))) {
                              String name = ChunkPalettedStorageFix.getName(this.getBlock(relative(pos, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(name) || "minecraft:snow_layer".equals(name)) {
                                 this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.SNOWY_GRASS);
                              }
                           }
                        }
                        break;
                     case 3:
                        IntListIterator var29 = ((IntList)entry.getValue()).iterator();

                        while (var29.hasNext()) {
                           int pos = (Integer)var29.next();
                           pos |= dy;
                           Dynamic<?> state = this.getBlock(pos);
                           if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName(state))) {
                              String name = ChunkPalettedStorageFix.getName(this.getBlock(relative(pos, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(name) || "minecraft:snow_layer".equals(name)) {
                                 this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.SNOWY_PODZOL);
                              }
                           }
                        }
                        break;
                     case 25:
                        IntListIterator var28 = ((IntList)entry.getValue()).iterator();

                        while (var28.hasNext()) {
                           int pos = (Integer)var28.next();
                           pos |= dy;
                           Dynamic<?> entity = this.removeBlockEntity(pos);
                           if (entity != null) {
                              String key = Boolean.toString(entity.get("powered").asBoolean(false))
                                 + (byte)Math.min(Math.max(entity.get("note").asInt(0), 0), 24);
                              this.setBlock(
                                 pos,
                                 ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP
                                    .getOrDefault(key, ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP.get("false0"))
                              );
                           }
                        }
                        break;
                     case 26:
                        IntListIterator var27 = ((IntList)entry.getValue()).iterator();

                        while (var27.hasNext()) {
                           int pos = (Integer)var27.next();
                           pos |= dy;
                           Dynamic<?> entity = this.getBlockEntity(pos);
                           Dynamic<?> state = this.getBlock(pos);
                           if (entity != null) {
                              int color = entity.get("color").asInt(0);
                              if (color != 14 && color >= 0 && color < 16) {
                                 String key = ChunkPalettedStorageFix.getProperty(state, "facing")
                                    + ChunkPalettedStorageFix.getProperty(state, "occupied")
                                    + ChunkPalettedStorageFix.getProperty(state, "part")
                                    + color;
                                 if (ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.containsKey(key)) {
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.get(key));
                                 }
                              }
                           }
                        }
                        break;
                     case 64:
                     case 71:
                     case 193:
                     case 194:
                     case 195:
                     case 196:
                     case 197:
                        IntListIterator var26 = ((IntList)entry.getValue()).iterator();

                        while (var26.hasNext()) {
                           int pos = (Integer)var26.next();
                           pos |= dy;
                           Dynamic<?> state = this.getBlock(pos);
                           if (ChunkPalettedStorageFix.getName(state).endsWith("_door")) {
                              Dynamic<?> lower = this.getBlock(pos);
                              if ("lower".equals(ChunkPalettedStorageFix.getProperty(lower, "half"))) {
                                 int abovePos = relative(pos, ChunkPalettedStorageFix.Direction.UP);
                                 Dynamic<?> upper = this.getBlock(abovePos);
                                 String name = ChunkPalettedStorageFix.getName(lower);
                                 if (name.equals(ChunkPalettedStorageFix.getName(upper))) {
                                    String facing = ChunkPalettedStorageFix.getProperty(lower, "facing");
                                    String open = ChunkPalettedStorageFix.getProperty(lower, "open");
                                    String hinge = convertedFromAlphaFormat ? "left" : ChunkPalettedStorageFix.getProperty(upper, "hinge");
                                    String powered = convertedFromAlphaFormat ? "false" : ChunkPalettedStorageFix.getProperty(upper, "powered");
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(name + facing + "lower" + hinge + open + powered));
                                    this.setBlock(
                                       abovePos, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(name + facing + "upper" + hinge + open + powered)
                                    );
                                 }
                              }
                           }
                        }
                        break;
                     case 86:
                        IntListIterator var25 = ((IntList)entry.getValue()).iterator();

                        while (var25.hasNext()) {
                           int pos = (Integer)var25.next();
                           pos |= dy;
                           Dynamic<?> state = this.getBlock(pos);
                           if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName(state))) {
                              String name = ChunkPalettedStorageFix.getName(this.getBlock(relative(pos, ChunkPalettedStorageFix.Direction.DOWN)));
                              if ("minecraft:grass_block".equals(name) || "minecraft:dirt".equals(name)) {
                                 this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.PUMPKIN);
                              }
                           }
                        }
                        break;
                     case 110:
                        IntListIterator var24 = ((IntList)entry.getValue()).iterator();

                        while (var24.hasNext()) {
                           int pos = (Integer)var24.next();
                           pos |= dy;
                           Dynamic<?> state = this.getBlock(pos);
                           if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName(state))) {
                              String name = ChunkPalettedStorageFix.getName(this.getBlock(relative(pos, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(name) || "minecraft:snow_layer".equals(name)) {
                                 this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.SNOWY_MYCELIUM);
                              }
                           }
                        }
                        break;
                     case 140:
                        IntListIterator var23 = ((IntList)entry.getValue()).iterator();

                        while (var23.hasNext()) {
                           int pos = (Integer)var23.next();
                           pos |= dy;
                           Dynamic<?> entity = this.removeBlockEntity(pos);
                           if (entity != null) {
                              String key = entity.get("Item").asString("") + entity.get("Data").asInt(0);
                              this.setBlock(
                                 pos,
                                 ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP
                                    .getOrDefault(key, ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP.get("minecraft:air0"))
                              );
                           }
                        }
                        break;
                     case 144:
                        IntListIterator var22 = ((IntList)entry.getValue()).iterator();

                        while (var22.hasNext()) {
                           int pos = (Integer)var22.next();
                           pos |= dy;
                           Dynamic<?> entity = this.getBlockEntity(pos);
                           if (entity != null) {
                              String type = String.valueOf(entity.get("SkullType").asInt(0));
                              String facing = ChunkPalettedStorageFix.getProperty(this.getBlock(pos), "facing");
                              String key;
                              if (!"up".equals(facing) && !"down".equals(facing)) {
                                 key = type + facing;
                              } else {
                                 key = type + entity.get("Rot").asInt(0);
                              }

                              entity.remove("SkullType");
                              entity.remove("facing");
                              entity.remove("Rot");
                              this.setBlock(
                                 pos,
                                 ChunkPalettedStorageFix.MappingConstants.SKULL_MAP
                                    .getOrDefault(key, ChunkPalettedStorageFix.MappingConstants.SKULL_MAP.get("0north"))
                              );
                           }
                        }
                        break;
                     case 175:
                        IntListIterator var21 = ((IntList)entry.getValue()).iterator();

                        while (var21.hasNext()) {
                           int pos = (Integer)var21.next();
                           pos |= dy;
                           Dynamic<?> block = this.getBlock(pos);
                           if ("upper".equals(ChunkPalettedStorageFix.getProperty(block, "half"))) {
                              Dynamic<?> below = this.getBlock(relative(pos, ChunkPalettedStorageFix.Direction.DOWN));
                              String variant = ChunkPalettedStorageFix.getName(below);
                              switch (variant) {
                                 case "minecraft:sunflower":
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.UPPER_SUNFLOWER);
                                    break;
                                 case "minecraft:lilac":
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.UPPER_LILAC);
                                    break;
                                 case "minecraft:tall_grass":
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.UPPER_TALL_GRASS);
                                    break;
                                 case "minecraft:large_fern":
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.UPPER_LARGE_FERN);
                                    break;
                                 case "minecraft:rose_bush":
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.UPPER_ROSE_BUSH);
                                    break;
                                 case "minecraft:peony":
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.UPPER_PEONY);
                              }
                           }
                        }
                        break;
                     case 176:
                     case 177:
                        IntListIterator var10 = ((IntList)entry.getValue()).iterator();

                        while (var10.hasNext()) {
                           int pos = (Integer)var10.next();
                           pos |= dy;
                           Dynamic<?> entity = this.getBlockEntity(pos);
                           Dynamic<?> state = this.getBlock(pos);
                           if (entity != null) {
                              int color = entity.get("Base").asInt(0);
                              if (color != 15 && color >= 0 && color < 16) {
                                 String key = ChunkPalettedStorageFix.getProperty(state, entry.getIntKey() == 176 ? "rotation" : "facing") + "_" + color;
                                 if (ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.containsKey(key)) {
                                    this.setBlock(pos, ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.get(key));
                                 }
                              }
                           }
                        }
                  }
               }
            }
         }
      }

      private @Nullable Dynamic<?> getBlockEntity(final int pos) {
         return (Dynamic<?>)this.blockEntities.get(pos);
      }

      private @Nullable Dynamic<?> removeBlockEntity(final int pos) {
         return (Dynamic<?>)this.blockEntities.remove(pos);
      }

      public static int relative(final int pos, final ChunkPalettedStorageFix.Direction direction) {
         return switch (direction.getAxis()) {
            case X -> {
               int x = (pos & 15) + direction.getAxisDirection().getStep();
               yield x >= 0 && x <= 15 ? pos & -16 | x : -1;
            }
            case Y -> {
               int y = (pos >> 8) + direction.getAxisDirection().getStep();
               yield y >= 0 && y <= 255 ? pos & 0xFF | y << 8 : -1;
            }
            case Z -> {
               int z = (pos >> 4 & 15) + direction.getAxisDirection().getStep();
               yield z >= 0 && z <= 15 ? pos & -241 | z << 4 : -1;
            }
         };
      }

      private void setBlock(final int pos, final Dynamic<?> block) {
         if (pos >= 0 && pos <= 65535) {
            ChunkPalettedStorageFix.Section section = this.getSection(pos);
            if (section != null) {
               section.setBlock(pos & 4095, block);
            }
         }
      }

      private ChunkPalettedStorageFix.@Nullable Section getSection(final int pos) {
         int sectionY = pos >> 12;
         return sectionY < this.sections.length ? this.sections[sectionY] : null;
      }

      public Dynamic<?> getBlock(final int pos) {
         if (pos >= 0 && pos <= 65535) {
            ChunkPalettedStorageFix.Section section = this.getSection(pos);
            return section == null ? ChunkPalettedStorageFix.MappingConstants.AIR : section.getBlock(pos & 4095);
         } else {
            return ChunkPalettedStorageFix.MappingConstants.AIR;
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> level = this.level;
         if (this.blockEntities.isEmpty()) {
            level = level.remove("TileEntities");
         } else {
            level = level.set("TileEntities", level.createList(this.blockEntities.values().stream()));
         }

         Dynamic<?> indices = level.emptyMap();
         List<Dynamic<?>> sections = Lists.newArrayList();

         for (ChunkPalettedStorageFix.Section section : this.sections) {
            if (section != null) {
               sections.add(section.write());
               indices = indices.set(String.valueOf(section.y), indices.createIntList(Arrays.stream(section.update.toIntArray())));
            }
         }

         Dynamic<?> tag = level.emptyMap();
         tag = tag.set("Sides", tag.createByte((byte)this.sides));
         tag = tag.set("Indices", indices);
         return level.set("UpgradeData", tag).set("Sections", tag.createList(sections.stream()));
      }
   }
}
