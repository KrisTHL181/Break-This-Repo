package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TemptingSensor extends Sensor<PathfinderMob> {
   private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().ignoreLineOfSight();
   private final BiPredicate<PathfinderMob, ItemStack> temptations;

   public TemptingSensor(Predicate<ItemStack> p_328517_) {
      this((p_449612_, p_449613_) -> p_328517_.test(p_449613_));
   }

   public static TemptingSensor forAnimal() {
      return new TemptingSensor((p_449609_, p_449610_) -> p_449609_ instanceof Animal animal ? animal.isFood(p_449610_) : false);
   }

   private TemptingSensor(BiPredicate<PathfinderMob, ItemStack> p_450662_) {
      this.temptations = p_450662_;
   }

   protected void doTick(ServerLevel p_148331_, PathfinderMob p_148332_) {
      Brain<?> brain = p_148332_.getBrain();
      TargetingConditions targetingconditions = TEMPT_TARGETING.copy().range((float)p_148332_.getAttributeValue(Attributes.TEMPT_RANGE));
      List<Player> list = p_148331_.players()
         .stream()
         .filter(EntitySelector.NO_SPECTATORS)
         .filter(p_359128_ -> targetingconditions.test(p_148331_, p_148332_, p_359128_))
         .filter(p_449615_ -> this.playerHoldingTemptation(p_148332_, p_449615_))
         .filter(p_405425_ -> !p_148332_.hasPassenger(p_405425_))
         .sorted(Comparator.comparingDouble(p_148332_::distanceToSqr))
         .collect(Collectors.toList());
      if (!list.isEmpty()) {
         Player player = list.get(0);
         brain.setMemory(MemoryModuleType.TEMPTING_PLAYER, player);
      } else {
         brain.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
      }
   }

   private boolean playerHoldingTemptation(PathfinderMob p_456814_, Player p_148337_) {
      return this.isTemptation(p_456814_, p_148337_.getMainHandItem()) || this.isTemptation(p_456814_, p_148337_.getOffhandItem());
   }

   private boolean isTemptation(PathfinderMob p_460428_, ItemStack p_148339_) {
      return this.temptations.test(p_460428_, p_148339_);
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
   }
}
