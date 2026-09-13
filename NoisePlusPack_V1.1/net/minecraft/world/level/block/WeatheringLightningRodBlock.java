package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringLightningRodBlock extends LightningRodBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringLightningRodBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_429977_ -> p_429977_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringLightningRodBlock::getAge), propertiesCodec()
         )
         .apply(p_429977_, WeatheringLightningRodBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<WeatheringLightningRodBlock> codec() {
      return CODEC;
   }

   protected WeatheringLightningRodBlock(WeatheringCopper.WeatherState p_425930_, BlockBehaviour.Properties p_425157_) {
      super(p_425157_);
      this.weatherState = p_425930_;
   }

   @Override
   protected void randomTick(BlockState p_428426_, ServerLevel p_429451_, BlockPos p_430728_, RandomSource p_429519_) {
      this.changeOverTime(p_428426_, p_429451_, p_430728_, p_429519_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_428949_) {
      return WeatheringCopper.getNext(p_428949_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
