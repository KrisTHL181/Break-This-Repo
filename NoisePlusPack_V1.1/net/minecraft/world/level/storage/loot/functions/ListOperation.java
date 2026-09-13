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

   static MapCodec<ListOperation> codec(int p_334114_) {
      return ListOperation.Type.CODEC.dispatchMap("mode", ListOperation::mode, p_328481_ -> p_328481_.mapCodec).validate(p_336261_ -> {
         if (p_336261_ instanceof ListOperation.ReplaceSection listoperation$replacesection && listoperation$replacesection.size().isPresent()) {
            int i = listoperation$replacesection.size().get();
            if (i > p_334114_) {
               return DataResult.error(() -> "Size value too large: " + i + ", max size is " + p_334114_);
            }
         }

         return DataResult.success(p_336261_);
      });
   }

   ListOperation.Type mode();

   default <T> List<T> apply(List<T> p_334598_, List<T> p_335380_) {
      return this.apply(p_334598_, p_335380_, Integer.MAX_VALUE);
   }

   <T> List<T> apply(List<T> var1, List<T> var2, int var3);

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
      public <T> List<T> apply(List<T> p_330728_, List<T> p_331859_, int p_335288_) {
         if (p_330728_.size() + p_331859_.size() > p_335288_) {
            LOGGER.error("Contents overflow in section append");
            return p_330728_;
         } else {
            return Stream.concat(p_330728_.stream(), p_331859_.stream()).toList();
         }
      }
   }

   record Insert(int offset) implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<ListOperation.Insert> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_329650_ -> p_329650_.group(ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", 0).forGetter(ListOperation.Insert::offset))
            .apply(p_329650_, ListOperation.Insert::new)
      );

      @Override
      public ListOperation.Type mode() {
         return ListOperation.Type.INSERT;
      }

      @Override
      public <T> List<T> apply(List<T> p_336295_, List<T> p_330545_, int p_335268_) {
         int i = p_336295_.size();
         if (this.offset > i) {
            LOGGER.error("Cannot insert when offset is out of bounds");
            return p_336295_;
         } else if (i + p_330545_.size() > p_335268_) {
            LOGGER.error("Contents overflow in section insertion");
            return p_336295_;
         } else {
            Builder<T> builder = ImmutableList.builder();
            builder.addAll(p_336295_.subList(0, this.offset));
            builder.addAll(p_330545_);
            builder.addAll(p_336295_.subList(this.offset, i));
            return builder.build();
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
      public <T> List<T> apply(List<T> p_333557_, List<T> p_331455_, int p_335044_) {
         return p_331455_;
      }
   }

   record ReplaceSection(int offset, Optional<Integer> size) implements ListOperation {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec<ListOperation.ReplaceSection> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_332380_ -> p_332380_.group(
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("offset", 0).forGetter(ListOperation.ReplaceSection::offset),
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("size").forGetter(ListOperation.ReplaceSection::size)
            )
            .apply(p_332380_, ListOperation.ReplaceSection::new)
      );

      public ReplaceSection(int p_335251_) {
         this(p_335251_, Optional.empty());
      }

      @Override
      public ListOperation.Type mode() {
         return ListOperation.Type.REPLACE_SECTION;
      }

      @Override
      public <T> List<T> apply(List<T> p_336048_, List<T> p_331104_, int p_333303_) {
         int i = p_336048_.size();
         if (this.offset > i) {
            LOGGER.error("Cannot replace when offset is out of bounds");
            return p_336048_;
         }

         Builder<T> builder = ImmutableList.builder();
         builder.addAll(p_336048_.subList(0, this.offset));
         builder.addAll(p_331104_);
         int j = this.offset + this.size.orElse(p_331104_.size());
         if (j < i) {
            builder.addAll(p_336048_.subList(j, i));
         }

         List<T> list = builder.build();
         if (list.size() > p_333303_) {
            LOGGER.error("Contents overflow in section replacement");
            return p_336048_;
         } else {
            return list;
         }
      }
   }

   record StandAlone<T>(List<T> value, ListOperation operation) {
      public static <T> Codec<ListOperation.StandAlone<T>> codec(Codec<T> p_333263_, int p_334839_) {
         return RecordCodecBuilder.create(
            p_334562_ -> p_334562_.group(
                  p_333263_.sizeLimitedListOf(p_334839_).fieldOf("values").forGetter(p_331378_ -> p_331378_.value),
                  ListOperation.codec(p_334839_).forGetter(p_330703_ -> p_330703_.operation)
               )
               .apply(p_334562_, ListOperation.StandAlone::new)
         );
      }

      public List<T> apply(List<T> p_334156_) {
         return this.operation.apply(p_334156_, this.value);
      }
   }

   enum Type implements StringRepresentable {
      REPLACE_ALL("replace_all", ListOperation.ReplaceAll.MAP_CODEC),
      REPLACE_SECTION("replace_section", ListOperation.ReplaceSection.MAP_CODEC),
      INSERT("insert", ListOperation.Insert.MAP_CODEC),
      APPEND("append", ListOperation.Append.MAP_CODEC);

      public static final Codec<ListOperation.Type> CODEC = StringRepresentable.fromEnum(ListOperation.Type::values);
      private final String id;
      final MapCodec<? extends ListOperation> mapCodec;

      Type(final String p_332297_, final MapCodec<? extends ListOperation> p_336238_) {
         this.id = p_332297_;
         this.mapCodec = p_336238_;
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
