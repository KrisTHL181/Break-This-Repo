package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.AnimationState;

public class SnifferRenderState extends LivingEntityRenderState {
   public boolean isSearching;
   public final AnimationState diggingAnimationState = new AnimationState();
   public final AnimationState sniffingAnimationState = new AnimationState();
   public final AnimationState risingAnimationState = new AnimationState();
   public final AnimationState feelingHappyAnimationState = new AnimationState();
   public final AnimationState scentingAnimationState = new AnimationState();
}
