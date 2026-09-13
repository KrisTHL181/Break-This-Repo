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

   protected boolean canStillUse(final ServerLevel level, final Villager body, final long timestamp) {
      return isHurt(body) || hasHostile(body);
   }

   protected void start(final ServerLevel level, final Villager body, final long timestamp) {
      if (isHurt(body) || hasHostile(body)) {
         Brain<?> brain = body.getBrain();
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

   protected void tick(final ServerLevel level, final Villager body, final long timestamp) {
      if (timestamp % 100L == 0L) {
         body.spawnGolemIfNeeded(level, timestamp, 3);
      }
   }

   public static boolean hasHostile(final LivingEntity myBody) {
      return myBody.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
   }

   public static boolean isHurt(final LivingEntity myBody) {
      return myBody.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
   }
}
