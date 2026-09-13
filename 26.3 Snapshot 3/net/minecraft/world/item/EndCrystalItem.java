package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class EndCrystalItem extends Item {
   public EndCrystalItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState blockState = level.getBlockState(pos);
      if (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK)) {
         return InteractionResult.FAIL;
      }

      BlockPos above = pos.above();
      if (!level.isEmptyBlock(above)) {
         return InteractionResult.FAIL;
      }

      double x = above.getX();
      double y = above.getY();
      double z = above.getZ();
      List<Entity> entities = level.getEntities(null, new AABB(x, y, z, x + 1.0, y + 2.0, z + 1.0));
      if (!entities.isEmpty()) {
         return InteractionResult.FAIL;
      }

      if (level instanceof ServerLevel serverLevel) {
         EndCrystal crystal = new EndCrystal(level, x + 0.5, y, z + 0.5);
         crystal.setShowBottom(false);
         level.addFreshEntity(crystal);
         level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, above);
         EnderDragonFight fight = serverLevel.getDragonFight();
         if (fight != null) {
            fight.tryRespawn();
         }
      }

      context.getItemInHand().shrink(1);
      return InteractionResult.SUCCESS;
   }
}
