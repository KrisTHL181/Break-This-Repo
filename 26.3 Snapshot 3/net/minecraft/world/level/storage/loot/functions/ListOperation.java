package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.slf4j.Logger;

public interface ListOperation {
   MapCodec<ListOperation> UNLIMITED_CODEC = codec(Integer.MAX_VALUE);

   static MapCodec<ListOperation> codec(final int maxSize) {
      return ListOperation.Type.CODEC.dispatchMap("mode", ListOperation::mode, e -> e.mapCodec).validate(op -> {
         if (op instanceof ListOperation.ReplaceSection section && section.size().isPresent()) {
            int size = section.size().get();
            if (size > maxSize) {
               return DataResult.error(() -> "Size value too large: " + size + ", max size is " + maxSize);
            }
         }

         return DataResult.success(op);
      });
   }

   ListOperation.Type mode();

   default <T> List<T> apply(final List<T> original, final List<T> replacement) {
      return this.apply(original, replacement, Integer.MAX_VALUE);
   }

   <T> List<T> apply(List<T> original, List<T> replacement, int maxSize);

   class Append implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final ListOperation.Append INSTANCE = new ListOperation.Append();
      public static final MapCodec<ListOperation.Append> MAP_CODEC = MapCodec.unit(() -> INSTANCE);

      private Append() {
      }

      @Override
      public ListOperation.Type mode() {
         return ListOperation.Type.APPEND;
      }

      @Override
      public <T> List<T> apply(final List<T> original, final List<T> replacement, final int maxSize) {
         if (original.size() + replacement.size() > maxSize) {
            LOGGER.error("Contents overflow in section append");
            return original;
         } else {
            return Stream.concat(original.stream(), replacement.stream()).toList();
         }
      }
   }

   record Insert(int offset) implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<ListOperation.Insert> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", 0).forGetter(ListOperation.Insert::offset)).apply(i, ListOperation.Insert::new)
      );

      @Override
      public ListOperation.Type mode() {
         return ListOperation.Type.INSERT;
      }

      @Override
      public <T> List<T> apply(final List<T> original, final List<T> replacement, final int maxSize) {
         int originalSize = original.size();
         if (this.offset > originalSize) {
            LOGGER.error("Cannot insert when offset is out of bounds");
            return original;
         } else if (originalSize + replacement.size() > maxSize) {
            LOGGER.error("Contents overflow in section insertion");
            return original;
         } else {
            Builder<T> newList = ImmutableList.builder();
            newList.addAll(original.subList(0, this.offset));
            newList.addAll(replacement);
            newList.addAll(original.subList(this.offset, originalSize));
            return newList.build();
         }
      }
   }

   class ReplaceAll implements ListOperation {
      public static final ListOperation.ReplaceAll INSTANCE = new ListOperation.ReplaceAll();
      public static final MapCodec<ListOperation.ReplaceAll> MAP_CODEC = MapCodec.unit(() -> INSTANCE);

      private ReplaceAll() {
      }

      @Override
      public ListOperation.Type mode() {
         return ListOperation.Type.REPLACE_ALL;
      }

      @Override
      public <T> List<T> apply(final List<T> original, final List<T> replacement, final int maxSize) {
         return replacement;
      }
   }

   record ReplaceSection(int offset, Optional<Integer> size) implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<ListOperation.ReplaceSection> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", 0).forGetter(ListOperation.ReplaceSection::offset),
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("size").forGetter(ListOperation.ReplaceSection::size)
            )
            .apply(i, ListOperation.ReplaceSection::new)
      );

      public ReplaceSection(final int offset) {
         this(offset, Optional.empty());
      }

      @Override
      public ListOperation.Type mode() {
         return ListOperation.Type.REPLACE_SECTION;
      }

      @Override
      public <T> List<T> apply(final List<T> original, final List<T> replacement, final int maxSize) {
         int originalSize = original.size();
         if (this.offset > originalSize) {
            LOGGER.error("Cannot replace when offset is out of bounds");
            return original;
         }

         Builder<T> newList = ImmutableList.builder();
         newList.addAll(original.subList(0, this.offset));
         newList.addAll(replacement);
         int resumeIndex = this.offset + this.size.orElse(replacement.size());
         if (resumeIndex < originalSize) {
            newList.addAll(original.subList(resumeIndex, originalSize));
         }

         List<T> result = newList.build();
         if (result.size() > maxSize) {
            LOGGER.error("Contents overflow in section replacement");
            return original;
         } else {
            return result;
         }
      }
   }

   record StandAlone<T>(List<T> value, ListOperation operation) {
      public static <T> Codec<ListOperation.StandAlone<T>> codec(final Codec<T> valueCodec, final int maxSize) {
         return RecordCodecBuilder.create(
            i -> i.group(
                  valueCodec.sizeLimitedListOf(maxSize).fieldOf("values").forGetter(f -> f.value), ListOperation.codec(maxSize).forGetter(f -> f.operation)
               )
               .apply(i, ListOperation.StandAlone::new)
         );
      }

      public List<T> apply(final List<T> input) {
         return this.operation.apply(input, this.value);
      }
   }

   enum Type implements StringRepresentable {
      REPLACE_ALL("replace_all", ListOperation.ReplaceAll.MAP_CODEC),
      REPLACE_SECTION("replace_section", ListOperation.ReplaceSection.MAP_CODEC),
      INSERT("insert", ListOperation.Insert.MAP_CODEC),
      APPEND("append", ListOperation.Append.MAP_CODEC);

      public static final Codec<ListOperation.Type> CODEC = StringRepresentable.fromEnum(ListOperation.Type::values);
      private final String id;
      private final MapCodec<? extends ListOperation> mapCodec;

      Type(final String id, final MapCodec<? extends ListOperation> mapCodec) {
         this.id = id;
         this.mapCodec = mapCodec;
      }

      public MapCodec<? extends ListOperation> mapCodec() {
         return this.mapCodec;
      }

      @Override
      public String getSerializedName() {
         return this.id;
      }
   }
}
