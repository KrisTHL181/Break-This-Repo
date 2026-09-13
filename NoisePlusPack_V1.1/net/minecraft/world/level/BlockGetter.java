package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface BlockGetter extends LevelHeightAccessor {
   @Nullable BlockEntity getBlockEntity(BlockPos var1);

   default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos p_151367_, BlockEntityType<T> p_151368_) {
      BlockEntity blockentity = this.getBlockEntity(p_151367_);
      return blockentity != null && blockentity.getType() == p_151368_ ? Optional.of((T)blockentity) : Optional.empty();
   }

   BlockState getBlockState(BlockPos var1);

   FluidState getFluidState(BlockPos var1);

   default int getLightEmission(BlockPos p_45572_) {
      return this.getBlockState(p_45572_).getLightEmission();
   }

   default Stream<BlockState> getBlockStates(AABB p_45557_) {
      return BlockPos.betweenClosedStream(p_45557_).map(this::getBlockState);
   }

   default BlockHitResult isBlockInLine(ClipBlockStateContext p_151354_) {
      return traverseBlocks(
         p_151354_.getFrom(),
         p_151354_.getTo(),
         p_151354_,
         (p_275154_, p_275155_) -> {
            BlockState blockstate = this.getBlockState(p_275155_);
            Vec3 vec3 = p_275154_.getFrom().subtract(p_275154_.getTo());
            return p_275154_.isTargetBlock().test(blockstate)
               ? new BlockHitResult(p_275154_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_275154_.getTo()), false)
               : null;
         },
         p_275156_ -> {
            Vec3 vec3 = p_275156_.getFrom().subtract(p_275156_.getTo());
            return BlockHitResult.miss(p_275156_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_275156_.getTo()));
         }
      );
   }

   default BlockHitResult clip(ClipContext p_45548_) {
      return traverseBlocks(p_45548_.getFrom(), p_45548_.getTo(), p_45548_, (p_151359_, p_151360_) -> {
         BlockState blockstate = this.getBlockState(p_151360_);
         FluidState fluidstate = this.getFluidState(p_151360_);
         Vec3 vec3 = p_151359_.getFrom();
         Vec3 vec31 = p_151359_.getTo();
         VoxelShape voxelshape = p_151359_.getBlockShape(blockstate, this, p_151360_);
         BlockHitResult blockhitresult = this.clipWithInteractionOverride(vec3, vec31, p_151360_, voxelshape, blockstate);
         VoxelShape voxelshape1 = p_151359_.getFluidShape(fluidstate, this, p_151360_);
         BlockHitResult blockhitresult1 = voxelshape1.clip(vec3, vec31, p_151360_);
         double d0 = blockhitresult == null ? Double.MAX_VALUE : p_151359_.getFrom().distanceToSqr(blockhitresult.getLocation());
         double d1 = blockhitresult1 == null ? Double.MAX_VALUE : p_151359_.getFrom().distanceToSqr(blockhitresult1.getLocation());
         return d0 <= d1 ? blockhitresult : blockhitresult1;
      }, p_275153_ -> {
         Vec3 vec3 = p_275153_.getFrom().subtract(p_275153_.getTo());
         return BlockHitResult.miss(p_275153_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_275153_.getTo()));
      });
   }

   default @Nullable BlockHitResult clipWithInteractionOverride(Vec3 p_45559_, Vec3 p_45560_, BlockPos p_45561_, VoxelShape p_45562_, BlockState p_45563_) {
      BlockHitResult blockhitresult = p_45562_.clip(p_45559_, p_45560_, p_45561_);
      if (blockhitresult != null) {
         BlockHitResult blockhitresult1 = p_45563_.getInteractionShape(this, p_45561_).clip(p_45559_, p_45560_, p_45561_);
         if (blockhitresult1 != null
            && blockhitresult1.getLocation().subtract(p_45559_).lengthSqr() < blockhitresult.getLocation().subtract(p_45559_).lengthSqr()) {
            return blockhitresult.withDirection(blockhitresult1.getDirection());
         }
      }

      return blockhitresult;
   }

   default double getBlockFloorHeight(VoxelShape p_45565_, Supplier<VoxelShape> p_45566_) {
      if (!p_45565_.isEmpty()) {
         return p_45565_.max(Direction.Axis.Y);
      }

      double d0 = p_45566_.get().max(Direction.Axis.Y);
      return d0 >= 1.0 ? d0 - 1.0 : Double.NEGATIVE_INFINITY;
   }

   default double getBlockFloorHeight(BlockPos p_45574_) {
      return this.getBlockFloorHeight(this.getBlockState(p_45574_).getCollisionShape(this, p_45574_), () -> {
         BlockPos blockpos = p_45574_.below();
         return this.getBlockState(blockpos).getCollisionShape(this, blockpos);
      });
   }

   static <T, C> T traverseBlocks(Vec3 p_151362_, Vec3 p_151363_, C p_151364_, BiFunction<C, BlockPos, @Nullable T> p_151365_, Function<C, T> p_151366_) {
      if (p_151362_.equals(p_151363_)) {
         return p_151366_.apply(p_151364_);
      }

      double d0 = Mth.lerp(-1.0E-7, p_151363_.x, p_151362_.x);
      double d1 = Mth.lerp(-1.0E-7, p_151363_.y, p_151362_.y);
      double d2 = Mth.lerp(-1.0E-7, p_151363_.z, p_151362_.z);
      double d3 = Mth.lerp(-1.0E-7, p_151362_.x, p_151363_.x);
      double d4 = Mth.lerp(-1.0E-7, p_151362_.y, p_151363_.y);
      double d5 = Mth.lerp(-1.0E-7, p_151362_.z, p_151363_.z);
      int i = Mth.floor(d3);
      int j = Mth.floor(d4);
      int k = Mth.floor(d5);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(i, j, k);
      T t = p_151365_.apply(p_151364_, blockpos$mutableblockpos);
      if (t != null) {
         return t;
      }

      double d6 = d0 - d3;
      double d7 = d1 - d4;
      double d8 = d2 - d5;
      int l = Mth.sign(d6);
      int i1 = Mth.sign(d7);
      int j1 = Mth.sign(d8);
      double d9 = l == 0 ? Double.MAX_VALUE : l / d6;
      double d10 = i1 == 0 ? Double.MAX_VALUE : i1 / d7;
      double d11 = j1 == 0 ? Double.MAX_VALUE : j1 / d8;
      double d12 = d9 * (l > 0 ? 1.0 - Mth.frac(d3) : Mth.frac(d3));
      double d13 = d10 * (i1 > 0 ? 1.0 - Mth.frac(d4) : Mth.frac(d4));
      double d14 = d11 * (j1 > 0 ? 1.0 - Mth.frac(d5) : Mth.frac(d5));

      while (d12 <= 1.0 || d13 <= 1.0 || d14 <= 1.0) {
         if (d12 < d13) {
            if (d12 < d14) {
               i += l;
               d12 += d9;
            } else {
               k += j1;
               d14 += d11;
            }
         } else if (d13 < d14) {
            j += i1;
            d13 += d10;
         } else {
            k += j1;
            d14 += d11;
         }

         T t1 = p_151365_.apply(p_151364_, blockpos$mutableblockpos.set(i, j, k));
         if (t1 != null) {
            return t1;
         }
      }

      return p_151366_.apply(p_151364_);
   }

   static boolean forEachBlockIntersectedBetween(Vec3 p_395886_, Vec3 p_397649_, AABB p_393584_, BlockGetter.BlockStepVisitor p_396917_) {
      Vec3 vec3 = p_397649_.subtract(p_395886_);
      if (vec3.lengthSqr() < Mth.square(1.0E-5F)) {
         for (BlockPos blockpos2 : BlockPos.betweenClosed(p_393584_)) {
            if (!p_396917_.visit(blockpos2, 0)) {
               return false;
            }
         }

         return true;
      } else {
         LongSet longset = new LongOpenHashSet();

         for (BlockPos blockpos : BlockPos.betweenCornersInDirection(p_393584_.move(vec3.scale(-1.0)), vec3)) {
            if (!p_396917_.visit(blockpos, 0)) {
               return false;
            }

            longset.add(blockpos.asLong());
         }

         int i = addCollisionsAlongTravel(longset, vec3, p_393584_, p_396917_);
         if (i < 0) {
            return false;
         }

         for (BlockPos blockpos1 : BlockPos.betweenCornersInDirection(p_393584_, vec3)) {
            if (longset.add(blockpos1.asLong()) && !p_396917_.visit(blockpos1, i + 1)) {
               return false;
            }
         }

         return true;
      }
   }

   private static int addCollisionsAlongTravel(LongSet p_393931_, Vec3 p_362149_, AABB p_364725_, BlockGetter.BlockStepVisitor p_397525_) {
      double d0 = p_364725_.getXsize();
      double d1 = p_364725_.getYsize();
      double d2 = p_364725_.getZsize();
      Vec3i vec3i = getFurthestCorner(p_362149_);
      Vec3 vec3 = p_364725_.getCenter();
      Vec3 vec31 = new Vec3(vec3.x() + d0 * 0.5 * vec3i.getX(), vec3.y() + d1 * 0.5 * vec3i.getY(), vec3.z() + d2 * 0.5 * vec3i.getZ());
      Vec3 vec32 = vec31.subtract(p_362149_);
      int i = Mth.floor(vec32.x);
      int j = Mth.floor(vec32.y);
      int k = Mth.floor(vec32.z);
      int l = Mth.sign(p_362149_.x);
      int i1 = Mth.sign(p_362149_.y);
      int j1 = Mth.sign(p_362149_.z);
      double d3 = l == 0 ? Double.MAX_VALUE : l / p_362149_.x;
      double d4 = i1 == 0 ? Double.MAX_VALUE : i1 / p_362149_.y;
      double d5 = j1 == 0 ? Double.MAX_VALUE : j1 / p_362149_.z;
      double d6 = d3 * (l > 0 ? 1.0 - Mth.frac(vec32.x) : Mth.frac(vec32.x));
      double d7 = d4 * (i1 > 0 ? 1.0 - Mth.frac(vec32.y) : Mth.frac(vec32.y));
      double d8 = d5 * (j1 > 0 ? 1.0 - Mth.frac(vec32.z) : Mth.frac(vec32.z));
      int k1 = 0;

      while (d6 <= 1.0 || d7 <= 1.0 || d8 <= 1.0) {
         if (d6 < d7) {
            if (d6 < d8) {
               i += l;
               d6 += d3;
            } else {
               k += j1;
               d8 += d5;
            }
         } else if (d7 < d8) {
            j += i1;
            d7 += d4;
         } else {
            k += j1;
            d8 += d5;
         }

         Optional<Vec3> optional = AABB.clip(i, j, k, i + 1, j + 1, k + 1, vec32, vec31);
         if (!optional.isEmpty()) {
            k1++;
            Vec3 vec33 = optional.get();
            double d9 = Mth.clamp(vec33.x, i + 1.0E-5F, i + 1.0 - 1.0E-5F);
            double d10 = Mth.clamp(vec33.y, j + 1.0E-5F, j + 1.0 - 1.0E-5F);
            double d11 = Mth.clamp(vec33.z, k + 1.0E-5F, k + 1.0 - 1.0E-5F);
            int l1 = Mth.floor(d9 - d0 * vec3i.getX());
            int i2 = Mth.floor(d10 - d1 * vec3i.getY());
            int j2 = Mth.floor(d11 - d2 * vec3i.getZ());
            int k2 = k1;

            for (BlockPos blockpos : BlockPos.betweenCornersInDirection(i, j, k, l1, i2, j2, p_362149_)) {
               if (p_393931_.add(blockpos.asLong()) && !p_397525_.visit(blockpos, k2)) {
                  return -1;
               }
            }
         }
      }

      return k1;
   }

   private static Vec3i getFurthestCorner(Vec3 p_427842_) {
      double d0 = Math.abs(Vec3.X_AXIS.dot(p_427842_));
      double d1 = Math.abs(Vec3.Y_AXIS.dot(p_427842_));
      double d2 = Math.abs(Vec3.Z_AXIS.dot(p_427842_));
      int i = p_427842_.x >= 0.0 ? 1 : -1;
      int j = p_427842_.y >= 0.0 ? 1 : -1;
      int k = p_427842_.z >= 0.0 ? 1 : -1;
      if (d0 <= d1 && d0 <= d2) {
         return new Vec3i(-i, -k, j);
      } else {
         return d1 <= d2 ? new Vec3i(k, -j, -i) : new Vec3i(-j, i, -k);
      }
   }

   @FunctionalInterface
   interface BlockStepVisitor {
      boolean visit(BlockPos var1, int var2);
   }
}
