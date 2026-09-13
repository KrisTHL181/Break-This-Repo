package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;

public class DustParticleOptions extends ScalableParticleOptionsBase {
   public static final int REDSTONE_PARTICLE_COLOR = 16711680;
   public static final DustParticleOptions REDSTONE = new DustParticleOptions(16711680, 1.0F);
   public static final MapCodec<DustParticleOptions> CODEC = RecordCodecBuilder.mapCodec(
      p_358151_ -> p_358151_.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(p_358152_ -> p_358152_.color),
            SCALE.fieldOf("scale").forGetter(ScalableParticleOptionsBase::getScale)
         )
         .apply(p_358151_, DustParticleOptions::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DustParticleOptions> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, p_358153_ -> p_358153_.color, ByteBufCodecs.FLOAT, ScalableParticleOptionsBase::getScale, DustParticleOptions::new
   );
   private final int color;

   public DustParticleOptions(int p_364098_, float p_254154_) {
      super(p_254154_);
      this.color = p_364098_;
   }

   @Override
   public ParticleType<DustParticleOptions> getType() {
      return ParticleTypes.DUST;
   }

   public Vector3f getColor() {
      return ARGB.vector3fFromRGB24(this.color);
   }
}
