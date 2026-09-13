package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BrushableBlock extends BaseEntityBlock implements Fallable {
   private static final IntegerProperty DUSTED = BlockStateProperties.DUSTED;
   public static final int TICK_DELAY = 2;
   private final Block turnsInto;
   private final SoundEvent brushSound;
   private final SoundEvent brushCompletedSound;

   public BrushableBlock(final Block turnsInto, final SoundEvent brushSound, final SoundEvent brushCompletedSound, final BlockBehaviour.Properties properties) {
      super(properties);
      this.turnsInto = turnsInto;
      this.brushSound = brushSound;
      this.brushCompletedSound = brushCompletedSound;
      this.registerDefaultState(this.stateDefinition.any().setValue(DUSTED, 0));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(DUSTED);
   }

   @Override
   public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      level.scheduleTick(pos, this, 2);
   }

   @Override
   public BlockState updateShape(
      final BlockState state,
      final LevelReader level,
      final ScheduledTickAccess ticks,
      final BlockPos pos,
      final Direction directionToNeighbour,
      final BlockPos neighbourPos,
      final BlockState neighbourState,
      final RandomSource random
   ) {
      ticks.scheduleTick(pos, this, 2);
      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushableBlockEntity) {
         brushableBlockEntity.checkReset(level);
      }

      if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinY()) {
         FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
         entity.disableDrop();
      }
   }

   @Override
   public void onBrokenAfterFall(final Level level, final BlockPos pos, final FallingBlockEntity entity) {
      Vec3 centerOfEntity = entity.getBoundingBox().getCenter();
      level.levelEvent(2001, BlockPos.containing(centerOfEntity), Block.getId(entity.getBlockState()));
      level.gameEvent(entity, GameEvent.BLOCK_DESTROY, centerOfEntity);
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (random.nextInt(16) == 0) {
         BlockPos below = pos.below();
         if (FallingBlock.isFree(level.getBlockState(below))) {
            double xx = pos.getX() + random.nextDouble();
            double yy = pos.getY() - 0.05;
            double zz = pos.getZ() + random.nextDouble();
            level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), xx, yy, zz, 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new BrushableBlockEntity(worldPosition, blockState);
   }

   public Block getTurnsInto() {
      return this.turnsInto;
   }

   public SoundEvent getBrushSound() {
      return this.brushSound;
   }

   public SoundEvent getBrushCompletedSound() {
      return this.brushCompletedSound;
   }
}
