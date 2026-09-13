package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperStairBlock extends StairBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperStairBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422145_ -> p_422145_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge),
            BlockState.CODEC.fieldOf("base_state").forGetter(p_312323_ -> p_312323_.baseState),
            propertiesCodec()
         )
         .apply(p_422145_, WeatheringCopperStairBlock::new)
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<WeatheringCopperStairBlock> codec() {
      return CODEC;
   }

   public WeatheringCopperStairBlock(WeatheringCopper.WeatherState p_154951_, BlockState p_154952_, BlockBehaviour.Properties p_154953_) {
      super(p_154952_, p_154953_);
      this.weatherState = p_154951_;
   }

   @Override
   protected void randomTick(BlockState p_222675_, ServerLevel p_222676_, BlockPos p_222677_, RandomSource p_222678_) {
      this.changeOverTime(p_222675_, p_222676_, p_222677_, p_222678_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_154961_) {
      return WeatheringCopper.getNext(p_154961_.getBlock()).isPresent();
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.weatherState;
   }
}
