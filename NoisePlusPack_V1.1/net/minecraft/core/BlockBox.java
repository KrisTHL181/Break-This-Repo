package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;

public record BlockBox(BlockPos min, BlockPos max) implements Iterable<BlockPos> {
   public static final StreamCodec<ByteBuf, BlockBox> STREAM_CODEC = new StreamCodec<ByteBuf, BlockBox>() {
      public BlockBox decode(ByteBuf p_328358_) {
         return new BlockBox(FriendlyByteBuf.readBlockPos(p_328358_), FriendlyByteBuf.readBlockPos(p_328358_));
      }

      public void encode(ByteBuf p_335006_, BlockBox p_331887_) {
         FriendlyByteBuf.writeBlockPos(p_335006_, p_331887_.min());
         FriendlyByteBuf.writeBlockPos(p_335006_, p_331887_.max());
      }
   };

   public BlockBox(final BlockPos min, final BlockPos max) {
      this.min = BlockPos.min(min, max);
      this.max = BlockPos.max(min, max);
   }

   public static BlockBox of(BlockPos p_333581_) {
      return new BlockBox(p_333581_, p_333581_);
   }

   public static BlockBox of(BlockPos p_333861_, BlockPos p_330004_) {
      return new BlockBox(p_333861_, p_330004_);
   }

   public BlockBox include(BlockPos p_330504_) {
      return new BlockBox(BlockPos.min(this.min, p_330504_), BlockPos.max(this.max, p_330504_));
   }

   public boolean isBlock() {
      return this.min.equals(this.max);
   }

   public boolean contains(BlockPos p_327940_) {
      return p_327940_.getX() >= this.min.getX()
         && p_327940_.getY() >= this.min.getY()
         && p_327940_.getZ() >= this.min.getZ()
         && p_327940_.getX() <= this.max.getX()
         && p_327940_.getY() <= this.max.getY()
         && p_327940_.getZ() <= this.max.getZ();
   }

   public AABB aabb() {
      return AABB.encapsulatingFullBlocks(this.min, this.max);
   }

   @Override
   public Iterator<BlockPos> iterator() {
      return BlockPos.betweenClosed(this.min, this.max).iterator();
   }

   public int sizeX() {
      return this.max.getX() - this.min.getX() + 1;
   }

   public int sizeY() {
      return this.max.getY() - this.min.getY() + 1;
   }

   public int sizeZ() {
      return this.max.getZ() - this.min.getZ() + 1;
   }

   public BlockBox extend(Direction p_336349_, int p_329831_) {
      if (p_329831_ == 0) {
         return this;
      } else {
         return p_336349_.getAxisDirection() == Direction.AxisDirection.POSITIVE
            ? of(this.min, BlockPos.max(this.min, this.max.relative(p_336349_, p_329831_)))
            : of(BlockPos.min(this.min.relative(p_336349_, p_329831_), this.max), this.max);
      }
   }

   public BlockBox move(Direction p_335445_, int p_328653_) {
      return p_328653_ == 0 ? this : new BlockBox(this.min.relative(p_335445_, p_328653_), this.max.relative(p_335445_, p_328653_));
   }

   public BlockBox offset(Vec3i p_327763_) {
      return new BlockBox(this.min.offset(p_327763_), this.max.offset(p_327763_));
   }
}
