package net.minecraft.world.level.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StrawBedBlock extends AbstractBedBlock {
   private static final VoxelShape BASE_SHAPE = Block.column(16.0, 0.0, 4.0);
   private static final VoxelShape PILLOW_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 8.0);
   private static final Map<Direction, VoxelShape> FOOT_SHAPES = Util.make(() -> Shapes.rotateHorizontal(BASE_SHAPE));
   private static final Map<Direction, VoxelShape> HEAD_SHAPES = Util.make(() -> Shapes.rotateHorizontal(Shapes.or(BASE_SHAPE, PILLOW_SHAPE)));

   public StrawBedBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   private void destroyBed(final Level level, final BlockPos pos) {
      level.playSound(null, pos, SoundEvents.STRAW_BED_BREAK_LEAVE, SoundSource.BLOCKS, 1.0F, 1.0F);
      level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      Map<Direction, VoxelShape> shapes = state.getValue(PART) == BedPart.HEAD ? HEAD_SHAPES : FOOT_SHAPES;
      return shapes.get(getConnectedDirection(state).getOpposite());
   }

   @Override
   protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
      return EnvironmentAttributes.STRAW_BED_RULE;
   }

   @Override
   public void spawnDestroyParticles(final Level level, final BlockPos pos, final BlockState state) {
      level.levelEvent(2014, pos, getId(state));
   }

   @Override
   public Identifier getSleptInBedStatType() {
      return Stats.SLEEP_IN_STRAW_BED;
   }

   @Override
   public double getSleepHeight(final BlockState state, final Level level, final BlockPos pos) {
      BlockPos layingOnPos;
      BlockState layingOnState;
      if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
         layingOnPos = pos.relative(getConnectedDirection(state));
         layingOnState = level.getBlockState(layingOnPos);
      } else {
         layingOnPos = pos;
         layingOnState = state;
      }

      return super.getSleepHeight(layingOnState, level, layingOnPos);
   }

   @Override
   protected InteractionResult destroyOnUse(final BlockState state, final Level level, final BlockPos pos, final Player player) {
      this.destroyBed(level, pos);
      return InteractionResult.SUCCESS_SERVER;
   }

   @Override
   protected void destroyOnLeave(final Level level, final BlockPos pos) {
      this.destroyBed(level, pos);
   }
}
