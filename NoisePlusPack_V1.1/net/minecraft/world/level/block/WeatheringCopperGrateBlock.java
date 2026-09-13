package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperGrateBlock extends WaterloggedTransparentBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperGrateBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422143_ -> p_422143_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperGrateBlock::getAge), propertiesCodec()
         )
         .apply(p_422143_, WeatheringCopperGrateBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   protected MapCodec<WeatheringCopperGrateBlock> codec() {
      return CODEC;
   }

   protected WeatheringCopperGrateBlock(WeatheringCopper.WeatherState p_311827_, BlockBehaviour.Properties p_311858_) {
      super(p_311858_);
      this.weatherState = p_311827_;
   }

   @Override
   protected void randomTick(BlockState p_309962_, ServerLevel p_309911_, BlockPos p_311585_, RandomSource p_310772_) {
      this.changeOverTime(p_309962_, p_309911_, p_311585_, p_310772_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_310531_) {
      return WeatheringCopper.getNext(p_310531_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
