package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TradeWithVillager extends Behavior<Villager> {
   private Set<Item> trades = ImmutableSet.of();

   public TradeWithVillager() {
      super(
         ImmutableMap.of(
            MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         )
      );
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Villager body) {
      return BehaviorUtils.targetIsValid(body.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityTypes.VILLAGER);
   }

   protected boolean canStillUse(final ServerLevel level, final Villager body, final long timestamp) {
      return this.checkExtraStartConditions(level, body);
   }

   protected void start(final ServerLevel level, final Villager myBody, final long timestamp) {
      Villager target = (Villager)myBody.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(myBody, target, 0.5F, 2);
      this.trades = figureOutWhatIAmWillingToTrade(myBody, target);
   }

   protected void tick(final ServerLevel level, final Villager body, final long timestamp) {
      Villager target = (Villager)body.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      if (!(body.distanceToSqr(target) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(body, target, 0.5F, 2);
         body.gossip(level, target, timestamp);
         boolean isFarmer = body.getVillagerData().profession().is(VillagerProfession.FARMER);
         if (body.hasExcessFood() && (isFarmer || target.wantsMoreFood())) {
            throwHalfStack(body, Villager.FOOD_POINTS.keySet(), target);
         }

         if (isFarmer && body.getInventory().countItem(Items.WHEAT) > Items.WHEAT.getDefaultMaxStackSize() / 2) {
            throwHalfStack(body, ImmutableSet.of(Items.WHEAT), target);
         }

         if (!this.trades.isEmpty() && body.getInventory().hasAnyOf(this.trades)) {
            throwHalfStack(body, this.trades, target);
         }
      }
   }

   protected void stop(final ServerLevel level, final Villager body, final long timestamp) {
      body.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
   }

   private static Set<Item> figureOutWhatIAmWillingToTrade(final Villager myBody, final Villager target) {
      ImmutableSet<Item> targetItems = target.getVillagerData().profession().value().requestedItems();
      ImmutableSet<Item> selfItems = myBody.getVillagerData().profession().value().requestedItems();
      return targetItems.stream().filter(entry -> !selfItems.contains(entry)).collect(Collectors.toSet());
   }

   private static void throwHalfStack(final Villager villager, final Set<Item> items, final LivingEntity target) {
      SimpleContainer inventory = villager.getInventory();
      ItemStack toThrow = ItemStack.EMPTY;
      int i = 0;

      while (i < inventory.getContainerSize()) {
         ItemStack itemStack;
         Item item;
         int count;
         label28: {
            itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
               item = itemStack.getItem();
               if (items.contains(item)) {
                  if (itemStack.getCount() > itemStack.getMaxStackSize() / 2) {
                     count = itemStack.getCount() / 2;
                     break label28;
                  }

                  if (itemStack.getCount() > 24) {
                     count = itemStack.getCount() - 24;
                     break label28;
                  }
               }
            }

            i++;
            continue;
         }

         itemStack.shrink(count);
         toThrow = new ItemStack(item, count);
         break;
      }

      if (!toThrow.isEmpty()) {
         BehaviorUtils.throwItem(villager, toThrow, target.position());
      }
   }
}
