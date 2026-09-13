package net.minecraft.client.resources.metadata.animation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ExtraCodecs;

public record AnimationMetadataSection(
   Optional<List<AnimationFrame>> frames, Optional<Integer> frameWidth, Optional<Integer> frameHeight, int defaultFrameTime, boolean interpolatedFrames
) {
   public static final Codec<AnimationMetadataSection> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            AnimationFrame.CODEC.listOf().optionalFieldOf("frames").forGetter(AnimationMetadataSection::frames),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("width").forGetter(AnimationMetadataSection::frameWidth),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("height").forGetter(AnimationMetadataSection::frameHeight),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("frametime", 1).forGetter(AnimationMetadataSection::defaultFrameTime),
            Codec.BOOL.optionalFieldOf("interpolate", false).forGetter(AnimationMetadataSection::interpolatedFrames)
         )
         .apply(i, AnimationMetadataSection::new)
   );
   public static final MetadataSectionType<AnimationMetadataSection> TYPE = new MetadataSectionType<>("animation", CODEC);

   public FrameSize calculateFrameSize(final int spriteWidth, final int spriteHeight) {
      if (this.frameWidth.isPresent()) {
         return this.frameHeight.isPresent()
            ? new FrameSize(this.frameWidth.get(), this.frameHeight.get())
            : new FrameSize(this.frameWidth.get(), spriteHeight);
      }

      if (this.frameHeight.isPresent()) {
         return new FrameSize(spriteWidth, this.frameHeight.get());
      }

      int minDimension = Math.min(spriteWidth, spriteHeight);
      return new FrameSize(minDimension, minDimension);
   }
}
