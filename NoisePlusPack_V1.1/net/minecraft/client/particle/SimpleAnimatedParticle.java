package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class SimpleAnimatedParticle extends SingleQuadParticle {
   protected final SpriteSet sprites;
   private float fadeR;
   private float fadeG;
   private float fadeB;
   private boolean hasFade;

   protected SimpleAnimatedParticle(ClientLevel p_107647_, double p_107648_, double p_107649_, double p_107650_, SpriteSet p_107651_, float p_107652_) {
      super(p_107647_, p_107648_, p_107649_, p_107650_, p_107651_.first());
      this.friction = 0.91F;
      this.gravity = p_107652_;
      this.sprites = p_107651_;
   }

   public void setColor(int p_107658_) {
      float f = ((p_107658_ & 0xFF0000) >> 16) / 255.0F;
      float f1 = ((p_107658_ & 0xFF00) >> 8) / 255.0F;
      float f2 = ((p_107658_ & 0xFF) >> 0) / 255.0F;
      float f3 = 1.0F;
      this.setColor(f * 1.0F, f1 * 1.0F, f2 * 1.0F);
   }

   public void setFadeColor(int p_107660_) {
      this.fadeR = ((p_107660_ & 0xFF0000) >> 16) / 255.0F;
      this.fadeG = ((p_107660_ & 0xFF00) >> 8) / 255.0F;
      this.fadeB = ((p_107660_ & 0xFF) >> 0) / 255.0F;
      this.hasFade = true;
   }

   @Override
   public SingleQuadParticle.Layer getLayer() {
      return SingleQuadParticle.Layer.TRANSLUCENT;
   }

   @Override
   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      if (this.age > this.lifetime / 2) {
         this.setAlpha(1.0F - ((float)this.age - this.lifetime / 2) / this.lifetime);
         if (this.hasFade) {
            this.rCol = this.rCol + (this.fadeR - this.rCol) * 0.2F;
            this.gCol = this.gCol + (this.fadeG - this.gCol) * 0.2F;
            this.bCol = this.bCol + (this.fadeB - this.bCol) * 0.2F;
         }
      }
   }

   @Override
   public int getLightColor(float p_107655_) {
      return 15728880;
   }
}
