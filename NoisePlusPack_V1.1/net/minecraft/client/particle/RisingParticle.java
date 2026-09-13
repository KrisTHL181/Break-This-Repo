package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RisingParticle extends SingleQuadParticle {
   protected RisingParticle(
      ClientLevel p_107631_,
      double p_107632_,
      double p_107633_,
      double p_107634_,
      double p_107635_,
      double p_107636_,
      double p_107637_,
      TextureAtlasSprite p_430814_
   ) {
      super(p_107631_, p_107632_, p_107633_, p_107634_, p_107635_, p_107636_, p_107637_, p_430814_);
      this.friction = 0.96F;
      this.xd = this.xd * 0.01F + p_107635_;
      this.yd = this.yd * 0.01F + p_107636_;
      this.zd = this.zd * 0.01F + p_107637_;
      this.x = this.x + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
      this.y = this.y + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
      this.z = this.z + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
      this.lifetime = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.2)) + 4;
   }
}
