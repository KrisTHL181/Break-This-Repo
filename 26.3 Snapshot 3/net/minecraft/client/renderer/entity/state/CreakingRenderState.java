package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.AnimationState;

public class CreakingRenderState extends LivingEntityRenderState {
   public final AnimationState invulnerabilityAnimationState = new AnimationState();
   public final AnimationState attackAnimationState = new AnimationState();
   public final AnimationState deathAnimationState = new AnimationState();
   public boolean eyesGlowing;
   public boolean canMove;
}
