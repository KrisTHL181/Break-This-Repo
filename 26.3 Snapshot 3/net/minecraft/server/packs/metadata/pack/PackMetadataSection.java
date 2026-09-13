package net.minecraft.server.packs.metadata.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.InclusiveRange;

public record PackMetadataSection(Component description, InclusiveRange<PackFormat> supportedFormats) {
   private static final Codec<PackMetadataSection> FALLBACK_CODEC = RecordCodecBuilder.create(
      i -> i.group(ComponentSerialization.CODEC.fieldOf("description").forGetter(PackMetadataSection::description))
         .apply(i, description -> new PackMetadataSection(description, new InclusiveRange<>(PackFormat.of(Integer.MAX_VALUE))))
   );
   public static final MetadataSectionType<PackMetadataSection> CLIENT_TYPE = new MetadataSectionType<>("pack", codecForPackType(PackType.CLIENT_RESOURCES));
   public static final MetadataSectionType<PackMetadataSection> SERVER_TYPE = new MetadataSectionType<>("pack", codecForPackType(PackType.SERVER_DATA));
   public static final MetadataSectionType<PackMetadataSection> FALLBACK_TYPE = new MetadataSectionType<>("pack", FALLBACK_CODEC);

   private static Codec<PackMetadataSection> codecForPackType(final PackType packType) {
      return RecordCodecBuilder.create(
         i -> i.group(
               ComponentSerialization.CODEC.fieldOf("description").forGetter(PackMetadataSection::description),
               PackFormat.packCodec(packType).forGetter(PackMetadataSection::supportedFormats)
            )
            .apply(i, PackMetadataSection::new)
      );
   }

   public static MetadataSectionType<PackMetadataSection> forPackType(final PackType packType) {
      return switch (packType) {
         case CLIENT_RESOURCES -> CLIENT_TYPE;
         case SERVER_DATA -> SERVER_TYPE;
      };
   }
}
