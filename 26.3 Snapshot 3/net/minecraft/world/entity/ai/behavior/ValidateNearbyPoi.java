package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ValidateNearbyPoi {
   private static final int MAX_DISTANCE = 16;

   public static BehaviorControl<LivingEntity> create(final Predicate<Holder<PoiType>> poiType, final MemoryModuleType<GlobalPos> memoryType) {
      return BehaviorBuilder.create(i -> i.group(i.present(memoryType)).apply(i, memory -> (level, body, timestamp) -> {
         GlobalPos globalPos = i.get(memory);
         BlockPos pos = globalPos.pos();
         if (level.dimension() == globalPos.dimension() && pos.closerToCenterThan(body.position(), 16.0)) {
            ServerLevel poiLevel = level.getServer().getLevel(globalPos.dimension());
            if (poiLevel == null || !poiLevel.getPoiManager().exists(pos, poiType)) {
               memory.erase();
            } else if (bedIsOccupied(poiLevel, pos, body)) {
               memory.erase();
               if (!bedIsOccupiedByVillager(poiLevel, pos)) {
                  level.getPoiManager().release(pos);
                  level.debugSynchronizers().updatePoi(pos);
               }
            }

            return true;
         } else {
            return false;
         }
      }));
   }

   private static boolean bedIsOccupied(final ServerLevel poiLevel, final BlockPos poiPos, final LivingEntity body) {
      BlockState blockState = poiLevel.getBlockState(poiPos);
      return blockState.is(BlockTags.VILLAGERS_CAN_SLEEP_ON_BED) && blockState.getValue(BedBlock.OCCUPIED) && !body.isSleeping();
   }

   private static boolean bedIsOccupiedByVillager(final ServerLevel poiLevel, final BlockPos poiPos) {
      List<Villager> villagers = poiLevel.getEntitiesOfClass(Villager.class, new AABB(poiPos), LivingEntity::isSleeping);
      return !villagers.isEmpty();
   }
}
