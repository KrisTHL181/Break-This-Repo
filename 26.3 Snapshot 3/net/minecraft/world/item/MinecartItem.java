package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class MinecartItem extends Item {
   private final EntityType<? extends AbstractMinecart> type;

   public MinecartItem(final EntityType<? extends AbstractMinecart> type, final Item.Properties properties) {
      super(properties);
      this.type = type;
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState blockState = level.getBlockState(pos);
      if (!blockState.is(BlockTags.RAILS)) {
         return InteractionResult.FAIL;
      }

      ItemStack itemStack = context.getItemInHand();
      RailShape shape = blockState.getBlock() instanceof BaseRailBlock
         ? blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty())
         : RailShape.NORTH_SOUTH;
      double offset = 0.0;
      if (shape.isSlope()) {
         offset = 0.5;
      }

      Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.0625 + offset, pos.getZ() + 0.5);
      AbstractMinecart cart = AbstractMinecart.createMinecart(
         level, spawnPos.x, spawnPos.y, spawnPos.z, this.type, EntitySpawnReason.DISPENSER, itemStack, context.getPlayer()
      );
      if (cart == null) {
         return InteractionResult.FAIL;
      }

      if (AbstractMinecart.useExperimentalMovement(level)) {
         for (Entity entity : level.getEntities(null, cart.getBoundingBox())) {
            if (entity instanceof AbstractMinecart) {
               return InteractionResult.FAIL;
            }
         }
      }

      if (level instanceof ServerLevel serverLevel) {
         serverLevel.addFreshEntity(cart);
         serverLevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), serverLevel.getBlockState(pos.below())));
      }

      itemStack.shrink(1);
      return InteractionResult.SUCCESS;
   }
}
