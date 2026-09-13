package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class AnvilBlock extends FallingBlock {
   public static final MapCodec<AnvilBlock> CODEC = simpleCodec(AnvilBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(
      Shapes.or(Block.column(12.0, 0.0, 4.0), Block.column(8.0, 10.0, 4.0, 5.0), Block.column(4.0, 8.0, 5.0, 10.0), Block.column(10.0, 16.0, 10.0, 16.0))
   );
   private static final Component CONTAINER_TITLE = Component.translatable("container.repair");
   private static final float FALL_DAMAGE_PER_DISTANCE = 2.0F;
   private static final int FALL_DAMAGE_MAX = 40;

   @Override
   public MapCodec<AnvilBlock> codec() {
      return CODEC;
   }

   public AnvilBlock(BlockBehaviour.Properties p_48777_) {
      super(p_48777_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_48781_) {
      return this.defaultBlockState().setValue(FACING, p_48781_.getHorizontalDirection().getClockWise());
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_48804_, Level p_48805_, BlockPos p_48806_, Player p_48807_, BlockHitResult p_48809_) {
      if (!p_48805_.isClientSide()) {
         p_48807_.openMenu(p_48804_.getMenuProvider(p_48805_, p_48806_));
         p_48807_.awardStat(Stats.INTERACT_WITH_ANVIL);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   protected @Nullable MenuProvider getMenuProvider(BlockState p_48821_, Level p_48822_, BlockPos p_48823_) {
      return new SimpleMenuProvider(
         (p_48785_, p_48786_, p_48787_) -> new AnvilMenu(p_48785_, p_48786_, ContainerLevelAccess.create(p_48822_, p_48823_)), CONTAINER_TITLE
      );
   }

   @Override
   protected VoxelShape getShape(BlockState p_48816_, BlockGetter p_48817_, BlockPos p_48818_, CollisionContext p_48819_) {
      return SHAPES.get(p_48816_.getValue(FACING).getAxis());
   }

   @Override
   protected void falling(FallingBlockEntity p_48779_) {
      p_48779_.setHurtsEntities(2.0F, 40);
   }

   @Override
   public void onLand(Level p_48793_, BlockPos p_48794_, BlockState p_48795_, BlockState p_48796_, FallingBlockEntity p_48797_) {
      if (!p_48797_.isSilent()) {
         p_48793_.levelEvent(1031, p_48794_, 0);
      }
   }

   @Override
   public void onBrokenAfterFall(Level p_152053_, BlockPos p_152054_, FallingBlockEntity p_152055_) {
      if (!p_152055_.isSilent()) {
         p_152053_.levelEvent(1029, p_152054_, 0);
      }
   }

   @Override
   public DamageSource getFallDamageSource(Entity p_254036_) {
      return p_254036_.damageSources().anvil(p_254036_);
   }

   public static @Nullable BlockState damage(BlockState p_48825_) {
      if (p_48825_.is(Blocks.ANVIL)) {
         return Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(FACING, p_48825_.getValue(FACING));
      } else {
         return p_48825_.is(Blocks.CHIPPED_ANVIL) ? Blocks.DAMAGED_ANVIL.defaultBlockState().setValue(FACING, p_48825_.getValue(FACING)) : null;
      }
   }

   @Override
   protected BlockState rotate(BlockState p_48811_, Rotation p_48812_) {
      return p_48811_.setValue(FACING, p_48812_.rotate(p_48811_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48814_) {
      p_48814_.add(FACING);
   }

   @Override
   protected boolean isPathfindable(BlockState p_48799_, PathComputationType p_48802_) {
      return false;
   }

   @Override
   public int getDustColor(BlockState p_48827_, BlockGetter p_48828_, BlockPos p_48829_) {
      return p_48827_.getMapColor(p_48828_, p_48829_).col;
   }
}
