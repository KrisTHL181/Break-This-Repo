package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperBulbBlock extends CopperBulbBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperBulbBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422140_ -> p_422140_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperBulbBlock::getAge), propertiesCodec()
         )
         .apply(p_422140_, WeatheringCopperBulbBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   protected MapCodec<WeatheringCopperBulbBlock> codec() {
      return CODEC;
   }

   public WeatheringCopperBulbBlock(WeatheringCopper.WeatherState p_309695_, BlockBehaviour.Properties p_311798_) {
      super(p_311798_);
      this.weatherState = p_309695_;
   }

   @Override
   protected void randomTick(BlockState p_311293_, ServerLevel p_312278_, BlockPos p_309441_, RandomSource p_312720_) {
      this.changeOverTime(p_311293_, p_312278_, p_309441_, p_312720_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_310542_) {
      return WeatheringCopper.getNext(p_310542_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
