package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class VillageBoundRandomStroll {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;

   public static OneShot<PathfinderMob> create(float p_260156_) {
      return create(p_260156_, 10, 7);
   }

   public static OneShot<PathfinderMob> create(float p_259320_, int p_259708_, int p_259311_) {
      return BehaviorBuilder.create(
         p_258869_ -> p_258869_.group(p_258869_.absent(MemoryModuleType.WALK_TARGET)).apply(p_258869_, p_258863_ -> (p_258874_, p_258875_, p_258876_) -> {
            BlockPos blockpos = p_258875_.blockPosition();
            Vec3 vec3;
            if (p_258874_.isVillage(blockpos)) {
               vec3 = LandRandomPos.getPos(p_258875_, p_259708_, p_259311_);
            } else {
               SectionPos sectionpos = SectionPos.of(blockpos);
               SectionPos sectionpos1 = BehaviorUtils.findSectionClosestToVillage(p_258874_, sectionpos, 2);
               if (sectionpos1 != sectionpos) {
                  vec3 = DefaultRandomPos.getPosTowards(p_258875_, p_259708_, p_259311_, Vec3.atBottomCenterOf(sectionpos1.center()), (float) (Math.PI / 2));
               } else {
                  vec3 = LandRandomPos.getPos(p_258875_, p_259708_, p_259311_);
               }
            }

            p_258863_.setOrErase(Optional.ofNullable(vec3).map(p_258865_ -> new WalkTarget(p_258865_, p_259320_, 0)));
            return true;
         })
      );
   }
}
