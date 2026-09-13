package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoRenderParticle extends Particle {
    protected NoRenderParticle(final ClientLevel level, final double x, final double y, final double z) {
        super(level, x, y, z);
    }

    protected NoRenderParticle(final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za) {
        super(level, x, y, z, xa, ya, za);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.NO_RENDER;
    }
}