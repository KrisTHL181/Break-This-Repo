package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class GoToClosestVillage {
   public static BehaviorControl<Villager> create(float p_260342_, int p_259691_) {
      return BehaviorBuilder.create(
         p_258357_ -> p_258357_.group(p_258357_.absent(MemoryModuleType.WALK_TARGET)).apply(p_258357_, p_258366_ -> (p_274970_, p_457179_, p_274972_) -> {
            if (p_274970_.isVillage(p_457179_.blockPosition())) {
               return false;
            }

            PoiManager poimanager = p_274970_.getPoiManager();
            int i = poimanager.sectionsToVillage(SectionPos.of(p_457179_.blockPosition()));
            Vec3 vec3 = null;

            for (int j = 0; j < 5; j++) {
               Vec3 vec31 = LandRandomPos.getPos(p_457179_, 15, 7, p_147554_ -> -poimanager.sectionsToVillage(SectionPos.of(p_147554_)));
               if (vec31 != null) {
                  int k = poimanager.sectionsToVillage(SectionPos.of(BlockPos.containing(vec31)));
                  if (k < i) {
                     vec3 = vec31;
                     break;
                  }

                  if (k == i) {
                     vec3 = vec31;
                  }
               }
            }

            if (vec3 != null) {
               p_258366_.set(new WalkTarget(vec3, p_260342_, p_259691_));
            }

            return true;
         })
      );
   }
}
