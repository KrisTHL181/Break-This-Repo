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

   private static Codec<PackFormat> fullCodec(final int defaultMinor) {
      return ExtraCodecs.compactListCodec(ExtraCodecs.NON_NEGATIVE_INT, ExtraCodecs.NON_NEGATIVE_INT.listOf(1, 256))
         .xmap(
            list -> list.size() > 1 ? of((Integer)list.getFirst(), (Integer)list.get(1)) : of((Integer)list.getFirst(), defaultMinor),
            pf -> pf.minor != defaultMinor ? List.of(pf.major(), pf.minor()) : List.of(pf.major())
         );
   }

   public static <ResultType, HolderType extends PackFormat.IntermediaryFormatHolder> DataResult<List<ResultType>> validateHolderList(
      final List<HolderType> list, final int lastPreMinorVersion, final BiFunction<HolderType, InclusiveRange<PackFormat>, ResultType> constructor
   ) {
      int minVersion = list.stream()
         .map(PackFormat.IntermediaryFormatHolder::format)
         .mapToInt(PackFormat.IntermediaryFormat::effectiveMinMajorVersion)
         .min()
         .orElse(Integer.MAX_VALUE);
      List<ResultType> result = new ArrayList<>(list.size());

      for (HolderType entry : list) {
         PackFormat.IntermediaryFormat format = entry.format();
         if (format.min().isEmpty() && format.max().isEmpty() && format.supported().isEmpty()) {
            LOGGER.warn("Unknown or broken overlay entry {}", entry);
         } else {
            DataResult<InclusiveRange<PackFormat>> entryResult = format.validate(
               lastPreMinorVersion, false, minVersion <= lastPreMinorVersion, "Overlay \"" + entry + "\"", "formats"
            );
            if (!entryResult.isSuccess()) {
               return DataResult.error(((Error)entryResult.error().get())::message);
            }

            result.add(constructor.apply(entry, (InclusiveRange<PackFormat>)entryResult.getOrThrow()));
         }
      }

      return DataResult.success(List.copyOf(result));
   }

   @VisibleForTesting
   public static int lastPreMinorVersion(final PackType type) {
      return switch (type) {
         case CLIENT_RESOURCES -> 64;
         case SERVER_DATA -> 81;
      };
   }

   public static MapCodec<InclusiveRange<PackFormat>> packCodec(final PackType type) {
      int lastPreMinorVersion = lastPreMinorVersion(type);
      return PackFormat.IntermediaryFormat.PACK_CODEC
         .flatXmap(
            intermediaryFormat -> intermediaryFormat.validate(lastPreMinorVersion, true, false, "Pack", "supported_formats"),
            range -> DataResult.success(PackFormat.IntermediaryFormat.fromRange(range, lastPreMinorVersion))
         );
   }

   public static PackFormat of(final int major, final int minor) {
      return new PackFormat(major, minor);
   }

   public static PackFormat of(final int major) {
      return new PackFormat(major, 0);
   }

   public InclusiveRange<PackFormat> minorRange() {
      return new InclusiveRange<>(this, of(this.major, Integer.MAX_VALUE));
   }

   public int compareTo(final PackFormat other) {
      int majorDiff = Integer.compare(this.major(), other.major());
      return majorDiff != 0 ? majorDiff : Integer.compare(this.minor(), other.minor());
   }

   @Override
   public String toString() {
      return this.minor == Integer.MAX_VALUE
         ? String.format(Locale.ROOT, "%d.*", this.major())
         : String.format(Locale.ROOT, "%d.%d", this.major(), this.minor());
   }

   public record IntermediaryFormat(Optional<PackFormat> min, Optional<PackFormat> max, Optional<Integer> format, Optional<InclusiveRange<Integer>> supported) {
      private static final MapCodec<PackFormat.IntermediaryFormat> PACK_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               Codec.INT.optionalFieldOf("pack_format").forGetter(PackFormat.IntermediaryFormat::format),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("supported_formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(i, PackFormat.IntermediaryFormat::new)
      );
      public static final MapCodec<PackFormat.IntermediaryFormat> OVERLAY_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               PackFormat.BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(PackFormat.IntermediaryFormat::min),
               PackFormat.TOP_CODEC.optionalFieldOf("max_format").forGetter(PackFormat.IntermediaryFormat::max),
               InclusiveRange.codec(Codec.INT).optionalFieldOf("formats").forGetter(PackFormat.IntermediaryFormat::supported)
            )
            .apply(i, (min, max, formats) -> new PackFormat.IntermediaryFormat(min, max, min.map(PackFormat::major), formats))
      );

      public static PackFormat.IntermediaryFormat fromRange(final InclusiveRange<PackFormat> range, final int lastPreMinorVersion) {
         InclusiveRange<Integer> majorRange = range.map(PackFormat::major);
         return new PackFormat.IntermediaryFormat(
            Optional.of(range.minInclusive()),
            Optional.of(range.maxInclusive()),
            majorRange.isValueInRange(lastPreMinorVersion) ? Optional.of(majorRange.minInclusive()) : Optional.empty(),
            majorRange.isValueInRange(lastPreMinorVersion)
               ? Optional.of(new InclusiveRange<>(majorRange.minInclusive(), majorRange.maxInclusive()))
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

      public DataResult<InclusiveRange<PackFormat>> validate(
         final int lastPreMinorVersion, final boolean hasPackFormatField, final boolean requireOldField, final String context, final String oldFieldName
      ) {
         if (this.min.isPresent() != this.max.isPresent()) {
            return DataResult.error(() -> context + " missing field, must declare both min_format and max_format");
         } else if (requireOldField && this.supported.isEmpty()) {
            return DataResult.error(
               () -> context + " missing required field " + oldFieldName + ", must be present in all overlays for any overlays to work across game versions"
            );
         } else if (this.min.isPresent()) {
            return this.validateNewFormat(lastPreMinorVersion, hasPackFormatField, requireOldField, context, oldFieldName);
         } else if (this.supported.isPresent()) {
            return this.validateOldFormat(lastPreMinorVersion, hasPackFormatField, context, oldFieldName);
         } else if (hasPackFormatField && this.format.isPresent()) {
            int mainFormat = this.format.get();
            return mainFormat > lastPreMinorVersion
               ? DataResult.error(
                  () -> context
                     + " declares support for version newer than "
                     + lastPreMinorVersion
                     + ", but is missing mandatory fields min_format and max_format"
               )
               : DataResult.success(new InclusiveRange<>(PackFormat.of(mainFormat)));
         } else {
            return DataResult.error(() -> context + " could not be parsed, missing format version information");
         }
      }

      private DataResult<InclusiveRange<PackFormat>> validateNewFormat(
         final int lastPreMinorVersion, final boolean hasPackFormatField, final boolean requireOldField, final String context, final String oldFieldName
      ) {
         int majorMin = this.min.get().major();
         int majorMax = this.max.get().major();
         if (this.min.get().compareTo(this.max.get()) > 0) {
            return DataResult.error(() -> context + " min_format (" + this.min.get() + ") is greater than max_format (" + this.max.get() + ")");
         }

         if (majorMin > lastPreMinorVersion && !requireOldField) {
            if (this.supported.isPresent()) {
               return DataResult.error(
                  () -> context
                     + " key "
                     + oldFieldName
                     + " is deprecated starting from pack format "
                     + (lastPreMinorVersion + 1)
                     + ". Remove "
                     + oldFieldName
                     + " from your pack.mcmeta."
               );
            }

            if (hasPackFormatField && this.format.isPresent()) {
               String packFormatError = this.validatePackFormatForRange(majorMin, majorMax);
               if (packFormatError != null) {
                  return DataResult.error(() -> packFormatError);
               }
            }
         } else {
            if (!this.supported.isPresent()) {
               return DataResult.error(
                  () -> context
                     + " declares support for format "
                     + majorMin
                     + ", but game versions supporting formats 17 to "
                     + lastPreMinorVersion
                     + " require a "
                     + oldFieldName
                     + " field. Add \""
                     + oldFieldName
                     + "\": ["
                     + majorMin
                     + ", "
                     + lastPreMinorVersion
                     + "] or require a version greater or equal to "
                     + (lastPreMinorVersion + 1)
                     + ".0."
               );
            }

            InclusiveRange<Integer> oldSupportedVersions = this.supported.get();
            if (oldSupportedVersions.minInclusive() != majorMin) {
               return DataResult.error(
                  () -> context
                     + " version declaration mismatch between "
                     + oldFieldName
                     + " (from "
                     + oldSupportedVersions.minInclusive()
                     + ") and min_format ("
                     + this.min.get()
                     + ")"
               );
            }

            if (oldSupportedVersions.maxInclusive() != majorMax && oldSupportedVersions.maxInclusive() != lastPreMinorVersion) {
               return DataResult.error(
                  () -> context
                     + " version declaration mismatch between "
                     + oldFieldName
                     + " (up to "
                     + oldSupportedVersions.maxInclusive()
                     + ") and max_format ("
                     + this.max.get()
                     + ")"
               );
            }

            if (hasPackFormatField) {
               if (!this.format.isPresent()) {
                  return DataResult.error(
                     () -> context
                        + " declares support for formats up to "
                        + lastPreMinorVersion
                        + ", but game versions supporting formats 17 to "
                        + lastPreMinorVersion
                        + " require a pack_format field. Add \"pack_format\": "
                        + majorMin
                        + " or require a version greater or equal to "
                        + (lastPreMinorVersion + 1)
                        + ".0."
                  );
               }

               String packFormatError = this.validatePackFormatForRange(majorMin, majorMax);
               if (packFormatError != null) {
                  return DataResult.error(() -> packFormatError);
               }
            }
         }

         return DataResult.success(new InclusiveRange<>(this.min.get(), this.max.get()));
      }

      private DataResult<InclusiveRange<PackFormat>> validateOldFormat(
         final int lastPreMinorVersion, final boolean hasPackFormatField, final String context, final String oldFieldName
      ) {
         InclusiveRange<Integer> oldSupportedVersions = this.supported.get();
         int min = oldSupportedVersions.minInclusive();
         int max = oldSupportedVersions.maxInclusive();
         if (max > lastPreMinorVersion) {
            return DataResult.error(
               () -> context
                  + " declares support for version newer than "
                  + lastPreMinorVersion
                  + ", but is missing mandatory fields min_format and max_format"
            );
         }

         if (hasPackFormatField) {
            if (!this.format.isPresent()) {
               return DataResult.error(
                  () -> context
                     + " declares support for formats up to "
                     + lastPreMinorVersion
                     + ", but game versions supporting formats 17 to "
                     + lastPreMinorVersion
                     + " require a pack_format field. Add \"pack_format\": "
                     + min
                     + " or require a version greater or equal to "
                     + (lastPreMinorVersion + 1)
                     + ".0."
               );
            }

            String packFormatError = this.validatePackFormatForRange(min, max);
            if (packFormatError != null) {
               return DataResult.error(() -> packFormatError);
            }
         }

         return DataResult.success(new InclusiveRange<>(min, max).map(PackFormat::of));
      }

      private @Nullable String validatePackFormatForRange(final int min, final int max) {
         int mainFormat = this.format.get();
         if (mainFormat < min || mainFormat > max) {
            return "Pack declared support for versions " + min + " to " + max + " but declared main format is " + mainFormat;
         } else {
            return mainFormat < 15
               ? "Multi-version packs cannot support minimum version of less than 15, since this will leave versions in range unable to load pack."
               : null;
         }
      }
   }

   public interface IntermediaryFormatHolder {
      PackFormat.IntermediaryFormat format();
   }
}
