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

   protected void doTick(final ServerLevel level, final Villager body) {
      ResourceKey<Level> dimensionType = level.dimension();
      BlockPos center = body.blockPosition();
      List<GlobalPos> jobSites = Lists.newArrayList();
      int horizontalSearch = 4;

      for (int x = -4; x <= 4; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -4; z <= 4; z++) {
               BlockPos testPos = center.offset(x, y, z);
               if (body.getVillagerData().profession().value().secondaryPoi().contains(level.getBlockState(testPos).getBlock())) {
                  jobSites.add(GlobalPos.of(dimensionType, testPos));
               }
            }
         }
      }

      Brain<?> brain = body.getBrain();
      if (!jobSites.isEmpty()) {
         brain.setMemory(MemoryModuleType.SECONDARY_JOB_SITE, jobSites);
      } else {
         brain.eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
      }
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.SECONDARY_JOB_SITE);
   }
}
