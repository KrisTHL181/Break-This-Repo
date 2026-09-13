package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PistonBaseBlock extends DirectionalBlock {
   public static final MapCodec<PistonBaseBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422193_ -> p_422193_.group(Codec.BOOL.fieldOf("sticky").forGetter(p_309381_ -> p_309381_.isSticky), propertiesCodec())
         .apply(p_422193_, PistonBaseBlock::new)
   );
   public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
   public static final int TRIGGER_EXTEND = 0;
   public static final int TRIGGER_CONTRACT = 1;
   public static final int TRIGGER_DROP = 2;
   public static final int PLATFORM_THICKNESS = 4;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.boxZ(16.0, 4.0, 16.0));
   private final boolean isSticky;

   @Override
   public MapCodec<PistonBaseBlock> codec() {
      return CODEC;
   }

   public PistonBaseBlock(boolean p_60163_, BlockBehaviour.Properties p_60164_) {
      super(p_60164_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EXTENDED, false));
      this.isSticky = p_60163_;
   }

   @Override
   protected VoxelShape getShape(BlockState p_60220_, BlockGetter p_60221_, BlockPos p_60222_, CollisionContext p_60223_) {
      return p_60220_.getValue(EXTENDED) ? SHAPES.get(p_60220_.getValue(FACING)) : Shapes.block();
   }

   @Override
   public void setPlacedBy(Level p_60172_, BlockPos p_60173_, BlockState p_60174_, @Nullable LivingEntity p_60175_, ItemStack p_60176_) {
      if (!p_60172_.isClientSide()) {
         this.checkIfExtend(p_60172_, p_60173_, p_60174_);
      }
   }

   @Override
   protected void neighborChanged(BlockState p_60198_, Level p_60199_, BlockPos p_60200_, Block p_60201_, @Nullable Orientation p_366407_, boolean p_60203_) {
      if (!p_60199_.isClientSide()) {
         this.checkIfExtend(p_60199_, p_60200_, p_60198_);
      }
   }

   @Override
   protected void onPlace(BlockState p_60225_, Level p_60226_, BlockPos p_60227_, BlockState p_60228_, boolean p_60229_) {
      if (!p_60228_.is(p_60225_.getBlock()) && !p_60226_.isClientSide() && p_60226_.getBlockEntity(p_60227_) == null) {
         this.checkIfExtend(p_60226_, p_60227_, p_60225_);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_60166_) {
      return this.defaultBlockState().setValue(FACING, p_60166_.getNearestLookingDirection().getOpposite()).setValue(EXTENDED, false);
   }

   private void checkIfExtend(Level p_60168_, BlockPos p_60169_, BlockState p_60170_) {
      Direction direction = p_60170_.getValue(FACING);
      boolean flag = this.getNeighborSignal(p_60168_, p_60169_, direction);
      if (flag && !p_60170_.getValue(EXTENDED)) {
         if (new PistonStructureResolver(p_60168_, p_60169_, direction, true).resolve()) {
            p_60168_.blockEvent(p_60169_, this, 0, direction.get3DDataValue());
         }
      } else if (!flag && p_60170_.getValue(EXTENDED)) {
         BlockPos blockpos = p_60169_.relative(direction, 2);
         BlockState blockstate = p_60168_.getBlockState(blockpos);
         int i = 1;
         if (blockstate.is(Blocks.MOVING_PISTON)
            && blockstate.getValue(FACING) == direction
            && p_60168_.getBlockEntity(blockpos) instanceof PistonMovingBlockEntity pistonmovingblockentity
            && pistonmovingblockentity.isExtending()
            && (
               pistonmovingblockentity.getProgress(0.0F) < 0.5F
                  || p_60168_.getGameTime() == pistonmovingblockentity.getLastTicked()
                  || ((ServerLevel)p_60168_).isHandlingTick()
            )) {
            i = 2;
         }

         p_60168_.blockEvent(p_60169_, this, i, direction.get3DDataValue());
      }
   }

   private boolean getNeighborSignal(SignalGetter p_277378_, BlockPos p_60179_, Direction p_60180_) {
      for (Direction direction : Direction.values()) {
         if (direction != p_60180_ && p_277378_.hasSignal(p_60179_.relative(direction), direction)) {
            return true;
         }
      }

      if (p_277378_.hasSignal(p_60179_, Direction.DOWN)) {
         return true;
      }

      BlockPos blockpos = p_60179_.above();

      for (Direction direction1 : Direction.values()) {
         if (direction1 != Direction.DOWN && p_277378_.hasSignal(blockpos.relative(direction1), direction1)) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected boolean triggerEvent(BlockState p_60192_, Level p_60193_, BlockPos p_60194_, int p_60195_, int p_60196_) {
      Direction direction = p_60192_.getValue(FACING);
      BlockState blockstate = p_60192_.setValue(EXTENDED, true);
      if (!p_60193_.isClientSide()) {
         boolean flag = this.getNeighborSignal(p_60193_, p_60194_, direction);
         if (flag && (p_60195_ == 1 || p_60195_ == 2)) {
            p_60193_.setBlock(p_60194_, blockstate, 2);
            return false;
         }

         if (!flag && p_60195_ == 0) {
            return false;
         }
      }

      if (p_60195_ == 0) {
         if (!this.moveBlocks(p_60193_, p_60194_, direction, true)) {
            return false;
         }

         p_60193_.setBlock(p_60194_, blockstate, 67);
         p_60193_.playSound(null, p_60194_, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, p_60193_.random.nextFloat() * 0.25F + 0.6F);
         p_60193_.gameEvent(GameEvent.BLOCK_ACTIVATE, p_60194_, GameEvent.Context.of(blockstate));
      } else if (p_60195_ == 1 || p_60195_ == 2) {
         BlockEntity blockentity = p_60193_.getBlockEntity(p_60194_.relative(direction));
         if (blockentity instanceof PistonMovingBlockEntity) {
            ((PistonMovingBlockEntity)blockentity).finalTick();
         }

         BlockState blockstate1 = Blocks.MOVING_PISTON
            .defaultBlockState()
            .setValue(MovingPistonBlock.FACING, direction)
            .setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
         p_60193_.setBlock(p_60194_, blockstate1, 276);
         p_60193_.setBlockEntity(
            MovingPistonBlock.newMovingBlockEntity(
               p_60194_, blockstate1, this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(p_60196_ & 7)), direction, false, true
            )
         );
         p_60193_.updateNeighborsAt(p_60194_, blockstate1.getBlock());
         blockstate1.updateNeighbourShapes(p_60193_, p_60194_, 2);
         if (this.isSticky) {
            BlockPos blockpos = p_60194_.offset(direction.getStepX() * 2, direction.getStepY() * 2, direction.getStepZ() * 2);
            BlockState blockstate2 = p_60193_.getBlockState(blockpos);
            boolean flag1 = false;
            if (blockstate2.is(Blocks.MOVING_PISTON)
               && p_60193_.getBlockEntity(blockpos) instanceof PistonMovingBlockEntity pistonmovingblockentity
               && pistonmovingblockentity.getDirection() == direction
               && pistonmovingblockentity.isExtending()) {
               pistonmovingblockentity.finalTick();
               flag1 = true;
            }

            if (!flag1) {
               if (p_60195_ == 1
                  && !blockstate2.isAir()
                  && isPushable(blockstate2, p_60193_, blockpos, direction.getOpposite(), false, direction)
                  && (blockstate2.getPistonPushReaction() == PushReaction.NORMAL || blockstate2.is(Blocks.PISTON) || blockstate2.is(Blocks.STICKY_PISTON))) {
                  this.moveBlocks(p_60193_, p_60194_, direction, false);
               } else {
                  p_60193_.removeBlock(p_60194_.relative(direction), false);
               }
            }
         } else {
            p_60193_.removeBlock(p_60194_.relative(direction), false);
         }

         p_60193_.playSound(null, p_60194_, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, p_60193_.random.nextFloat() * 0.15F + 0.6F);
         p_60193_.gameEvent(GameEvent.BLOCK_DEACTIVATE, p_60194_, GameEvent.Context.of(blockstate1));
      }

      return true;
   }

   public static boolean isPushable(BlockState p_60205_, Level p_60206_, BlockPos p_60207_, Direction p_60208_, boolean p_60209_, Direction p_60210_) {
      if (p_60207_.getY() < p_60206_.getMinY() || p_60207_.getY() > p_60206_.getMaxY() || !p_60206_.getWorldBorder().isWithinBounds(p_60207_)) {
         return false;
      }

      if (p_60205_.isAir()) {
         return true;
      }

      if (p_60205_.is(Blocks.OBSIDIAN) || p_60205_.is(Blocks.CRYING_OBSIDIAN) || p_60205_.is(Blocks.RESPAWN_ANCHOR) || p_60205_.is(Blocks.REINFORCED_DEEPSLATE)
         )
       {
         return false;
      }

      if (p_60208_ == Direction.DOWN && p_60207_.getY() == p_60206_.getMinY()) {
         return false;
      }

      if (p_60208_ == Direction.UP && p_60207_.getY() == p_60206_.getMaxY()) {
         return false;
      }

      if (!p_60205_.is(Blocks.PISTON) && !p_60205_.is(Blocks.STICKY_PISTON)) {
         if (p_60205_.getDestroySpeed(p_60206_, p_60207_) == -1.0F) {
            return false;
         }

         switch (p_60205_.getPistonPushReaction()) {
            case BLOCK:
               return false;
            case DESTROY:
               return p_60209_;
            case PUSH_ONLY:
               return p_60208_ == p_60210_;
         }
      } else if (p_60205_.getValue(EXTENDED)) {
         return false;
      }

      return !p_60205_.hasBlockEntity();
   }

   private boolean moveBlocks(Level p_60182_, BlockPos p_60183_, Direction p_60184_, boolean p_60185_) {
      BlockPos blockpos = p_60183_.relative(p_60184_);
      if (!p_60185_ && p_60182_.getBlockState(blockpos).is(Blocks.PISTON_HEAD)) {
         p_60182_.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 276);
      }

      PistonStructureResolver pistonstructureresolver = new PistonStructureResolver(p_60182_, p_60183_, p_60184_, p_60185_);
      if (!pistonstructureresolver.resolve()) {
         return false;
      }

      Map<BlockPos, BlockState> map = Maps.newHashMap();
      List<BlockPos> list = pistonstructureresolver.getToPush();
      List<BlockState> list1 = Lists.newArrayList();

      for (BlockPos blockpos1 : list) {
         BlockState blockstate = p_60182_.getBlockState(blockpos1);
         list1.add(blockstate);
         map.put(blockpos1, blockstate);
      }

      List<BlockPos> list2 = pistonstructureresolver.getToDestroy();
      BlockState[] ablockstate = new BlockState[list.size() + list2.size()];
      Direction direction = p_60185_ ? p_60184_ : p_60184_.getOpposite();
      int i = 0;

      for (int j = list2.size() - 1; j >= 0; j--) {
         BlockPos blockpos2 = list2.get(j);
         BlockState blockstate1 = p_60182_.getBlockState(blockpos2);
         BlockEntity blockentity = blockstate1.hasBlockEntity() ? p_60182_.getBlockEntity(blockpos2) : null;
         dropResources(blockstate1, p_60182_, blockpos2, blockentity);
         if (!blockstate1.is(BlockTags.FIRE) && p_60182_.isClientSide()) {
            p_60182_.levelEvent(2001, blockpos2, getId(blockstate1));
         }

         p_60182_.setBlock(blockpos2, Blocks.AIR.defaultBlockState(), 18);
         p_60182_.gameEvent(GameEvent.BLOCK_DESTROY, blockpos2, GameEvent.Context.of(blockstate1));
         ablockstate[i++] = blockstate1;
      }

      for (int k = list.size() - 1; k >= 0; k--) {
         BlockPos blockpos3 = list.get(k);
         BlockState blockstate5 = p_60182_.getBlockState(blockpos3);
         blockpos3 = blockpos3.relative(direction);
         map.remove(blockpos3);
         BlockState blockstate7 = Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, p_60184_);
         p_60182_.setBlock(blockpos3, blockstate7, 324);
         p_60182_.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockpos3, blockstate7, list1.get(k), p_60184_, p_60185_, false));
         ablockstate[i++] = blockstate5;
      }

      if (p_60185_) {
         PistonType pistontype = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
         BlockState blockstate4 = Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, p_60184_).setValue(PistonHeadBlock.TYPE, pistontype);
         BlockState blockstate6 = Blocks.MOVING_PISTON
            .defaultBlockState()
            .setValue(MovingPistonBlock.FACING, p_60184_)
            .setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
         map.remove(blockpos);
         p_60182_.setBlock(blockpos, blockstate6, 324);
         p_60182_.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockpos, blockstate6, blockstate4, p_60184_, true, true));
      }

      BlockState blockstate3 = Blocks.AIR.defaultBlockState();

      for (BlockPos blockpos4 : map.keySet()) {
         p_60182_.setBlock(blockpos4, blockstate3, 82);
      }

      for (Entry<BlockPos, BlockState> entry : map.entrySet()) {
         BlockPos blockpos5 = entry.getKey();
         BlockState blockstate2 = entry.getValue();
         blockstate2.updateIndirectNeighbourShapes(p_60182_, blockpos5, 2);
         blockstate3.updateNeighbourShapes(p_60182_, blockpos5, 2);
         blockstate3.updateIndirectNeighbourShapes(p_60182_, blockpos5, 2);
      }

      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(p_60182_, pistonstructureresolver.getPushDirection(), null);
      i = 0;

      for (int l = list2.size() - 1; l >= 0; l--) {
         BlockState blockstate8 = ablockstate[i++];
         BlockPos blockpos6 = list2.get(l);
         if (p_60182_ instanceof ServerLevel serverlevel) {
            blockstate8.affectNeighborsAfterRemoval(serverlevel, blockpos6, false);
         }

         blockstate8.updateIndirectNeighbourShapes(p_60182_, blockpos6, 2);
         p_60182_.updateNeighborsAt(blockpos6, blockstate8.getBlock(), orientation);
      }

      for (int i1 = list.size() - 1; i1 >= 0; i1--) {
         p_60182_.updateNeighborsAt(list.get(i1), ablockstate[i++].getBlock(), orientation);
      }

      if (p_60185_) {
         p_60182_.updateNeighborsAt(blockpos, Blocks.PISTON_HEAD, orientation);
      }

      return true;
   }

   @Override
   protected BlockState rotate(BlockState p_60215_, Rotation p_60216_) {
      return p_60215_.setValue(FACING, p_60216_.rotate(p_60215_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_60212_, Mirror p_60213_) {
      return p_60212_.rotate(p_60213_.getRotation(p_60212_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_60218_) {
      p_60218_.add(FACING, EXTENDED);
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_60231_) {
      return p_60231_.getValue(EXTENDED);
   }

   @Override
   protected boolean isPathfindable(BlockState p_60187_, PathComputationType p_60190_) {
      return false;
   }
}
