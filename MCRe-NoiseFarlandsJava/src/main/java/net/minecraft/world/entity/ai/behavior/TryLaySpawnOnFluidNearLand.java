package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class TryLaySpawnOnFluidNearLand {
    public static BehaviorControl<LivingEntity> create(final Block spawnBlock) {
        return BehaviorBuilder.create(
            i -> i.group(i.absent(MemoryModuleType.ATTACK_TARGET), i.present(MemoryModuleType.WALK_TARGET), i.present(MemoryModuleType.IS_PREGNANT))
                .apply(
                    i,
                    (attackTarget, walkTarget, pregnant) -> (level, body, timestamp) -> {
                        if (!body.isInWater() && body.onGround()) {
                            BlockPos belowPos = body.blockPosition().below();

                            for (Direction direction : Direction.Plane.HORIZONTAL) {
                                BlockPos relativePos = belowPos.relative(direction);
                                if (level.getBlockState(relativePos).getCollisionShape(level, relativePos).getFaceShape(Direction.UP).isEmpty()
                                    && (
                                        level.getFluidState(relativePos).is(FluidTags.SUPPORTS_FROGSPAWN)
                                            || level.getBlockState(relativePos).is(BlockTags.SUPPORTS_FROGSPAWN)
                                    )) {
                                    BlockPos spawnPos = relativePos.above();
                                    if (level.getBlockState(spawnPos).isAir()) {
                                        BlockState newState = spawnBlock.defaultBlockState();
                                        level.setBlock(spawnPos, newState, 3);
                                        level.gameEvent(GameEvent.BLOCK_PLACE, spawnPos, GameEvent.Context.of(body, newState));
                                        level.playSound(null, body, SoundEvents.FROG_LAY_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                                        pregnant.erase();
                                        return true;
                                    }
                                }
                            }

                            return true;
                        } else {
                            return false;
                        }
                    }
                )
        );
    }
}