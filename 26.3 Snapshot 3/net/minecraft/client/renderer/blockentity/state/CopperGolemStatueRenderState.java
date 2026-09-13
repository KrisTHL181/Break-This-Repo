package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;

public class CopperGolemStatueRenderState extends BlockEntityRenderState {
   public CopperGolemStatueBlock.Pose pose = CopperGolemStatueBlock.Pose.STANDING;
   public Direction direction = Direction.NORTH;
   public WeatheringCopper.WeatherState oxidationState = WeatheringCopper.WeatherState.UNAFFECTED;
}
