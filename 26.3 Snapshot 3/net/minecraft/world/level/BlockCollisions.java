package net.minecraft.world.level;

import com.google.common.collect.AbstractIterator;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockCollisions<T> extends AbstractIterator<T> {
   private final AABB box;
   private final CollisionContext context;
   private final Cursor3D cursor;
   private final BlockPos.MutableBlockPos pos;
   private final VoxelShape entityShape;
   private final CollisionGetter collisionGetter;
   private final boolean onlySuffocatingBlocks;
   private @Nullable BlockGetter cachedBlockGetter;
   private long cachedBlockGetterPos;
   private final BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider;

   public BlockCollisions(
      final CollisionGetter collisionGetter,
      final @Nullable Entity source,
      final AABB box,
      final boolean onlySuffocatingBlocks,
      final BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider
   ) {
      this(collisionGetter, source == null ? CollisionContext.empty() : CollisionContext.of(source), box, onlySuffocatingBlocks, resultProvider);
   }

   public BlockCollisions(
      final CollisionGetter collisionGetter,
      final CollisionContext context,
      final AABB box,
      final boolean onlySuffocatingBlocks,
      final BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider
   ) {
      this.context = context;
      this.pos = new BlockPos.MutableBlockPos();
      this.entityShape = Shapes.create(box);
      this.collisionGetter = collisionGetter;
      this.box = box;
      this.onlySuffocatingBlocks = onlySuffocatingBlocks;
      this.resultProvider = resultProvider;
      int x0 = Mth.floor(box.minX - 1.0E-7) - 1;
      int x1 = Mth.floor(box.maxX + 1.0E-7) + 1;
      int y0 = Mth.floor(box.minY - 1.0E-7) - 1;
      int y1 = Mth.floor(box.maxY + 1.0E-7) + 1;
      int z0 = Mth.floor(box.minZ - 1.0E-7) - 1;
      int z1 = Mth.floor(box.maxZ + 1.0E-7) + 1;
      this.cursor = new Cursor3D(x0, y0, z0, x1, y1, z1);
   }

   private @Nullable BlockGetter getChunk(final int x, final int z) {
      int chunkX = SectionPos.blockToSectionCoord(x);
      int chunkZ = SectionPos.blockToSectionCoord(z);
      long chunkPos = ChunkPos.pack(chunkX, chunkZ);
      if (this.cachedBlockGetter != null && this.cachedBlockGetterPos == chunkPos) {
         return this.cachedBlockGetter;
      }

      BlockGetter result = this.collisionGetter.getChunkForCollisions(chunkX, chunkZ);
      this.cachedBlockGetter = result;
      this.cachedBlockGetterPos = chunkPos;
      return result;
   }

   protected T computeNext() {
      while (this.cursor.advance()) {
         int x = this.cursor.nextX();
         int y = this.cursor.nextY();
         int z = this.cursor.nextZ();
         int cursorFaceType = this.cursor.getNextType();
         if (cursorFaceType != 3) {
            BlockGetter chunk = this.getChunk(x, z);
            if (chunk != null) {
               this.pos.set(x, y, z);
               BlockState blockState = chunk.getBlockState(this.pos);
               if ((!this.onlySuffocatingBlocks || blockState.isSuffocating(chunk, this.pos))
                  && (cursorFaceType != 1 || blockState.hasLargeCollisionShape())
                  && (cursorFaceType != 2 || blockState.is(Blocks.MOVING_PISTON))) {
                  VoxelShape blockShape = this.context.getCollisionShape(blockState, this.collisionGetter, this.pos);
                  if (blockShape == Shapes.block()) {
                     if (this.box.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0)) {
                        return this.resultProvider.apply(this.pos, blockShape.move(this.pos));
                     }
                  } else {
                     VoxelShape shape = blockShape.move(this.pos);
                     if (!shape.isEmpty() && Shapes.joinIsNotEmpty(shape, this.entityShape, BooleanOp.AND)) {
                        return this.resultProvider.apply(this.pos, shape);
                     }
                  }
               }
            }
         }
      }

      return (T)this.endOfData();
   }
}
