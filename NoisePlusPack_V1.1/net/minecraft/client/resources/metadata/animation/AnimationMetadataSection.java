package net.minecraft.client.resources.metadata.animation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record AnimationMetadataSection(
   Optional<List<AnimationFrame>> frames, Optional<Integer> frameWidth, Optional<Integer> frameHeight, int defaultFrameTime, boolean interpolatedFrames
) {
   public static final Codec<AnimationMetadataSection> CODEC = RecordCodecBuilder.create(
      p_376272_ -> p_376272_.group(
            AnimationFrame.CODEC.listOf().optionalFieldOf("frames").forGetter(AnimationMetadataSection::frames),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("width").forGetter(AnimationMetadataSection::frameWidth),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("height").forGetter(AnimationMetadataSection::frameHeight),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("frametime", 1).forGetter(AnimationMetadataSection::defaultFrameTime),
            Codec.BOOL.optionalFieldOf("interpolate", false).forGetter(AnimationMetadataSection::interpolatedFrames)
         )
         .apply(p_376272_, AnimationMetadataSection::new)
   );
   public static final MetadataSectionType<AnimationMetadataSection> TYPE = new MetadataSectionType<>("animation", CODEC);

   public FrameSize calculateFrameSize(int p_249859_, int p_250148_) {
      if (this.frameWidth.isPresent()) {
         return this.frameHeight.isPresent() ? new FrameSize(this.frameWidth.get(), this.frameHeight.get()) : new FrameSize(this.frameWidth.get(), p_250148_);
      }

      if (this.frameHeight.isPresent()) {
         return new FrameSize(p_249859_, this.frameHeight.get());
      }

      int i = Math.min(p_249859_, p_250148_);
      return new FrameSize(i, i);
   }
}
