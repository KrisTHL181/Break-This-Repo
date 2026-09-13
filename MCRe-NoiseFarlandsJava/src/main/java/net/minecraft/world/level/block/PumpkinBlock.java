package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;

public class PumpkinBlock extends Block {
    public static final MapCodec<PumpkinBlock> CODEC = simpleCodec(PumpkinBlock::new);

    @Override
    public MapCodec<PumpkinBlock> codec() {
        return CODEC;
    }

    protected PumpkinBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(
        final ItemStack itemStack,
        final BlockState state,
        final Level level,
        final BlockPos pos,
        final Player player,
        final InteractionHand hand,
        final BlockHitResult hitResult
    ) {
        if (!itemStack.is(Items.SHEARS)) {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        } else if (level instanceof ServerLevel serverLevel) {
            Direction clickedDirection = hitResult.getDirection();
            Direction direction = clickedDirection.getAxis() == Direction.Axis.Y ? player.getDirection().getOpposite() : clickedDirection;
            dropFromBlockInteractLootTable(
                serverLevel,
                BuiltInLootTables.CARVE_PUMPKIN,
                state,
                level.getBlockEntity(pos),
                itemStack,
                player,
                (ignored, pumpkinSeeds) -> {
                    ItemEntity entity = new ItemEntity(
                        level, pos.getX() + 0.5 + direction.getStepX() * 0.65, pos.getY() + 0.1, pos.getZ() + 0.5 + direction.getStepZ() * 0.65, pumpkinSeeds
                    );
                    RandomSource random = level.getRandom();
                    entity.setDeltaMovement(
                        0.05 * direction.getStepX() + random.nextDouble() * 0.02, 0.05, 0.05 * direction.getStepZ() + random.nextDouble() * 0.02
                    );
                    level.addFreshEntity(entity);
                }
            );
            level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, direction), 11);
            itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            level.gameEvent(player, GameEvent.SHEAR, pos);
            player.awardStat(Stats.ITEM_USED.get(Items.SHEARS));
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }
}