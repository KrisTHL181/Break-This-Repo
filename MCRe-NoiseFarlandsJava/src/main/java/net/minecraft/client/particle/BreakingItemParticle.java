package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreakingItemParticle extends SingleQuadParticle {
    private final float uo;
    private final float vo;
    private final SingleQuadParticle.Layer layer;

    private BreakingItemParticle(
        final ClientLevel level,
        final double x,
        final double y,
        final double z,
        final double xa,
        final double ya,
        final double za,
        final TextureAtlasSprite sprite
    ) {
        this(level, x, y, z, sprite);
        this.xd *= 0.1F;
        this.yd *= 0.1F;
        this.zd *= 0.1F;
        this.xd += xa;
        this.yd += ya;
        this.zd += za;
    }

    protected BreakingItemParticle(final ClientLevel level, final double x, final double y, final double z, final TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
        this.gravity = 1.0F;
        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
        this.layer = SingleQuadParticle.Layer.bySprite(sprite);
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return this.layer;
    }

    @OnlyIn(Dist.CLIENT)
    public static class CobwebProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
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
            return new BreakingItemParticle(level, x, y, z, this.getSprite(new ItemStackTemplate(Items.COBWEB), level, random));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class ItemParticleProvider<T extends ParticleOptions> implements ParticleProvider<T> {
        private final ItemStackRenderState scratchRenderState = new ItemStackRenderState();

        protected TextureAtlasSprite getSprite(final ItemStackTemplate item, final ClientLevel level, final RandomSource random) {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(this.scratchRenderState, item.create(), ItemDisplayContext.GROUND, level, null, 0);
            Material.Baked material = this.scratchRenderState.pickParticleMaterial(random);
            return material != null ? material.sprite() : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider extends BreakingItemParticle.ItemParticleProvider<ItemParticleOption> {
        public Particle createParticle(
            final ItemParticleOption options,
            final ClientLevel level,
            final double x,
            final double y,
            final double z,
            final double xAux,
            final double yAux,
            final double zAux,
            final RandomSource random
        ) {
            return new BreakingItemParticle(level, x, y, z, xAux, yAux, zAux, this.getSprite(options.getItem(), level, random));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SlimeProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
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
            return new BreakingItemParticle(level, x, y, z, this.getSprite(new ItemStackTemplate(Items.SLIME_BALL), level, random));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SnowballProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
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
            return new BreakingItemParticle(level, x, y, z, this.getSprite(new ItemStackTemplate(Items.SNOWBALL), level, random));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SulfurCubeProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SulfurCubeProvider(final SpriteSet sprites) {
            this.sprites = sprites;
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
            return new BreakingItemParticle(level, x, y, z, this.sprites.first());
        }
    }
}