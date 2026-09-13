package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class WeatheringCopperChestBlock extends CopperChestBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_425464_ -> p_425464_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getState),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenChestSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseChestSound),
            propertiesCodec()
         )
         .apply(p_425464_, WeatheringCopperChestBlock::new)
   );

   @Override
   public MapCodec<WeatheringCopperChestBlock> codec() {
      return CODEC;
   }

   public WeatheringCopperChestBlock(WeatheringCopper.WeatherState p_426564_, SoundEvent p_429549_, SoundEvent p_429913_, BlockBehaviour.Properties p_427663_) {
      super(p_426564_, p_429549_, p_429913_, p_427663_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_428505_) {
      return WeatheringCopper.getNext(p_428505_.getBlock()).isPresent();
   }

   @Override
   protected void randomTick(BlockState p_427446_, ServerLevel p_428114_, BlockPos p_428542_, RandomSource p_426819_) {
      if (!p_427446_.getValue(ChestBlock.TYPE).equals(ChestType.RIGHT)
         && p_428114_.getBlockEntity(p_428542_) instanceof ChestBlockEntity chestblockentity
         && chestblockentity.getEntitiesWithContainerOpen().isEmpty()) {
         this.changeOverTime(p_427446_, p_428114_, p_428542_, p_426819_);
      }
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.getState();
   }

   @Override
   public boolean isWaxed() {
      return false;
   }
}
