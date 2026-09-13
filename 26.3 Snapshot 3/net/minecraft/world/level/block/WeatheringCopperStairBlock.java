package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperStairBlock extends StairBlock implements WeatheringCopper {
   private final WeatheringCopper.WeatherState weatherState;

   public WeatheringCopperStairBlock(final WeatheringCopper.WeatherState weatherState, final BlockState baseState, final BlockBehaviour.Properties properties) {
      super(baseState, properties);
      this.weatherState = weatherState;
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      this.changeOverTime(state, level, pos, random);
   }

   @Override
   protected boolean isRandomlyTicking(final BlockState state) {
      return WeatheringCopper.getNext(state.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
