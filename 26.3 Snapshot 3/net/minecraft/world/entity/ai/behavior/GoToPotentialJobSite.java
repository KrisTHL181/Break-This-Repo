package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

public class GoToPotentialJobSite extends Behavior<Villager> {
   private static final int TICKS_UNTIL_TIMEOUT = 1200;
   private final float speedModifier;

   public GoToPotentialJobSite(final float speedModifier) {
      super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT), 1200);
      this.speedModifier = speedModifier;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Villager body) {
      return body.getBrain()
         .getActiveNonCoreActivity()
         .map(activity -> activity == Activity.IDLE || activity == Activity.WORK || activity == Activity.PLAY)
         .orElse(true);
   }

   protected boolean canStillUse(final ServerLevel level, final Villager body, final long timestamp) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE);
   }

   protected void tick(final ServerLevel level, final Villager body, final long timestamp) {
      BehaviorUtils.setWalkAndLookTargetMemories(body, body.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos(), this.speedModifier, 1);
   }

   protected void stop(final ServerLevel level, final Villager body, final long timestamp) {
      Optional<GlobalPos> potentialJobSitePos = body.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
      potentialJobSitePos.ifPresent(globalPos -> {
         BlockPos pos = globalPos.pos();
         ServerLevel serverLevel = level.getServer().getLevel(globalPos.dimension());
         if (serverLevel != null) {
            PoiManager manager = serverLevel.getPoiManager();
            if (manager.exists(pos, p -> true)) {
               manager.release(pos);
            }

            level.debugSynchronizers().updatePoi(pos);
         }
      });
      body.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
   }
}
