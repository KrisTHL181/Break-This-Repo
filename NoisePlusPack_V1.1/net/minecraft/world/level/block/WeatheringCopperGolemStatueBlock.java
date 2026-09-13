package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WeatheringCopperGolemStatueBlock extends CopperGolemStatueBlock implements WeatheringCopper {
   public static final MapCodec<WeatheringCopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_430718_ -> p_430718_.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge), propertiesCodec())
         .apply(p_430718_, WeatheringCopperGolemStatueBlock::new)
   );

   @Override
   public MapCodec<WeatheringCopperGolemStatueBlock> codec() {
      return CODEC;
   }

   public WeatheringCopperGolemStatueBlock(WeatheringCopper.WeatherState p_430107_, BlockBehaviour.Properties p_431407_) {
      super(p_430107_, p_431407_);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_426616_) {
      return WeatheringCopper.getNext(p_426616_.getBlock()).isPresent();
   }

   @Override
   protected void randomTick(BlockState p_422819_, ServerLevel p_426494_, BlockPos p_430363_, RandomSource p_422396_) {
      this.changeOverTime(p_422819_, p_426494_, p_430363_, p_422396_);
   }

   public WeatheringCopper.WeatherState getAge() {
      return this.getWeatheringState();
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_423592_, BlockState p_424813_, Level p_423072_, BlockPos p_423103_, Player p_424834_, InteractionHand p_426411_, BlockHitResult p_422591_
   ) {
      if (p_423072_.getBlockEntity(p_423103_) instanceof CopperGolemStatueBlockEntity coppergolemstatueblockentity) {
         if (!p_423592_.is(ItemTags.AXES)) {
            if (p_423592_.is(Items.HONEYCOMB)) {
               return InteractionResult.PASS;
            }

            this.updatePose(p_423072_, p_424813_, p_423103_, p_424834_);
            return InteractionResult.SUCCESS;
         }

         if (this.getAge().equals(WeatheringCopper.WeatherState.UNAFFECTED)) {
            CopperGolem coppergolem = coppergolemstatueblockentity.removeStatue(p_424813_);
            p_423592_.hurtAndBreak(1, p_424834_, p_426411_.asEquipmentSlot());
            if (coppergolem != null) {
               p_423072_.addFreshEntity(coppergolem);
               p_423072_.removeBlock(p_423103_, false);
               return InteractionResult.SUCCESS;
            }
         }
      }

      return InteractionResult.PASS;
   }
}
