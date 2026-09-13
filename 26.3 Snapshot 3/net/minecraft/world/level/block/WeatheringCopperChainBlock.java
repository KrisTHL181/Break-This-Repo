package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperChainBlock extends ChainBlock implements WeatheringCopper {
   private final WeatheringCopper.WeatherState weatherState;

   protected WeatheringCopperChainBlock(final WeatheringCopper.WeatherState weatherState, final BlockBehaviour.Properties properties) {
      super(properties);
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
