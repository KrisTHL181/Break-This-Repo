package net.minecraft.server.packs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public record FeatureFlagsMetadataSection(FeatureFlagSet flags) {
   private static final Codec<FeatureFlagsMetadataSection> CODEC = RecordCodecBuilder.create(
      i -> i.group(FeatureFlags.CODEC.fieldOf("enabled").forGetter(FeatureFlagsMetadataSection::flags)).apply(i, FeatureFlagsMetadataSection::new)
   );
   public static final MetadataSectionType<FeatureFlagsMetadataSection> TYPE = new MetadataSectionType<>("features", CODEC);
}
