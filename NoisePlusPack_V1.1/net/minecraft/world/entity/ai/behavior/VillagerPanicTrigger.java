package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

public class VillagerPanicTrigger extends Behavior<Villager> {
   public VillagerPanicTrigger() {
      super(ImmutableMap.of());
   }

   protected boolean canStillUse(ServerLevel p_24684_, Villager p_456484_, long p_24686_) {
      return isHurt(p_456484_) || hasHostile(p_456484_);
   }

   protected void start(ServerLevel p_24694_, Villager p_458171_, long p_24696_) {
      if (isHurt(p_458171_) || hasHostile(p_458171_)) {
         Brain<?> brain = p_458171_.getBrain();
         if (!brain.isActive(Activity.PANIC)) {
            brain.eraseMemory(MemoryModuleType.PATH);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            brain.eraseMemory(MemoryModuleType.BREED_TARGET);
            brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
         }

         brain.setActiveActivityIfPossible(Activity.PANIC);
      }
   }

   protected void tick(ServerLevel p_24700_, Villager p_457484_, long p_24702_) {
      if (p_24702_ % 100L == 0L) {
         p_457484_.spawnGolemIfNeeded(p_24700_, p_24702_, 3);
      }
   }

   public static boolean hasHostile(LivingEntity p_24688_) {
      return p_24688_.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
   }

   public static boolean isHurt(LivingEntity p_24698_) {
      return p_24698_.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
   }
}
