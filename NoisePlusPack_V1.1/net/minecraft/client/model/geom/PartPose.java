package net.minecraft.client.model.geom;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PartPose(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
   public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

   public static PartPose offset(float p_171420_, float p_171421_, float p_171422_) {
      return offsetAndRotation(p_171420_, p_171421_, p_171422_, 0.0F, 0.0F, 0.0F);
   }

   public static PartPose rotation(float p_171431_, float p_171432_, float p_171433_) {
      return offsetAndRotation(0.0F, 0.0F, 0.0F, p_171431_, p_171432_, p_171433_);
   }

   public static PartPose offsetAndRotation(float p_171424_, float p_171425_, float p_171426_, float p_171427_, float p_171428_, float p_171429_) {
      return new PartPose(p_171424_, p_171425_, p_171426_, p_171427_, p_171428_, p_171429_, 1.0F, 1.0F, 1.0F);
   }

   public PartPose translated(float p_361488_, float p_366991_, float p_369868_) {
      return new PartPose(this.x + p_361488_, this.y + p_366991_, this.z + p_369868_, this.xRot, this.yRot, this.zRot, this.xScale, this.yScale, this.zScale);
   }

   public PartPose withScale(float p_361962_) {
      return new PartPose(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot, p_361962_, p_361962_, p_361962_);
   }

   public PartPose scaled(float p_369914_) {
      return p_369914_ == 1.0F ? this : this.scaled(p_369914_, p_369914_, p_369914_);
   }

   public PartPose scaled(float p_367285_, float p_364743_, float p_366216_) {
      return new PartPose(
         this.x * p_367285_,
         this.y * p_364743_,
         this.z * p_366216_,
         this.xRot,
         this.yRot,
         this.zRot,
         this.xScale * p_367285_,
         this.yScale * p_364743_,
         this.zScale * p_366216_
      );
   }
}
