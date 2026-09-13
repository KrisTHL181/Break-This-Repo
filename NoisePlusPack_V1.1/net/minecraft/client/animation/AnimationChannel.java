package net.minecraft.client.animation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record AnimationChannel(AnimationChannel.Target target, Keyframe... keyframes) {
   @OnlyIn(Dist.CLIENT)
   public interface Interpolation {
      Vector3f apply(Vector3f var1, float var2, Keyframe[] var3, int var4, int var5, float var6);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Interpolations {
      public static final AnimationChannel.Interpolation LINEAR = (p_420672_, p_420673_, p_420674_, p_420675_, p_420676_, p_420677_) -> {
         Vector3fc vector3fc = p_420674_[p_420675_].postTarget();
         Vector3fc vector3fc1 = p_420674_[p_420676_].preTarget();
         return vector3fc.lerp(vector3fc1, p_420673_, p_420672_).mul(p_420677_);
      };
      public static final AnimationChannel.Interpolation CATMULLROM = (p_254076_, p_232235_, p_232236_, p_232237_, p_232238_, p_232239_) -> {
         Vector3fc vector3fc = p_232236_[Math.max(0, p_232237_ - 1)].postTarget();
         Vector3fc vector3fc1 = p_232236_[p_232237_].postTarget();
         Vector3fc vector3fc2 = p_232236_[p_232238_].postTarget();
         Vector3fc vector3fc3 = p_232236_[Math.min(p_232236_.length - 1, p_232238_ + 1)].postTarget();
         p_254076_.set(
            Mth.catmullrom(p_232235_, vector3fc.x(), vector3fc1.x(), vector3fc2.x(), vector3fc3.x()) * p_232239_,
            Mth.catmullrom(p_232235_, vector3fc.y(), vector3fc1.y(), vector3fc2.y(), vector3fc3.y()) * p_232239_,
            Mth.catmullrom(p_232235_, vector3fc.z(), vector3fc1.z(), vector3fc2.z(), vector3fc3.z()) * p_232239_
         );
         return p_254076_;
      };
   }

   @OnlyIn(Dist.CLIENT)
   public interface Target {
      void apply(ModelPart var1, Vector3f var2);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Targets {
      public static final AnimationChannel.Target POSITION = ModelPart::offsetPos;
      public static final AnimationChannel.Target ROTATION = ModelPart::offsetRotation;
      public static final AnimationChannel.Target SCALE = ModelPart::offsetScale;
   }
}
