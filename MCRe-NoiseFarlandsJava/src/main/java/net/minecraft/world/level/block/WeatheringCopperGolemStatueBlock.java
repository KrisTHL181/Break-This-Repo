package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WeatheringCopperGolemStatueBlock extends CopperGolemStatueBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge), propertiesCodec())
            .apply(i, WeatheringCopperGolemStatueBlock::new)
    );

    @Override
    public MapCodec<WeatheringCopperGolemStatueBlock> codec() {
        return CODEC;
    }

    public WeatheringCopperGolemStatueBlock(final WeatheringCopper.WeatherState weatherState, final BlockBehaviour.Properties properties) {
        super(weatherState, properties);
    }

    @Override
    protected boolean isRandomlyTicking(final BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    @Override
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        this.changeOverTime(state, level, pos, random);
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.getWeatheringState();
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
        if (level.getBlockEntity(pos) instanceof CopperGolemStatueBlockEntity copperGolemStatueBlockEntity) {
            if (!itemStack.is(ItemTags.AXES)) {
                if (itemStack.is(Items.HONEYCOMB)) {
                    return InteractionResult.PASS;
                }

                this.updatePose(level, state, pos, player);
                return InteractionResult.SUCCESS;
            }

            if (this.getAge().equals(WeatheringCopper.WeatherState.UNAFFECTED)) {
                CopperGolem copperGolem = copperGolemStatueBlockEntity.removeStatue(state);
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                if (copperGolem != null) {
                    level.addFreshEntity(copperGolem);
                    level.removeBlock(pos, false);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }
}