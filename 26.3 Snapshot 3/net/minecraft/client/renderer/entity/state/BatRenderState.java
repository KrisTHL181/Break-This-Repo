package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.AnimationState;

public class BatRenderState extends LivingEntityRenderState {
   public boolean isResting;
   public final AnimationState flyAnimationState = new AnimationState();
   public final AnimationState restAnimationState = new AnimationState();
}
