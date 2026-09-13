package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperSlabBlock extends SlabBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperSlabBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422144_ -> p_422144_.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge), propertiesCodec())
         .apply(p_422144_, WeatheringCopperSlabBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<WeatheringCopperSlabBlock> codec() {
      return CODEC;
   }

   public WeatheringCopperSlabBlock(WeatheringCopper.WeatherState p_154938_, BlockBehaviour.Properties p_154939_) {
      super(p_154939_);
      this.weatherState = p_154938_;
   }

   @Override
   protected void randomTick(BlockState p_222670_, ServerLevel p_222671_, BlockPos p_222672_, RandomSource p_222673_) {
      this.changeOverTime(p_222670_, p_222671_, p_222672_, p_222673_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_154947_) {
      return WeatheringCopper.getNext(p_154947_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
