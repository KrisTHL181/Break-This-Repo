package net.minecraft.core.dispenser;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class FlintAndSteelDispenseItemBehavior extends OptionalDispenseItemBehavior {
   @Override
   protected ItemStack execute(final BlockSource source, final ItemStack dispensed) {
      ServerLevel level = source.level();
      this.setSuccess(true);
      Direction facing = source.state().getValue(DispenserBlock.FACING);
      BlockPos targetPos = source.pos().relative(facing);
      BlockState target = level.getBlockState(targetPos);
      if (!tryIgniteExplosiveEntities(level, targetPos)) {
         if (BaseFireBlock.canBePlacedAt(level, targetPos, facing)) {
            level.setBlockAndUpdate(targetPos, BaseFireBlock.getState(level, targetPos));
            level.gameEvent(null, GameEvent.BLOCK_PLACE, targetPos);
         } else if (CampfireBlock.canLight(target) || CandleBlock.canLight(target) || CandleCakeBlock.canLight(target)) {
            level.setBlockAndUpdate(targetPos, target.setValue(BlockStateProperties.LIT, true));
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, targetPos);
         } else if (target.getBlock() instanceof TntBlock) {
            if (TntBlock.prime(level, targetPos, null, dispensed)) {
               level.removeBlock(targetPos, false);
            } else {
               this.setSuccess(false);
            }
         }
      }

      if (this.isSuccess()) {
         dispensed.hurtAndBreak(1, level, null, item -> {});
      }

      return dispensed;
   }

   private static boolean tryIgniteExplosiveEntities(final ServerLevel level, final BlockPos pos) {
      List<SulfurCube> entities = level.getEntitiesOfClass(SulfurCube.class, new AABB(pos), SulfurCube::canExplode);
      if (entities.isEmpty()) {
         return false;
      }

      entities.getFirst().primeTime(false);
      return true;
   }
}
