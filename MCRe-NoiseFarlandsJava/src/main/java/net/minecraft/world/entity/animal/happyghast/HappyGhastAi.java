package net.minecraft.world.entity.animal.happyghast;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Set;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public class HappyGhastAi {
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25F;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 1.1F;
    private static final double BABY_GHAST_CLOSE_ENOUGH_DIST = 3.0;
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(3, 16);

    protected static List<ActivityData<HappyGhast>> getActivities() {
        return List.of(initCoreActivity(), initIdleActivity(), initPanicActivity());
    }

    private static ActivityData<HappyGhast> initCoreActivity() {
        return ActivityData.<HappyGhast>create(
            Activity.CORE,
            0,
            ImmutableList.of(
                new Swim<>(0.8F),
                new AnimalPanic<HappyGhast>(2.0F, 0),
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
            )
        );
    }

    private static ActivityData<HappyGhast> initIdleActivity() {
        return ActivityData.<HappyGhast>create(
            Activity.IDLE,
            ImmutableList.of(
                Pair.of(1, new FollowTemptation(mob -> 1.25F, mob -> 3.0, true)),
                Pair.of(2, BabyFollowAdult.create(ADULT_FOLLOW_RANGE, mob -> 1.1F, MemoryModuleType.NEAREST_VISIBLE_PLAYER, true)),
                Pair.of(3, BabyFollowAdult.create(ADULT_FOLLOW_RANGE, mob -> 1.1F, MemoryModuleType.NEAREST_VISIBLE_ADULT, true)),
                Pair.of(4, new RunOne<>(ImmutableList.of(Pair.of(RandomStroll.fly(1.0F), 1), Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 1))))
            )
        );
    }

    private static ActivityData<HappyGhast> initPanicActivity() {
        return ActivityData.create(Activity.PANIC, ImmutableList.of(), Set.of(Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT)));
    }

    public static void updateActivity(final HappyGhast body) {
        body.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
    }
}
