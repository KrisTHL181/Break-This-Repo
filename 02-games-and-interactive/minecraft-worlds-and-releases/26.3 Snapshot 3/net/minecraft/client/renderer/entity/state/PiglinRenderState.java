package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.monster.piglin.PiglinArmPose;

public class PiglinRenderState extends HumanoidRenderState {
   public boolean isBrute;
   public boolean isConverting;
   public float maxCrossbowChageDuration;
   public PiglinArmPose armPose = PiglinArmPose.DEFAULT;
}
