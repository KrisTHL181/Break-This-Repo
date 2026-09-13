package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

public class PortalShape {
   private static final int MIN_WIDTH = 2;
   public static final int MAX_WIDTH = 21;
   private static final int MIN_HEIGHT = 3;
   public static final int MAX_HEIGHT = 21;
   private static final BlockBehaviour.StatePredicate FRAME = (p_77720_, p_77721_, p_77722_) -> p_77720_.is(Blocks.OBSIDIAN);
   private static final float SAFE_TRAVEL_MAX_ENTITY_XY = 4.0F;
   private static final double SAFE_TRAVEL_MAX_VERTICAL_DELTA = 1.0;
   private final Direction.Axis axis;
   private final Direction rightDir;
   private final int numPortalBlocks;
   private final BlockPos bottomLeft;
   private final int height;
   private final int width;

   private PortalShape(Direction.Axis p_77697_, int p_361774_, Direction p_367618_, BlockPos p_77696_, int p_370026_, int p_368760_) {
      this.axis = p_77697_;
      this.numPortalBlocks = p_361774_;
      this.rightDir = p_367618_;
      this.bottomLeft = p_77696_;
      this.width = p_370026_;
      this.height = p_368760_;
   }

   public static Optional<PortalShape> findEmptyPortalShape(LevelAccessor p_77709_, BlockPos p_77710_, Direction.Axis p_77711_) {
      return findPortalShape(p_77709_, p_77710_, p_77727_ -> p_77727_.isValid() && p_77727_.numPortalBlocks == 0, p_77711_);
   }

   public static Optional<PortalShape> findPortalShape(LevelAccessor p_77713_, BlockPos p_77714_, Predicate<PortalShape> p_77715_, Direction.Axis p_77716_) {
      Optional<PortalShape> optional = Optional.of(findAnyShape(p_77713_, p_77714_, p_77716_)).filter(p_77715_);
      if (optional.isPresent()) {
         return optional;
      }

      Direction.Axis direction$axis = p_77716_ == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
      return Optional.of(findAnyShape(p_77713_, p_77714_, direction$axis)).filter(p_77715_);
   }

   public static PortalShape findAnyShape(BlockGetter p_362003_, BlockPos p_369293_, Direction.Axis p_363410_) {
      Direction direction = p_363410_ == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
      BlockPos blockpos = calculateBottomLeft(p_362003_, direction, p_369293_);
      if (blockpos == null) {
         return new PortalShape(p_363410_, 0, direction, p_369293_, 0, 0);
      }

      int i = calculateWidth(p_362003_, blockpos, direction);
      if (i == 0) {
         return new PortalShape(p_363410_, 0, direction, blockpos, 0, 0);
      }

      MutableInt mutableint = new MutableInt();
      int j = calculateHeight(p_362003_, blockpos, direction, i, mutableint);
      return new PortalShape(p_363410_, mutableint.intValue(), direction, blockpos, i, j);
   }

   private static @Nullable BlockPos calculateBottomLeft(BlockGetter p_366894_, Direction p_361188_, BlockPos p_77734_) {
      int i = Math.max(p_366894_.getMinY(), p_77734_.getY() - 21);

      while (p_77734_.getY() > i && isEmpty(p_366894_.getBlockState(p_77734_.below()))) {
         p_77734_ = p_77734_.below();
      }

      Direction direction = p_361188_.getOpposite();
      int j = getDistanceUntilEdgeAboveFrame(p_366894_, p_77734_, direction) - 1;
      return j < 0 ? null : p_77734_.relative(direction, j);
   }

   private static int calculateWidth(BlockGetter p_362377_, BlockPos p_369982_, Direction p_367434_) {
      int i = getDistanceUntilEdgeAboveFrame(p_362377_, p_369982_, p_367434_);
      return i >= 2 && i <= 21 ? i : 0;
   }

   private static int getDistanceUntilEdgeAboveFrame(BlockGetter p_366562_, BlockPos p_77736_, Direction p_77737_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int i = 0; i <= 21; i++) {
         blockpos$mutableblockpos.set(p_77736_).move(p_77737_, i);
         BlockState blockstate = p_366562_.getBlockState(blockpos$mutableblockpos);
         if (!isEmpty(blockstate)) {
            if (FRAME.test(blockstate, p_366562_, blockpos$mutableblockpos)) {
               return i;
            }
            break;
         }

         BlockState blockstate1 = p_366562_.getBlockState(blockpos$mutableblockpos.move(Direction.DOWN));
         if (!FRAME.test(blockstate1, p_366562_, blockpos$mutableblockpos)) {
            break;
         }
      }

      return 0;
   }

   private static int calculateHeight(BlockGetter p_366874_, BlockPos p_367382_, Direction p_369713_, int p_364755_, MutableInt p_366395_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      int i = getDistanceUntilTop(p_366874_, p_367382_, p_369713_, blockpos$mutableblockpos, p_364755_, p_366395_);
      return i >= 3 && i <= 21 && hasTopFrame(p_366874_, p_367382_, p_369713_, blockpos$mutableblockpos, p_364755_, i) ? i : 0;
   }

   private static boolean hasTopFrame(
      BlockGetter p_360937_, BlockPos p_362624_, Direction p_365783_, BlockPos.MutableBlockPos p_77731_, int p_77732_, int p_369385_
   ) {
      for (int i = 0; i < p_77732_; i++) {
         BlockPos.MutableBlockPos blockpos$mutableblockpos = p_77731_.set(p_362624_).move(Direction.UP, p_369385_).move(p_365783_, i);
         if (!FRAME.test(p_360937_.getBlockState(blockpos$mutableblockpos), p_360937_, blockpos$mutableblockpos)) {
            return false;
         }
      }

      return true;
   }

   private static int getDistanceUntilTop(
      BlockGetter p_366399_, BlockPos p_367032_, Direction p_362252_, BlockPos.MutableBlockPos p_77729_, int p_361664_, MutableInt p_363201_
   ) {
      for (int i = 0; i < 21; i++) {
         p_77729_.set(p_367032_).move(Direction.UP, i).move(p_362252_, -1);
         if (!FRAME.test(p_366399_.getBlockState(p_77729_), p_366399_, p_77729_)) {
            return i;
         }

         p_77729_.set(p_367032_).move(Direction.UP, i).move(p_362252_, p_361664_);
         if (!FRAME.test(p_366399_.getBlockState(p_77729_), p_366399_, p_77729_)) {
            return i;
         }

         for (int j = 0; j < p_361664_; j++) {
            p_77729_.set(p_367032_).move(Direction.UP, i).move(p_362252_, j);
            BlockState blockstate = p_366399_.getBlockState(p_77729_);
            if (!isEmpty(blockstate)) {
               return i;
            }

            if (blockstate.is(Blocks.NETHER_PORTAL)) {
               p_363201_.increment();
            }
         }
      }

      return 21;
   }

   private static boolean isEmpty(BlockState p_77718_) {
      return p_77718_.isAir() || p_77718_.is(BlockTags.FIRE) || p_77718_.is(Blocks.NETHER_PORTAL);
   }

   public boolean isValid() {
      return this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
   }

   public void createPortalBlocks(LevelAccessor p_366077_) {
      BlockState blockstate = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);
      BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
         .forEach(p_360642_ -> p_366077_.setBlock(p_360642_, blockstate, 18));
   }

   public boolean isComplete() {
      return this.isValid() && this.numPortalBlocks == this.width * this.height;
   }

   public static Vec3 getRelativePosition(BlockUtil.FoundRectangle p_454839_, Direction.Axis p_77740_, Vec3 p_77741_, EntityDimensions p_77742_) {
      double d0 = (double)p_454839_.axis1Size - p_77742_.width();
      double d1 = (double)p_454839_.axis2Size - p_77742_.height();
      BlockPos blockpos = p_454839_.minCorner;
      double d2;
      if (d0 > 0.0) {
         double d3 = blockpos.get(p_77740_) + p_77742_.width() / 2.0;
         d2 = Mth.clamp(Mth.inverseLerp(p_77741_.get(p_77740_) - d3, 0.0, d0), 0.0, 1.0);
      } else {
         d2 = 0.5;
      }

      double d5;
      if (d1 > 0.0) {
         Direction.Axis direction$axis = Direction.Axis.Y;
         d5 = Mth.clamp(Mth.inverseLerp(p_77741_.get(direction$axis) - blockpos.get(direction$axis), 0.0, d1), 0.0, 1.0);
      } else {
         d5 = 0.0;
      }

      Direction.Axis direction$axis1 = p_77740_ == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
      double d4 = p_77741_.get(direction$axis1) - (blockpos.get(direction$axis1) + 0.5);
      return new Vec3(d2, d5, d4);
   }

   public static Vec3 findCollisionFreePosition(Vec3 p_260315_, ServerLevel p_259704_, Entity p_259626_, EntityDimensions p_259816_) {
      if (!(p_259816_.width() > 4.0F) && !(p_259816_.height() > 4.0F)) {
         double d0 = p_259816_.height() / 2.0;
         Vec3 vec3 = p_260315_.add(0.0, d0, 0.0);
         VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec3, p_259816_.width(), 0.0, p_259816_.width()).expandTowards(0.0, 1.0, 0.0).inflate(1.0E-6));
         Optional<Vec3> optional = p_259704_.findFreePosition(p_259626_, voxelshape, vec3, p_259816_.width(), p_259816_.height(), p_259816_.width());
         Optional<Vec3> optional1 = optional.map(p_259019_ -> p_259019_.subtract(0.0, d0, 0.0));
         return optional1.orElse(p_260315_);
      } else {
         return p_260315_;
      }
   }
}
