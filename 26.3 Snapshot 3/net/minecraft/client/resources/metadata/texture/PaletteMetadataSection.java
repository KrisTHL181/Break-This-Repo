package net.minecraft.client.resources.metadata.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;

public record PaletteMetadataSection(Identifier basePalette) {
   public static final Codec<PaletteMetadataSection> CODEC = RecordCodecBuilder.create(
      i -> i.group(Identifier.CODEC.fieldOf("base_palette").forGetter(PaletteMetadataSection::basePalette)).apply(i, PaletteMetadataSection::new)
   );
   public static final MetadataSectionType<PaletteMetadataSection> TYPE = new MetadataSectionType<>("palette", CODEC);
}
