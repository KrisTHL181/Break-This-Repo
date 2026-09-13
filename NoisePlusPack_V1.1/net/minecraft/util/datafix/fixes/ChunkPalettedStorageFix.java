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
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int SIZE = 4096;

   public ChunkPalettedStorageFix(Schema p_15058_, boolean p_15059_) {
      super(p_15058_, p_15059_);
   }

   public static String getName(Dynamic<?> p_15065_) {
      return p_15065_.get("Name").asString("");
   }

   public static String getProperty(Dynamic<?> p_15067_, String p_15068_) {
      return p_15067_.get("Properties").get(p_15068_).asString("");
   }

   public static int idFor(CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> p_15062_, Dynamic<?> p_15063_) {
      int i = p_15062_.getId(p_15063_);
      if (i == -1) {
         i = p_15062_.add(p_15063_);
      }

      return i;
   }

   private Dynamic<?> fix(Dynamic<?> p_15093_) {
      Optional<? extends Dynamic<?>> optional = p_15093_.get("Level").result();
      return optional.isPresent() && optional.get().get("Sections").asStreamOpt().result().isPresent()
         ? p_15093_.set("Level", new ChunkPalettedStorageFix.UpgradeChunk((Dynamic<?>)optional.get()).write())
         : p_15093_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.CHUNK);
      Type<?> type1 = this.getOutputSchema().getType(References.CHUNK);
      return this.writeFixAndRead("ChunkPalettedStorageFix", type, type1, this::fix);
   }

   public static int getSideMask(boolean p_15087_, boolean p_15088_, boolean p_15089_, boolean p_15090_) {
      int i = 0;
      if (p_15089_) {
         if (p_15088_) {
            i |= 2;
         } else if (p_15087_) {
            i |= 128;
         } else {
            i |= 1;
         }
      } else if (p_15090_) {
         if (p_15087_) {
            i |= 32;
         } else if (p_15088_) {
            i |= 8;
         } else {
            i |= 16;
         }
      } else if (p_15088_) {
         i |= 4;
      } else if (p_15087_) {
         i |= 64;
      }

      return i;
   }

   static class DataLayer {
      private static final int SIZE = 2048;
      private static final int NIBBLE_SIZE = 4;
      private final byte[] data;

      public DataLayer() {
         this.data = new byte[2048];
      }

      public DataLayer(byte[] p_15132_) {
         this.data = p_15132_;
         if (p_15132_.length != 2048) {
            throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + p_15132_.length);
         }
      }

      public int get(int p_15136_, int p_15137_, int p_15138_) {
         int i = this.getPosition(p_15137_ << 8 | p_15138_ << 4 | p_15136_);
         return this.isFirst(p_15137_ << 8 | p_15138_ << 4 | p_15136_) ? this.data[i] & 15 : this.data[i] >> 4 & 15;
      }

      private boolean isFirst(int p_15134_) {
         return (p_15134_ & 1) == 0;
      }

      private int getPosition(int p_15140_) {
         return p_15140_ >> 1;
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

      Direction(final ChunkPalettedStorageFix.Direction.AxisDirection p_15154_, final ChunkPalettedStorageFix.Direction.Axis p_15155_) {
         this.axis = p_15155_;
         this.axisDirection = p_15154_;
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

         AxisDirection(final int p_15180_) {
            this.step = p_15180_;
         }

         public int getStep() {
            return this.step;
         }
      }
   }

   static class MappingConstants {
      static final BitSet VIRTUAL = new BitSet(256);
      static final BitSet FIX = new BitSet(256);
      static final Dynamic<?> PUMPKIN = ExtraDataFixUtils.blockState("minecraft:pumpkin");
      static final Dynamic<?> SNOWY_PODZOL = ExtraDataFixUtils.blockState("minecraft:podzol", Map.of("snowy", "true"));
      static final Dynamic<?> SNOWY_GRASS = ExtraDataFixUtils.blockState("minecraft:grass_block", Map.of("snowy", "true"));
      static final Dynamic<?> SNOWY_MYCELIUM = ExtraDataFixUtils.blockState("minecraft:mycelium", Map.of("snowy", "true"));
      static final Dynamic<?> UPPER_SUNFLOWER = ExtraDataFixUtils.blockState("minecraft:sunflower", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_LILAC = ExtraDataFixUtils.blockState("minecraft:lilac", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_TALL_GRASS = ExtraDataFixUtils.blockState("minecraft:tall_grass", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_LARGE_FERN = ExtraDataFixUtils.blockState("minecraft:large_fern", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_ROSE_BUSH = ExtraDataFixUtils.blockState("minecraft:rose_bush", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_PEONY = ExtraDataFixUtils.blockState("minecraft:peony", Map.of("half", "upper"));
      static final Map<String, Dynamic<?>> FLOWER_POT_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_369284_ -> {
         p_369284_.put("minecraft:air0", ExtraDataFixUtils.blockState("minecraft:flower_pot"));
         p_369284_.put("minecraft:red_flower0", ExtraDataFixUtils.blockState("minecraft:potted_poppy"));
         p_369284_.put("minecraft:red_flower1", ExtraDataFixUtils.blockState("minecraft:potted_blue_orchid"));
         p_369284_.put("minecraft:red_flower2", ExtraDataFixUtils.blockState("minecraft:potted_allium"));
         p_369284_.put("minecraft:red_flower3", ExtraDataFixUtils.blockState("minecraft:potted_azure_bluet"));
         p_369284_.put("minecraft:red_flower4", ExtraDataFixUtils.blockState("minecraft:potted_red_tulip"));
         p_369284_.put("minecraft:red_flower5", ExtraDataFixUtils.blockState("minecraft:potted_orange_tulip"));
         p_369284_.put("minecraft:red_flower6", ExtraDataFixUtils.blockState("minecraft:potted_white_tulip"));
         p_369284_.put("minecraft:red_flower7", ExtraDataFixUtils.blockState("minecraft:potted_pink_tulip"));
         p_369284_.put("minecraft:red_flower8", ExtraDataFixUtils.blockState("minecraft:potted_oxeye_daisy"));
         p_369284_.put("minecraft:yellow_flower0", ExtraDataFixUtils.blockState("minecraft:potted_dandelion"));
         p_369284_.put("minecraft:sapling0", ExtraDataFixUtils.blockState("minecraft:potted_oak_sapling"));
         p_369284_.put("minecraft:sapling1", ExtraDataFixUtils.blockState("minecraft:potted_spruce_sapling"));
         p_369284_.put("minecraft:sapling2", ExtraDataFixUtils.blockState("minecraft:potted_birch_sapling"));
         p_369284_.put("minecraft:sapling3", ExtraDataFixUtils.blockState("minecraft:potted_jungle_sapling"));
         p_369284_.put("minecraft:sapling4", ExtraDataFixUtils.blockState("minecraft:potted_acacia_sapling"));
         p_369284_.put("minecraft:sapling5", ExtraDataFixUtils.blockState("minecraft:potted_dark_oak_sapling"));
         p_369284_.put("minecraft:red_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_red_mushroom"));
         p_369284_.put("minecraft:brown_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_brown_mushroom"));
         p_369284_.put("minecraft:deadbush0", ExtraDataFixUtils.blockState("minecraft:potted_dead_bush"));
         p_369284_.put("minecraft:tallgrass2", ExtraDataFixUtils.blockState("minecraft:potted_fern"));
         p_369284_.put("minecraft:cactus0", ExtraDataFixUtils.blockState("minecraft:potted_cactus"));
      });
      static final Map<String, Dynamic<?>> SKULL_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_366440_ -> {
         mapSkull(p_366440_, 0, "skeleton", "skull");
         mapSkull(p_366440_, 1, "wither_skeleton", "skull");
         mapSkull(p_366440_, 2, "zombie", "head");
         mapSkull(p_366440_, 3, "player", "head");
         mapSkull(p_366440_, 4, "creeper", "head");
         mapSkull(p_366440_, 5, "dragon", "head");
      });
      static final Map<String, Dynamic<?>> DOOR_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_366018_ -> {
         mapDoor(p_366018_, "oak_door");
         mapDoor(p_366018_, "iron_door");
         mapDoor(p_366018_, "spruce_door");
         mapDoor(p_366018_, "birch_door");
         mapDoor(p_366018_, "jungle_door");
         mapDoor(p_366018_, "acacia_door");
         mapDoor(p_366018_, "dark_oak_door");
      });
      static final Map<String, Dynamic<?>> NOTE_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_361432_ -> {
         for (int i = 0; i < 26; i++) {
            p_361432_.put("true" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "true", "note", String.valueOf(i))));
            p_361432_.put("false" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "false", "note", String.valueOf(i))));
         }
      });
      private static final Int2ObjectMap<String> DYE_COLOR_MAP = (Int2ObjectMap<String>)DataFixUtils.make(new Int2ObjectOpenHashMap(), p_366854_ -> {
         p_366854_.put(0, "white");
         p_366854_.put(1, "orange");
         p_366854_.put(2, "magenta");
         p_366854_.put(3, "light_blue");
         p_366854_.put(4, "yellow");
         p_366854_.put(5, "lime");
         p_366854_.put(6, "pink");
         p_366854_.put(7, "gray");
         p_366854_.put(8, "light_gray");
         p_366854_.put(9, "cyan");
         p_366854_.put(10, "purple");
         p_366854_.put(11, "blue");
         p_366854_.put(12, "brown");
         p_366854_.put(13, "green");
         p_366854_.put(14, "red");
         p_366854_.put(15, "black");
      });
      static final Map<String, Dynamic<?>> BED_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_362358_ -> {
         ObjectIterator var1 = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

         while (var1.hasNext()) {
            Entry<String> entry = (Entry<String>)var1.next();
            if (!Objects.equals(entry.getValue(), "red")) {
               addBeds(p_362358_, entry.getIntKey(), (String)entry.getValue());
            }
         }
      });
      static final Map<String, Dynamic<?>> BANNER_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_362796_ -> {
         ObjectIterator var1 = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

         while (var1.hasNext()) {
            Entry<String> entry = (Entry<String>)var1.next();
            if (!Objects.equals(entry.getValue(), "white")) {
               addBanners(p_362796_, 15 - entry.getIntKey(), (String)entry.getValue());
            }
         }
      });
      static final Dynamic<?> AIR = ExtraDataFixUtils.blockState("minecraft:air");

      private MappingConstants() {
      }

      private static void mapSkull(Map<String, Dynamic<?>> p_364399_, int p_365434_, String p_368075_, String p_367880_) {
         p_364399_.put(p_365434_ + "north", ExtraDataFixUtils.blockState("minecraft:" + p_368075_ + "_wall_" + p_367880_, Map.of("facing", "north")));
         p_364399_.put(p_365434_ + "east", ExtraDataFixUtils.blockState("minecraft:" + p_368075_ + "_wall_" + p_367880_, Map.of("facing", "east")));
         p_364399_.put(p_365434_ + "south", ExtraDataFixUtils.blockState("minecraft:" + p_368075_ + "_wall_" + p_367880_, Map.of("facing", "south")));
         p_364399_.put(p_365434_ + "west", ExtraDataFixUtils.blockState("minecraft:" + p_368075_ + "_wall_" + p_367880_, Map.of("facing", "west")));

         for (int i = 0; i < 16; i++) {
            p_364399_.put("" + p_365434_ + i, ExtraDataFixUtils.blockState("minecraft:" + p_368075_ + "_" + p_367880_, Map.of("rotation", String.valueOf(i))));
         }
      }

      private static void mapDoor(Map<String, Dynamic<?>> p_368492_, String p_362981_) {
         String s = "minecraft:" + p_362981_;
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastlowerrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "eastupperrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northlowerrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "northupperrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southlowerrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "southupperrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westlowerrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperleftfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperleftfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperlefttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperlefttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperrightfalsefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperrightfalsetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperrighttruefalse",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         p_368492_.put(
            "minecraft:" + p_362981_ + "westupperrighttruetrue",
            ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
      }

      private static void addBeds(Map<String, Dynamic<?>> p_369009_, int p_366326_, String p_363252_) {
         p_369009_.put(
            "southfalsefoot" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "south", "occupied", "false", "part", "foot"))
         );
         p_369009_.put(
            "westfalsefoot" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "west", "occupied", "false", "part", "foot"))
         );
         p_369009_.put(
            "northfalsefoot" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "north", "occupied", "false", "part", "foot"))
         );
         p_369009_.put(
            "eastfalsefoot" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "east", "occupied", "false", "part", "foot"))
         );
         p_369009_.put(
            "southfalsehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "south", "occupied", "false", "part", "head"))
         );
         p_369009_.put(
            "westfalsehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "west", "occupied", "false", "part", "head"))
         );
         p_369009_.put(
            "northfalsehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "north", "occupied", "false", "part", "head"))
         );
         p_369009_.put(
            "eastfalsehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "east", "occupied", "false", "part", "head"))
         );
         p_369009_.put(
            "southtruehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "south", "occupied", "true", "part", "head"))
         );
         p_369009_.put(
            "westtruehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "west", "occupied", "true", "part", "head"))
         );
         p_369009_.put(
            "northtruehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "north", "occupied", "true", "part", "head"))
         );
         p_369009_.put(
            "easttruehead" + p_366326_,
            ExtraDataFixUtils.blockState("minecraft:" + p_363252_ + "_bed", Map.of("facing", "east", "occupied", "true", "part", "head"))
         );
      }

      private static void addBanners(Map<String, Dynamic<?>> p_368021_, int p_361887_, String p_364451_) {
         for (int i = 0; i < 16; i++) {
            p_368021_.put(i + "_" + p_361887_, ExtraDataFixUtils.blockState("minecraft:" + p_364451_ + "_banner", Map.of("rotation", String.valueOf(i))));
         }

         p_368021_.put("north_" + p_361887_, ExtraDataFixUtils.blockState("minecraft:" + p_364451_ + "_wall_banner", Map.of("facing", "north")));
         p_368021_.put("south_" + p_361887_, ExtraDataFixUtils.blockState("minecraft:" + p_364451_ + "_wall_banner", Map.of("facing", "south")));
         p_368021_.put("west_" + p_361887_, ExtraDataFixUtils.blockState("minecraft:" + p_364451_ + "_wall_banner", Map.of("facing", "west")));
         p_368021_.put("east_" + p_361887_, ExtraDataFixUtils.blockState("minecraft:" + p_364451_ + "_wall_banner", Map.of("facing", "east")));
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

   static class Section {
      private final CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette = CrudeIncrementalIntIdentityHashBiMap.create(32);
      private final List<Dynamic<?>> listTag;
      private final Dynamic<?> section;
      private final boolean hasData;
      final Int2ObjectMap<IntList> toFix = new Int2ObjectLinkedOpenHashMap();
      final IntList update = new IntArrayList();
      public final int y;
      private final Set<Dynamic<?>> seen = Sets.newIdentityHashSet();
      private final int[] buffer = new int[4096];

      public Section(Dynamic<?> p_15195_) {
         this.listTag = Lists.newArrayList();
         this.section = p_15195_;
         this.y = p_15195_.get("Y").asInt(0);
         this.hasData = p_15195_.get("Blocks").result().isPresent();
      }

      public Dynamic<?> getBlock(int p_15198_) {
         if (p_15198_ >= 0 && p_15198_ <= 4095) {
            Dynamic<?> dynamic = this.palette.byId(this.buffer[p_15198_]);
            return dynamic == null ? ChunkPalettedStorageFix.MappingConstants.AIR : dynamic;
         } else {
            return ChunkPalettedStorageFix.MappingConstants.AIR;
         }
      }

      public void setBlock(int p_15203_, Dynamic<?> p_15204_) {
         if (this.seen.add(p_15204_)) {
            this.listTag.add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(p_15204_)) ? ChunkPalettedStorageFix.MappingConstants.AIR : p_15204_);
         }

         this.buffer[p_15203_] = ChunkPalettedStorageFix.idFor(this.palette, p_15204_);
      }

      public int upgrade(int p_15210_) {
         if (!this.hasData) {
            return p_15210_;
         }

         ByteBuffer bytebuffer = (ByteBuffer)this.section.get("Blocks").asByteBufferOpt().result().get();
         ChunkPalettedStorageFix.DataLayer chunkpalettedstoragefix$datalayer = this.section
            .get("Data")
            .asByteBufferOpt()
            .map(p_15214_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15214_)))
            .result()
            .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
         ChunkPalettedStorageFix.DataLayer chunkpalettedstoragefix$datalayer1 = this.section
            .get("Add")
            .asByteBufferOpt()
            .map(p_15208_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15208_)))
            .result()
            .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
         this.seen.add(ChunkPalettedStorageFix.MappingConstants.AIR);
         ChunkPalettedStorageFix.idFor(this.palette, ChunkPalettedStorageFix.MappingConstants.AIR);
         this.listTag.add(ChunkPalettedStorageFix.MappingConstants.AIR);

         for (int i = 0; i < 4096; i++) {
            int j = i & 15;
            int k = i >> 8 & 15;
            int l = i >> 4 & 15;
            int i1 = chunkpalettedstoragefix$datalayer1.get(j, k, l) << 12 | (bytebuffer.get(i) & 255) << 4 | chunkpalettedstoragefix$datalayer.get(j, k, l);
            if (ChunkPalettedStorageFix.MappingConstants.FIX.get(i1 >> 4)) {
               this.addFix(i1 >> 4, i);
            }

            if (ChunkPalettedStorageFix.MappingConstants.VIRTUAL.get(i1 >> 4)) {
               int j1 = ChunkPalettedStorageFix.getSideMask(j == 0, j == 15, l == 0, l == 15);
               if (j1 == 0) {
                  this.update.add(i);
               } else {
                  p_15210_ |= j1;
               }
            }

            this.setBlock(i, BlockStateData.getTag(i1));
         }

         return p_15210_;
      }

      private void addFix(int p_15200_, int p_15201_) {
         IntList intlist = (IntList)this.toFix.get(p_15200_);
         if (intlist == null) {
            intlist = new IntArrayList();
            this.toFix.put(p_15200_, intlist);
         }

         intlist.add(p_15201_);
      }

      public Dynamic<?> write() {
         Dynamic<?> dynamic = this.section;
         if (!this.hasData) {
            return dynamic;
         }

         dynamic = dynamic.set("Palette", dynamic.createList(this.listTag.stream()));
         int i = Math.max(4, DataFixUtils.ceillog2(this.seen.size()));
         PackedBitStorage packedbitstorage = new PackedBitStorage(i, 4096);

         for (int j = 0; j < this.buffer.length; j++) {
            packedbitstorage.set(j, this.buffer[j]);
         }

         dynamic = dynamic.set("BlockStates", dynamic.createLongList(Arrays.stream(packedbitstorage.getRaw())));
         dynamic = dynamic.remove("Blocks");
         dynamic = dynamic.remove("Data");
         return dynamic.remove("Add");
      }
   }

   static final class UpgradeChunk {
      private int sides;
      private final ChunkPalettedStorageFix.@Nullable Section[] sections = new ChunkPalettedStorageFix.Section[16];
      private final Dynamic<?> level;
      private final int x;
      private final int z;
      private final Int2ObjectMap<Dynamic<?>> blockEntities = new Int2ObjectLinkedOpenHashMap(16);

      public UpgradeChunk(Dynamic<?> p_15222_) {
         this.level = p_15222_;
         this.x = p_15222_.get("xPos").asInt(0) << 4;
         this.z = p_15222_.get("zPos").asInt(0) << 4;
         p_15222_.get("TileEntities")
            .asStreamOpt()
            .ifSuccess(
               p_15241_ -> p_15241_.forEach(
                  p_145228_ -> {
                     int l3 = p_145228_.get("x").asInt(0) - this.x & 15;
                     int i4 = p_145228_.get("y").asInt(0);
                     int j4 = p_145228_.get("z").asInt(0) - this.z & 15;
                     int k4 = i4 << 8 | j4 << 4 | l3;
                     if (this.blockEntities.put(k4, p_145228_) != null) {
                        ChunkPalettedStorageFix.LOGGER
                           .warn("In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]", new Object[]{this.x, this.z, l3, i4, j4});
                     }
                  }
               )
            );
         boolean flag = p_15222_.get("convertedFromAlphaFormat").asBoolean(false);
         p_15222_.get("Sections").asStreamOpt().ifSuccess(p_15235_ -> p_15235_.forEach(p_145226_ -> {
            ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section1 = new ChunkPalettedStorageFix.Section((Dynamic<?>)p_145226_);
            this.sides = chunkpalettedstoragefix$section1.upgrade(this.sides);
            this.sections[chunkpalettedstoragefix$section1.y] = chunkpalettedstoragefix$section1;
         }));

         for (ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section : this.sections) {
            if (chunkpalettedstoragefix$section != null) {
               ObjectIterator var7 = chunkpalettedstoragefix$section.toFix.int2ObjectEntrySet().iterator();

               while (var7.hasNext()) {
                  Entry<IntList> entry = (Entry<IntList>)var7.next();
                  int i = chunkpalettedstoragefix$section.y << 12;
                  switch (entry.getIntKey()) {
                     case 2:
                        IntListIterator var30 = ((IntList)entry.getValue()).iterator();

                        while (var30.hasNext()) {
                           int i3 = (Integer)var30.next();
                           i3 |= i;
                           Dynamic<?> dynamic11 = this.getBlock(i3);
                           if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName(dynamic11))) {
                              String s11 = ChunkPalettedStorageFix.getName(this.getBlock(relative(i3, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(s11) || "minecraft:snow_layer".equals(s11)) {
                                 this.setBlock(i3, ChunkPalettedStorageFix.MappingConstants.SNOWY_GRASS);
                              }
                           }
                        }
                        break;
                     case 3:
                        IntListIterator var29 = ((IntList)entry.getValue()).iterator();

                        while (var29.hasNext()) {
                           int l2 = (Integer)var29.next();
                           l2 |= i;
                           Dynamic<?> dynamic10 = this.getBlock(l2);
                           if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName(dynamic10))) {
                              String s10 = ChunkPalettedStorageFix.getName(this.getBlock(relative(l2, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(s10) || "minecraft:snow_layer".equals(s10)) {
                                 this.setBlock(l2, ChunkPalettedStorageFix.MappingConstants.SNOWY_PODZOL);
                              }
                           }
                        }
                        break;
                     case 25:
                        IntListIterator var28 = ((IntList)entry.getValue()).iterator();

                        while (var28.hasNext()) {
                           int k2 = (Integer)var28.next();
                           k2 |= i;
                           Dynamic<?> dynamic9 = this.removeBlockEntity(k2);
                           if (dynamic9 != null) {
                              String s9 = Boolean.toString(dynamic9.get("powered").asBoolean(false))
                                 + (byte)Math.min(Math.max(dynamic9.get("note").asInt(0), 0), 24);
                              this.setBlock(
                                 k2,
                                 ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP
                                    .getOrDefault(s9, ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP.get("false0"))
                              );
                           }
                        }
                        break;
                     case 26:
                        IntListIterator var27 = ((IntList)entry.getValue()).iterator();

                        while (var27.hasNext()) {
                           int j2 = (Integer)var27.next();
                           j2 |= i;
                           Dynamic<?> dynamic8 = this.getBlockEntity(j2);
                           Dynamic<?> dynamic14 = this.getBlock(j2);
                           if (dynamic8 != null) {
                              int k3 = dynamic8.get("color").asInt(0);
                              if (k3 != 14 && k3 >= 0 && k3 < 16) {
                                 String s15 = ChunkPalettedStorageFix.getProperty(dynamic14, "facing")
                                    + ChunkPalettedStorageFix.getProperty(dynamic14, "occupied")
                                    + ChunkPalettedStorageFix.getProperty(dynamic14, "part")
                                    + k3;
                                 if (ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.containsKey(s15)) {
                                    this.setBlock(j2, ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.get(s15));
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
                           int i2 = (Integer)var26.next();
                           i2 |= i;
                           Dynamic<?> dynamic7 = this.getBlock(i2);
                           if (ChunkPalettedStorageFix.getName(dynamic7).endsWith("_door")) {
                              Dynamic<?> dynamic13 = this.getBlock(i2);
                              if ("lower".equals(ChunkPalettedStorageFix.getProperty(dynamic13, "half"))) {
                                 int j3 = relative(i2, ChunkPalettedStorageFix.Direction.UP);
                                 Dynamic<?> dynamic15 = this.getBlock(j3);
                                 String s16 = ChunkPalettedStorageFix.getName(dynamic13);
                                 if (s16.equals(ChunkPalettedStorageFix.getName(dynamic15))) {
                                    String s1 = ChunkPalettedStorageFix.getProperty(dynamic13, "facing");
                                    String s2 = ChunkPalettedStorageFix.getProperty(dynamic13, "open");
                                    String s3 = flag ? "left" : ChunkPalettedStorageFix.getProperty(dynamic15, "hinge");
                                    String s4 = flag ? "false" : ChunkPalettedStorageFix.getProperty(dynamic15, "powered");
                                    this.setBlock(i2, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(s16 + s1 + "lower" + s3 + s2 + s4));
                                    this.setBlock(j3, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(s16 + s1 + "upper" + s3 + s2 + s4));
                                 }
                              }
                           }
                        }
                        break;
                     case 86:
                        IntListIterator var25 = ((IntList)entry.getValue()).iterator();

                        while (var25.hasNext()) {
                           int l1 = (Integer)var25.next();
                           l1 |= i;
                           Dynamic<?> dynamic6 = this.getBlock(l1);
                           if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName(dynamic6))) {
                              String s8 = ChunkPalettedStorageFix.getName(this.getBlock(relative(l1, ChunkPalettedStorageFix.Direction.DOWN)));
                              if ("minecraft:grass_block".equals(s8) || "minecraft:dirt".equals(s8)) {
                                 this.setBlock(l1, ChunkPalettedStorageFix.MappingConstants.PUMPKIN);
                              }
                           }
                        }
                        break;
                     case 110:
                        IntListIterator var24 = ((IntList)entry.getValue()).iterator();

                        while (var24.hasNext()) {
                           int k1 = (Integer)var24.next();
                           k1 |= i;
                           Dynamic<?> dynamic5 = this.getBlock(k1);
                           if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName(dynamic5))) {
                              String s7 = ChunkPalettedStorageFix.getName(this.getBlock(relative(k1, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(s7) || "minecraft:snow_layer".equals(s7)) {
                                 this.setBlock(k1, ChunkPalettedStorageFix.MappingConstants.SNOWY_MYCELIUM);
                              }
                           }
                        }
                        break;
                     case 140:
                        IntListIterator var23 = ((IntList)entry.getValue()).iterator();

                        while (var23.hasNext()) {
                           int j1 = (Integer)var23.next();
                           j1 |= i;
                           Dynamic<?> dynamic4 = this.removeBlockEntity(j1);
                           if (dynamic4 != null) {
                              String s6 = dynamic4.get("Item").asString("") + dynamic4.get("Data").asInt(0);
                              this.setBlock(
                                 j1,
                                 ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP
                                    .getOrDefault(s6, ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP.get("minecraft:air0"))
                              );
                           }
                        }
                        break;
                     case 144:
                        IntListIterator var22 = ((IntList)entry.getValue()).iterator();

                        while (var22.hasNext()) {
                           int i1 = (Integer)var22.next();
                           i1 |= i;
                           Dynamic<?> dynamic3 = this.getBlockEntity(i1);
                           if (dynamic3 != null) {
                              String s5 = String.valueOf(dynamic3.get("SkullType").asInt(0));
                              String s13 = ChunkPalettedStorageFix.getProperty(this.getBlock(i1), "facing");
                              String s14;
                              if (!"up".equals(s13) && !"down".equals(s13)) {
                                 s14 = s5 + s13;
                              } else {
                                 s14 = s5 + dynamic3.get("Rot").asInt(0);
                              }

                              dynamic3.remove("SkullType");
                              dynamic3.remove("facing");
                              dynamic3.remove("Rot");
                              this.setBlock(
                                 i1,
                                 ChunkPalettedStorageFix.MappingConstants.SKULL_MAP
                                    .getOrDefault(s14, ChunkPalettedStorageFix.MappingConstants.SKULL_MAP.get("0north"))
                              );
                           }
                        }
                        break;
                     case 175:
                        IntListIterator var21 = ((IntList)entry.getValue()).iterator();

                        while (var21.hasNext()) {
                           int l = (Integer)var21.next();
                           l |= i;
                           Dynamic<?> dynamic2 = this.getBlock(l);
                           if ("upper".equals(ChunkPalettedStorageFix.getProperty(dynamic2, "half"))) {
                              Dynamic<?> dynamic12 = this.getBlock(relative(l, ChunkPalettedStorageFix.Direction.DOWN));
                              String s12 = ChunkPalettedStorageFix.getName(dynamic12);
                              switch (s12) {
                                 case "minecraft:sunflower":
                                    this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_SUNFLOWER);
                                    break;
                                 case "minecraft:lilac":
                                    this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_LILAC);
                                    break;
                                 case "minecraft:tall_grass":
                                    this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_TALL_GRASS);
                                    break;
                                 case "minecraft:large_fern":
                                    this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_LARGE_FERN);
                                    break;
                                 case "minecraft:rose_bush":
                                    this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_ROSE_BUSH);
                                    break;
                                 case "minecraft:peony":
                                    this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_PEONY);
                              }
                           }
                        }
                        break;
                     case 176:
                     case 177:
                        IntListIterator var10 = ((IntList)entry.getValue()).iterator();

                        while (var10.hasNext()) {
                           int j = (Integer)var10.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlockEntity(j);
                           Dynamic<?> dynamic1 = this.getBlock(j);
                           if (dynamic != null) {
                              int k = dynamic.get("Base").asInt(0);
                              if (k != 15 && k >= 0 && k < 16) {
                                 String s = ChunkPalettedStorageFix.getProperty(dynamic1, entry.getIntKey() == 176 ? "rotation" : "facing") + "_" + k;
                                 if (ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.containsKey(s)) {
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.get(s));
                                 }
                              }
                           }
                        }
                  }
               }
            }
         }
      }

      private @Nullable Dynamic<?> getBlockEntity(int p_15237_) {
         return (Dynamic<?>)this.blockEntities.get(p_15237_);
      }

      private @Nullable Dynamic<?> removeBlockEntity(int p_15243_) {
         return (Dynamic<?>)this.blockEntities.remove(p_15243_);
      }

      public static int relative(int p_15227_, ChunkPalettedStorageFix.Direction p_15228_) {
         return switch (p_15228_.getAxis()) {
            case X -> {
               int k = (p_15227_ & 15) + p_15228_.getAxisDirection().getStep();
               yield k >= 0 && k <= 15 ? p_15227_ & -16 | k : -1;
            }
            case Y -> {
               int j = (p_15227_ >> 8) + p_15228_.getAxisDirection().getStep();
               yield j >= 0 && j <= 255 ? p_15227_ & 0xFF | j << 8 : -1;
            }
            case Z -> {
               int i = (p_15227_ >> 4 & 15) + p_15228_.getAxisDirection().getStep();
               yield i >= 0 && i <= 15 ? p_15227_ & -241 | i << 4 : -1;
            }
         };
      }

      private void setBlock(int p_15230_, Dynamic<?> p_15231_) {
         if (p_15230_ >= 0 && p_15230_ <= 65535) {
            ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section = this.getSection(p_15230_);
            if (chunkpalettedstoragefix$section != null) {
               chunkpalettedstoragefix$section.setBlock(p_15230_ & 4095, p_15231_);
            }
         }
      }

      private ChunkPalettedStorageFix.@Nullable Section getSection(int p_15245_) {
         int i = p_15245_ >> 12;
         return i < this.sections.length ? this.sections[i] : null;
      }

      public Dynamic<?> getBlock(int p_15225_) {
         if (p_15225_ >= 0 && p_15225_ <= 65535) {
            ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section = this.getSection(p_15225_);
            return chunkpalettedstoragefix$section == null
               ? ChunkPalettedStorageFix.MappingConstants.AIR
               : chunkpalettedstoragefix$section.getBlock(p_15225_ & 4095);
         } else {
            return ChunkPalettedStorageFix.MappingConstants.AIR;
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> dynamic = this.level;
         if (this.blockEntities.isEmpty()) {
            dynamic = dynamic.remove("TileEntities");
         } else {
            dynamic = dynamic.set("TileEntities", dynamic.createList(this.blockEntities.values().stream()));
         }

         Dynamic<?> dynamic1 = dynamic.emptyMap();
         List<Dynamic<?>> list = Lists.newArrayList();

         for (ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section : this.sections) {
            if (chunkpalettedstoragefix$section != null) {
               list.add(chunkpalettedstoragefix$section.write());
               dynamic1 = dynamic1.set(
                  String.valueOf(chunkpalettedstoragefix$section.y), dynamic1.createIntList(Arrays.stream(chunkpalettedstoragefix$section.update.toIntArray()))
               );
            }
         }

         Dynamic<?> dynamic2 = dynamic.emptyMap();
         dynamic2 = dynamic2.set("Sides", dynamic2.createByte((byte)this.sides));
         dynamic2 = dynamic2.set("Indices", dynamic1);
         return dynamic.set("UpgradeData", dynamic2).set("Sections", dynamic2.createList(list.stream()));
      }
   }
}
