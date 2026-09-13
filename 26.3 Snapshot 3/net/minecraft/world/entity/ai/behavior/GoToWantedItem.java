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
   public static BehaviorControl<LivingEntity> create(final float speedModifier, final boolean interruptOngoingWalk, final int maxDistToWalk) {
      return create(body -> true, speedModifier, interruptOngoingWalk, maxDistToWalk);
   }

   public static <E extends LivingEntity> BehaviorControl<E> create(
      final Predicate<E> predicate, final float speedModifier, final boolean interruptOngoingWalk, final int maxDistToWalk
   ) {
      return BehaviorBuilder.create(
         i -> {
            BehaviorBuilder<E, ? extends MemoryAccessor<? extends K1, WalkTarget>> walkCondition = interruptOngoingWalk
               ? i.registered(MemoryModuleType.WALK_TARGET)
               : i.absent(MemoryModuleType.WALK_TARGET);
            return i.group(
                  i.registered(MemoryModuleType.LOOK_TARGET),
                  walkCondition,
                  i.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
                  i.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)
               )
               .apply(
                  i,
                  (lookTarget, walkTarget, wantedItem, cooldown) -> (level, body, timestamp) -> {
                     ItemEntity item = i.get(wantedItem);
                     if (i.tryGet(cooldown).isEmpty()
                        && predicate.test((E)body)
                        && item.closerThan(body, maxDistToWalk)
                        && body.level().getWorldBorder().isWithinBounds(item.blockPosition())
                        && body.canPickUpLoot()) {
                        WalkTarget target = new WalkTarget(new EntityTracker(item, false), speedModifier, 0);
                        lookTarget.set(new EntityTracker(item, true));
                        walkTarget.set(target);
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
