package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractCauldronBlock extends Block {
   protected static final int FLOOR_LEVEL = 4;
   private static final VoxelShape SHAPE_INSIDE = Block.column(12.0, 4.0, 16.0);
   protected static final VoxelShape SHAPE = Util.make(
      () -> {
         int legWidth = 4;
         int legHeight = 3;
         int legThickness = 2;
         return Shapes.join(
            Shapes.block(),
            Shapes.or(Block.column(16.0, 8.0, 0.0, 3.0), Block.column(8.0, 16.0, 0.0, 3.0), Block.column(12.0, 0.0, 3.0), SHAPE_INSIDE),
            BooleanOp.ONLY_FIRST
         );
      }
   );
   protected final CauldronInteraction.Dispatcher interactions;

   public AbstractCauldronBlock(final BlockBehaviour.Properties properties, final CauldronInteraction.Dispatcher interactions) {
      super(properties);
      this.interactions = interactions;
   }

   protected double getContentHeight(final BlockState state) {
      return 0.0;
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
      CauldronInteraction behavior = this.interactions.get(itemStack);
      return behavior.interact(state, level, pos, player, hand, itemStack);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return SHAPE_INSIDE;
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   public abstract boolean isFull(final BlockState state);

   @Override
   protected void tick(final BlockState cauldronState, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      BlockPos stalactitePos = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
      if (stalactitePos != null) {
         Fluid fluid = PointedDripstoneBlock.getCauldronFillFluidType(level, stalactitePos);
         if (fluid != Fluids.EMPTY && this.canReceiveStalactiteDrip(fluid)) {
            this.receiveStalactiteDrip(cauldronState, level, pos, fluid);
         }
      }
   }

   protected boolean canReceiveStalactiteDrip(final Fluid fluid) {
      return false;
   }

   protected void receiveStalactiteDrip(final BlockState state, final Level level, final BlockPos pos, final Fluid fluid) {
   }
}
