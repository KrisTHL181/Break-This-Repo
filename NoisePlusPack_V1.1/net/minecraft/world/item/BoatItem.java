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

   public BoatItem(EntityType<? extends AbstractBoat> p_363828_, Item.Properties p_220015_) {
      super(p_220015_);
      this.entityType = p_363828_;
   }

   @Override
   public InteractionResult use(Level p_40622_, Player p_40623_, InteractionHand p_40624_) {
      ItemStack itemstack = p_40623_.getItemInHand(p_40624_);
      HitResult hitresult = getPlayerPOVHitResult(p_40622_, p_40623_, ClipContext.Fluid.ANY);
      if (hitresult.getType() == HitResult.Type.MISS) {
         return InteractionResult.PASS;
      }

      Vec3 vec3 = p_40623_.getViewVector(1.0F);
      double d0 = 5.0;
      List<Entity> list = p_40622_.getEntities(p_40623_, p_40623_.getBoundingBox().expandTowards(vec3.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);
      if (!list.isEmpty()) {
         Vec3 vec31 = p_40623_.getEyePosition();

         for (Entity entity : list) {
            AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (aabb.contains(vec31)) {
               return InteractionResult.PASS;
            }
         }
      }

      if (hitresult.getType() == HitResult.Type.BLOCK) {
         AbstractBoat abstractboat = this.getBoat(p_40622_, hitresult, itemstack, p_40623_);
         if (abstractboat == null) {
            return InteractionResult.FAIL;
         }

         abstractboat.setYRot(p_40623_.getYRot());
         if (!p_40622_.noCollision(abstractboat, abstractboat.getBoundingBox())) {
            return InteractionResult.FAIL;
         }

         if (!p_40622_.isClientSide()) {
            p_40622_.addFreshEntity(abstractboat);
            p_40622_.gameEvent(p_40623_, GameEvent.ENTITY_PLACE, hitresult.getLocation());
            itemstack.consume(1, p_40623_);
         }

         p_40623_.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   private @Nullable AbstractBoat getBoat(Level p_220017_, HitResult p_220018_, ItemStack p_311821_, Player p_313119_) {
      AbstractBoat abstractboat = this.entityType.create(p_220017_, EntitySpawnReason.SPAWN_ITEM_USE);
      if (abstractboat != null) {
         Vec3 vec3 = p_220018_.getLocation();
         abstractboat.setInitialPos(vec3.x, vec3.y, vec3.z);
         if (p_220017_ instanceof ServerLevel serverlevel) {
            EntityType.<AbstractBoat>createDefaultStackConfig(serverlevel, p_311821_, p_313119_).accept(abstractboat);
         }
      }

      return abstractboat;
   }
}
