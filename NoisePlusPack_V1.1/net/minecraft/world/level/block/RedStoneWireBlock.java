package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.ExperimentalRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RedStoneWireBlock extends Block {
   public static final MapCodec<RedStoneWireBlock> CODEC = simpleCodec(RedStoneWireBlock::new);
   public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
   public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
   public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
   public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
   );
   private static final int[] COLORS = Util.make(new int[16], p_360448_ -> {
      for (int i = 0; i <= 15; i++) {
         float f = i / 15.0F;
         float f1 = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
         float f2 = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
         float f3 = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
         p_360448_[i] = ARGB.colorFromFloat(1.0F, f1, f2, f3);
      }
   });
   private static final float PARTICLE_DENSITY = 0.2F;
   private final Function<BlockState, VoxelShape> shapes;
   private final BlockState crossState;
   private final RedstoneWireEvaluator evaluator = new DefaultRedstoneWireEvaluator(this);
   private boolean shouldSignal = true;

   @Override
   public MapCodec<RedStoneWireBlock> codec() {
      return CODEC;
   }

   public RedStoneWireBlock(BlockBehaviour.Properties p_55511_) {
      super(p_55511_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, RedstoneSide.NONE)
            .setValue(EAST, RedstoneSide.NONE)
            .setValue(SOUTH, RedstoneSide.NONE)
            .setValue(WEST, RedstoneSide.NONE)
            .setValue(POWER, 0)
      );
      this.shapes = this.makeShapes();
      this.crossState = this.defaultBlockState()
         .setValue(NORTH, RedstoneSide.SIDE)
         .setValue(EAST, RedstoneSide.SIDE)
         .setValue(SOUTH, RedstoneSide.SIDE)
         .setValue(WEST, RedstoneSide.SIDE);
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      int i = 1;
      int j = 10;
      VoxelShape voxelshape = Block.column(10.0, 0.0, 1.0);
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(10.0, 0.0, 1.0, 0.0, 8.0));
      Map<Direction, VoxelShape> map1 = Shapes.rotateHorizontal(Block.boxZ(10.0, 16.0, 0.0, 1.0));
      return this.getShapeForEachState(p_390953_ -> {
         VoxelShape voxelshape1 = voxelshape;

         for (Entry<Direction, EnumProperty<RedstoneSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            voxelshape1 = switch ((RedstoneSide)p_390953_.getValue(entry.getValue())) {
               case UP -> Shapes.or(voxelshape1, map.get(entry.getKey()), map1.get(entry.getKey()));
               case SIDE -> Shapes.or(voxelshape1, map.get(entry.getKey()));
               case NONE -> voxelshape1;
            };
         }

         return voxelshape1;
      }, POWER);
   }

   @Override
   protected VoxelShape getShape(BlockState p_55620_, BlockGetter p_55621_, BlockPos p_55622_, CollisionContext p_55623_) {
      return this.shapes.apply(p_55620_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_55513_) {
      return this.getConnectionState(p_55513_.getLevel(), this.crossState, p_55513_.getClickedPos());
   }

   private BlockState getConnectionState(BlockGetter p_55515_, BlockState p_55516_, BlockPos p_55517_) {
      boolean flag = isDot(p_55516_);
      p_55516_ = this.getMissingConnections(p_55515_, this.defaultBlockState().setValue(POWER, p_55516_.getValue(POWER)), p_55517_);
      if (flag && isDot(p_55516_)) {
         return p_55516_;
      }

      boolean flag1 = p_55516_.getValue(NORTH).isConnected();
      boolean flag2 = p_55516_.getValue(SOUTH).isConnected();
      boolean flag3 = p_55516_.getValue(EAST).isConnected();
      boolean flag4 = p_55516_.getValue(WEST).isConnected();
      boolean flag5 = !flag1 && !flag2;
      boolean flag6 = !flag3 && !flag4;
      if (!flag4 && flag5) {
         p_55516_ = p_55516_.setValue(WEST, RedstoneSide.SIDE);
      }

      if (!flag3 && flag5) {
         p_55516_ = p_55516_.setValue(EAST, RedstoneSide.SIDE);
      }

      if (!flag1 && flag6) {
         p_55516_ = p_55516_.setValue(NORTH, RedstoneSide.SIDE);
      }

      if (!flag2 && flag6) {
         p_55516_ = p_55516_.setValue(SOUTH, RedstoneSide.SIDE);
      }

      return p_55516_;
   }

   private BlockState getMissingConnections(BlockGetter p_55609_, BlockState p_55610_, BlockPos p_55611_) {
      boolean flag = !p_55609_.getBlockState(p_55611_.above()).isRedstoneConductor(p_55609_, p_55611_);

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         if (!p_55610_.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()) {
            RedstoneSide redstoneside = this.getConnectingSide(p_55609_, p_55611_, direction, flag);
            p_55610_ = p_55610_.setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneside);
         }
      }

      return p_55610_;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_55598_,
      LevelReader p_364425_,
      ScheduledTickAccess p_364740_,
      BlockPos p_55602_,
      Direction p_55599_,
      BlockPos p_55603_,
      BlockState p_55600_,
      RandomSource p_363398_
   ) {
      if (p_55599_ == Direction.DOWN) {
         return !this.canSurviveOn(p_364425_, p_55603_, p_55600_) ? Blocks.AIR.defaultBlockState() : p_55598_;
      }

      if (p_55599_ == Direction.UP) {
         return this.getConnectionState(p_364425_, p_55598_, p_55602_);
      }

      RedstoneSide redstoneside = this.getConnectingSide(p_364425_, p_55602_, p_55599_);
      return redstoneside.isConnected() == p_55598_.getValue(PROPERTY_BY_DIRECTION.get(p_55599_)).isConnected() && !isCross(p_55598_)
         ? p_55598_.setValue(PROPERTY_BY_DIRECTION.get(p_55599_), redstoneside)
         : this.getConnectionState(
            p_364425_, this.crossState.setValue(POWER, p_55598_.getValue(POWER)).setValue(PROPERTY_BY_DIRECTION.get(p_55599_), redstoneside), p_55602_
         );
   }

   private static boolean isCross(BlockState p_55645_) {
      return p_55645_.getValue(NORTH).isConnected()
         && p_55645_.getValue(SOUTH).isConnected()
         && p_55645_.getValue(EAST).isConnected()
         && p_55645_.getValue(WEST).isConnected();
   }

   private static boolean isDot(BlockState p_55647_) {
      return !p_55647_.getValue(NORTH).isConnected()
         && !p_55647_.getValue(SOUTH).isConnected()
         && !p_55647_.getValue(EAST).isConnected()
         && !p_55647_.getValue(WEST).isConnected();
   }

   @Override
   protected void updateIndirectNeighbourShapes(BlockState p_55579_, LevelAccessor p_55580_, BlockPos p_55581_, @Block.UpdateFlags int p_55582_, int p_55583_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         RedstoneSide redstoneside = p_55579_.getValue(PROPERTY_BY_DIRECTION.get(direction));
         if (redstoneside != RedstoneSide.NONE && !p_55580_.getBlockState(blockpos$mutableblockpos.setWithOffset(p_55581_, direction)).is(this)) {
            blockpos$mutableblockpos.move(Direction.DOWN);
            BlockState blockstate = p_55580_.getBlockState(blockpos$mutableblockpos);
            if (blockstate.is(this)) {
               BlockPos blockpos = blockpos$mutableblockpos.relative(direction.getOpposite());
               p_55580_.neighborShapeChanged(direction.getOpposite(), blockpos$mutableblockpos, blockpos, p_55580_.getBlockState(blockpos), p_55582_, p_55583_);
            }

            blockpos$mutableblockpos.setWithOffset(p_55581_, direction).move(Direction.UP);
            BlockState blockstate1 = p_55580_.getBlockState(blockpos$mutableblockpos);
            if (blockstate1.is(this)) {
               BlockPos blockpos1 = blockpos$mutableblockpos.relative(direction.getOpposite());
               p_55580_.neighborShapeChanged(
                  direction.getOpposite(), blockpos$mutableblockpos, blockpos1, p_55580_.getBlockState(blockpos1), p_55582_, p_55583_
               );
            }
         }
      }
   }

   private RedstoneSide getConnectingSide(BlockGetter p_55519_, BlockPos p_55520_, Direction p_55521_) {
      return this.getConnectingSide(p_55519_, p_55520_, p_55521_, !p_55519_.getBlockState(p_55520_.above()).isRedstoneConductor(p_55519_, p_55520_));
   }

   private RedstoneSide getConnectingSide(BlockGetter p_55523_, BlockPos p_55524_, Direction p_55525_, boolean p_55526_) {
      BlockPos blockpos = p_55524_.relative(p_55525_);
      BlockState blockstate = p_55523_.getBlockState(blockpos);
      if (p_55526_) {
         boolean flag = blockstate.getBlock() instanceof TrapDoorBlock || this.canSurviveOn(p_55523_, blockpos, blockstate);
         if (flag && shouldConnectTo(p_55523_.getBlockState(blockpos.above()))) {
            if (blockstate.isFaceSturdy(p_55523_, blockpos, p_55525_.getOpposite())) {
               return RedstoneSide.UP;
            }

            return RedstoneSide.SIDE;
         }
      }

      return shouldConnectTo(blockstate, p_55525_)
            || !blockstate.isRedstoneConductor(p_55523_, blockpos) && shouldConnectTo(p_55523_.getBlockState(blockpos.below()))
         ? RedstoneSide.SIDE
         : RedstoneSide.NONE;
   }

   @Override
   protected boolean canSurvive(BlockState p_55585_, LevelReader p_55586_, BlockPos p_55587_) {
      BlockPos blockpos = p_55587_.below();
      BlockState blockstate = p_55586_.getBlockState(blockpos);
      return this.canSurviveOn(p_55586_, blockpos, blockstate);
   }

   private boolean canSurviveOn(BlockGetter p_55613_, BlockPos p_55614_, BlockState p_55615_) {
      return p_55615_.isFaceSturdy(p_55613_, p_55614_, Direction.UP) || p_55615_.is(Blocks.HOPPER);
   }

   private void updatePowerStrength(Level p_55531_, BlockPos p_55532_, BlockState p_55533_, @Nullable Orientation p_363629_, boolean p_368749_) {
      if (useExperimentalEvaluator(p_55531_)) {
         new ExperimentalRedstoneWireEvaluator(this).updatePowerStrength(p_55531_, p_55532_, p_55533_, p_363629_, p_368749_);
      } else {
         this.evaluator.updatePowerStrength(p_55531_, p_55532_, p_55533_, p_363629_, p_368749_);
      }
   }

   public int getBlockSignal(Level p_365214_, BlockPos p_368064_) {
      this.shouldSignal = false;
      int i = p_365214_.getBestNeighborSignal(p_368064_);
      this.shouldSignal = true;
      return i;
   }

   private void checkCornerChangeAt(Level p_55617_, BlockPos p_55618_) {
      if (p_55617_.getBlockState(p_55618_).is(this)) {
         p_55617_.updateNeighborsAt(p_55618_, this);

         for (Direction direction : Direction.values()) {
            p_55617_.updateNeighborsAt(p_55618_.relative(direction), this);
         }
      }
   }

   @Override
   protected void onPlace(BlockState p_55630_, Level p_55631_, BlockPos p_55632_, BlockState p_55633_, boolean p_55634_) {
      if (!p_55633_.is(p_55630_.getBlock()) && !p_55631_.isClientSide()) {
         this.updatePowerStrength(p_55631_, p_55632_, p_55630_, null, true);

         for (Direction direction : Direction.Plane.VERTICAL) {
            p_55631_.updateNeighborsAt(p_55632_.relative(direction), this);
         }

         this.updateNeighborsOfNeighboringWires(p_55631_, p_55632_);
      }
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_396119_, ServerLevel p_392722_, BlockPos p_393494_, boolean p_392611_) {
      if (!p_392611_) {
         for (Direction direction : Direction.values()) {
            p_392722_.updateNeighborsAt(p_393494_.relative(direction), this);
         }

         this.updatePowerStrength(p_392722_, p_393494_, p_396119_, null, false);
         this.updateNeighborsOfNeighboringWires(p_392722_, p_393494_);
      }
   }

   private void updateNeighborsOfNeighboringWires(Level p_55638_, BlockPos p_55639_) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         this.checkCornerChangeAt(p_55638_, p_55639_.relative(direction));
      }

      for (Direction direction1 : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_55639_.relative(direction1);
         if (p_55638_.getBlockState(blockpos).isRedstoneConductor(p_55638_, blockpos)) {
            this.checkCornerChangeAt(p_55638_, blockpos.above());
         } else {
            this.checkCornerChangeAt(p_55638_, blockpos.below());
         }
      }
   }

   @Override
   protected void neighborChanged(BlockState p_55561_, Level p_55562_, BlockPos p_55563_, Block p_55564_, @Nullable Orientation p_369069_, boolean p_55566_) {
      if (!p_55562_.isClientSide() && (p_55564_ != this || !useExperimentalEvaluator(p_55562_))) {
         if (p_55561_.canSurvive(p_55562_, p_55563_)) {
            this.updatePowerStrength(p_55562_, p_55563_, p_55561_, p_369069_, false);
         } else {
            dropResources(p_55561_, p_55562_, p_55563_);
            p_55562_.removeBlock(p_55563_, false);
         }
      }
   }

   private static boolean useExperimentalEvaluator(Level p_369145_) {
      return p_369145_.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS);
   }

   @Override
   protected int getDirectSignal(BlockState p_55625_, BlockGetter p_55626_, BlockPos p_55627_, Direction p_55628_) {
      return !this.shouldSignal ? 0 : p_55625_.getSignal(p_55626_, p_55627_, p_55628_);
   }

   @Override
   protected int getSignal(BlockState p_55549_, BlockGetter p_55550_, BlockPos p_55551_, Direction p_55552_) {
      if (this.shouldSignal && p_55552_ != Direction.DOWN) {
         int i = p_55549_.getValue(POWER);
         if (i == 0) {
            return 0;
         } else {
            return p_55552_ != Direction.UP
                  && !this.getConnectionState(p_55550_, p_55549_, p_55551_).getValue(PROPERTY_BY_DIRECTION.get(p_55552_.getOpposite())).isConnected()
               ? 0
               : i;
         }
      } else {
         return 0;
      }
   }

   protected static boolean shouldConnectTo(BlockState p_55641_) {
      return shouldConnectTo(p_55641_, null);
   }

   protected static boolean shouldConnectTo(BlockState p_55595_, @Nullable Direction p_55596_) {
      if (p_55595_.is(Blocks.REDSTONE_WIRE)) {
         return true;
      } else if (p_55595_.is(Blocks.REPEATER)) {
         Direction direction = p_55595_.getValue(RepeaterBlock.FACING);
         return direction == p_55596_ || direction.getOpposite() == p_55596_;
      } else {
         return p_55595_.is(Blocks.OBSERVER) ? p_55596_ == p_55595_.getValue(ObserverBlock.FACING) : p_55595_.isSignalSource() && p_55596_ != null;
      }
   }

   @Override
   protected boolean isSignalSource(BlockState p_55636_) {
      return this.shouldSignal;
   }

   public static int getColorForPower(int p_55607_) {
      return COLORS[p_55607_];
   }

   private static void spawnParticlesAlongLine(
      Level p_221923_, RandomSource p_221924_, BlockPos p_221925_, int p_363544_, Direction p_221927_, Direction p_221928_, float p_221929_, float p_221930_
   ) {
      float f = p_221930_ - p_221929_;
      if (!(p_221924_.nextFloat() >= 0.2F * f)) {
         float f1 = 0.4375F;
         float f2 = p_221929_ + f * p_221924_.nextFloat();
         double d0 = 0.5 + 0.4375F * p_221927_.getStepX() + f2 * p_221928_.getStepX();
         double d1 = 0.5 + 0.4375F * p_221927_.getStepY() + f2 * p_221928_.getStepY();
         double d2 = 0.5 + 0.4375F * p_221927_.getStepZ() + f2 * p_221928_.getStepZ();
         p_221923_.addParticle(new DustParticleOptions(p_363544_, 1.0F), p_221925_.getX() + d0, p_221925_.getY() + d1, p_221925_.getZ() + d2, 0.0, 0.0, 0.0);
      }
   }

   @Override
   public void animateTick(BlockState p_221932_, Level p_221933_, BlockPos p_221934_, RandomSource p_221935_) {
      int i = p_221932_.getValue(POWER);
      if (i != 0) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide redstoneside = p_221932_.getValue(PROPERTY_BY_DIRECTION.get(direction));
            switch (redstoneside) {
               case UP:
                  spawnParticlesAlongLine(p_221933_, p_221935_, p_221934_, COLORS[i], direction, Direction.UP, -0.5F, 0.5F);
               case SIDE:
                  spawnParticlesAlongLine(p_221933_, p_221935_, p_221934_, COLORS[i], Direction.DOWN, direction, 0.0F, 0.5F);
                  break;
               case NONE:
               default:
                  spawnParticlesAlongLine(p_221933_, p_221935_, p_221934_, COLORS[i], Direction.DOWN, direction, 0.0F, 0.3F);
            }
         }
      }
   }

   @Override
   protected BlockState rotate(BlockState p_55592_, Rotation p_55593_) {
      switch (p_55593_) {
         case CLOCKWISE_180:
            return p_55592_.setValue(NORTH, p_55592_.getValue(SOUTH))
               .setValue(EAST, p_55592_.getValue(WEST))
               .setValue(SOUTH, p_55592_.getValue(NORTH))
               .setValue(WEST, p_55592_.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return p_55592_.setValue(NORTH, p_55592_.getValue(EAST))
               .setValue(EAST, p_55592_.getValue(SOUTH))
               .setValue(SOUTH, p_55592_.getValue(WEST))
               .setValue(WEST, p_55592_.getValue(NORTH));
         case CLOCKWISE_90:
            return p_55592_.setValue(NORTH, p_55592_.getValue(WEST))
               .setValue(EAST, p_55592_.getValue(NORTH))
               .setValue(SOUTH, p_55592_.getValue(EAST))
               .setValue(WEST, p_55592_.getValue(SOUTH));
         default:
            return p_55592_;
      }
   }

   @Override
   protected BlockState mirror(BlockState p_55589_, Mirror p_55590_) {
      switch (p_55590_) {
         case LEFT_RIGHT:
            return p_55589_.setValue(NORTH, p_55589_.getValue(SOUTH)).setValue(SOUTH, p_55589_.getValue(NORTH));
         case FRONT_BACK:
            return p_55589_.setValue(EAST, p_55589_.getValue(WEST)).setValue(WEST, p_55589_.getValue(EAST));
         default:
            return super.mirror(p_55589_, p_55590_);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55605_) {
      p_55605_.add(NORTH, EAST, SOUTH, WEST, POWER);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_55554_, Level p_55555_, BlockPos p_55556_, Player p_55557_, BlockHitResult p_55559_) {
      if (!p_55557_.getAbilities().mayBuild) {
         return InteractionResult.PASS;
      }

      if (isCross(p_55554_) || isDot(p_55554_)) {
         BlockState blockstate = isCross(p_55554_) ? this.defaultBlockState() : this.crossState;
         blockstate = blockstate.setValue(POWER, p_55554_.getValue(POWER));
         blockstate = this.getConnectionState(p_55555_, blockstate, p_55556_);
         if (blockstate != p_55554_) {
            p_55555_.setBlock(p_55556_, blockstate, 3);
            this.updatesOnShapeChange(p_55555_, p_55556_, p_55554_, blockstate);
            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   private void updatesOnShapeChange(Level p_55535_, BlockPos p_55536_, BlockState p_55537_, BlockState p_55538_) {
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(p_55535_, null, Direction.UP);

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_55536_.relative(direction);
         if (p_55537_.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() != p_55538_.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()
            && p_55535_.getBlockState(blockpos).isRedstoneConductor(p_55535_, blockpos)) {
            p_55535_.updateNeighborsAtExceptFromFacing(
               blockpos, p_55538_.getBlock(), direction.getOpposite(), ExperimentalRedstoneUtils.withFront(orientation, direction)
            );
         }
      }
   }
}
