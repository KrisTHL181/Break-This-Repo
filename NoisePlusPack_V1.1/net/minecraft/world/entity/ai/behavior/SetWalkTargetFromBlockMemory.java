package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetFromBlockMemory {
   public static OneShot<Villager> create(MemoryModuleType<GlobalPos> p_259685_, float p_259842_, int p_259530_, int p_260360_, int p_259504_) {
      return BehaviorBuilder.create(
         p_258717_ -> p_258717_.group(
               p_258717_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
               p_258717_.absent(MemoryModuleType.WALK_TARGET),
               p_258717_.present(p_259685_)
            )
            .apply(p_258717_, (p_258709_, p_258710_, p_258711_) -> (p_275056_, p_454238_, p_275058_) -> {
               GlobalPos globalpos = p_258717_.get(p_258711_);
               Optional<Long> optional = p_258717_.tryGet(p_258709_);
               if (globalpos.dimension() == p_275056_.dimension() && (!optional.isPresent() || p_275056_.getGameTime() - optional.get() <= p_259504_)) {
                  if (globalpos.pos().distManhattan(p_454238_.blockPosition()) > p_260360_) {
                     Vec3 vec3 = null;
                     int i = 0;
                     int j = 1000;

                     while (vec3 == null || BlockPos.containing(vec3).distManhattan(p_454238_.blockPosition()) > p_260360_) {
                        vec3 = DefaultRandomPos.getPosTowards(p_454238_, 15, 7, Vec3.atBottomCenterOf(globalpos.pos()), (float) (Math.PI / 2));
                        if (++i == 1000) {
                           p_454238_.releasePoi(p_259685_);
                           p_258711_.erase();
                           p_258709_.set(p_275058_);
                           return true;
                        }
                     }

                     p_258710_.set(new WalkTarget(vec3, p_259842_, p_259530_));
                  } else if (globalpos.pos().distManhattan(p_454238_.blockPosition()) > p_259530_) {
                     p_258710_.set(new WalkTarget(globalpos.pos(), p_259842_, p_259530_));
                  }
               } else {
                  p_454238_.releasePoi(p_259685_);
                  p_258711_.erase();
                  p_258709_.set(p_275058_);
               }

               return true;
            })
      );
   }
}
