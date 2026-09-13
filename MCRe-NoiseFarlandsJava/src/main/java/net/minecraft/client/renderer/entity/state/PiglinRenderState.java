package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PiglinRenderState extends HumanoidRenderState {
    public boolean isBrute;
    public boolean isConverting;
    public float maxCrossbowChageDuration;
    public PiglinArmPose armPose = PiglinArmPose.DEFAULT;
}