package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringLanternBlock extends LanternBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringLanternBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_430354_ -> p_430354_.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringLanternBlock::getAge), propertiesCodec())
         .apply(p_430354_, WeatheringLanternBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<WeatheringLanternBlock> codec() {
      return CODEC;
   }

   protected WeatheringLanternBlock(WeatheringCopper.WeatherState p_426575_, BlockBehaviour.Properties p_424036_) {
      super(p_424036_);
      this.weatherState = p_426575_;
   }

   @Override
   protected void randomTick(BlockState p_430392_, ServerLevel p_422997_, BlockPos p_423656_, RandomSource p_422400_) {
      this.changeOverTime(p_430392_, p_422997_, p_423656_, p_422400_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_425418_) {
      return WeatheringCopper.getNext(p_425418_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
