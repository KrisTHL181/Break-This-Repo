package net.minecraft.world.entity.monster.warden;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.warden.Digging;
import net.minecraft.world.entity.ai.behavior.warden.Emerging;
import net.minecraft.world.entity.ai.behavior.warden.ForceUnmount;
import net.minecraft.world.entity.ai.behavior.warden.Roar;
import net.minecraft.world.entity.ai.behavior.warden.SetRoarTarget;
import net.minecraft.world.entity.ai.behavior.warden.SetWardenLookTarget;
import net.minecraft.world.entity.ai.behavior.warden.Sniffing;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.ai.behavior.warden.TryToSniff;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public class WardenAi {
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.5F;
    private static final float SPEED_MULTIPLIER_WHEN_INVESTIGATING = 0.7F;
    private static final float SPEED_MULTIPLIER_WHEN_FIGHTING = 1.2F;
    private static final int MELEE_ATTACK_COOLDOWN = 18;
    private static final int DIGGING_DURATION = Mth.ceil(100.0F);
    public static final int EMERGE_DURATION = Mth.ceil(133.59999F);
    public static final int ROAR_DURATION = Mth.ceil(84.0F);
    private static final int SNIFFING_DURATION = Mth.ceil(83.2F);
    public static final int DIGGING_COOLDOWN = 1200;
    private static final int DISTURBANCE_LOCATION_EXPIRY_TIME = 100;
    private static final BehaviorControl<Warden> DIG_COOLDOWN_SETTER = BehaviorBuilder.create(
        i -> i.group(i.registered(MemoryModuleType.DIG_COOLDOWN)).apply(i, cooldown -> (level, body, timestamp) -> {
            if (i.tryGet(cooldown).isPresent()) {
                cooldown.setWithExpiry(Unit.INSTANCE, 1200L);
            }

            return true;
        })
    );

    protected static List<ActivityData<Warden>> getActivities(final Warden body) {
        return List.of(
            initCoreActivity(),
            initEmergeActivity(),
            initDiggingActivity(),
            initIdleActivity(),
            initRoarActivity(),
            initFightActivity(body),
            initInvestigateActivity(),
            initSniffingActivity()
        );
    }

    public static void updateActivity(final Brain<Warden> brain) {
        brain.setActiveActivityToFirstValid(
            ImmutableList.of(Activity.EMERGE, Activity.DIG, Activity.ROAR, Activity.FIGHT, Activity.INVESTIGATE, Activity.SNIFF, Activity.IDLE)
        );
    }

    private static ActivityData<Warden> initCoreActivity() {
        return ActivityData.<Warden>create(
            Activity.CORE, 0, ImmutableList.of(new Swim<>(0.8F), SetWardenLookTarget.create(), new LookAtTargetSink(45, 90), new MoveToTargetSink())
        );
    }

    private static ActivityData<Warden> initEmergeActivity() {
        return ActivityData.create(Activity.EMERGE, 5, ImmutableList.of(new Emerging<>(EMERGE_DURATION)), MemoryModuleType.IS_EMERGING);
    }

    private static ActivityData<Warden> initDiggingActivity() {
        return ActivityData.<Warden>create(
            Activity.DIG,
            ImmutableList.of(Pair.of(0, new ForceUnmount()), Pair.of(1, new Digging<>(DIGGING_DURATION))),
            ImmutableSet.of(Pair.of(MemoryModuleType.ROAR_TARGET, MemoryStatus.VALUE_ABSENT), Pair.of(MemoryModuleType.DIG_COOLDOWN, MemoryStatus.VALUE_ABSENT))
        );
    }

    private static ActivityData<Warden> initIdleActivity() {
        return ActivityData.<Warden>create(
            Activity.IDLE,
            10,
            ImmutableList.of(
                SetRoarTarget.create(Warden::getEntityAngryAt),
                TryToSniff.create(),
                new RunOne<>(
                    ImmutableMap.of(MemoryModuleType.IS_SNIFFING, MemoryStatus.VALUE_ABSENT),
                    ImmutableList.of(Pair.of(RandomStroll.stroll(0.5F), 2), Pair.of(new DoNothing(30, 60), 1))
                )
            )
        );
    }

    private static ActivityData<Warden> initInvestigateActivity() {
        return ActivityData.<Warden>create(
            Activity.INVESTIGATE,
            5,
            ImmutableList.of(SetRoarTarget.create(Warden::getEntityAngryAt), GoToTargetLocation.create(MemoryModuleType.DISTURBANCE_LOCATION, 2, 0.7F)),
            MemoryModuleType.DISTURBANCE_LOCATION
        );
    }

    private static ActivityData<Warden> initSniffingActivity() {
        return ActivityData.create(
            Activity.SNIFF,
            5,
            ImmutableList.of(SetRoarTarget.create(Warden::getEntityAngryAt), new Sniffing<>(SNIFFING_DURATION)),
            MemoryModuleType.IS_SNIFFING
        );
    }

    private static ActivityData<Warden> initRoarActivity() {
        return ActivityData.create(Activity.ROAR, 10, ImmutableList.of(new Roar()), MemoryModuleType.ROAR_TARGET);
    }

    private static ActivityData<Warden> initFightActivity(final Warden body) {
        return ActivityData.<Warden>create(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                DIG_COOLDOWN_SETTER,
                StopAttackingIfTargetInvalid.<Warden>create(
                    (level, target) -> !body.getAngerLevel().isAngry() || !body.canTargetEntity(target), WardenAi::onTargetInvalid, false
                ),
                SetEntityLookTarget.create(entity -> isTarget(body, entity), (float)body.getAttributeValue(Attributes.FOLLOW_RANGE)),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.2F),
                new SonicBoom(),
                MeleeAttack.create(18)
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static boolean isTarget(final Warden body, final LivingEntity living) {
        return body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(e -> e == living).isPresent();
    }

    private static void onTargetInvalid(final ServerLevel level, final Warden body, final LivingEntity attackTarget) {
        if (!body.canTargetEntity(attackTarget)) {
            body.clearAnger(attackTarget);
        }

        setDigCooldown(body);
    }

    public static void setDigCooldown(final LivingEntity body) {
        if (body.getBrain().hasMemoryValue(MemoryModuleType.DIG_COOLDOWN)) {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        }
    }

    public static void setDisturbanceLocation(final Warden body, final BlockPos position) {
        if (body.level().getWorldBorder().isWithinBounds(position)
            && !body.getEntityAngryAt().isPresent()
            && !body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()) {
            setDigCooldown(body);
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 100L);
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(position), 100L);
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.DISTURBANCE_LOCATION, position, 100L);
            body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }
}
