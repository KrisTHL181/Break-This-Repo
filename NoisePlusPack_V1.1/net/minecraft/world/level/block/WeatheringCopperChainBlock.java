package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperChainBlock extends ChainBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperChainBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422585_ -> p_422585_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperChainBlock::getAge), propertiesCodec()
         )
         .apply(p_422585_, WeatheringCopperChainBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<WeatheringCopperChainBlock> codec() {
      return CODEC;
   }

   protected WeatheringCopperChainBlock(WeatheringCopper.WeatherState p_425385_, BlockBehaviour.Properties p_427058_) {
      super(p_427058_);
      this.weatherState = p_425385_;
   }

   @Override
   protected void randomTick(BlockState p_430151_, ServerLevel p_427523_, BlockPos p_426700_, RandomSource p_430621_) {
      this.changeOverTime(p_430151_, p_427523_, p_426700_, p_430621_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_426056_) {
      return WeatheringCopper.getNext(p_426056_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
