package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IllagerRenderState extends UndeadRenderState {
    public boolean isRiding;
    public boolean isAggressive;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public AbstractIllager.IllagerArmPose armPose = AbstractIllager.IllagerArmPose.NEUTRAL;
    public int maxCrossbowChargeDuration;
    public float ticksUsingItem;
    public float attackAnim;
}