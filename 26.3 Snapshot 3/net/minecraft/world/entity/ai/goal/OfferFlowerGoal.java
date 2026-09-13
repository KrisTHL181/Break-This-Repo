package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class OfferFlowerGoal extends Goal {
   private static final TargetingConditions OFFER_TARGET_CONTEXT = TargetingConditions.forNonCombat().range(6.0);
   private static final Item OFFER_ITEM = Items.POPPY;
   public static final int OFFER_TICKS = 400;
   private final IronGolem golem;
   private @Nullable LivingEntity entity;
   private int tick;

   public OfferFlowerGoal(final IronGolem golem) {
      this.golem = golem;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      if (!this.golem.level().isBrightOutside()) {
         return false;
      }

      if (this.golem.getRandom().nextInt(8000) != 0) {
         return false;
      }

      this.entity = getServerLevel(this.golem)
         .getNearestEntity(
            EntityTypeTags.CANDIDATE_FOR_IRON_GOLEM_GIFT,
            OFFER_TARGET_CONTEXT,
            this.golem,
            this.golem.getX(),
            this.golem.getY(),
            this.golem.getZ(),
            this.getGolemBoundingBox()
         );
      return this.entity != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.tick > 0;
   }

   @Override
   public void start() {
      this.tick = this.adjustedTickDelay(400);
      this.golem.offerFlower(true);
   }

   @Override
   public void stop() {
      this.golem.offerFlower(false);
      if (this.tick == 0
         && this.entity instanceof Mob mob
         && mob.is(EntityTypeTags.ACCEPTS_IRON_GOLEM_GIFT)
         && mob.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).isEmpty()
         && this.getGolemBoundingBox().intersects(mob.getBoundingBox())) {
         mob.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA, OFFER_ITEM.getDefaultInstance());
         mob.setGuaranteedDrop(CopperGolem.EQUIPMENT_SLOT_ANTENNA);
      }

      this.entity = null;
   }

   @Override
   public void tick() {
      if (this.entity != null) {
         this.golem.getLookControl().setLookAt(this.entity, 30.0F, 30.0F);
      }

      this.tick--;
   }

   private AABB getGolemBoundingBox() {
      return this.golem.getBoundingBox().inflate(6.0, 2.0, 6.0);
   }
}
