package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class WeatheringCopperDoorBlock extends DoorBlock implements WeatheringCopper {
   private final WeatheringCopper.WeatherState weatherState;

   protected WeatheringCopperDoorBlock(final BlockSetType type, final WeatheringCopper.WeatherState weatherState, final BlockBehaviour.Properties properties) {
      super(type, properties);
      this.weatherState = weatherState;
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
         this.changeOverTime(state, level, pos, random);
      }
   }

   @Override
   protected boolean isRandomlyTicking(final BlockState state) {
      return WeatheringCopper.getNext(state.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
