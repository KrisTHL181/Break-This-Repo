package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.level.block.WeatheringCopper;

public class CopperGolemRenderState extends ArmedEntityRenderState {
   public WeatheringCopper.WeatherState weathering = WeatheringCopper.WeatherState.UNAFFECTED;
   public CopperGolemState copperGolemState = CopperGolemState.IDLE;
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState interactionGetItem = new AnimationState();
   public final AnimationState interactionGetNoItem = new AnimationState();
   public final AnimationState interactionDropItem = new AnimationState();
   public final AnimationState interactionDropNoItem = new AnimationState();
   public final BlockModelRenderState blockOnAntenna = new BlockModelRenderState();
}
