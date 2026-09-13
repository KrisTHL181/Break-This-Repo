package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ParticleProvider<T extends ParticleOptions> {
   @Nullable Particle createParticle(
      T var1, ClientLevel var2, double var3, double var5, double var7, double var9, double var11, double var13, RandomSource var15
   );

   @OnlyIn(Dist.CLIENT)
   interface Sprite<T extends ParticleOptions> {
      @Nullable SingleQuadParticle createParticle(
         T var1, ClientLevel var2, double var3, double var5, double var7, double var9, double var11, double var13, RandomSource var15
      );
   }
}
