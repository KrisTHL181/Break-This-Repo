package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class NoteBlock extends Block {
    public static final MapCodec<NoteBlock> CODEC = simpleCodec(NoteBlock::new);
    public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty NOTE = BlockStateProperties.NOTE;
    public static final int NOTE_VOLUME = 3;

    @Override
    public MapCodec<NoteBlock> codec() {
        return CODEC;
    }

    public NoteBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(INSTRUMENT, NoteBlockInstrument.HARP).setValue(NOTE, 0).setValue(POWERED, false));
    }

    private BlockState setInstrument(final LevelReader level, final BlockPos position, final BlockState state) {
        NoteBlockInstrument instrumentAbove = level.getBlockState(position.above()).instrument();
        if (instrumentAbove.worksAboveNoteBlock()) {
            return state.setValue(INSTRUMENT, instrumentAbove);
        }

        NoteBlockInstrument instrumentBelow = level.getBlockState(position.below()).instrument();
        NoteBlockInstrument newBelow = instrumentBelow.worksAboveNoteBlock() ? NoteBlockInstrument.HARP : instrumentBelow;
        return state.setValue(INSTRUMENT, newBelow);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.setInstrument(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(
        final BlockState state,
        final LevelReader level,
        final ScheduledTickAccess ticks,
        final BlockPos pos,
        final Direction directionToNeighbour,
        final BlockPos neighbourPos,
        final BlockState neighbourState,
        final RandomSource random
    ) {
        boolean neighborDirectionSetsInstrument = directionToNeighbour.getAxis() == Direction.Axis.Y;
        return neighborDirectionSetsInstrument
            ? this.setInstrument(level, pos, state)
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void neighborChanged(
        final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
    ) {
        boolean signal = level.hasNeighborSignal(pos);
        if (signal != state.getValue(POWERED)) {
            if (signal) {
                this.playNote(null, state, level, pos);
            }

            level.setBlock(pos, state.setValue(POWERED, signal), 3);
        }
    }

    private void playNote(final @Nullable Entity source, final BlockState state, final Level level, final BlockPos pos) {
        if (state.getValue(INSTRUMENT).worksAboveNoteBlock() || level.getBlockState(pos.above()).isAir()) {
            level.blockEvent(pos, this, 0, 0);
            level.gameEvent(source, GameEvent.NOTE_BLOCK_PLAY, pos);
        }
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
        return itemStack.is(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS) && hitResult.getDirection() == Direction.UP
            ? InteractionResult.PASS
            : super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            state = state.cycle(NOTE);
            level.setBlock(pos, state, 3);
            this.playNote(player, state, level, pos);
            player.awardStat(Stats.TUNE_NOTEBLOCK);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        if (!level.isClientSide()) {
            this.playNote(player, state, level, pos);
            player.awardStat(Stats.PLAY_NOTEBLOCK);
        }
    }

    public static float getPitchFromNote(final int twoOctaveRangeNote) {
        return (float)Math.pow(2.0, (twoOctaveRangeNote - 12) / 12.0);
    }

    @Override
    protected boolean triggerEvent(final BlockState state, final Level level, final BlockPos pos, final int b0, final int b1) {
        NoteBlockInstrument instrument = state.getValue(INSTRUMENT);
        float pitch;
        if (instrument.isTunable()) {
            int note = state.getValue(NOTE);
            pitch = getPitchFromNote(note);
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, note / 24.0, 0.0, 0.0);
        } else {
            pitch = 1.0F;
        }

        Holder<SoundEvent> soundEvent;
        if (instrument.hasCustomSound()) {
            Identifier soundId = this.getCustomSoundId(level, pos);
            if (soundId == null) {
                return false;
            }

            soundEvent = Holder.direct(SoundEvent.createVariableRangeEvent(soundId));
        } else {
            soundEvent = instrument.getSoundEvent();
        }

        level.playSeededSound(
            null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, soundEvent, SoundSource.RECORDS, 3.0F, pitch, level.getRandom().nextLong()
        );
        return true;
    }

    private @Nullable Identifier getCustomSoundId(final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos.above()) instanceof SkullBlockEntity head ? head.getNoteBlockSound() : null;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INSTRUMENT, POWERED, NOTE);
    }
}