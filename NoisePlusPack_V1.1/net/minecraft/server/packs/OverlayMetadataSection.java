package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.InclusiveRange;

public record OverlayMetadataSection(List<OverlayMetadataSection.OverlayEntry> overlays) {
   private static final Pattern DIR_VALIDATOR = Pattern.compile("[-_a-zA-Z0-9.]+");
   public static final MetadataSectionType<OverlayMetadataSection> CLIENT_TYPE = new MetadataSectionType<>(
      "overlays", codecForPackType(PackType.CLIENT_RESOURCES)
   );
   public static final MetadataSectionType<OverlayMetadataSection> SERVER_TYPE = new MetadataSectionType<>("overlays", codecForPackType(PackType.SERVER_DATA));

   private static DataResult<String> validateOverlayDir(String p_301366_) {
      return !DIR_VALIDATOR.matcher(p_301366_).matches()
         ? DataResult.error(() -> p_301366_ + " is not accepted directory name")
         : DataResult.success(p_301366_);
   }

   @VisibleForTesting
   public static Codec<OverlayMetadataSection> codecForPackType(PackType p_423078_) {
      return RecordCodecBuilder.create(
         p_421502_ -> p_421502_.group(
               OverlayMetadataSection.OverlayEntry.listCodecForPackType(p_423078_).fieldOf("entries").forGetter(OverlayMetadataSection::overlays)
            )
            .apply(p_421502_, OverlayMetadataSection::new)
      );
   }

   public static MetadataSectionType<OverlayMetadataSection> forPackType(PackType p_424194_) {
      return switch (p_424194_) {
         case CLIENT_RESOURCES -> CLIENT_TYPE;
         case SERVER_DATA -> SERVER_TYPE;
      };
   }

   public List<String> overlaysForVersion(PackFormat p_425205_) {
      return this.overlays.stream().filter(p_421500_ -> p_421500_.isApplicable(p_425205_)).map(OverlayMetadataSection.OverlayEntry::overlay).toList();
   }

   public record OverlayEntry(InclusiveRange<PackFormat> format, String overlay) {
      static Codec<List<OverlayMetadataSection.OverlayEntry>> listCodecForPackType(PackType p_426646_) {
         int i = PackFormat.lastPreMinorVersion(p_426646_);
         return OverlayMetadataSection.OverlayEntry.IntermediateEntry.CODEC
            .listOf()
            .flatXmap(
               p_421506_ -> PackFormat.validateHolderList(
                  p_421506_, i, (p_421507_, p_421508_) -> new OverlayMetadataSection.OverlayEntry(p_421508_, p_421507_.overlay())
               ),
               p_421504_ -> DataResult.success(
                  p_421504_.stream()
                     .map(
                        p_421510_ -> new OverlayMetadataSection.OverlayEntry.IntermediateEntry(
                           PackFormat.IntermediaryFormat.fromRange(p_421510_.format(), i), p_421510_.overlay()
                        )
                     )
                     .toList()
               )
            );
      }

      public boolean isApplicable(PackFormat p_424066_) {
         return this.format.isValueInRange(p_424066_);
      }

      record IntermediateEntry(PackFormat.IntermediaryFormat format, String overlay) implements PackFormat.IntermediaryFormatHolder {
         static final Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> CODEC = RecordCodecBuilder.create(
            p_427213_ -> p_427213_.group(
                  PackFormat.IntermediaryFormat.OVERLAY_CODEC.forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::format),
                  Codec.STRING
                     .validate(OverlayMetadataSection::validateOverlayDir)
                     .fieldOf("directory")
                     .forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::overlay)
               )
               .apply(p_427213_, OverlayMetadataSection.OverlayEntry.IntermediateEntry::new)
         );

         @Override
         public String toString() {
            return this.overlay;
         }
      }
   }
}
