package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public class BegGoal extends Goal {
   private final Wolf wolf;
   private @Nullable Player player;
   private final ServerLevel level;
   private final float lookDistance;
   private int lookTime;
   private final TargetingConditions begTargeting;

   public BegGoal(Wolf p_393988_, float p_25064_) {
      this.wolf = p_393988_;
      this.level = getServerLevel(p_393988_);
      this.lookDistance = p_25064_;
      this.begTargeting = TargetingConditions.forNonCombat().range(p_25064_);
      this.setFlags(EnumSet.of(Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      this.player = this.level.getNearestPlayer(this.begTargeting, this.wolf);
      return this.player == null ? false : this.playerHoldingInteresting(this.player);
   }

   @Override
   public boolean canContinueToUse() {
      if (!this.player.isAlive()) {
         return false;
      } else {
         return this.wolf.distanceToSqr(this.player) > this.lookDistance * this.lookDistance
            ? false
            : this.lookTime > 0 && this.playerHoldingInteresting(this.player);
      }
   }

   @Override
   public void start() {
      this.wolf.setIsInterested(true);
      this.lookTime = this.adjustedTickDelay(40 + this.wolf.getRandom().nextInt(40));
   }

   @Override
   public void stop() {
      this.wolf.setIsInterested(false);
      this.player = null;
   }

   @Override
   public void tick() {
      this.wolf.getLookControl().setLookAt(this.player.getX(), this.player.getEyeY(), this.player.getZ(), 10.0F, this.wolf.getMaxHeadXRot());
      this.lookTime--;
   }

   private boolean playerHoldingInteresting(Player p_25067_) {
      for (InteractionHand interactionhand : InteractionHand.values()) {
         ItemStack itemstack = p_25067_.getItemInHand(interactionhand);
         if (itemstack.is(Items.BONE) || this.wolf.isFood(itemstack)) {
            return true;
         }
      }

      return false;
   }
}
