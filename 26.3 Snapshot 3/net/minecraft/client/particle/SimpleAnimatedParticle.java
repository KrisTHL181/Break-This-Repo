package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;

public abstract class SimpleAnimatedParticle extends SingleQuadParticle {
   protected final SpriteSet sprites;
   private float fadeR;
   private float fadeG;
   private float fadeB;
   private boolean hasFade;

   protected SimpleAnimatedParticle(final ClientLevel level, final double x, final double y, final double z, final SpriteSet sprites, final float gravity) {
      super(level, x, y, z, sprites.first());
      this.friction = 0.91F;
      this.gravity = gravity;
      this.sprites = sprites;
   }

   public void setColor(final int rgb) {
      float r = ((rgb & 0xFF0000) >> 16) / 255.0F;
      float g = ((rgb & 0xFF00) >> 8) / 255.0F;
      float b = ((rgb & 0xFF) >> 0) / 255.0F;
      float scale = 1.0F;
      this.setColor(r * 1.0F, g * 1.0F, b * 1.0F);
   }

   public void setFadeColor(final int rgb) {
      this.fadeR = ((rgb & 0xFF0000) >> 16) / 255.0F;
      this.fadeG = ((rgb & 0xFF00) >> 8) / 255.0F;
      this.fadeB = ((rgb & 0xFF) >> 0) / 255.0F;
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
   public int getLightCoords(final float a) {
      return 15728880;
   }
}
