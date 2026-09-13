package net.minecraft.world.item;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BoatItem extends Item {
   private final EntityType<? extends AbstractBoat> entityType;

   public BoatItem(final EntityType<? extends AbstractBoat> entityType, final Item.Properties properties) {
      super(properties);
      this.entityType = entityType;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
      if (hitResult.getType() == HitResult.Type.MISS) {
         return InteractionResult.PASS;
      }

      Vec3 viewVector = player.getViewVector(1.0F);
      double range = 5.0;
      List<Entity> entities = level.getEntities(player, player.getBoundingBox().expandTowards(viewVector.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);
      if (!entities.isEmpty()) {
         Vec3 from = player.getEyePosition();

         for (Entity entity : entities) {
            AABB bb = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (bb.contains(from)) {
               return InteractionResult.PASS;
            }
         }
      }

      if (hitResult.getType() == HitResult.Type.BLOCK) {
         AbstractBoat boat = this.getBoat(level, hitResult, itemStack, player);
         if (boat == null) {
            return InteractionResult.FAIL;
         }

         boat.setYRot(player.getYRot());
         if (!level.noCollision(boat, boat.getBoundingBox())) {
            return InteractionResult.FAIL;
         }

         if (!level.isClientSide()) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hitResult.getLocation());
            itemStack.consume(1, player);
         }

         player.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   private @Nullable AbstractBoat getBoat(final Level level, final HitResult hitResult, final ItemStack itemStack, final Player player) {
      AbstractBoat boat = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
      if (boat != null) {
         Vec3 location = hitResult.getLocation();
         boat.setInitialPos(location.x, location.y, location.z);
         if (level instanceof ServerLevel serverLevel) {
            EntityType.<AbstractBoat>createDefaultStackConfig(serverLevel, itemStack, player).apply(boat);
         }
      }

      return boat;
   }
}
