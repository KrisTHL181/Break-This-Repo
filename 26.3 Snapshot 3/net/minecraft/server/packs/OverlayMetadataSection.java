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

   private static DataResult<String> validateOverlayDir(final String path) {
      return !DIR_VALIDATOR.matcher(path).matches() ? DataResult.error(() -> path + " is not accepted directory name") : DataResult.success(path);
   }

   @VisibleForTesting
   public static Codec<OverlayMetadataSection> codecForPackType(final PackType packType) {
      return RecordCodecBuilder.create(
         i -> i.group(OverlayMetadataSection.OverlayEntry.listCodecForPackType(packType).fieldOf("entries").forGetter(OverlayMetadataSection::overlays))
            .apply(i, OverlayMetadataSection::new)
      );
   }

   public static MetadataSectionType<OverlayMetadataSection> forPackType(final PackType packType) {
      return switch (packType) {
         case CLIENT_RESOURCES -> CLIENT_TYPE;
         case SERVER_DATA -> SERVER_TYPE;
      };
   }

   public List<String> overlaysForVersion(final PackFormat version) {
      return this.overlays.stream().filter(entry -> entry.isApplicable(version)).map(OverlayMetadataSection.OverlayEntry::overlay).toList();
   }

   public record OverlayEntry(InclusiveRange<PackFormat> format, String overlay) {
      private static Codec<List<OverlayMetadataSection.OverlayEntry>> listCodecForPackType(final PackType packType) {
         int lastPreMinorVersion = PackFormat.lastPreMinorVersion(packType);
         return OverlayMetadataSection.OverlayEntry.IntermediateEntry.CODEC
            .listOf()
            .flatXmap(
               list -> PackFormat.validateHolderList(
                  list, lastPreMinorVersion, (entry, formats) -> new OverlayMetadataSection.OverlayEntry(formats, entry.overlay())
               ),
               list -> DataResult.success(
                  list.stream()
                     .map(
                        entry -> new OverlayMetadataSection.OverlayEntry.IntermediateEntry(
                           PackFormat.IntermediaryFormat.fromRange(entry.format(), lastPreMinorVersion), entry.overlay()
                        )
                     )
                     .toList()
               )
            );
      }

      public boolean isApplicable(final PackFormat formatToTest) {
         return this.format.isValueInRange(formatToTest);
      }

      private record IntermediateEntry(PackFormat.IntermediaryFormat format, String overlay) implements PackFormat.IntermediaryFormatHolder {
         private static final Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                  PackFormat.IntermediaryFormat.OVERLAY_CODEC.forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::format),
                  Codec.STRING
                     .validate(OverlayMetadataSection::validateOverlayDir)
                     .fieldOf("directory")
                     .forGetter(OverlayMetadataSection.OverlayEntry.IntermediateEntry::overlay)
               )
               .apply(i, OverlayMetadataSection.OverlayEntry.IntermediateEntry::new)
         );

         @Override
         public String toString() {
            return this.overlay;
         }
      }
   }
}
