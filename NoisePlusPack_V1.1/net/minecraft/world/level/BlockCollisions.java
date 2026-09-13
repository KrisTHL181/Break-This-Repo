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
      CollisionGetter p_286817_, @Nullable Entity p_286246_, AABB p_286624_, boolean p_286354_, BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> p_286303_
   ) {
      this(p_286817_, p_286246_ == null ? CollisionContext.empty() : CollisionContext.of(p_286246_), p_286624_, p_286354_, p_286303_);
   }

   public BlockCollisions(
      CollisionGetter p_367210_, CollisionContext p_366924_, AABB p_365384_, boolean p_365770_, BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> p_361273_
   ) {
      this.context = p_366924_;
      this.pos = new BlockPos.MutableBlockPos();
      this.entityShape = Shapes.create(p_365384_);
      this.collisionGetter = p_367210_;
      this.box = p_365384_;
      this.onlySuffocatingBlocks = p_365770_;
      this.resultProvider = p_361273_;
      int i = Mth.floor(p_365384_.minX - 1.0E-7) - 1;
      int j = Mth.floor(p_365384_.maxX + 1.0E-7) + 1;
      int k = Mth.floor(p_365384_.minY - 1.0E-7) - 1;
      int l = Mth.floor(p_365384_.maxY + 1.0E-7) + 1;
      int i1 = Mth.floor(p_365384_.minZ - 1.0E-7) - 1;
      int j1 = Mth.floor(p_365384_.maxZ + 1.0E-7) + 1;
      this.cursor = new Cursor3D(i, k, i1, j, l, j1);
   }

   private @Nullable BlockGetter getChunk(int p_186412_, int p_186413_) {
      int i = SectionPos.blockToSectionCoord(p_186412_);
      int j = SectionPos.blockToSectionCoord(p_186413_);
      long k = ChunkPos.asLong(i, j);
      if (this.cachedBlockGetter != null && this.cachedBlockGetterPos == k) {
         return this.cachedBlockGetter;
      }

      BlockGetter blockgetter = this.collisionGetter.getChunkForCollisions(i, j);
      this.cachedBlockGetter = blockgetter;
      this.cachedBlockGetterPos = k;
      return blockgetter;
   }

   protected T computeNext() {
      while (this.cursor.advance()) {
         int i = this.cursor.nextX();
         int j = this.cursor.nextY();
         int k = this.cursor.nextZ();
         int l = this.cursor.getNextType();
         if (l != 3) {
            BlockGetter blockgetter = this.getChunk(i, k);
            if (blockgetter != null) {
               this.pos.set(i, j, k);
               BlockState blockstate = blockgetter.getBlockState(this.pos);
               if ((!this.onlySuffocatingBlocks || blockstate.isSuffocating(blockgetter, this.pos))
                  && (l != 1 || blockstate.hasLargeCollisionShape())
                  && (l != 2 || blockstate.is(Blocks.MOVING_PISTON))) {
                  VoxelShape voxelshape = this.context.getCollisionShape(blockstate, this.collisionGetter, this.pos);
                  if (voxelshape == Shapes.block()) {
                     if (this.box.intersects(i, j, k, i + 1.0, j + 1.0, k + 1.0)) {
                        return this.resultProvider.apply(this.pos, voxelshape.move(this.pos));
                     }
                  } else {
                     VoxelShape voxelshape1 = voxelshape.move(this.pos);
                     if (!voxelshape1.isEmpty() && Shapes.joinIsNotEmpty(voxelshape1, this.entityShape, BooleanOp.AND)) {
                        return this.resultProvider.apply(this.pos, voxelshape1);
                     }
                  }
               }
            }
         }
      }

      return (T)this.endOfData();
   }
}
