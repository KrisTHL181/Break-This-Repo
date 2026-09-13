package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperFullBlock extends Block implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperFullBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422142_ -> p_422142_.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge), propertiesCodec())
         .apply(p_422142_, WeatheringCopperFullBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<WeatheringCopperFullBlock> codec() {
      return CODEC;
   }

   public WeatheringCopperFullBlock(WeatheringCopper.WeatherState p_154925_, BlockBehaviour.Properties p_154926_) {
      super(p_154926_);
      this.weatherState = p_154925_;
   }

   @Override
   protected void randomTick(BlockState p_222665_, ServerLevel p_222666_, BlockPos p_222667_, RandomSource p_222668_) {
      this.changeOverTime(p_222665_, p_222666_, p_222667_, p_222668_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_154935_) {
      return WeatheringCopper.getNext(p_154935_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
