package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CushionItem extends Item {
   private final DyeColor color;

   public CushionItem(final Item.Properties properties, final DyeColor color) {
      super(properties);
      this.color = color;
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Direction clickedFace = context.getClickedFace();
      if (clickedFace != Direction.UP) {
         return InteractionResult.FAIL;
      }

      Level level = context.getLevel();
      BlockPlaceContext placeContext = new BlockPlaceContext(context);
      BlockPos blockPos = placeContext.getClickedPos();
      Vec3 entityPos = Vec3.atCenterOfWithY(blockPos, context.getClickLocation().y);
      AABB spawnAABB = EntityTypes.CUSHION.getSpawnAABB(entityPos);
      if (!Cushion.wouldSuriveAt(level, spawnAABB)) {
         return InteractionResult.FAIL;
      }

      ItemStack itemStack = context.getItemInHand();
      if (level instanceof ServerLevel serverLevel) {
         if (!serverLevel.getEntitiesOfClass(Cushion.class, spawnAABB).isEmpty()) {
            return InteractionResult.FAIL;
         }

         PostSpawnProcessor<Cushion> entityConfig = EntityType.createDefaultStackConfig(serverLevel, itemStack, context.getPlayer());
         Cushion cushion = EntityTypes.CUSHION.create(serverLevel, entityConfig, blockPos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
         if (cushion == null) {
            return InteractionResult.FAIL;
         }

         cushion.snapTo(entityPos, Direction.fromYRot(placeContext.getRotation()).toYRot(), 0.0F);
         cushion.setColor(this.color);
         serverLevel.addFreshEntity(cushion);
         level.playSound(null, cushion.getX(), cushion.getY(), cushion.getZ(), SoundEvents.CUSHION_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
         cushion.gameEvent(GameEvent.ENTITY_PLACE);
         itemStack.consume(1, placeContext.getPlayer());
      }

      return InteractionResult.SUCCESS;
   }
}
