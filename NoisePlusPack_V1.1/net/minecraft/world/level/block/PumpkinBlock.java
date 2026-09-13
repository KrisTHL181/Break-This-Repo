package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;

public class PumpkinBlock extends Block {
   public static final MapCodec<PumpkinBlock> CODEC = simpleCodec(PumpkinBlock::new);

   @Override
   public MapCodec<PumpkinBlock> codec() {
      return CODEC;
   }

   protected PumpkinBlock(BlockBehaviour.Properties p_55284_) {
      super(p_55284_);
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_330568_, BlockState p_330263_, Level p_327756_, BlockPos p_328675_, Player p_334049_, InteractionHand p_331851_, BlockHitResult p_329008_
   ) {
      if (!p_330568_.is(Items.SHEARS)) {
         return super.useItemOn(p_330568_, p_330263_, p_327756_, p_328675_, p_334049_, p_331851_, p_329008_);
      } else if (p_327756_ instanceof ServerLevel serverlevel) {
         Direction direction = p_329008_.getDirection();
         Direction direction1 = direction.getAxis() == Direction.Axis.Y ? p_334049_.getDirection().getOpposite() : direction;
         dropFromBlockInteractLootTable(
            serverlevel,
            BuiltInLootTables.CARVE_PUMPKIN,
            p_330263_,
            p_327756_.getBlockEntity(p_328675_),
            p_330568_,
            p_334049_,
            (p_430451_, p_429051_) -> {
               ItemEntity itementity = new ItemEntity(
                  p_327756_,
                  p_328675_.getX() + 0.5 + direction1.getStepX() * 0.65,
                  p_328675_.getY() + 0.1,
                  p_328675_.getZ() + 0.5 + direction1.getStepZ() * 0.65,
                  p_429051_
               );
               itementity.setDeltaMovement(
                  0.05 * direction1.getStepX() + p_327756_.random.nextDouble() * 0.02,
                  0.05,
                  0.05 * direction1.getStepZ() + p_327756_.random.nextDouble() * 0.02
               );
               p_327756_.addFreshEntity(itementity);
            }
         );
         p_327756_.playSound(null, p_328675_, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_327756_.setBlock(p_328675_, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, direction1), 11);
         p_330568_.hurtAndBreak(1, p_334049_, p_331851_.asEquipmentSlot());
         p_327756_.gameEvent(p_334049_, GameEvent.SHEAR, p_328675_);
         p_334049_.awardStat(Stats.ITEM_USED.get(Items.SHEARS));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.SUCCESS;
      }
   }
}
