package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class NbtUtils {
   private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.<ListTag>comparingInt(p_389895_ -> p_389895_.getIntOr(1, 0))
      .thenComparingInt(p_389897_ -> p_389897_.getIntOr(0, 0))
      .thenComparingInt(p_389901_ -> p_389901_.getIntOr(2, 0));
   private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.<ListTag>comparingDouble(p_389902_ -> p_389902_.getDoubleOr(1, 0.0))
      .thenComparingDouble(p_389889_ -> p_389889_.getDoubleOr(0, 0.0))
      .thenComparingDouble(p_389886_ -> p_389886_.getDoubleOr(2, 0.0));
   private static final Codec<ResourceKey<Block>> BLOCK_NAME_CODEC = ResourceKey.codec(Registries.BLOCK);
   public static final String SNBT_DATA_TAG = "data";
   private static final char PROPERTIES_START = '{';
   private static final char PROPERTIES_END = '}';
   private static final String ELEMENT_SEPARATOR = ",";
   private static final char KEY_VALUE_SEPARATOR = ':';
   private static final Splitter COMMA_SPLITTER = Splitter.on(",");
   private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int INDENT = 2;
   private static final int NOT_FOUND = -1;

   private NbtUtils() {
   }

   @VisibleForTesting
   public static boolean compareNbt(@Nullable Tag p_129236_, @Nullable Tag p_129237_, boolean p_129238_) {
      if (p_129236_ == p_129237_) {
         return true;
      }

      if (p_129236_ == null) {
         return true;
      }

      if (p_129237_ == null) {
         return false;
      }

      if (!p_129236_.getClass().equals(p_129237_.getClass())) {
         return false;
      }

      if (p_129236_ instanceof CompoundTag compoundtag) {
         CompoundTag compoundtag1 = (CompoundTag)p_129237_;
         if (compoundtag1.size() < compoundtag.size()) {
            return false;
         }

         for (Entry<String, Tag> entry : compoundtag.entrySet()) {
            Tag tag2 = entry.getValue();
            if (!compareNbt(tag2, compoundtag1.get(entry.getKey()), p_129238_)) {
               return false;
            }
         }

         return true;
      } else if (p_129236_ instanceof ListTag listtag && p_129238_) {
         ListTag listtag1 = (ListTag)p_129237_;
         if (listtag.isEmpty()) {
            return listtag1.isEmpty();
         }

         if (listtag1.size() < listtag.size()) {
            return false;
         }

         for (Tag tag : listtag) {
            boolean flag = false;

            for (Tag tag1 : listtag1) {
               if (compareNbt(tag, tag1, p_129238_)) {
                  flag = true;
                  break;
               }
            }

            if (!flag) {
               return false;
            }
         }

         return true;
      } else {
         return p_129236_.equals(p_129237_);
      }
   }

   public static BlockState readBlockState(HolderGetter<Block> p_256363_, CompoundTag p_250775_) {
      Optional<? extends Holder<Block>> optional = p_250775_.<ResourceKey<T>>read("Name", BLOCK_NAME_CODEC).flatMap(p_256363_::get);
      if (optional.isEmpty()) {
         return Blocks.AIR.defaultBlockState();
      }

      Block block = optional.get().value();
      BlockState blockstate = block.defaultBlockState();
      Optional<CompoundTag> optional1 = p_250775_.getCompound("Properties");
      if (optional1.isPresent()) {
         StateDefinition<Block, BlockState> statedefinition = block.getStateDefinition();

         for (String s : optional1.get().keySet()) {
            Property<?> property = statedefinition.getProperty(s);
            if (property != null) {
               blockstate = setValueHelper(blockstate, property, s, optional1.get(), p_250775_);
            }
         }
      }

      return blockstate;
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(
      S p_129205_, Property<T> p_129206_, String p_129207_, CompoundTag p_129208_, CompoundTag p_129209_
   ) {
      Optional<T> optional = p_129208_.getString(p_129207_).flatMap(p_129206_::getValue);
      if (optional.isPresent()) {
         return p_129205_.setValue(p_129206_, optional.get());
      }

      LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", new Object[]{p_129207_, p_129208_.get(p_129207_), p_129209_});
      return p_129205_;
   }

   public static CompoundTag writeBlockState(BlockState p_129203_) {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putString("Name", BuiltInRegistries.BLOCK.getKey(p_129203_.getBlock()).toString());
      Map<Property<?>, Comparable<?>> map = p_129203_.getValues();
      if (!map.isEmpty()) {
         CompoundTag compoundtag1 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            Property<?> property = entry.getKey();
            compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
         }

         compoundtag.put("Properties", compoundtag1);
      }

      return compoundtag;
   }

   public static CompoundTag writeFluidState(FluidState p_178023_) {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putString("Name", BuiltInRegistries.FLUID.getKey(p_178023_.getType()).toString());
      Map<Property<?>, Comparable<?>> map = p_178023_.getValues();
      if (!map.isEmpty()) {
         CompoundTag compoundtag1 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            Property<?> property = entry.getKey();
            compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
         }

         compoundtag.put("Properties", compoundtag1);
      }

      return compoundtag;
   }

   private static <T extends Comparable<T>> String getName(Property<T> p_129211_, Comparable<?> p_129212_) {
      return p_129211_.getName((T)p_129212_);
   }

   public static String prettyPrint(Tag p_178058_) {
      return prettyPrint(p_178058_, false);
   }

   public static String prettyPrint(Tag p_178051_, boolean p_178052_) {
      return prettyPrint(new StringBuilder(), p_178051_, 0, p_178052_).toString();
   }

   public static StringBuilder prettyPrint(StringBuilder p_178027_, Tag p_178028_, int p_178029_, boolean p_178030_) {
      return switch (p_178028_) {
         case PrimitiveTag primitivetag -> p_178027_.append(primitivetag);
         case EndTag endtag -> p_178027_;
         case ByteArrayTag bytearraytag -> {
            byte[] abyte = bytearraytag.getAsByteArray();
            int i1 = abyte.length;
            indent(p_178029_, p_178027_).append("byte[").append(i1).append("] {\n");
            if (p_178030_) {
               indent(p_178029_ + 1, p_178027_);

               for (int k1 = 0; k1 < abyte.length; k1++) {
                  if (k1 != 0) {
                     p_178027_.append(',');
                  }

                  if (k1 % 16 == 0 && k1 / 16 > 0) {
                     p_178027_.append('\n');
                     if (k1 < abyte.length) {
                        indent(p_178029_ + 1, p_178027_);
                     }
                  } else if (k1 != 0) {
                     p_178027_.append(' ');
                  }

                  p_178027_.append(String.format(Locale.ROOT, "0x%02X", abyte[k1] & 255));
               }
            } else {
               indent(p_178029_ + 1, p_178027_).append(" // Skipped, supply withBinaryBlobs true");
            }

            p_178027_.append('\n');
            indent(p_178029_, p_178027_).append('}');
            yield p_178027_;
         }
         case ListTag listtag -> {
            int l = listtag.size();
            indent(p_178029_, p_178027_).append("list").append("[").append(l).append("] [");
            if (l != 0) {
               p_178027_.append('\n');
            }

            for (int j1 = 0; j1 < l; j1++) {
               if (j1 != 0) {
                  p_178027_.append(",\n");
               }

               indent(p_178029_ + 1, p_178027_);
               prettyPrint(p_178027_, listtag.get(j1), p_178029_ + 1, p_178030_);
            }

            if (l != 0) {
               p_178027_.append('\n');
            }

            indent(p_178029_, p_178027_).append(']');
            yield p_178027_;
         }
         case IntArrayTag intarraytag -> {
            int[] aint = intarraytag.getAsIntArray();
            int l1 = 0;

            for (int i3 : aint) {
               l1 = Math.max(l1, String.format(Locale.ROOT, "%X", i3).length());
            }

            int j2 = aint.length;
            indent(p_178029_, p_178027_).append("int[").append(j2).append("] {\n");
            if (p_178030_) {
               indent(p_178029_ + 1, p_178027_);

               for (int k2 = 0; k2 < aint.length; k2++) {
                  if (k2 != 0) {
                     p_178027_.append(',');
                  }

                  if (k2 % 16 == 0 && k2 / 16 > 0) {
                     p_178027_.append('\n');
                     if (k2 < aint.length) {
                        indent(p_178029_ + 1, p_178027_);
                     }
                  } else if (k2 != 0) {
                     p_178027_.append(' ');
                  }

                  p_178027_.append(String.format(Locale.ROOT, "0x%0" + l1 + "X", aint[k2]));
               }
            } else {
               indent(p_178029_ + 1, p_178027_).append(" // Skipped, supply withBinaryBlobs true");
            }

            p_178027_.append('\n');
            indent(p_178029_, p_178027_).append('}');
            yield p_178027_;
         }
         case CompoundTag compoundtag -> {
            List<String> list = Lists.newArrayList(compoundtag.keySet());
            Collections.sort(list);
            indent(p_178029_, p_178027_).append('{');
            if (p_178027_.length() - p_178027_.lastIndexOf("\n") > 2 * (p_178029_ + 1)) {
               p_178027_.append('\n');
               indent(p_178029_ + 1, p_178027_);
            }

            int i2 = list.stream().mapToInt(String::length).max().orElse(0);
            String s = Strings.repeat(" ", i2);

            for (int j = 0; j < list.size(); j++) {
               if (j != 0) {
                  p_178027_.append(",\n");
               }

               String s1 = list.get(j);
               indent(p_178029_ + 1, p_178027_).append('"').append(s1).append('"').append(s, 0, s.length() - s1.length()).append(": ");
               prettyPrint(p_178027_, compoundtag.get(s1), p_178029_ + 1, p_178030_);
            }

            if (!list.isEmpty()) {
               p_178027_.append('\n');
            }

            indent(p_178029_, p_178027_).append('}');
            yield p_178027_;
         }
         case LongArrayTag longarraytag -> {
            long[] along = longarraytag.getAsLongArray();
            long i = 0L;

            for (long k : along) {
               i = Math.max(i, String.format(Locale.ROOT, "%X", k).length());
            }

            long l2 = along.length;
            indent(p_178029_, p_178027_).append("long[").append(l2).append("] {\n");
            if (p_178030_) {
               indent(p_178029_ + 1, p_178027_);

               for (int j3 = 0; j3 < along.length; j3++) {
                  if (j3 != 0) {
                     p_178027_.append(',');
                  }

                  if (j3 % 16 == 0 && j3 / 16 > 0) {
                     p_178027_.append('\n');
                     if (j3 < along.length) {
                        indent(p_178029_ + 1, p_178027_);
                     }
                  } else if (j3 != 0) {
                     p_178027_.append(' ');
                  }

                  p_178027_.append(String.format(Locale.ROOT, "0x%0" + i + "X", along[j3]));
               }
            } else {
               indent(p_178029_ + 1, p_178027_).append(" // Skipped, supply withBinaryBlobs true");
            }

            p_178027_.append('\n');
            indent(p_178029_, p_178027_).append('}');
            yield p_178027_;
         }
         default -> throw new MatchException(null, null);
      };
   }

   private static StringBuilder indent(int p_178020_, StringBuilder p_178021_) {
      int i = p_178021_.lastIndexOf("\n") + 1;
      int j = p_178021_.length() - i;

      for (int k = 0; k < 2 * p_178020_ - j; k++) {
         p_178021_.append(' ');
      }

      return p_178021_;
   }

   public static Component toPrettyComponent(Tag p_178062_) {
      return new TextComponentTagVisitor("").visit(p_178062_);
   }

   public static String structureToSnbt(CompoundTag p_178064_) {
      return new SnbtPrinterTagVisitor().visit(packStructureTemplate(p_178064_));
   }

   public static CompoundTag snbtToStructure(String p_178025_) throws CommandSyntaxException {
      return unpackStructureTemplate(TagParser.parseCompoundFully(p_178025_));
   }

   @VisibleForTesting
   static CompoundTag packStructureTemplate(CompoundTag p_178068_) {
      Optional<ListTag> optional = p_178068_.getList("palettes");
      ListTag listtag;
      if (optional.isPresent()) {
         listtag = optional.get().getListOrEmpty(0);
      } else {
         listtag = p_178068_.getListOrEmpty("palette");
      }

      ListTag listtag1 = listtag.compoundStream().map(NbtUtils::packBlockState).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
      p_178068_.put("palette", listtag1);
      if (optional.isPresent()) {
         ListTag listtag2 = new ListTag();
         optional.get().stream().flatMap(p_389905_ -> p_389905_.asList().stream()).forEach(p_389894_ -> {
            CompoundTag compoundtag = new CompoundTag();

            for (int i = 0; i < p_389894_.size(); i++) {
               compoundtag.putString(listtag1.getString(i).orElseThrow(), packBlockState(p_389894_.getCompound(i).orElseThrow()));
            }

            listtag2.add(compoundtag);
         });
         p_178068_.put("palettes", listtag2);
      }

      Optional<ListTag> optional1 = p_178068_.getList("entities");
      if (optional1.isPresent()) {
         ListTag listtag3 = optional1.get()
            .compoundStream()
            .sorted(Comparator.comparing(p_389903_ -> p_389903_.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_DOUBLE_COMPARATOR)))
            .collect(Collectors.toCollection(ListTag::new));
         p_178068_.put("entities", listtag3);
      }

      ListTag listtag4 = p_178068_.getList("blocks")
         .stream()
         .flatMap(ListTag::compoundStream)
         .sorted(Comparator.comparing(p_389898_ -> p_389898_.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_INT_COMPARATOR)))
         .peek(p_389885_ -> p_389885_.putString("state", listtag1.getString(p_389885_.getIntOr("state", 0)).orElseThrow()))
         .collect(Collectors.toCollection(ListTag::new));
      p_178068_.put("data", listtag4);
      p_178068_.remove("blocks");
      return p_178068_;
   }

   @VisibleForTesting
   static CompoundTag unpackStructureTemplate(CompoundTag p_178072_) {
      ListTag listtag = p_178072_.getListOrEmpty("palette");
      Map<String, Tag> map = listtag.stream()
         .flatMap(p_389904_ -> p_389904_.asString().stream())
         .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
      Optional<ListTag> optional = p_178072_.getList("palettes");
      if (optional.isPresent()) {
         p_178072_.put(
            "palettes",
            optional.get()
               .compoundStream()
               .map(
                  p_389891_ -> map.keySet()
                     .stream()
                     .map(p_389900_ -> p_389891_.getString(p_389900_).orElseThrow())
                     .map(NbtUtils::unpackBlockState)
                     .collect(Collectors.toCollection(ListTag::new))
               )
               .collect(Collectors.toCollection(ListTag::new))
         );
         p_178072_.remove("palette");
      } else {
         p_178072_.put("palette", map.values().stream().collect(Collectors.toCollection(ListTag::new)));
      }

      Optional<ListTag> optional1 = p_178072_.getList("data");
      if (optional1.isPresent()) {
         Object2IntMap<String> object2intmap = new Object2IntOpenHashMap();
         object2intmap.defaultReturnValue(-1);

         for (int i = 0; i < listtag.size(); i++) {
            object2intmap.put(listtag.getString(i).orElseThrow(), i);
         }

         ListTag listtag1 = optional1.get();

         for (int j = 0; j < listtag1.size(); j++) {
            CompoundTag compoundtag = listtag1.getCompound(j).orElseThrow();
            String s = compoundtag.getString("state").orElseThrow();
            int k = object2intmap.getInt(s);
            if (k == -1) {
               throw new IllegalStateException("Entry " + s + " missing from palette");
            }

            compoundtag.putInt("state", k);
         }

         p_178072_.put("blocks", listtag1);
         p_178072_.remove("data");
      }

      return p_178072_;
   }

   @VisibleForTesting
   static String packBlockState(CompoundTag p_178076_) {
      StringBuilder stringbuilder = new StringBuilder(p_178076_.getString("Name").orElseThrow());
      p_178076_.getCompound("Properties")
         .ifPresent(
            p_389888_ -> {
               String s = p_389888_.entrySet()
                  .stream()
                  .sorted(Entry.comparingByKey())
                  .map(p_389896_ -> p_389896_.getKey() + ":" + p_389896_.getValue().asString().orElseThrow())
                  .collect(Collectors.joining(","));
               stringbuilder.append('{').append(s).append('}');
            }
         );
      return stringbuilder.toString();
   }

   @VisibleForTesting
   static CompoundTag unpackBlockState(String p_178054_) {
      CompoundTag compoundtag = new CompoundTag();
      int i = p_178054_.indexOf(123);
      String s;
      if (i >= 0) {
         s = p_178054_.substring(0, i);
         CompoundTag compoundtag1 = new CompoundTag();
         if (i + 2 <= p_178054_.length()) {
            String s1 = p_178054_.substring(i + 1, p_178054_.indexOf(125, i));
            COMMA_SPLITTER.split(s1).forEach(p_178040_ -> {
               List<String> list = COLON_SPLITTER.splitToList(p_178040_);
               if (list.size() == 2) {
                  compoundtag1.putString(list.get(0), list.get(1));
               } else {
                  LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", p_178054_);
               }
            });
            compoundtag.put("Properties", compoundtag1);
         }
      } else {
         s = p_178054_;
      }

      compoundtag.putString("Name", s);
      return compoundtag;
   }

   public static CompoundTag addCurrentDataVersion(CompoundTag p_265050_) {
      int i = SharedConstants.getCurrentVersion().dataVersion().version();
      return addDataVersion(p_265050_, i);
   }

   public static CompoundTag addDataVersion(CompoundTag p_265534_, int p_265686_) {
      p_265534_.putInt("DataVersion", p_265686_);
      return p_265534_;
   }

   public static Dynamic<Tag> addCurrentDataVersion(Dynamic<Tag> p_422696_) {
      int i = SharedConstants.getCurrentVersion().dataVersion().version();
      return addDataVersion(p_422696_, i);
   }

   public static Dynamic<Tag> addDataVersion(Dynamic<Tag> p_425483_, int p_429566_) {
      return p_425483_.set("DataVersion", p_425483_.createInt(p_429566_));
   }

   public static void addCurrentDataVersion(ValueOutput p_409606_) {
      int i = SharedConstants.getCurrentVersion().dataVersion().version();
      addDataVersion(p_409606_, i);
   }

   public static void addDataVersion(ValueOutput p_406531_, int p_406276_) {
      p_406531_.putInt("DataVersion", p_406276_);
   }

   public static int getDataVersion(CompoundTag p_456219_) {
      return getDataVersion(p_456219_, -1);
   }

   public static int getDataVersion(CompoundTag p_265397_, int p_265399_) {
      return p_265397_.getIntOr("DataVersion", p_265399_);
   }

   public static int getDataVersion(Dynamic<?> p_391363_, int p_396363_) {
      return p_391363_.get("DataVersion").asInt(p_396363_);
   }
}
