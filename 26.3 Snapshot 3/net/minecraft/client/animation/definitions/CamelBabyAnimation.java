package net.minecraft.client.animation.definitions;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class CamelBabyAnimation {
   public static final AnimationDefinition CAMEL_BABY_WALK = AnimationDefinition.Builder.withLength(1.5F)
      .looping()
      .addAnimation(
         "root",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.5F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.125F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.1F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.075F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.75F, KeyframeAnimations.posVec(0.075F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.2083F, KeyframeAnimations.posVec(0.075F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.posVec(0.075F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4583F, KeyframeAnimations.posVec(-0.1F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.75F, KeyframeAnimations.posVec(-0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.posVec(-0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(-9.49F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-17.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.2083F, KeyframeAnimations.degreeVec(7.38F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.25F, KeyframeAnimations.posVec(-0.1F, 5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5833F, KeyframeAnimations.posVec(-0.1F, 0.0F, -0.1F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.posVec(-0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-15.83F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-7.38F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.25F, KeyframeAnimations.degreeVec(-21.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(-15.83F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6667F, KeyframeAnimations.posVec(0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.0F, KeyframeAnimations.posVec(0.1F, 4.0F, 0.17F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.2083F, KeyframeAnimations.posVec(0.1F, 0.0F, -0.11F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.posVec(0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(15.94F, -8.42F, 20.94F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.75F, KeyframeAnimations.degreeVec(15.94F, 8.42F, -20.94F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(15.94F, -8.42F, 20.94F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.6F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -0.6F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition CAMEL_BABY_STANDUP = AnimationDefinition.Builder.withLength(2.6F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-12.6F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -13.25F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, -13.25F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, -11.52F, -0.85F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(0.0F, -9.4335F, -1.6246F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, -9.15F, -1.58F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.posVec(0.0F, -8.5F, -1.3F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.posVec(0.0F, -1.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.3F, KeyframeAnimations.posVec(0.0F, -0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.degreeVec(-75.21F, 9.37F, 2.45F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3F, KeyframeAnimations.degreeVec(-70.21F, 9.37F, 2.45F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.degreeVec(-40.55F, 8.68F, 1.23F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-40.55F, 8.68F, 1.23F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(-20.45F, 7.61F, 0.74F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(-20.45F, 7.61F, 0.74F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(-20.45F, 7.61F, 0.74F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.1F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.posVec(0.1F, -8.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3F, KeyframeAnimations.posVec(-0.12F, -7.18F, 5.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.posVec(-0.9942F, -2.3474F, 2.2929F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-0.9942F, -2.3474F, 2.2929F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.posVec(-0.72F, -0.6F, 1.71F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.posVec(-0.72F, -0.6F, 1.71F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.posVec(-0.72F, -0.6F, 1.71F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.posVec(-0.21F, -0.55F, 1.05F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8F, KeyframeAnimations.posVec(-0.21F, 0.73F, 0.52F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.posVec(0.05F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1F, KeyframeAnimations.posVec(0.05F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.2F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.degreeVec(-74.2F, -9.3F, -2.45F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3F, KeyframeAnimations.degreeVec(-69.2F, -9.32F, -2.4F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.degreeVec(-46.75F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-46.75F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(-24.31F, -9.2F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(-24.31F, -9.2F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.degreeVec(-24.31F, -9.2F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.degreeVec(-12.15F, -4.6F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(-12.15F, -4.6F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, -7.89F, 4.96F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, -6.89F, 4.96F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.posVec(0.65F, -2.98F, 3.56F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(0.65F, -2.98F, 3.56F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.posVec(0.5F, -0.69F, 2.47F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.posVec(0.5F, -0.69F, 2.47F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.posVec(0.5F, -0.69F, 2.47F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.posVec(0.4F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1F, KeyframeAnimations.posVec(0.2F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.2F, KeyframeAnimations.posVec(-0.025F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.posVec(-0.02F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, -12.0F, -0.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.degreeVec(-90.0F, -12.0F, -0.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-89.79F, -17.4F, -0.8F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.degreeVec(-89.38F, -11.59F, -0.99F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(-89.09F, -7.49F, -0.98F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-88.5F, -4.3F, -1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(-79.5F, -4.3F, -1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(-66.11F, -4.19F, -1.05F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(-47.51F, -4.19F, -1.05F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.degreeVec(-35.11F, -4.19F, -1.05F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8F, KeyframeAnimations.degreeVec(-20.63F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(1.5F, -10.949F, 1.9507F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.posVec(1.5F, -10.949F, 1.9507F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(1.0F, -11.0F, 1.75F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.posVec(0.16F, -11.11F, 0.49F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(-0.81F, -10.95F, -0.02F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-0.77F, -10.8F, -0.94F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.posVec(-0.77F, -8.7F, -0.94F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(-1.6F, -6.43F, -1.59F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.posVec(-1.36F, -3.37F, -1.65F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.posVec(-1.2F, -1.33F, -1.69F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8F, KeyframeAnimations.posVec(-0.6F, 2.0F, -0.3F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.posVec(-0.2F, 1.9F, -1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 12.0F, 0.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.degreeVec(-90.0F, 12.0F, 0.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-90.0F, 17.5F, 1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.degreeVec(-90.0F, 8.5F, 0.73F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(-90.0F, 3.0F, 0.37F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-75.38F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(-56.38F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(-30.15F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(3.6F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-1.3F, -11.0F, 1.95F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.posVec(-1.3F, -11.0F, 1.95F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(-0.7F, -10.9F, 1.8F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.posVec(-0.06F, -11.2F, 0.21F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(0.39F, -11.2F, -0.23F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(0.24F, -8.0F, -1.27F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(0.34F, -4.53F, -0.8F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.posVec(0.3F, -1.88F, -0.42F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.posVec(0.25F, 0.18F, 0.3F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.posVec(0.25F, 1.48F, -1.75F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.posVec(0.1F, 0.0F, -0.4F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.posVec(0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.degreeVec(5.65F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(46.68F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(37.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.8368F, 1.9553F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8F, KeyframeAnimations.posVec(0.0F, -0.95F, 1.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.posVec(0.0F, -0.95F, 1.42F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, -0.95F, 1.42F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(46.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.degreeVec(46.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3F, KeyframeAnimations.degreeVec(39.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7F, KeyframeAnimations.degreeVec(47.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.degreeVec(49.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(52.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(50.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.degreeVec(42.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(1.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -23.75F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -23.75F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 67.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(2.3F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.4F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -55.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(2.3F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.4F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition CAMEL_BABY_DASH = AnimationDefinition.Builder.withLength(0.5F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -2.8F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(67.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.125F, KeyframeAnimations.degreeVec(112.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(67.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(112.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(67.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(44.9727F, 1.7675F, -1.7683F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.125F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(44.9727F, 1.7675F, -1.7683F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(44.9727F, 1.7675F, -1.7683F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.05F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.125F, KeyframeAnimations.degreeVec(44.9727F, -1.7675F, 1.7683F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(44.9727F, -1.7675F, 1.7683F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(-0.05F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.125F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(-0.05F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.125F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.375F, KeyframeAnimations.degreeVec(90.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.05F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -67.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -67.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 67.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 67.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition CAMEL_BABY_IDLE = AnimationDefinition.Builder.withLength(4.0F)
      .looping()
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(4.9811F, 0.4352F, -4.9811F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(3.0F, KeyframeAnimations.degreeVec(4.9872F, -0.2942F, 3.3674F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(4.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.875F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(2.875F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM)
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition CAMEL_BABY_SIT = AnimationDefinition.Builder.withLength(2.0F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4F, KeyframeAnimations.degreeVec(9.26F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6F, KeyframeAnimations.degreeVec(18.15F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(21.14F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, -1.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4F, KeyframeAnimations.posVec(0.0F, -2.21F, 0.04F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6F, KeyframeAnimations.posVec(0.0F, -2.21F, 0.04F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.posVec(0.0F, -5.23F, 0.33F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.posVec(0.0F, -6.06F, 0.17F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(0.0F, -11.7F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.posVec(0.0F, -13.35F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.posVec(0.0F, -13.25F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.posVec(0.0F, -13.25F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, -13.25F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7F, KeyframeAnimations.degreeVec(-23.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.degreeVec(-44.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(-73.25F, 6.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(-90.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.degreeVec(-90.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.125F, -0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(0.125F, -0.1F, 0.4F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6F, KeyframeAnimations.posVec(0.12F, -0.15F, 2.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7F, KeyframeAnimations.posVec(0.12F, -0.6F, 4.6F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(0.12F, -0.87F, 5.97F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(0.125F, -1.0F, 6.9F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.posVec(0.325F, -2.6F, 7.6F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(-0.28F, -7.45F, 8.06F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.posVec(0.1F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.posVec(0.1F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.1F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7F, KeyframeAnimations.degreeVec(-23.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.degreeVec(-45.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(-73.5F, -6.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(-90.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.degreeVec(-90.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-0.125F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(-0.125F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6F, KeyframeAnimations.posVec(-0.13F, -0.18F, 2.35F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7F, KeyframeAnimations.posVec(-0.13F, -0.57F, 4.7F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(-0.13F, -0.86F, 6.07F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0F, KeyframeAnimations.posVec(-0.125F, -1.0F, 6.8F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.posVec(-0.22F, -2.7F, 7.8F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(0.19F, -7.5F, 8.01F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.posVec(0.0F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7F, KeyframeAnimations.posVec(0.0F, -10.9F, 6.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.degreeVec(-31.47F, -0.45F, 1.29F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(-49.78F, -10.29F, -1.15F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(-79.21F, -13.72F, -1.53F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(-90.0F, -11.88F, -0.54F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, -11.88F, -0.54F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(-0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(-0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(-0.1F, 0.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.posVec(0.56F, -3.49F, 2.18F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(1.22F, -8.72F, 3.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.posVec(1.4F, -10.95F, 1.95F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(1.4F, -10.95F, 1.95F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.degreeVec(-31.13F, 0.56F, -1.3F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(-49.8F, 10.3F, 1.15F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(-79.21F, 13.72F, 1.53F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(-90.0F, 11.89F, 0.54F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(-90.0F, 11.89F, 0.54F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5F, KeyframeAnimations.posVec(0.1F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(0.1F, 0.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.posVec(0.2F, -0.75F, 1.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.posVec(-0.51F, -3.49F, 2.18F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(-1.22F, -8.72F, 3.1F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.posVec(-1.33F, -11.0F, 1.95F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(-1.33F, -11.0F, 1.95F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4F, KeyframeAnimations.degreeVec(-20.42F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(-31.25F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.degreeVec(-26.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(-19.25F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(-14.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(-4.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(-4.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(-4.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4F, KeyframeAnimations.posVec(0.0F, 0.38F, 0.44F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.posVec(0.0F, 0.35F, 0.88F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1F, KeyframeAnimations.posVec(0.0F, -0.3F, 1.21F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.posVec(0.0F, -0.3F, 1.22F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.22F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.9F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(62.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe(1.4F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.8F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -62.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 20.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(1.2F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 60.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -20.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(1.2F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .build();
   public static final AnimationDefinition CAMEL_BABY_SIT_POSE = AnimationDefinition.Builder.withLength(0.0F)
      .looping()
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "body",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -13.25F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.1F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_front_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -11.0F, 6.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, -11.88F, -0.54F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(1.4F, -10.95F, 1.95F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(-90.0F, 11.89F, 0.54F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_hind_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(-1.33F, -11.0F, 1.95F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.75F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.22F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "tail",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "left_ear",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION, new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "right_ear",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "root",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION, new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .addAnimation(
         "root",
         new AnimationChannel(
            AnimationChannel.Targets.SCALE, new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
         )
      )
      .build();
}
