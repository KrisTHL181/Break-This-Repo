package net.minecraft.server.packs.metadata.pack;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record PackFormat(int major, int minor) implements Comparable<PackFormat> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<PackFormat> BOTTOM_CODEC = fullCodec(0);
   public static final Codec<PackFormat> TOP_CODEC = fullCodec(Integer.MAX_VALUE);

   private static Codec<PackFormat> fullCodec(int p_427200_) {
      return ExtraCodecs.compactListCodec(ExtraCodecs.NON_NEGATIVE_INT, ExtraCodecs.NON_NEGATIVE_INT.listOf(1, 256))
         .xmap(
            p_427372_ -> p_427372_.size() > 1 ? of((Integer)p_427372_.getFirst(), (Integer)p_427372_.get(1)) : of((Integer)p_427372_.getFirst(), p_427200_),
            p_422935_ -> p_422935_.minor != p_427200_ ? List.of(p_422935_.major(), p_422935_.minor()) : List.of(p_422935_.major())
         );
   }

   public static <ResultType, HolderType extends PackFormat.IntermediaryFormatHolder> DataResult<List<ResultType>> validateHolderList(
      List<HolderType> p_430458_, int p_425651_, BiFunction<HolderType, InclusiveRange<PackFormat>, ResultType> p_424880_
   ) {
      int i = p_430458_.stream()
         .map(PackFormat.IntermediaryFormatHolder::format)
         .mapToInt(PackFormat.IntermediaryFormat::effectiveMinMajorVersion)
         .min()
         .orElse(Integer.MAX_VALUE);
      List<ResultType> list = new ArrayList<>(p_430458_.size());

      for (HolderType holdertype : p_430458_) {
         PackFormat.IntermediaryFormat packformat$intermediaryformat = holdertype.format();
         if (packformat$intermediaryformat.min().isEmpty()
            && packformat$intermediaryformat.max().isEmpty()
            && packformat$intermediaryformat.supported().isEmpty()) {
            LOGGER.warn("Unknown or broken overlay entry {}", holdertype);
         } else {
            DataResult<InclusiveRange<PackFormat>> dataresult = packformat$intermediaryformat.validate(
               p_425651_, false, i <= p_425651_, "Overlay \"" + holdertype + "\"", "formats"
            );
            if (!dataresult.isSuccess()) {
               return DataResult.error(((Error)dataresult.error().get())::message);
            }

            list.add(p_424880_.apply(holdertype, (InclusiveRange<PackFormat>)dataresult.getOrThrow()));
         }
      }

      return DataResult.success(List.copyOf(list));
   }

   @VisibleForTesting
   public static int lastPreMinorVersion(PackType p_427613_) {
      return switch (p_427613_) {
         case CLIENT_RESOURCES -> 64;
         case SERVER_DATA -> 81;
      };
   }

   public static MapCodec<InclusiveRange<PackFormat>> packCodec(PackType p_429903_) {
      int i = lastPreMinorVersion(p_429903_);
      return PackFormat.IntermediaryFormat.PACK_CODEC
         .flatXmap(
            p_431617_ -> p_431617_.validate(i, true, false, "Pack", "supported_formats"),
            p_427817_ -> DataResult.success(PackFormat.IntermediaryFormat.fromRange(p_427817_, i))
         );
   }

   public static PackFormat of(int p_422754_, int p_430074_) {
      return new PackFormat(p_422754_, p_430074_);
   }

   public static PackFormat of(int p_425458_) {
      return new PackFormat(p_425458_, 0);
   }

   public InclusiveRange<PackFormat> minorRange() {
      return new InclusiveRange<>(this, of(this.major, Integer.MAX_VALUE));
   }

   public int compareTo(PackFormat p_428386_) {
      int i = Integer.compare(this.major(), p_428386_.major());
      return i != 0 ? i : Integer.compare(this.minor(), p_428386_.minor());
   }

   @Override
   public String toString() {
      return this.minor == Integer.MAX_VALUE
         ? String.format(Locale.ROOT, "%d.*", this.major())
         : String.format(Locale.ROOT, "%d.%d", this.major(), this.minor());
   }

   public record IntermediaryFormat(Optional<PackFormat> min, Optional<PackFormat> max, Optional<Integer> format, Optional<InclusiveRange<Integer>> supported) {
      static final MapCodec<PackFormat.IntermediaryFormat> PACK_CODEC = RecordCodecBuilder.mapCodec(
         p_425312_ -> p_425312_.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               Codec.INT.optionalFieldOf("pack_format").forGetter(PackFormat.IntermediaryFormat::format),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("supported_formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(p_425312_, PackFormat.IntermediaryFormat::new)
      );
      public static final MapCodec<PackFormat.IntermediaryFormat> OVERLAY_CODEC = RecordCodecBuilder.mapCodec(
         p_427838_ -> p_427838_.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(
               p_427838_,
               (p_429082_, p_429192_, p_423793_) -> new PackFormat.IntermediaryFormat(p_429082_, p_429192_, p_429082_.map(PackFormat::major), p_423793_)
            )
      );

      public static PackFormat.IntermediaryFormat fromRange(InclusiveRange<PackFormat> p_425606_, int p_425298_) {
         InclusiveRange<Integer> inclusiverange = p_425606_.map(PackFormat::major);
         return new PackFormat.IntermediaryFormat(
            Optional.of(p_425606_.minInclusive()),
            Optional.of(p_425606_.maxInclusive()),
            inclusiverange.isValueInRange(p_425298_) ? Optional.of(inclusiverange.minInclusive()) : Optional.empty(),
            inclusiverange.isValueInRange(p_425298_)
               ? Optional.of(new InclusiveRange<>(inclusiverange.minInclusive(), inclusiverange.maxInclusive()))
               : Optional.empty()
         );
      }

      public int effectiveMinMajorVersion() {
         if (this.min.isPresent()) {
            return this.supported.isPresent() ? Math.min(this.min.get().major(), this.supported.get().minInclusive()) : this.min.get().major();
         } else {
            return this.supported.isPresent() ? this.supported.get().minInclusive() : Integer.MAX_VALUE;
         }
      }

      public DataResult<InclusiveRange<PackFormat>> validate(int p_427155_, boolean p_424023_, boolean p_423736_, String p_428659_, String p_425874_) {
         if (this.min.isPresent() != this.max.isPresent()) {
            return DataResult.error(() -> p_428659_ + " missing field, must declare both min_format and max_format");
         } else if (p_423736_ && this.supported.isEmpty()) {
            return DataResult.error(
               () -> p_428659_ + " missing required field " + p_425874_ + ", must be present in all overlays for any overlays to work across game versions"
            );
         } else if (this.min.isPresent()) {
            return this.validateNewFormat(p_427155_, p_424023_, p_423736_, p_428659_, p_425874_);
         } else if (this.supported.isPresent()) {
            return this.validateOldFormat(p_427155_, p_424023_, p_428659_, p_425874_);
         } else if (p_424023_ && this.format.isPresent()) {
            int i = this.format.get();
            return i > p_427155_
               ? DataResult.error(
                  () -> p_428659_ + " declares support for version newer than " + p_427155_ + ", but is missing mandatory fields min_format and max_format"
               )
               : DataResult.success(new InclusiveRange<>(PackFormat.of(i)));
         } else {
            return DataResult.error(() -> p_428659_ + " could not be parsed, missing format version information");
         }
      }

      private DataResult<InclusiveRange<PackFormat>> validateNewFormat(int p_424558_, boolean p_429681_, boolean p_426088_, String p_424356_, String p_425096_) {
         int i = this.min.get().major();
         int j = this.max.get().major();
         if (this.min.get().compareTo(this.max.get()) > 0) {
            return DataResult.error(() -> p_424356_ + " min_format (" + this.min.get() + ") is greater than max_format (" + this.max.get() + ")");
         }

         if (i > p_424558_ && !p_426088_) {
            if (this.supported.isPresent()) {
               return DataResult.error(
                  () -> p_424356_
                     + " key "
                     + p_425096_
                     + " is deprecated starting from pack format "
                     + (p_424558_ + 1)
                     + ". Remove "
                     + p_425096_
                     + " from your pack.mcmeta."
               );
            }

            if (p_429681_ && this.format.isPresent()) {
               String s1 = this.validatePackFormatForRange(i, j);
               if (s1 != null) {
                  return DataResult.error(() -> s1);
               }
            }
         } else {
            if (!this.supported.isPresent()) {
               return DataResult.error(
                  () -> p_424356_
                     + " declares support for format "
                     + i
                     + ", but game versions supporting formats 17 to "
                     + p_424558_
                     + " require a "
                     + p_425096_
                     + " field. Add \""
                     + p_425096_
                     + "\": ["
                     + i
                     + ", "
                     + p_424558_
                     + "] or require a version greater or equal to "
                     + (p_424558_ + 1)
                     + ".0."
               );
            }

            InclusiveRange<Integer> inclusiverange = this.supported.get();
            if (inclusiverange.minInclusive() != i) {
               return DataResult.error(
                  () -> p_424356_
                     + " version declaration mismatch between "
                     + p_425096_
                     + " (from "
                     + inclusiverange.minInclusive()
                     + ") and min_format ("
                     + this.min.get()
                     + ")"
               );
            }

            if (inclusiverange.maxInclusive() != j && inclusiverange.maxInclusive() != p_424558_) {
               return DataResult.error(
                  () -> p_424356_
                     + " version declaration mismatch between "
                     + p_425096_
                     + " (up to "
                     + inclusiverange.maxInclusive()
                     + ") and max_format ("
                     + this.max.get()
                     + ")"
               );
            }

            if (p_429681_) {
               if (!this.format.isPresent()) {
                  return DataResult.error(
                     () -> p_424356_
                        + " declares support for formats up to "
                        + p_424558_
                        + ", but game versions supporting formats 17 to "
                        + p_424558_
                        + " require a pack_format field. Add \"pack_format\": "
                        + i
                        + " or require a version greater or equal to "
                        + (p_424558_ + 1)
                        + ".0."
                  );
               }

               String s = this.validatePackFormatForRange(i, j);
               if (s != null) {
                  return DataResult.error(() -> s);
               }
            }
         }

         return DataResult.success(new InclusiveRange<>(this.min.get(), this.max.get()));
      }

      private DataResult<InclusiveRange<PackFormat>> validateOldFormat(int p_422726_, boolean p_422409_, String p_428790_, String p_426864_) {
         InclusiveRange<Integer> inclusiverange = this.supported.get();
         int i = inclusiverange.minInclusive();
         int j = inclusiverange.maxInclusive();
         if (j > p_422726_) {
            return DataResult.error(
               () -> p_428790_ + " declares support for version newer than " + p_422726_ + ", but is missing mandatory fields min_format and max_format"
            );
         }

         if (p_422409_) {
            if (!this.format.isPresent()) {
               return DataResult.error(
                  () -> p_428790_
                     + " declares support for formats up to "
                     + p_422726_
                     + ", but game versions supporting formats 17 to "
                     + p_422726_
                     + " require a pack_format field. Add \"pack_format\": "
                     + i
                     + " or require a version greater or equal to "
                     + (p_422726_ + 1)
                     + ".0."
               );
            }

            String s = this.validatePackFormatForRange(i, j);
            if (s != null) {
               return DataResult.error(() -> s);
            }
         }

         return DataResult.success(new InclusiveRange<>(i, j).map(PackFormat::of));
      }

      private @Nullable String validatePackFormatForRange(int p_430819_, int p_423380_) {
         int i = this.format.get();
         if (i >= p_430819_ && i <= p_423380_) {
            return i < 15
               ? "Multi-version packs cannot support minimum version of less than 15, since this will leave versions in range unable to load pack."
               : null;
         } else {
            return "Pack declared support for versions " + p_430819_ + " to " + p_423380_ + " but declared main format is " + i;
         }
      }
   }

   public interface IntermediaryFormatHolder {
      PackFormat.IntermediaryFormat format();
   }
}
