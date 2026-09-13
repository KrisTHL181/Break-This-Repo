package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

public class ShowTradesToPlayer extends Behavior<Villager> {
   private static final int MAX_LOOK_TIME = 900;
   private static final int STARTING_LOOK_TIME = 40;
   private @Nullable ItemStack playerItemStack;
   private final List<ItemStack> displayItems = Lists.newArrayList();
   private int cycleCounter;
   private int displayIndex;
   private int lookTime;

   public ShowTradesToPlayer(int p_24096_, int p_24097_) {
      super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT), p_24096_, p_24097_);
   }

   public boolean checkExtraStartConditions(ServerLevel p_24106_, Villager p_451979_) {
      Brain<?> brain = p_451979_.getBrain();
      if (brain.getMemory(MemoryModuleType.INTERACTION_TARGET).isEmpty()) {
         return false;
      }

      LivingEntity livingentity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      return livingentity.getType() == EntityType.PLAYER
         && p_451979_.isAlive()
         && livingentity.isAlive()
         && !p_451979_.isBaby()
         && p_451979_.distanceToSqr(livingentity) <= 17.0;
   }

   public boolean canStillUse(ServerLevel p_24109_, Villager p_457685_, long p_24111_) {
      return this.checkExtraStartConditions(p_24109_, p_457685_)
         && this.lookTime > 0
         && p_457685_.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
   }

   public void start(ServerLevel p_24124_, Villager p_453722_, long p_24126_) {
      super.start(p_24124_, p_453722_, p_24126_);
      this.lookAtTarget(p_453722_);
      this.cycleCounter = 0;
      this.displayIndex = 0;
      this.lookTime = 40;
   }

   public void tick(ServerLevel p_24134_, Villager p_452969_, long p_24136_) {
      LivingEntity livingentity = this.lookAtTarget(p_452969_);
      this.findItemsToDisplay(livingentity, p_452969_);
      if (!this.displayItems.isEmpty()) {
         this.displayCyclingItems(p_452969_);
      } else {
         clearHeldItem(p_452969_);
         this.lookTime = Math.min(this.lookTime, 40);
      }

      this.lookTime--;
   }

   public void stop(ServerLevel p_24144_, Villager p_450687_, long p_24146_) {
      super.stop(p_24144_, p_450687_, p_24146_);
      p_450687_.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
      clearHeldItem(p_450687_);
      this.playerItemStack = null;
   }

   private void findItemsToDisplay(LivingEntity p_24113_, Villager p_454506_) {
      boolean flag = false;
      ItemStack itemstack = p_24113_.getMainHandItem();
      if (this.playerItemStack == null || !ItemStack.isSameItem(this.playerItemStack, itemstack)) {
         this.playerItemStack = itemstack;
         flag = true;
         this.displayItems.clear();
      }

      if (flag && !this.playerItemStack.isEmpty()) {
         this.updateDisplayItems(p_454506_);
         if (!this.displayItems.isEmpty()) {
            this.lookTime = 900;
            this.displayFirstItem(p_454506_);
         }
      }
   }

   private void displayFirstItem(Villager p_456995_) {
      displayAsHeldItem(p_456995_, this.displayItems.get(0));
   }

   private void updateDisplayItems(Villager p_451053_) {
      for (MerchantOffer merchantoffer : p_451053_.getOffers()) {
         if (!merchantoffer.isOutOfStock() && this.playerItemStackMatchesCostOfOffer(merchantoffer)) {
            this.displayItems.add(merchantoffer.assemble());
         }
      }
   }

   private boolean playerItemStackMatchesCostOfOffer(MerchantOffer p_24118_) {
      return ItemStack.isSameItem(this.playerItemStack, p_24118_.getCostA()) || ItemStack.isSameItem(this.playerItemStack, p_24118_.getCostB());
   }

   private static void clearHeldItem(Villager p_451115_) {
      p_451115_.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      p_451115_.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
   }

   private static void displayAsHeldItem(Villager p_459522_, ItemStack p_182372_) {
      p_459522_.setItemSlot(EquipmentSlot.MAINHAND, p_182372_);
      p_459522_.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   private LivingEntity lookAtTarget(Villager p_461067_) {
      Brain<?> brain = p_461067_.getBrain();
      LivingEntity livingentity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
      return livingentity;
   }

   private void displayCyclingItems(Villager p_453641_) {
      if (this.displayItems.size() >= 2 && ++this.cycleCounter >= 40) {
         this.displayIndex++;
         this.cycleCounter = 0;
         if (this.displayIndex > this.displayItems.size() - 1) {
            this.displayIndex = 0;
         }

         displayAsHeldItem(p_453641_, this.displayItems.get(this.displayIndex));
      }
   }
}
