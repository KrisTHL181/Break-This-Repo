package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

public class BedBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final MapCodec<BedBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422037_ -> p_422037_.group(DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), propertiesCodec()).apply(p_422037_, BedBlock::new)
   );
   public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
   public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
   private static final Map<Direction, VoxelShape> SHAPES = Util.make(() -> {
      VoxelShape voxelshape = Block.box(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
      VoxelShape voxelshape1 = Shapes.rotate(voxelshape, OctahedralGroup.BLOCK_ROT_Y_90);
      return Shapes.rotateHorizontal(Shapes.or(Block.column(16.0, 3.0, 9.0), voxelshape, voxelshape1));
   });
   private final DyeColor color;

   @Override
   public MapCodec<BedBlock> codec() {
      return CODEC;
   }

   public BedBlock(DyeColor p_49454_, BlockBehaviour.Properties p_49455_) {
      super(p_49455_);
      this.color = p_49454_;
      this.registerDefaultState(this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false));
   }

   public static @Nullable Direction getBedOrientation(BlockGetter p_49486_, BlockPos p_49487_) {
      BlockState blockstate = p_49486_.getBlockState(p_49487_);
      return blockstate.getBlock() instanceof BedBlock ? blockstate.getValue(FACING) : null;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_49515_, Level p_49516_, BlockPos p_49517_, Player p_49518_, BlockHitResult p_49520_) {
      if (p_49516_.isClientSide()) {
         return InteractionResult.SUCCESS_SERVER;
      }

      if (p_49515_.getValue(PART) != BedPart.HEAD) {
         p_49517_ = p_49517_.relative(p_49515_.getValue(FACING));
         p_49515_ = p_49516_.getBlockState(p_49517_);
         if (!p_49515_.is(this)) {
            return InteractionResult.CONSUME;
         }
      }

      BedRule bedrule = p_49516_.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, p_49517_);
      if (bedrule.explodes()) {
         bedrule.errorMessage().ifPresent(p_449886_ -> p_49518_.displayClientMessage(p_449886_, true));
         p_49516_.removeBlock(p_49517_, false);
         BlockPos blockpos = p_49517_.relative(p_49515_.getValue(FACING).getOpposite());
         if (p_49516_.getBlockState(blockpos).is(this)) {
            p_49516_.removeBlock(blockpos, false);
         }

         Vec3 vec3 = p_49517_.getCenter();
         p_49516_.explode(null, p_49516_.damageSources().badRespawnPointExplosion(vec3), null, vec3, 5.0F, true, Level.ExplosionInteraction.BLOCK);
         return InteractionResult.SUCCESS_SERVER;
      } else if (p_49515_.getValue(OCCUPIED)) {
         if (!this.kickVillagerOutOfBed(p_49516_, p_49517_)) {
            p_49518_.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
         }

         return InteractionResult.SUCCESS_SERVER;
      } else {
         p_49518_.startSleepInBed(p_49517_).ifLeft(p_449884_ -> {
            if (p_449884_.message() != null) {
               p_49518_.displayClientMessage(p_449884_.message(), true);
            }
         });
         return InteractionResult.SUCCESS_SERVER;
      }
   }

   private boolean kickVillagerOutOfBed(Level p_49491_, BlockPos p_49492_) {
      List<Villager> list = p_49491_.getEntitiesOfClass(Villager.class, new AABB(p_49492_), LivingEntity::isSleeping);
      if (list.isEmpty()) {
         return false;
      }

      list.get(0).stopSleeping();
      return true;
   }

   @Override
   public void fallOn(Level p_152169_, BlockState p_152170_, BlockPos p_152171_, Entity p_152172_, double p_396743_) {
      super.fallOn(p_152169_, p_152170_, p_152171_, p_152172_, p_396743_ * 0.5);
   }

   @Override
   public void updateEntityMovementAfterFallOn(BlockGetter p_49483_, Entity p_49484_) {
      if (p_49484_.isSuppressingBounce()) {
         super.updateEntityMovementAfterFallOn(p_49483_, p_49484_);
      } else {
         this.bounceUp(p_49484_);
      }
   }

   private void bounceUp(Entity p_49457_) {
      Vec3 vec3 = p_49457_.getDeltaMovement();
      if (vec3.y < 0.0) {
         double d0 = p_49457_ instanceof LivingEntity ? 1.0 : 0.8;
         p_49457_.setDeltaMovement(vec3.x, -vec3.y * 0.66F * d0, vec3.z);
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_49525_,
      LevelReader p_367181_,
      ScheduledTickAccess p_361759_,
      BlockPos p_49529_,
      Direction p_49526_,
      BlockPos p_49530_,
      BlockState p_49527_,
      RandomSource p_361707_
   ) {
      if (p_49526_ != getNeighbourDirection(p_49525_.getValue(PART), p_49525_.getValue(FACING))) {
         return super.updateShape(p_49525_, p_367181_, p_361759_, p_49529_, p_49526_, p_49530_, p_49527_, p_361707_);
      } else {
         return p_49527_.is(this) && p_49527_.getValue(PART) != p_49525_.getValue(PART)
            ? p_49525_.setValue(OCCUPIED, p_49527_.getValue(OCCUPIED))
            : Blocks.AIR.defaultBlockState();
      }
   }

   private static Direction getNeighbourDirection(BedPart p_49534_, Direction p_49535_) {
      return p_49534_ == BedPart.FOOT ? p_49535_ : p_49535_.getOpposite();
   }

   @Override
   public BlockState playerWillDestroy(Level p_49505_, BlockPos p_49506_, BlockState p_49507_, Player p_49508_) {
      if (!p_49505_.isClientSide() && p_49508_.preventsBlockDrops()) {
         BedPart bedpart = p_49507_.getValue(PART);
         if (bedpart == BedPart.FOOT) {
            BlockPos blockpos = p_49506_.relative(getNeighbourDirection(bedpart, p_49507_.getValue(FACING)));
            BlockState blockstate = p_49505_.getBlockState(blockpos);
            if (blockstate.is(this) && blockstate.getValue(PART) == BedPart.HEAD) {
               p_49505_.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35);
               p_49505_.levelEvent(p_49508_, 2001, blockpos, Block.getId(blockstate));
            }
         }
      }

      return super.playerWillDestroy(p_49505_, p_49506_, p_49507_, p_49508_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_49479_) {
      Direction direction = p_49479_.getHorizontalDirection();
      BlockPos blockpos = p_49479_.getClickedPos();
      BlockPos blockpos1 = blockpos.relative(direction);
      Level level = p_49479_.getLevel();
      return level.getBlockState(blockpos1).canBeReplaced(p_49479_) && level.getWorldBorder().isWithinBounds(blockpos1)
         ? this.defaultBlockState().setValue(FACING, direction)
         : null;
   }

   @Override
   protected VoxelShape getShape(BlockState p_49547_, BlockGetter p_49548_, BlockPos p_49549_, CollisionContext p_49550_) {
      return SHAPES.get(getConnectedDirection(p_49547_).getOpposite());
   }

   public static Direction getConnectedDirection(BlockState p_49558_) {
      Direction direction = p_49558_.getValue(FACING);
      return p_49558_.getValue(PART) == BedPart.HEAD ? direction.getOpposite() : direction;
   }

   public static DoubleBlockCombiner.BlockType getBlockType(BlockState p_49560_) {
      BedPart bedpart = p_49560_.getValue(PART);
      return bedpart == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
   }

   private static boolean isBunkBed(BlockGetter p_49542_, BlockPos p_49543_) {
      return p_49542_.getBlockState(p_49543_.below()).getBlock() instanceof BedBlock;
   }

   public static Optional<Vec3> findStandUpPosition(
      EntityType<?> p_261547_, CollisionGetter p_261946_, BlockPos p_261614_, Direction p_261648_, float p_261680_
   ) {
      Direction direction = p_261648_.getClockWise();
      Direction direction1 = direction.isFacingAngle(p_261680_) ? direction.getOpposite() : direction;
      if (isBunkBed(p_261946_, p_261614_)) {
         return findBunkBedStandUpPosition(p_261547_, p_261946_, p_261614_, p_261648_, direction1);
      }

      int[][] aint = bedStandUpOffsets(p_261648_, direction1);
      Optional<Vec3> optional = findStandUpPositionAtOffset(p_261547_, p_261946_, p_261614_, aint, true);
      return optional.isPresent() ? optional : findStandUpPositionAtOffset(p_261547_, p_261946_, p_261614_, aint, false);
   }

   private static Optional<Vec3> findBunkBedStandUpPosition(
      EntityType<?> p_49464_, CollisionGetter p_49465_, BlockPos p_49466_, Direction p_49467_, Direction p_49468_
   ) {
      int[][] aint = bedSurroundStandUpOffsets(p_49467_, p_49468_);
      Optional<Vec3> optional = findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, aint, true);
      if (optional.isPresent()) {
         return optional;
      }

      BlockPos blockpos = p_49466_.below();
      Optional<Vec3> optional1 = findStandUpPositionAtOffset(p_49464_, p_49465_, blockpos, aint, true);
      if (optional1.isPresent()) {
         return optional1;
      }

      int[][] aint1 = bedAboveStandUpOffsets(p_49467_);
      Optional<Vec3> optional2 = findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, aint1, true);
      if (optional2.isPresent()) {
         return optional2;
      }

      Optional<Vec3> optional3 = findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, aint, false);
      if (optional3.isPresent()) {
         return optional3;
      }

      Optional<Vec3> optional4 = findStandUpPositionAtOffset(p_49464_, p_49465_, blockpos, aint, false);
      return optional4.isPresent() ? optional4 : findStandUpPositionAtOffset(p_49464_, p_49465_, p_49466_, aint1, false);
   }

   private static Optional<Vec3> findStandUpPositionAtOffset(
      EntityType<?> p_49470_, CollisionGetter p_49471_, BlockPos p_49472_, int[][] p_49473_, boolean p_49474_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int[] aint : p_49473_) {
         blockpos$mutableblockpos.set(p_49472_.getX() + aint[0], p_49472_.getY(), p_49472_.getZ() + aint[1]);
         Vec3 vec3 = DismountHelper.findSafeDismountLocation(p_49470_, p_49471_, blockpos$mutableblockpos, p_49474_);
         if (vec3 != null) {
            return Optional.of(vec3);
         }
      }

      return Optional.empty();
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49532_) {
      p_49532_.add(FACING, PART, OCCUPIED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_152175_, BlockState p_152176_) {
      return new BedBlockEntity(p_152175_, p_152176_, this.color);
   }

   @Override
   public void setPlacedBy(Level p_49499_, BlockPos p_49500_, BlockState p_49501_, @Nullable LivingEntity p_49502_, ItemStack p_49503_) {
      super.setPlacedBy(p_49499_, p_49500_, p_49501_, p_49502_, p_49503_);
      if (!p_49499_.isClientSide()) {
         BlockPos blockpos = p_49500_.relative(p_49501_.getValue(FACING));
         p_49499_.setBlock(blockpos, p_49501_.setValue(PART, BedPart.HEAD), 3);
         p_49499_.updateNeighborsAt(p_49500_, Blocks.AIR);
         p_49501_.updateNeighbourShapes(p_49499_, p_49500_, 3);
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   @Override
   protected long getSeed(BlockState p_49522_, BlockPos p_49523_) {
      BlockPos blockpos = p_49523_.relative(p_49522_.getValue(FACING), p_49522_.getValue(PART) == BedPart.HEAD ? 0 : 1);
      return Mth.getSeed(blockpos.getX(), p_49523_.getY(), blockpos.getZ());
   }

   @Override
   protected boolean isPathfindable(BlockState p_49510_, PathComputationType p_49513_) {
      return false;
   }

   private static int[][] bedStandUpOffsets(Direction p_49539_, Direction p_49540_) {
      return (int[][])ArrayUtils.addAll(bedSurroundStandUpOffsets(p_49539_, p_49540_), bedAboveStandUpOffsets(p_49539_));
   }

   private static int[][] bedSurroundStandUpOffsets(Direction p_49552_, Direction p_49553_) {
      return new int[][]{
         {p_49553_.getStepX(), p_49553_.getStepZ()},
         {p_49553_.getStepX() - p_49552_.getStepX(), p_49553_.getStepZ() - p_49552_.getStepZ()},
         {p_49553_.getStepX() - p_49552_.getStepX() * 2, p_49553_.getStepZ() - p_49552_.getStepZ() * 2},
         {-p_49552_.getStepX() * 2, -p_49552_.getStepZ() * 2},
         {-p_49553_.getStepX() - p_49552_.getStepX() * 2, -p_49553_.getStepZ() - p_49552_.getStepZ() * 2},
         {-p_49553_.getStepX() - p_49552_.getStepX(), -p_49553_.getStepZ() - p_49552_.getStepZ()},
         {-p_49553_.getStepX(), -p_49553_.getStepZ()},
         {-p_49553_.getStepX() + p_49552_.getStepX(), -p_49553_.getStepZ() + p_49552_.getStepZ()},
         {p_49552_.getStepX(), p_49552_.getStepZ()},
         {p_49553_.getStepX() + p_49552_.getStepX(), p_49553_.getStepZ() + p_49552_.getStepZ()}
      };
   }

   private static int[][] bedAboveStandUpOffsets(Direction p_49537_) {
      return new int[][]{{0, 0}, {-p_49537_.getStepX(), -p_49537_.getStepZ()}};
   }
}
