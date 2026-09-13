package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

public class SecondaryPoiSensor extends Sensor<Villager> {
   private static final int SCAN_RATE = 40;

   public SecondaryPoiSensor() {
      super(40);
   }

   protected void doTick(ServerLevel p_26754_, Villager p_460377_) {
      ResourceKey<Level> resourcekey = p_26754_.dimension();
      BlockPos blockpos = p_460377_.blockPosition();
      List<GlobalPos> list = Lists.newArrayList();
      int i = 4;

      for (int j = -4; j <= 4; j++) {
         for (int k = -2; k <= 2; k++) {
            for (int l = -4; l <= 4; l++) {
               BlockPos blockpos1 = blockpos.offset(j, k, l);
               if (p_460377_.getVillagerData().profession().value().secondaryPoi().contains(p_26754_.getBlockState(blockpos1).getBlock())) {
                  list.add(GlobalPos.of(resourcekey, blockpos1));
               }
            }
         }
      }

      Brain<?> brain = p_460377_.getBrain();
      if (!list.isEmpty()) {
         brain.setMemory(MemoryModuleType.SECONDARY_JOB_SITE, list);
      } else {
         brain.eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
      }
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.SECONDARY_JOB_SITE);
   }
}
