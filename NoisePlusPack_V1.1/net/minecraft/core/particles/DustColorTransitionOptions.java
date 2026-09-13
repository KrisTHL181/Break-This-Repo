package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

public class DustColorTransitionOptions extends ScalableParticleOptionsBase {
   public static final int SCULK_PARTICLE_COLOR = 3790560;
   public static final DustColorTransitionOptions SCULK_TO_REDSTONE = new DustColorTransitionOptions(3790560, 16711680, 1.0F);
   public static final MapCodec<DustColorTransitionOptions> CODEC = RecordCodecBuilder.mapCodec(
      p_358148_ -> p_358148_.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("from_color").forGetter(p_358146_ -> p_358146_.fromColor),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("to_color").forGetter(p_358147_ -> p_358147_.toColor),
            SCALE.fieldOf("scale").forGetter(ScalableParticleOptionsBase::getScale)
         )
         .apply(p_358148_, DustColorTransitionOptions::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DustColorTransitionOptions> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      p_358150_ -> p_358150_.fromColor,
      ByteBufCodecs.INT,
      p_358149_ -> p_358149_.toColor,
      ByteBufCodecs.FLOAT,
      ScalableParticleOptionsBase::getScale,
      DustColorTransitionOptions::new
   );
   private final int fromColor;
   private final int toColor;

   public DustColorTransitionOptions(int p_367182_, int p_360907_, float p_254178_) {
      super(p_254178_);
      this.fromColor = p_367182_;
      this.toColor = p_360907_;
   }

   public Vector3f getFromColor() {
      return ARGB.vector3fFromRGB24(this.fromColor);
   }

   public Vector3f getToColor() {
      return ARGB.vector3fFromRGB24(this.toColor);
   }

   @Override
   public ParticleType<DustColorTransitionOptions> getType() {
      return ParticleTypes.DUST_COLOR_TRANSITION;
   }
}
