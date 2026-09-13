package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreakingItemParticle extends SingleQuadParticle {
   private final float uo;
   private final float vo;
   private final SingleQuadParticle.Layer layer;

   BreakingItemParticle(
      ClientLevel p_105646_,
      double p_105647_,
      double p_105648_,
      double p_105649_,
      double p_105650_,
      double p_105651_,
      double p_105652_,
      TextureAtlasSprite p_428284_
   ) {
      this(p_105646_, p_105647_, p_105648_, p_105649_, p_428284_);
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      this.xd += p_105650_;
      this.yd += p_105651_;
      this.zd += p_105652_;
   }

   protected BreakingItemParticle(ClientLevel p_105665_, double p_105666_, double p_105667_, double p_105668_, TextureAtlasSprite p_427941_) {
      super(p_105665_, p_105666_, p_105667_, p_105668_, 0.0, 0.0, 0.0, p_427941_);
      this.gravity = 1.0F;
      this.quadSize /= 2.0F;
      this.uo = this.random.nextFloat() * 3.0F;
      this.vo = this.random.nextFloat() * 3.0F;
      this.layer = p_427941_.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? SingleQuadParticle.Layer.TERRAIN : SingleQuadParticle.Layer.ITEMS;
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
         SimpleParticleType p_329960_,
         ClientLevel p_334942_,
         double p_332141_,
         double p_335808_,
         double p_331451_,
         double p_330404_,
         double p_335788_,
         double p_329792_,
         RandomSource p_424308_
      ) {
         return new BreakingItemParticle(p_334942_, p_332141_, p_335808_, p_331451_, this.getSprite(new ItemStack(Items.COBWEB), p_334942_, p_424308_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class ItemParticleProvider<T extends ParticleOptions> implements ParticleProvider<T> {
      private final ItemStackRenderState scratchRenderState = new ItemStackRenderState();

      protected TextureAtlasSprite getSprite(ItemStack p_422701_, ClientLevel p_425581_, RandomSource p_422331_) {
         Minecraft.getInstance().getItemModelResolver().updateForTopItem(this.scratchRenderState, p_422701_, ItemDisplayContext.GROUND, p_425581_, null, 0);
         TextureAtlasSprite textureatlassprite = this.scratchRenderState.pickParticleIcon(p_422331_);
         return textureatlassprite != null ? textureatlassprite : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider extends BreakingItemParticle.ItemParticleProvider<ItemParticleOption> {
      public Particle createParticle(
         ItemParticleOption p_424773_,
         ClientLevel p_105687_,
         double p_105688_,
         double p_105689_,
         double p_105690_,
         double p_105691_,
         double p_105692_,
         double p_105693_,
         RandomSource p_428906_
      ) {
         return new BreakingItemParticle(
            p_105687_, p_105688_, p_105689_, p_105690_, p_105691_, p_105692_, p_105693_, this.getSprite(p_424773_.getItem(), p_105687_, p_428906_)
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SlimeProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         SimpleParticleType p_105705_,
         ClientLevel p_105706_,
         double p_105707_,
         double p_105708_,
         double p_105709_,
         double p_105710_,
         double p_105711_,
         double p_105712_,
         RandomSource p_426894_
      ) {
         return new BreakingItemParticle(p_105706_, p_105707_, p_105708_, p_105709_, this.getSprite(new ItemStack(Items.SLIME_BALL), p_105706_, p_426894_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SnowballProvider extends BreakingItemParticle.ItemParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         SimpleParticleType p_105724_,
         ClientLevel p_105725_,
         double p_105726_,
         double p_105727_,
         double p_105728_,
         double p_105729_,
         double p_105730_,
         double p_105731_,
         RandomSource p_426355_
      ) {
         return new BreakingItemParticle(p_105725_, p_105726_, p_105727_, p_105728_, this.getSprite(new ItemStack(Items.SNOWBALL), p_105725_, p_426355_));
      }
   }
}
