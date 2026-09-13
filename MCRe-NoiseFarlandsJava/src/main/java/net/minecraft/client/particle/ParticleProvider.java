package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ParticleProvider<T extends ParticleOptions> {
    @Nullable Particle createParticle(T options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random);

    @OnlyIn(Dist.CLIENT)
    interface Sprite<T extends ParticleOptions> {
        @Nullable SingleQuadParticle createParticle(
            T options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random
        );
    }
}