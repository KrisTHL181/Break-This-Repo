package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public abstract class StructurePiece {
   protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
   protected BoundingBox boundingBox;
   private @Nullable Direction orientation;
   private Mirror mirror;
   private Rotation rotation;
   protected int genDepth;
   private final StructurePieceType type;
   private static final Set<Block> SHAPE_CHECK_BLOCKS = ImmutableSet.builder()
      .add(Blocks.NETHER_BRICK_FENCE)
      .add(Blocks.TORCH)
      .add(Blocks.WALL_TORCH)
      .add(Blocks.OAK_FENCE)
      .add(Blocks.SPRUCE_FENCE)
      .add(Blocks.DARK_OAK_FENCE)
      .add(Blocks.PALE_OAK_FENCE)
      .add(Blocks.ACACIA_FENCE)
      .add(Blocks.BIRCH_FENCE)
      .add(Blocks.JUNGLE_FENCE)
      .add(Blocks.LADDER)
      .add(Blocks.IRON_BARS)
      .build();

   protected StructurePiece(final StructurePieceType type, final int genDepth, final BoundingBox boundingBox) {
      this.type = type;
      this.genDepth = genDepth;
      this.boundingBox = boundingBox;
   }

   public StructurePiece(final StructurePieceType type, final CompoundTag tag) {
      this(type, tag.getIntOr("GD", 0), tag.<BoundingBox>read("BB", BoundingBox.CODEC).orElseThrow());
      int orientation = tag.getIntOr("O", 0);
      this.setOrientation(orientation == -1 ? null : Direction.from2DDataValue(orientation));
   }

   protected static BoundingBox makeBoundingBox(
      final int x, final int y, final int z, final Direction direction, final int width, final int height, final int depth
   ) {
      return direction.getAxis() == Direction.Axis.Z
         ? new BoundingBox(x, y, z, x + width - 1, y + height - 1, z + depth - 1)
         : new BoundingBox(x, y, z, x + depth - 1, y + height - 1, z + width - 1);
   }

   protected static Direction getRandomHorizontalDirection(final RandomSource random) {
      return Direction.Plane.HORIZONTAL.getRandomDirection(random);
   }

   public final CompoundTag createTag(final StructurePieceSerializationContext context) {
      CompoundTag tag = new CompoundTag();
      tag.putString("id", BuiltInRegistries.STRUCTURE_PIECE.getKey(this.getType()).toString());
      tag.store("BB", BoundingBox.CODEC, this.boundingBox);
      Direction orientation = this.getOrientation();
      tag.putInt("O", orientation == null ? -1 : orientation.get2DDataValue());
      tag.putInt("GD", this.genDepth);
      this.addAdditionalSaveData(context, tag);
      return tag;
   }

   protected abstract void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag);

   public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
   }

   public abstract void postProcess(
      final WorldGenLevel level,
      final StructureManager structureManager,
      final ChunkGenerator generator,
      final RandomSource random,
      final BoundingBox chunkBB,
      final ChunkPos chunkPos,
      final BlockPos referencePos
   );

   public BoundingBox getBoundingBox() {
      return this.boundingBox;
   }

   public int getGenDepth() {
      return this.genDepth;
   }

   public void setGenDepth(final int genDepth) {
      this.genDepth = genDepth;
   }

   public boolean isCloseToChunk(final ChunkPos pos, final int distance) {
      int cx = pos.getMinBlockX();
      int cz = pos.getMinBlockZ();
      return this.boundingBox.intersects(cx - distance, cz - distance, cx + 15 + distance, cz + 15 + distance);
   }

   public BlockPos getLocatorPosition() {
      return this.boundingBox.getCenter();
   }

   protected BlockPos.MutableBlockPos getWorldPos(final int x, final int y, final int z) {
      return new BlockPos.MutableBlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
   }

   protected int getWorldX(final int x, final int z) {
      Direction orientation = this.getOrientation();
      if (orientation == null) {
         return x;
      }

      return switch (orientation) {
         case NORTH, SOUTH -> this.boundingBox.minX() + x;
         case WEST -> this.boundingBox.maxX() - z;
         case EAST -> this.boundingBox.minX() + z;
         default -> x;
      };
   }

   protected int getWorldY(final int y) {
      return this.getOrientation() == null ? y : y + this.boundingBox.minY();
   }

   protected int getWorldZ(final int x, final int z) {
      Direction orientation = this.getOrientation();
      if (orientation == null) {
         return z;
      }

      return switch (orientation) {
         case NORTH -> this.boundingBox.maxZ() - z;
         case SOUTH -> this.boundingBox.minZ() + z;
         case WEST, EAST -> this.boundingBox.minZ() + x;
         default -> z;
      };
   }

   protected void placeBlock(final WorldGenLevel level, BlockState blockState, final int x, final int y, final int z, final BoundingBox chunkBB) {
      BlockPos pos = this.getWorldPos(x, y, z);
      if (chunkBB.isInside(pos)) {
         if (this.canBeReplaced(level, x, y, z, chunkBB)) {
            if (this.mirror != Mirror.NONE) {
               blockState = blockState.mirror(this.mirror);
            }

            if (this.rotation != Rotation.NONE) {
               blockState = blockState.rotate(this.rotation);
            }

            level.setBlock(pos, blockState, 2);
            FluidState fluidState = level.getFluidState(pos);
            if (!fluidState.isEmpty()) {
               level.scheduleTick(pos, fluidState.getType(), 0);
            }

            if (SHAPE_CHECK_BLOCKS.contains(blockState.getBlock())) {
               level.getChunk(pos).markPosForPostProcessing(pos);
            }
         }
      }
   }

   protected boolean canBeReplaced(final LevelReader level, final int x, final int y, final int z, final BoundingBox chunkBB) {
      return true;
   }

   protected BlockState getBlock(final BlockGetter level, final int x, final int y, final int z, final BoundingBox chunkBB) {
      BlockPos blockPos = this.getWorldPos(x, y, z);
      return !chunkBB.isInside(blockPos) ? Blocks.AIR.defaultBlockState() : level.getBlockState(blockPos);
   }

   protected boolean isInterior(final LevelReader level, final int x, final int y, final int z, final BoundingBox chunkBB) {
      BlockPos pos = this.getWorldPos(x, y + 1, z);
      return !chunkBB.isInside(pos) ? false : pos.getY() < level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());
   }

   protected void generateAirBox(
      final WorldGenLevel level, final BoundingBox chunkBB, final int x0, final int y0, final int z0, final int x1, final int y1, final int z1
   ) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, chunkBB);
            }
         }
      }
   }

   protected void generateBox(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final int x0,
      final int y0,
      final int z0,
      final int x1,
      final int y1,
      final int z1,
      final BlockState edgeBlock,
      final BlockState fillBlock,
      final boolean skipAir
   ) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               if (!skipAir || !this.getBlock(level, x, y, z, chunkBB).isAir()) {
                  if (y != y0 && y != y1 && x != x0 && x != x1 && z != z0 && z != z1) {
                     this.placeBlock(level, fillBlock, x, y, z, chunkBB);
                  } else {
                     this.placeBlock(level, edgeBlock, x, y, z, chunkBB);
                  }
               }
            }
         }
      }
   }

   protected void generateBox(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final BoundingBox boxBB,
      final BlockState edgeBlock,
      final BlockState fillBlock,
      final boolean skipAir
   ) {
      this.generateBox(level, chunkBB, boxBB.minX(), boxBB.minY(), boxBB.minZ(), boxBB.maxX(), boxBB.maxY(), boxBB.maxZ(), edgeBlock, fillBlock, skipAir);
   }

   protected void generateBox(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final int x0,
      final int y0,
      final int z0,
      final int x1,
      final int y1,
      final int z1,
      final boolean skipAir,
      final RandomSource random,
      final StructurePiece.BlockSelector selector
   ) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               if (!skipAir || !this.getBlock(level, x, y, z, chunkBB).isAir()) {
                  selector.next(random, x, y, z, y == y0 || y == y1 || x == x0 || x == x1 || z == z0 || z == z1);
                  this.placeBlock(level, selector.getNext(), x, y, z, chunkBB);
               }
            }
         }
      }
   }

   protected void generateBox(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final BoundingBox boxBB,
      final boolean skipAir,
      final RandomSource random,
      final StructurePiece.BlockSelector selector
   ) {
      this.generateBox(level, chunkBB, boxBB.minX(), boxBB.minY(), boxBB.minZ(), boxBB.maxX(), boxBB.maxY(), boxBB.maxZ(), skipAir, random, selector);
   }

   protected void generateMaybeBox(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final RandomSource random,
      final float probability,
      final int x0,
      final int y0,
      final int z0,
      final int x1,
      final int y1,
      final int z1,
      final BlockState edgeBlock,
      final BlockState fillBlock,
      final boolean skipAir,
      final boolean hasToBeInside
   ) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               if (!(random.nextFloat() > probability)
                  && (!skipAir || !this.getBlock(level, x, y, z, chunkBB).isAir())
                  && (!hasToBeInside || this.isInterior(level, x, y, z, chunkBB))) {
                  if (y != y0 && y != y1 && x != x0 && x != x1 && z != z0 && z != z1) {
                     this.placeBlock(level, fillBlock, x, y, z, chunkBB);
                  } else {
                     this.placeBlock(level, edgeBlock, x, y, z, chunkBB);
                  }
               }
            }
         }
      }
   }

   protected void maybeGenerateBlock(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final RandomSource random,
      final float probability,
      final int x,
      final int y,
      final int z,
      final BlockState blockState
   ) {
      if (random.nextFloat() < probability) {
         this.placeBlock(level, blockState, x, y, z, chunkBB);
      }
   }

   protected void generateUpperHalfSphere(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final int x0,
      final int y0,
      final int z0,
      final int x1,
      final int y1,
      final int z1,
      final BlockState fillBlock,
      final boolean skipAir
   ) {
      float diagX = x1 - x0 + 1;
      float diagY = y1 - y0 + 1;
      float diagZ = z1 - z0 + 1;
      float cx = x0 + diagX / 2.0F;
      float cz = z0 + diagZ / 2.0F;

      for (int y = y0; y <= y1; y++) {
         float normalizedYDistance = (y - y0) / diagY;

         for (int x = x0; x <= x1; x++) {
            float normalizedXDistance = (x - cx) / (diagX * 0.5F);

            for (int z = z0; z <= z1; z++) {
               float normalizedZDistance = (z - cz) / (diagZ * 0.5F);
               if (!skipAir || !this.getBlock(level, x, y, z, chunkBB).isAir()) {
                  float dist = normalizedXDistance * normalizedXDistance
                     + normalizedYDistance * normalizedYDistance
                     + normalizedZDistance * normalizedZDistance;
                  if (dist <= 1.05F) {
                     this.placeBlock(level, fillBlock, x, y, z, chunkBB);
                  }
               }
            }
         }
      }
   }

   protected void fillColumnDown(final WorldGenLevel level, final BlockState blockState, final int x, final int startY, final int z, final BoundingBox chunkBB) {
      BlockPos.MutableBlockPos pos = this.getWorldPos(x, startY, z);
      if (chunkBB.isInside(pos)) {
         while (this.isReplaceableByStructures(level.getBlockState(pos)) && pos.getY() > level.getMinY() + 1) {
            level.setBlock(pos, blockState, 2);
            pos.move(Direction.DOWN);
         }
      }
   }

   protected boolean isReplaceableByStructures(final BlockState state) {
      return state.isAir() || state.liquid() || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
   }

   protected boolean createChest(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final RandomSource random,
      final int x,
      final int y,
      final int z,
      final ResourceKey<LootTable> lootTable
   ) {
      return this.createChest(level, chunkBB, random, this.getWorldPos(x, y, z), lootTable, null);
   }

   public static BlockState reorient(final BlockGetter level, final BlockPos blockPos, final BlockState blockState) {
      Direction solidNeighbor = null;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos relativePos = blockPos.relative(direction);
         BlockState state = level.getBlockState(relativePos);
         if (state.is(Blocks.CHEST)) {
            return blockState;
         }

         if (state.isSolidRender()) {
            if (solidNeighbor != null) {
               solidNeighbor = null;
               break;
            }

            solidNeighbor = direction;
         }
      }

      if (solidNeighbor != null) {
         return blockState.setValue(HorizontalDirectionalBlock.FACING, solidNeighbor.getOpposite());
      }

      Direction lockDir = blockState.getValue(HorizontalDirectionalBlock.FACING);
      BlockPos relativePos = blockPos.relative(lockDir);
      if (level.getBlockState(relativePos).isSolidRender()) {
         lockDir = lockDir.getOpposite();
         relativePos = blockPos.relative(lockDir);
      }

      if (level.getBlockState(relativePos).isSolidRender()) {
         lockDir = lockDir.getClockWise();
         relativePos = blockPos.relative(lockDir);
      }

      if (level.getBlockState(relativePos).isSolidRender()) {
         lockDir = lockDir.getOpposite();
         relativePos = blockPos.relative(lockDir);
      }

      return blockState.setValue(HorizontalDirectionalBlock.FACING, lockDir);
   }

   protected boolean createChest(
      final ServerLevelAccessor level,
      final BoundingBox chunkBB,
      final RandomSource random,
      final BlockPos pos,
      final ResourceKey<LootTable> lootTable,
      @Nullable BlockState blockState
   ) {
      if (chunkBB.isInside(pos) && !level.getBlockState(pos).is(Blocks.CHEST)) {
         if (blockState == null) {
            blockState = reorient(level, pos, Blocks.CHEST.defaultBlockState());
         }

         level.setBlock(pos, blockState, 2);
         if (level.getBlockEntity(pos) instanceof ChestBlockEntity chestBlockEntity) {
            chestBlockEntity.setLootTable(lootTable, random.nextLong());
         }

         return true;
      } else {
         return false;
      }
   }

   protected boolean createDispenser(
      final WorldGenLevel level,
      final BoundingBox chunkBB,
      final RandomSource random,
      final int x,
      final int y,
      final int z,
      final Direction facing,
      final ResourceKey<LootTable> lootTable
   ) {
      BlockPos pos = this.getWorldPos(x, y, z);
      if (chunkBB.isInside(pos) && !level.getBlockState(pos).is(Blocks.DISPENSER)) {
         this.placeBlock(level, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing), x, y, z, chunkBB);
         if (level.getBlockEntity(pos) instanceof DispenserBlockEntity dispenserBlockEntity) {
            dispenserBlockEntity.setLootTable(lootTable, random.nextLong());
         }

         return true;
      } else {
         return false;
      }
   }

   public void move(final int dx, final int dy, final int dz) {
      this.boundingBox.move(dx, dy, dz);
   }

   public static BoundingBox createBoundingBox(final Stream<StructurePiece> pieces) {
      return BoundingBox.encapsulatingBoxes(pieces.map(StructurePiece::getBoundingBox)::iterator)
         .orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox without pieces"));
   }

   public static @Nullable StructurePiece findCollisionPiece(final List<StructurePiece> pieces, final BoundingBox box) {
      for (StructurePiece piece : pieces) {
         if (piece.getBoundingBox().intersects(box)) {
            return piece;
         }
      }

      return null;
   }

   public @Nullable Direction getOrientation() {
      return this.orientation;
   }

   public void setOrientation(final @Nullable Direction orientation) {
      this.orientation = orientation;
      if (orientation == null) {
         this.rotation = Rotation.NONE;
         this.mirror = Mirror.NONE;
      } else {
         switch (orientation) {
            case SOUTH:
               this.mirror = Mirror.LEFT_RIGHT;
               this.rotation = Rotation.NONE;
               break;
            case WEST:
               this.mirror = Mirror.LEFT_RIGHT;
               this.rotation = Rotation.CLOCKWISE_90;
               break;
            case EAST:
               this.mirror = Mirror.NONE;
               this.rotation = Rotation.CLOCKWISE_90;
               break;
            default:
               this.mirror = Mirror.NONE;
               this.rotation = Rotation.NONE;
         }
      }
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public StructurePieceType getType() {
      return this.type;
   }

   public abstract static class BlockSelector {
      protected BlockState next = Blocks.AIR.defaultBlockState();

      public abstract void next(RandomSource random, int worldX, int worldY, int worldZ, boolean isEdge);

      public BlockState getNext() {
         return this.next;
      }
   }
}
