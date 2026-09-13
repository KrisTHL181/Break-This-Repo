package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockMarker extends SingleQuadParticle {
   private final SingleQuadParticle.Layer layer;

   BlockMarker(ClientLevel p_194267_, double p_194268_, double p_194269_, double p_194270_, BlockState p_194271_) {
      super(p_194267_, p_194268_, p_194269_, p_194270_, Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(p_194271_));
      this.gravity = 0.0F;
      this.lifetime = 80;
      this.hasPhysics = false;
      this.layer = this.sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? SingleQuadParticle.Layer.TERRAIN : SingleQuadParticle.Layer.ITEMS;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return this.layer;
   }

   @Override
   public float getQuadSize(float p_194274_) {
      return 0.5F;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<BlockParticleOption> {
      public Particle createParticle(
         BlockParticleOption p_194277_,
         ClientLevel p_194278_,
         double p_194279_,
         double p_194280_,
         double p_194281_,
         double p_194282_,
         double p_194283_,
         double p_194284_,
         RandomSource p_425675_
      ) {
         return new BlockMarker(p_194278_, p_194279_, p_194280_, p_194281_, p_194277_.getState());
      }
   }
}
