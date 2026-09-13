package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperBarsBlock extends IronBarsBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperBarsBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_423092_ -> p_423092_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperBarsBlock::getAge), propertiesCodec()
         )
         .apply(p_423092_, WeatheringCopperBarsBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<WeatheringCopperBarsBlock> codec() {
      return CODEC;
   }

   protected WeatheringCopperBarsBlock(WeatheringCopper.WeatherState p_422730_, BlockBehaviour.Properties p_429003_) {
      super(p_429003_);
      this.weatherState = p_422730_;
   }

   @Override
   protected void randomTick(BlockState p_429656_, ServerLevel p_431227_, BlockPos p_425121_, RandomSource p_427994_) {
      this.changeOverTime(p_429656_, p_431227_, p_425121_, p_427994_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_424486_) {
      return WeatheringCopper.getNext(p_424486_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
