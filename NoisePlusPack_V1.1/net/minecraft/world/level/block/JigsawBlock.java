package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;

public class JigsawBlock extends Block implements EntityBlock, GameMasterBlock {
   public static final MapCodec<JigsawBlock> CODEC = simpleCodec(JigsawBlock::new);
   public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

   @Override
   public MapCodec<JigsawBlock> codec() {
      return CODEC;
   }

   protected JigsawBlock(BlockBehaviour.Properties p_54225_) {
      super(p_54225_);
      this.registerDefaultState(this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54244_) {
      p_54244_.add(ORIENTATION);
   }

   @Override
   protected BlockState rotate(BlockState p_54241_, Rotation p_54242_) {
      return p_54241_.setValue(ORIENTATION, p_54242_.rotation().rotate(p_54241_.getValue(ORIENTATION)));
   }

   @Override
   protected BlockState mirror(BlockState p_54238_, Mirror p_54239_) {
      return p_54238_.setValue(ORIENTATION, p_54239_.rotation().rotate(p_54238_.getValue(ORIENTATION)));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54227_) {
      Direction direction = p_54227_.getClickedFace();
      Direction direction1;
      if (direction.getAxis() == Direction.Axis.Y) {
         direction1 = p_54227_.getHorizontalDirection().getOpposite();
      } else {
         direction1 = Direction.UP;
      }

      return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(direction, direction1));
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153448_, BlockState p_153449_) {
      return new JigsawBlockEntity(p_153448_, p_153449_);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_54231_, Level p_54232_, BlockPos p_54233_, Player p_54234_, BlockHitResult p_54236_) {
      BlockEntity blockentity = p_54232_.getBlockEntity(p_54233_);
      if (blockentity instanceof JigsawBlockEntity && p_54234_.canUseGameMasterBlocks()) {
         p_54234_.openJigsawBlock((JigsawBlockEntity)blockentity);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static boolean canAttach(StructureTemplate.JigsawBlockInfo p_368058_, StructureTemplate.JigsawBlockInfo p_367580_) {
      Direction direction = getFrontFacing(p_368058_.info().state());
      Direction direction1 = getFrontFacing(p_367580_.info().state());
      Direction direction2 = getTopFacing(p_368058_.info().state());
      Direction direction3 = getTopFacing(p_367580_.info().state());
      JigsawBlockEntity.JointType jigsawblockentity$jointtype = p_368058_.jointType();
      boolean flag = jigsawblockentity$jointtype == JigsawBlockEntity.JointType.ROLLABLE;
      return direction == direction1.getOpposite() && (flag || direction2 == direction3) && p_368058_.target().equals(p_367580_.name());
   }

   public static Direction getFrontFacing(BlockState p_54251_) {
      return p_54251_.getValue(ORIENTATION).front();
   }

   public static Direction getTopFacing(BlockState p_54253_) {
      return p_54253_.getValue(ORIENTATION).top();
   }
}
