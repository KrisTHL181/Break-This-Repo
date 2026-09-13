package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class GoAndGiveItemsToTarget<E extends LivingEntity & InventoryCarrier> extends Behavior<E> {
    private static final int CLOSE_ENOUGH_DISTANCE_TO_TARGET = 3;
    private static final int ITEM_PICKUP_COOLDOWN_AFTER_THROWING = 60;
    private final Vec3 throwVelocity;
    private final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter;
    private final float speedModifier;
    private final GoAndGiveItemsToTarget.ItemThrower<E> itemThrower;

    public GoAndGiveItemsToTarget(
        final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter,
        final float speedModifier,
        final int timeoutDuration,
        final GoAndGiveItemsToTarget.ItemThrower<E> itemThrower
    ) {
        super(
            Map.of(
                MemoryModuleType.LOOK_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
                MemoryStatus.REGISTERED
            ),
            timeoutDuration
        );
        this.targetPositionGetter = targetPositionGetter;
        this.speedModifier = speedModifier;
        this.itemThrower = itemThrower;
        this.throwVelocity = new Vec3(0.2F, 0.3F, 0.2F);
    }

    @Override
    protected boolean checkExtraStartConditions(final ServerLevel level, final E body) {
        return this.canThrowItemToTarget(body);
    }

    @Override
    protected boolean canStillUse(final ServerLevel level, final E body, final long timestamp) {
        return this.canThrowItemToTarget(body);
    }

    @Override
    protected void start(final ServerLevel level, final E body, final long timestamp) {
        this.targetPositionGetter
            .apply(body)
            .ifPresent(positionTracker -> BehaviorUtils.setWalkAndLookTargetMemories(body, positionTracker, this.speedModifier, 3));
    }

    @Override
    protected void tick(final ServerLevel level, final E body, final long timestamp) {
        Optional<PositionTracker> targetPosition = this.targetPositionGetter.apply(body);
        if (!targetPosition.isEmpty()) {
            PositionTracker depositTarget = targetPosition.get();
            Vec3 depositPosition = depositTarget.currentPosition();
            double distanceToTarget = depositPosition.distanceTo(body.getEyePosition());
            if (distanceToTarget < 3.0) {
                ItemStack item = body.getInventory().removeItem(0, 1);
                if (!item.isEmpty()) {
                    BehaviorUtils.throwItem(body, item, depositPosition.add(0.0, 1.0, 0.0), this.throwVelocity, 0.2F);
                    this.itemThrower.onItemThrown(level, body, item, depositTarget.currentBlockPosition());
                    body.getBrain().setMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, 60);
                }
            }
        }
    }

    private boolean canThrowItemToTarget(final E body) {
        if (body.getInventory().isEmpty()) {
            return false;
        }

        Optional<PositionTracker> positionTracker = this.targetPositionGetter.apply(body);
        return positionTracker.isPresent();
    }

    @FunctionalInterface
    public interface ItemThrower<E> {
        void onItemThrown(ServerLevel level, E thrower, ItemStack item, final BlockPos targetPos);
    }
}