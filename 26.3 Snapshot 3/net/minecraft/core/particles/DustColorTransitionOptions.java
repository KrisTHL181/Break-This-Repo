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
      i -> i.group(
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("from_color").forGetter(o -> o.fromColor),
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("to_color").forGetter(o -> o.toColor),
            SCALE.fieldOf("scale").forGetter(ScalableParticleOptionsBase::getScale)
         )
         .apply(i, DustColorTransitionOptions::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DustColorTransitionOptions> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      o -> o.fromColor,
      ByteBufCodecs.INT,
      o -> o.toColor,
      ByteBufCodecs.FLOAT,
      ScalableParticleOptionsBase::getScale,
      DustColorTransitionOptions::new
   );
   private final int fromColor;
   private final int toColor;

   public DustColorTransitionOptions(final int fromColor, final int toColor, final float scale) {
      super(scale);
      this.fromColor = fromColor;
      this.toColor = toColor;
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
