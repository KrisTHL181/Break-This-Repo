package net.minecraft.client.animation.definitions;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class FoxBabyAnimation {
   public static final AnimationDefinition FOX_BABY_WALK = AnimationDefinition.Builder.withLength(0.5F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.025F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.05F, 0.6F, -0.02F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.25F, KeyframeAnimations.posVec(0.05F, 0.6F, -0.02F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(0.05F, 0.6F, -0.02F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.SCALE, new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.15F, 1.0), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-0.05F, 0.6F, -0.02F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.25F, KeyframeAnimations.posVec(-0.05F, 0.6F, -0.02F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(-0.05F, 0.6F, -0.02F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.SCALE, new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.15F, 1.0), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.05F, 0.6F, -0.4F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.SCALE, new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.15F, 1.0), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(-0.05F, 0.6F, -0.4F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.SCALE, new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.15F, 1.0), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.05F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .build();
}
