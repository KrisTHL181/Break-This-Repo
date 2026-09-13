package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimation {
   private final AnimationDefinition definition;
   private final List<KeyframeAnimation.Entry> entries;

   private KeyframeAnimation(AnimationDefinition p_409549_, List<KeyframeAnimation.Entry> p_405859_) {
      this.definition = p_409549_;
      this.entries = p_405859_;
   }

   static KeyframeAnimation bake(ModelPart p_407622_, AnimationDefinition p_407507_) {
      List<KeyframeAnimation.Entry> list = new ArrayList<>();
      Function<String, ModelPart> function = p_407622_.createPartLookup();

      for (Map.Entry<String, List<AnimationChannel>> entry : p_407507_.boneAnimations().entrySet()) {
         String s = entry.getKey();
         List<AnimationChannel> list1 = entry.getValue();
         ModelPart modelpart = function.apply(s);
         if (modelpart == null) {
            throw new IllegalArgumentException("Cannot animate " + s + ", which does not exist in model");
         }

         for (AnimationChannel animationchannel : list1) {
            list.add(new KeyframeAnimation.Entry(modelpart, animationchannel.target(), animationchannel.keyframes()));
         }
      }

      return new KeyframeAnimation(p_407507_, List.copyOf(list));
   }

   public void applyStatic() {
      this.apply(0L, 1.0F);
   }

   public void applyWalk(float p_407973_, float p_405934_, float p_406901_, float p_408620_) {
      long i = (long)(p_407973_ * 50.0F * p_406901_);
      float f = Math.min(p_405934_ * p_408620_, 1.0F);
      this.apply(i, f);
   }

   public void apply(AnimationState p_409774_, float p_405945_) {
      this.apply(p_409774_, p_405945_, 1.0F);
   }

   public void apply(AnimationState p_406283_, float p_408260_, float p_409294_) {
      p_406283_.ifStarted(p_408975_ -> this.apply((long)((float)p_408975_.getTimeInMillis(p_408260_) * p_409294_), 1.0F));
   }

   public void apply(long p_409599_, float p_408046_) {
      float f = this.getElapsedSeconds(p_409599_);
      Vector3f vector3f = new Vector3f();

      for (KeyframeAnimation.Entry keyframeanimation$entry : this.entries) {
         keyframeanimation$entry.apply(f, p_408046_, vector3f);
      }
   }

   private float getElapsedSeconds(long p_410209_) {
      float f = (float)p_410209_ / 1000.0F;
      return this.definition.looping() ? f % this.definition.lengthInSeconds() : f;
   }

   @OnlyIn(Dist.CLIENT)
   record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
      public void apply(float p_409122_, float p_408445_, Vector3f p_410397_) {
         int i = Math.max(0, Mth.binarySearch(0, this.keyframes.length, p_406117_ -> p_409122_ <= this.keyframes[p_406117_].timestamp()) - 1);
         int j = Math.min(this.keyframes.length - 1, i + 1);
         Keyframe keyframe = this.keyframes[i];
         Keyframe keyframe1 = this.keyframes[j];
         float f = p_409122_ - keyframe.timestamp();
         float f1;
         if (j != i) {
            f1 = Mth.clamp(f / (keyframe1.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
         } else {
            f1 = 0.0F;
         }

         keyframe1.interpolation().apply(p_410397_, f1, this.keyframes, i, j, p_408445_);
         this.target.apply(this.part, p_410397_);
      }
   }
}
