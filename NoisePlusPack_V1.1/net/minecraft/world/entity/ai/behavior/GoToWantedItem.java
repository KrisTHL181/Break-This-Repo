package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.K1;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;

public class GoToWantedItem {
   public static BehaviorControl<LivingEntity> create(float p_260027_, boolean p_259769_, int p_259671_) {
      return create(p_23158_ -> true, p_260027_, p_259769_, p_259671_);
   }

   public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> p_259490_, float p_260346_, boolean p_259637_, int p_259054_) {
      return BehaviorBuilder.create(
         p_258371_ -> {
            BehaviorBuilder<E, ? extends MemoryAccessor<? extends K1, WalkTarget>> behaviorbuilder = p_259637_
               ? p_258371_.registered(MemoryModuleType.WALK_TARGET)
               : p_258371_.absent(MemoryModuleType.WALK_TARGET);
            return p_258371_.group(
                  p_258371_.registered(MemoryModuleType.LOOK_TARGET),
                  behaviorbuilder,
                  p_258371_.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
                  p_258371_.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)
               )
               .apply(
                  p_258371_,
                  (p_258387_, p_258388_, p_258389_, p_258390_) -> (p_358949_, p_358950_, p_358951_) -> {
                     ItemEntity itementity = p_258371_.get(p_258389_);
                     if (p_258371_.tryGet(p_258390_).isEmpty()
                        && p_259490_.test((E)p_358950_)
                        && itementity.closerThan(p_358950_, p_259054_)
                        && p_358950_.level().getWorldBorder().isWithinBounds(itementity.blockPosition())
                        && p_358950_.canPickUpLoot()) {
                        WalkTarget walktarget = new WalkTarget(new EntityTracker(itementity, false), p_260346_, 0);
                        p_258387_.set(new EntityTracker(itementity, true));
                        p_258388_.set(walktarget);
                        return true;
                     } else {
                        return false;
                     }
                  }
               );
         }
      );
   }
}
