package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class WalkNodeEvaluator extends NodeEvaluator {
   public static final double SPACE_BETWEEN_WALL_POSTS = 0.5;
   private static final double DEFAULT_MOB_JUMP_HEIGHT = 1.125;
   private final Long2ObjectMap<PathType> pathTypesByPosCacheByMob = new Long2ObjectOpenHashMap();
   private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap();
   private final Node[] reusableNeighbors = new Node[Direction.Plane.HORIZONTAL.length()];

   @Override
   public void prepare(PathNavigationRegion p_77620_, Mob p_77621_) {
      super.prepare(p_77620_, p_77621_);
      p_77621_.onPathfindingStart();
   }

   @Override
   public void done() {
      this.mob.onPathfindingDone();
      this.pathTypesByPosCacheByMob.clear();
      this.collisionCache.clear();
      super.done();
   }

   @Override
   public Node getStart() {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      int i = this.mob.getBlockY();
      BlockState blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), i, this.mob.getZ()));
      if (!this.mob.canStandOnFluid(blockstate.getFluidState())) {
         if (this.canFloat() && this.mob.isInWater()) {
            while (true) {
               if (!blockstate.is(Blocks.WATER) && blockstate.getFluidState() != Fluids.WATER.getSource(false)) {
                  i--;
                  break;
               }

               blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), ++i, this.mob.getZ()));
            }
         } else if (this.mob.onGround()) {
            i = Mth.floor(this.mob.getY() + 0.5);
         } else {
            blockpos$mutableblockpos.set(this.mob.getX(), this.mob.getY() + 1.0, this.mob.getZ());

            while (blockpos$mutableblockpos.getY() > this.currentContext.level().getMinY()) {
               i = blockpos$mutableblockpos.getY();
               blockpos$mutableblockpos.setY(blockpos$mutableblockpos.getY() - 1);
               BlockState blockstate1 = this.currentContext.getBlockState(blockpos$mutableblockpos);
               if (!blockstate1.isAir() && !blockstate1.isPathfindable(PathComputationType.LAND)) {
                  break;
               }
            }
         }
      } else {
         while (this.mob.canStandOnFluid(blockstate.getFluidState())) {
            blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), ++i, this.mob.getZ()));
         }

         i--;
      }

      BlockPos blockpos = this.mob.blockPosition();
      if (!this.canStartAt(blockpos$mutableblockpos.set(blockpos.getX(), i, blockpos.getZ()))) {
         AABB aabb = this.mob.getBoundingBox();
         if (this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, i, aabb.minZ))
            || this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, i, aabb.maxZ))
            || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, i, aabb.minZ))
            || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, i, aabb.maxZ))) {
            return this.getStartNode(blockpos$mutableblockpos);
         }
      }

      return this.getStartNode(new BlockPos(blockpos.getX(), i, blockpos.getZ()));
   }

   protected Node getStartNode(BlockPos p_230632_) {
      Node node = this.getNode(p_230632_);
      node.type = this.getCachedPathType(node.x, node.y, node.z);
      node.costMalus = this.mob.getPathfindingMalus(node.type);
      return node;
   }

   protected boolean canStartAt(BlockPos p_262596_) {
      PathType pathtype = this.getCachedPathType(p_262596_.getX(), p_262596_.getY(), p_262596_.getZ());
      return pathtype != PathType.OPEN && this.mob.getPathfindingMalus(pathtype) >= 0.0F;
   }

   @Override
   public Target getTarget(double p_334058_, double p_329070_, double p_328068_) {
      return this.getTargetNodeAt(p_334058_, p_329070_, p_328068_);
   }

   @Override
   public int getNeighbors(Node[] p_77640_, Node p_77641_) {
      int i = 0;
      int j = 0;
      PathType pathtype = this.getCachedPathType(p_77641_.x, p_77641_.y + 1, p_77641_.z);
      PathType pathtype1 = this.getCachedPathType(p_77641_.x, p_77641_.y, p_77641_.z);
      if (this.mob.getPathfindingMalus(pathtype) >= 0.0F && pathtype1 != PathType.STICKY_HONEY) {
         j = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
      }

      double d0 = this.getFloorLevel(new BlockPos(p_77641_.x, p_77641_.y, p_77641_.z));

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         Node node = this.findAcceptedNode(p_77641_.x + direction.getStepX(), p_77641_.y, p_77641_.z + direction.getStepZ(), j, d0, direction, pathtype1);
         this.reusableNeighbors[direction.get2DDataValue()] = node;
         if (this.isNeighborValid(node, p_77641_)) {
            p_77640_[i++] = node;
         }
      }

      for (Direction direction1 : Direction.Plane.HORIZONTAL) {
         Direction direction2 = direction1.getClockWise();
         if (this.isDiagonalValid(p_77641_, this.reusableNeighbors[direction1.get2DDataValue()], this.reusableNeighbors[direction2.get2DDataValue()])) {
            Node node1 = this.findAcceptedNode(
               p_77641_.x + direction1.getStepX() + direction2.getStepX(),
               p_77641_.y,
               p_77641_.z + direction1.getStepZ() + direction2.getStepZ(),
               j,
               d0,
               direction1,
               pathtype1
            );
            if (this.isDiagonalValid(node1)) {
               p_77640_[i++] = node1;
            }
         }
      }

      return i;
   }

   protected boolean isNeighborValid(@Nullable Node p_77627_, Node p_77628_) {
      return p_77627_ != null && !p_77627_.closed && (p_77627_.costMalus >= 0.0F || p_77628_.costMalus < 0.0F);
   }

   protected boolean isDiagonalValid(Node p_77630_, @Nullable Node p_77631_, @Nullable Node p_77632_) {
      if (p_77632_ == null || p_77631_ == null || p_77632_.y > p_77630_.y || p_77631_.y > p_77630_.y) {
         return false;
      } else if (p_77631_.type != PathType.WALKABLE_DOOR && p_77632_.type != PathType.WALKABLE_DOOR) {
         boolean flag = p_77632_.type == PathType.FENCE && p_77631_.type == PathType.FENCE && this.mob.getBbWidth() < 0.5;
         return (p_77632_.y < p_77630_.y || p_77632_.costMalus >= 0.0F || flag) && (p_77631_.y < p_77630_.y || p_77631_.costMalus >= 0.0F || flag);
      } else {
         return false;
      }
   }

   protected boolean isDiagonalValid(@Nullable Node p_332817_) {
      if (p_332817_ != null && !p_332817_.closed) {
         return p_332817_.type == PathType.WALKABLE_DOOR ? false : p_332817_.costMalus >= 0.0F;
      } else {
         return false;
      }
   }

   private static boolean doesBlockHavePartialCollision(PathType p_332557_) {
      return p_332557_ == PathType.FENCE || p_332557_ == PathType.DOOR_WOOD_CLOSED || p_332557_ == PathType.DOOR_IRON_CLOSED;
   }

   private boolean canReachWithoutCollision(Node p_77625_) {
      AABB aabb = this.mob.getBoundingBox();
      Vec3 vec3 = new Vec3(
         p_77625_.x - this.mob.getX() + aabb.getXsize() / 2.0,
         p_77625_.y - this.mob.getY() + aabb.getYsize() / 2.0,
         p_77625_.z - this.mob.getZ() + aabb.getZsize() / 2.0
      );
      int i = Mth.ceil(vec3.length() / aabb.getSize());
      vec3 = vec3.scale(1.0F / i);

      for (int j = 1; j <= i; j++) {
         aabb = aabb.move(vec3);
         if (this.hasCollisions(aabb)) {
            return false;
         }
      }

      return true;
   }

   protected double getFloorLevel(BlockPos p_164733_) {
      BlockGetter blockgetter = this.currentContext.level();
      return (this.canFloat() || this.isAmphibious()) && blockgetter.getFluidState(p_164733_).is(FluidTags.WATER)
         ? p_164733_.getY() + 0.5
         : getFloorLevel(blockgetter, p_164733_);
   }

   public static double getFloorLevel(BlockGetter p_77612_, BlockPos p_77613_) {
      BlockPos blockpos = p_77613_.below();
      VoxelShape voxelshape = p_77612_.getBlockState(blockpos).getCollisionShape(p_77612_, blockpos);
      return blockpos.getY() + (voxelshape.isEmpty() ? 0.0 : voxelshape.max(Direction.Axis.Y));
   }

   protected boolean isAmphibious() {
      return false;
   }

   protected @Nullable Node findAcceptedNode(
      int p_164726_, int p_164727_, int p_164728_, int p_164729_, double p_164730_, Direction p_164731_, PathType p_330077_
   ) {
      Node node = null;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      double d0 = this.getFloorLevel(blockpos$mutableblockpos.set(p_164726_, p_164727_, p_164728_));
      if (d0 - p_164730_ > this.getMobJumpHeight()) {
         return null;
      }

      PathType pathtype = this.getCachedPathType(p_164726_, p_164727_, p_164728_);
      float f = this.mob.getPathfindingMalus(pathtype);
      if (f >= 0.0F) {
         node = this.getNodeAndUpdateCostToMax(p_164726_, p_164727_, p_164728_, pathtype, f);
      }

      if (doesBlockHavePartialCollision(p_330077_) && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
         node = null;
      }

      if (pathtype != PathType.WALKABLE && (!this.isAmphibious() || pathtype != PathType.WATER)) {
         if ((node == null || node.costMalus < 0.0F)
            && p_164729_ > 0
            && (pathtype != PathType.FENCE || this.canWalkOverFences())
            && pathtype != PathType.UNPASSABLE_RAIL
            && pathtype != PathType.TRAPDOOR
            && pathtype != PathType.POWDER_SNOW) {
            node = this.tryJumpOn(p_164726_, p_164727_, p_164728_, p_164729_, p_164730_, p_164731_, p_330077_, blockpos$mutableblockpos);
         } else if (!this.isAmphibious() && pathtype == PathType.WATER && !this.canFloat()) {
            node = this.tryFindFirstNonWaterBelow(p_164726_, p_164727_, p_164728_, node);
         } else if (pathtype == PathType.OPEN) {
            node = this.tryFindFirstGroundNodeBelow(p_164726_, p_164727_, p_164728_);
         } else if (doesBlockHavePartialCollision(pathtype) && node == null) {
            node = this.getClosedNode(p_164726_, p_164727_, p_164728_, pathtype);
         }

         return node;
      } else {
         return node;
      }
   }

   private double getMobJumpHeight() {
      return Math.max(1.125, this.mob.maxUpStep());
   }

   private Node getNodeAndUpdateCostToMax(int p_230620_, int p_230621_, int p_230622_, PathType p_335762_, float p_230624_) {
      Node node = this.getNode(p_230620_, p_230621_, p_230622_);
      node.type = p_335762_;
      node.costMalus = Math.max(node.costMalus, p_230624_);
      return node;
   }

   private Node getBlockedNode(int p_230628_, int p_230629_, int p_230630_) {
      Node node = this.getNode(p_230628_, p_230629_, p_230630_);
      node.type = PathType.BLOCKED;
      node.costMalus = -1.0F;
      return node;
   }

   private Node getClosedNode(int p_332713_, int p_333094_, int p_327804_, PathType p_334600_) {
      Node node = this.getNode(p_332713_, p_333094_, p_327804_);
      node.closed = true;
      node.type = p_334600_;
      node.costMalus = p_334600_.getMalus();
      return node;
   }

   private @Nullable Node tryJumpOn(
      int p_335353_, int p_333388_, int p_331837_, int p_329120_, double p_335627_, Direction p_334618_, PathType p_330418_, BlockPos.MutableBlockPos p_329431_
   ) {
      Node node = this.findAcceptedNode(p_335353_, p_333388_ + 1, p_331837_, p_329120_ - 1, p_335627_, p_334618_, p_330418_);
      if (node == null) {
         return null;
      }

      if (this.mob.getBbWidth() >= 1.0F) {
         return node;
      }

      if (node.type != PathType.OPEN && node.type != PathType.WALKABLE) {
         return node;
      }

      double d0 = p_335353_ - p_334618_.getStepX() + 0.5;
      double d1 = p_331837_ - p_334618_.getStepZ() + 0.5;
      double d2 = this.mob.getBbWidth() / 2.0;
      AABB aabb = new AABB(
         d0 - d2,
         this.getFloorLevel(p_329431_.set(d0, p_333388_ + 1, d1)) + 0.001,
         d1 - d2,
         d0 + d2,
         this.mob.getBbHeight() + this.getFloorLevel(p_329431_.set(node.x, node.y, node.z)) - 0.002,
         d1 + d2
      );
      return this.hasCollisions(aabb) ? null : node;
   }

   private @Nullable Node tryFindFirstNonWaterBelow(int p_334565_, int p_335840_, int p_330496_, @Nullable Node p_327969_) {
      p_335840_--;

      while (p_335840_ > this.mob.level().getMinY()) {
         PathType pathtype = this.getCachedPathType(p_334565_, p_335840_, p_330496_);
         if (pathtype != PathType.WATER) {
            return p_327969_;
         }

         p_327969_ = this.getNodeAndUpdateCostToMax(p_334565_, p_335840_, p_330496_, pathtype, this.mob.getPathfindingMalus(pathtype));
         p_335840_--;
      }

      return p_327969_;
   }

   private Node tryFindFirstGroundNodeBelow(int p_335495_, int p_328639_, int p_335885_) {
      for (int i = p_328639_ - 1; i >= this.mob.level().getMinY(); i--) {
         if (p_328639_ - i > this.mob.getMaxFallDistance()) {
            return this.getBlockedNode(p_335495_, i, p_335885_);
         }

         PathType pathtype = this.getCachedPathType(p_335495_, i, p_335885_);
         float f = this.mob.getPathfindingMalus(pathtype);
         if (pathtype != PathType.OPEN) {
            if (f >= 0.0F) {
               return this.getNodeAndUpdateCostToMax(p_335495_, i, p_335885_, pathtype, f);
            }

            return this.getBlockedNode(p_335495_, i, p_335885_);
         }
      }

      return this.getBlockedNode(p_335495_, p_328639_, p_335885_);
   }

   private boolean hasCollisions(AABB p_77635_) {
      return this.collisionCache.computeIfAbsent(p_77635_, p_327517_ -> !this.currentContext.level().noCollision(this.mob, p_77635_));
   }

   protected PathType getCachedPathType(int p_328411_, int p_334833_, int p_334446_) {
      return (PathType)this.pathTypesByPosCacheByMob
         .computeIfAbsent(
            BlockPos.asLong(p_328411_, p_334833_, p_334446_),
            p_327521_ -> this.getPathTypeOfMob(this.currentContext, p_328411_, p_334833_, p_334446_, this.mob)
         );
   }

   @Override
   public PathType getPathTypeOfMob(PathfindingContext p_336212_, int p_330284_, int p_332224_, int p_335362_, Mob p_327680_) {
      Set<PathType> set = this.getPathTypeWithinMobBB(p_336212_, p_330284_, p_332224_, p_335362_);
      if (set.contains(PathType.FENCE)) {
         return PathType.FENCE;
      }

      if (set.contains(PathType.UNPASSABLE_RAIL)) {
         return PathType.UNPASSABLE_RAIL;
      }

      PathType pathtype = PathType.BLOCKED;

      for (PathType pathtype1 : set) {
         if (p_327680_.getPathfindingMalus(pathtype1) < 0.0F) {
            return pathtype1;
         }

         if (p_327680_.getPathfindingMalus(pathtype1) >= p_327680_.getPathfindingMalus(pathtype)) {
            pathtype = pathtype1;
         }
      }

      return this.entityWidth <= 1
            && pathtype != PathType.OPEN
            && p_327680_.getPathfindingMalus(pathtype) == 0.0F
            && this.getPathType(p_336212_, p_330284_, p_332224_, p_335362_) == PathType.OPEN
         ? PathType.OPEN
         : pathtype;
   }

   public Set<PathType> getPathTypeWithinMobBB(PathfindingContext p_334304_, int p_335980_, int p_330052_, int p_334476_) {
      EnumSet<PathType> enumset = EnumSet.noneOf(PathType.class);

      for (int i = 0; i < this.entityWidth; i++) {
         for (int j = 0; j < this.entityHeight; j++) {
            for (int k = 0; k < this.entityDepth; k++) {
               int l = i + p_335980_;
               int i1 = j + p_330052_;
               int j1 = k + p_334476_;
               PathType pathtype = this.getPathType(p_334304_, l, i1, j1);
               BlockPos blockpos = this.mob.blockPosition();
               boolean flag = this.canPassDoors();
               if (pathtype == PathType.DOOR_WOOD_CLOSED && this.canOpenDoors() && flag) {
                  pathtype = PathType.WALKABLE_DOOR;
               }

               if (pathtype == PathType.DOOR_OPEN && !flag) {
                  pathtype = PathType.BLOCKED;
               }

               if (pathtype == PathType.RAIL
                  && this.getPathType(p_334304_, blockpos.getX(), blockpos.getY(), blockpos.getZ()) != PathType.RAIL
                  && this.getPathType(p_334304_, blockpos.getX(), blockpos.getY() - 1, blockpos.getZ()) != PathType.RAIL) {
                  pathtype = PathType.UNPASSABLE_RAIL;
               }

               enumset.add(pathtype);
            }
         }
      }

      return enumset;
   }

   @Override
   public PathType getPathType(PathfindingContext p_333098_, int p_327758_, int p_329863_, int p_328680_) {
      return getPathTypeStatic(p_333098_, new BlockPos.MutableBlockPos(p_327758_, p_329863_, p_328680_));
   }

   public static PathType getPathTypeStatic(Mob p_332988_, BlockPos p_332803_) {
      return getPathTypeStatic(new PathfindingContext(p_332988_.level(), p_332988_), p_332803_.mutable());
   }

   public static PathType getPathTypeStatic(PathfindingContext p_335315_, BlockPos.MutableBlockPos p_334167_) {
      int i = p_334167_.getX();
      int j = p_334167_.getY();
      int k = p_334167_.getZ();
      PathType pathtype = p_335315_.getPathTypeFromState(i, j, k);
      if (pathtype == PathType.OPEN && j >= p_335315_.level().getMinY() + 1) {
         return switch (p_335315_.getPathTypeFromState(i, j - 1, k)) {
            case OPEN, WATER, LAVA, WALKABLE -> PathType.OPEN;
            case DAMAGE_FIRE -> PathType.DAMAGE_FIRE;
            case DAMAGE_OTHER -> PathType.DAMAGE_OTHER;
            case STICKY_HONEY -> PathType.STICKY_HONEY;
            case POWDER_SNOW -> PathType.DANGER_POWDER_SNOW;
            case DAMAGE_CAUTIOUS -> PathType.DAMAGE_CAUTIOUS;
            case TRAPDOOR -> PathType.DANGER_TRAPDOOR;
            default -> checkNeighbourBlocks(p_335315_, i, j, k, PathType.WALKABLE);
         };
      } else {
         return pathtype;
      }
   }

   public static PathType checkNeighbourBlocks(PathfindingContext p_334221_, int p_336062_, int p_335259_, int p_336315_, PathType p_333971_) {
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
               if (i != 0 || k != 0) {
                  PathType pathtype = p_334221_.getPathTypeFromState(p_336062_ + i, p_335259_ + j, p_336315_ + k);
                  if (pathtype == PathType.DAMAGE_OTHER) {
                     return PathType.DANGER_OTHER;
                  }

                  if (pathtype == PathType.DAMAGE_FIRE || pathtype == PathType.LAVA) {
                     return PathType.DANGER_FIRE;
                  }

                  if (pathtype == PathType.WATER) {
                     return PathType.WATER_BORDER;
                  }

                  if (pathtype == PathType.DAMAGE_CAUTIOUS) {
                     return PathType.DAMAGE_CAUTIOUS;
                  }
               }
            }
         }
      }

      return p_333971_;
   }

   protected static PathType getPathTypeFromState(BlockGetter p_335222_, BlockPos p_331935_) {
      BlockState blockstate = p_335222_.getBlockState(p_331935_);
      Block block = blockstate.getBlock();
      if (blockstate.isAir()) {
         return PathType.OPEN;
      }

      if (blockstate.is(BlockTags.TRAPDOORS) || blockstate.is(Blocks.LILY_PAD) || blockstate.is(Blocks.BIG_DRIPLEAF)) {
         return PathType.TRAPDOOR;
      }

      if (blockstate.is(Blocks.POWDER_SNOW)) {
         return PathType.POWDER_SNOW;
      }

      if (blockstate.is(Blocks.CACTUS) || blockstate.is(Blocks.SWEET_BERRY_BUSH)) {
         return PathType.DAMAGE_OTHER;
      }

      if (blockstate.is(Blocks.HONEY_BLOCK)) {
         return PathType.STICKY_HONEY;
      }

      if (blockstate.is(Blocks.COCOA)) {
         return PathType.COCOA;
      }

      if (!blockstate.is(Blocks.WITHER_ROSE) && !blockstate.is(Blocks.POINTED_DRIPSTONE)) {
         FluidState fluidstate = blockstate.getFluidState();
         if (fluidstate.is(FluidTags.LAVA)) {
            return PathType.LAVA;
         }

         if (isBurningBlock(blockstate)) {
            return PathType.DAMAGE_FIRE;
         }

         if (block instanceof DoorBlock doorblock) {
            if (blockstate.getValue(DoorBlock.OPEN)) {
               return PathType.DOOR_OPEN;
            } else {
               return doorblock.type().canOpenByHand() ? PathType.DOOR_WOOD_CLOSED : PathType.DOOR_IRON_CLOSED;
            }
         } else {
            if (block instanceof BaseRailBlock) {
               return PathType.RAIL;
            }

            if (block instanceof LeavesBlock) {
               return PathType.LEAVES;
            }

            if (!blockstate.is(BlockTags.FENCES)
               && !blockstate.is(BlockTags.WALLS)
               && (!(block instanceof FenceGateBlock) || blockstate.getValue(FenceGateBlock.OPEN))) {
               if (!blockstate.isPathfindable(PathComputationType.LAND)) {
                  return PathType.BLOCKED;
               } else {
                  return fluidstate.is(FluidTags.WATER) ? PathType.WATER : PathType.OPEN;
               }
            } else {
               return PathType.FENCE;
            }
         }
      } else {
         return PathType.DAMAGE_CAUTIOUS;
      }
   }
}
