package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SulfurBubbleParticle extends SingleQuadParticle {
    private static final float SIZE_START = 0.02F;
    private static final float SIZE_END = 0.15F;
    private static final float UPWARDS_SPEED = 0.04F;
    private static final float HORIZONTAL_WIGGLING = 0.003F;
    private final double yStart;
    private final double yEnd;
    private final float sizeStart;
    private double yPrev;

    private SulfurBubbleParticle(
        final ClientLevel level, final double x, final double y, final double z, final double xa, final double za, final TextureAtlasSprite sprite
    ) {
        super(level, x, y, z, sprite);
        this.gravity = -0.04F;
        this.friction = 0.85F;
        this.setSize(0.02F, 0.02F);
        this.xd = xa * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
        this.zd = za * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
        this.sizeStart = 0.02F + 0.02F * this.random.nextFloat();
        this.quadSize = this.sizeStart;
        this.lifetime = Integer.MAX_VALUE;
        this.yStart = this.yo;
        this.yEnd = this.yo + 4.0 - 1.0;
        this.yPrev = y;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && !this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).isSourceOfType(Fluids.WATER)) {
            this.remove();
        }

        if (!this.removed && this.y >= this.yEnd) {
            this.remove();
        }

        if (!this.removed && this.y <= this.yPrev) {
            this.remove();
        }

        this.xd = this.xd + this.randomHorizontalWiggling();
        this.zd = this.zd + this.randomHorizontalWiggling();
        this.move(this.xd, 0.0, this.zd);
        float travelProgress = (float)((this.y - this.yStart) / (this.yEnd - this.yStart));
        this.quadSize = this.sizeStart + travelProgress * (0.15F - this.sizeStart);
        this.yPrev = this.y;
    }

    private double randomHorizontalWiggling() {
        return this.random.nextFloat() * 0.003F * (this.random.nextBoolean() ? 1 : -1) * 0.5;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(final SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(
            final SimpleParticleType options,
            final ClientLevel level,
            final double x,
            final double y,
            final double z,
            final double xAux,
            final double yAux,
            final double zAux,
            final RandomSource random
        ) {
            return new SulfurBubbleParticle(level, x, y, z, xAux, yAux, this.sprite.get(random));
        }
    }
}