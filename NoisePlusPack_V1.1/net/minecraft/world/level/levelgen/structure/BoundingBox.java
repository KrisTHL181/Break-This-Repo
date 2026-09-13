package net.minecraft.world.level.levelgen.structure;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class BoundingBox {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<BoundingBox> CODEC = Codec.INT_STREAM
      .comapFlatMap(
         p_450006_ -> Util.fixedSize(p_450006_, 6)
            .map(p_162385_ -> new BoundingBox(p_162385_[0], p_162385_[1], p_162385_[2], p_162385_[3], p_162385_[4], p_162385_[5])),
         p_162391_ -> IntStream.of(p_162391_.minX, p_162391_.minY, p_162391_.minZ, p_162391_.maxX, p_162391_.maxY, p_162391_.maxZ)
      )
      .stable();
   public static final StreamCodec<ByteBuf, BoundingBox> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      p_422238_ -> new BlockPos(p_422238_.minX, p_422238_.minY, p_422238_.minZ),
      BlockPos.STREAM_CODEC,
      p_422239_ -> new BlockPos(p_422239_.maxX, p_422239_.maxY, p_422239_.maxZ),
      (p_422240_, p_422241_) -> new BoundingBox(p_422240_.getX(), p_422240_.getY(), p_422240_.getZ(), p_422241_.getX(), p_422241_.getY(), p_422241_.getZ())
   );
   private int minX;
   private int minY;
   private int minZ;
   private int maxX;
   private int maxY;
   private int maxZ;

   public BoundingBox(BlockPos p_162364_) {
      this(p_162364_.getX(), p_162364_.getY(), p_162364_.getZ(), p_162364_.getX(), p_162364_.getY(), p_162364_.getZ());
   }

   public BoundingBox(int p_71001_, int p_71002_, int p_71003_, int p_71004_, int p_71005_, int p_71006_) {
      this.minX = p_71001_;
      this.minY = p_71002_;
      this.minZ = p_71003_;
      this.maxX = p_71004_;
      this.maxY = p_71005_;
      this.maxZ = p_71006_;
      if (p_71004_ < p_71001_ || p_71005_ < p_71002_ || p_71006_ < p_71003_) {
         Util.logAndPauseIfInIde("Invalid bounding box data, inverted bounds for: " + this);
         this.minX = Math.min(p_71001_, p_71004_);
         this.minY = Math.min(p_71002_, p_71005_);
         this.minZ = Math.min(p_71003_, p_71006_);
         this.maxX = Math.max(p_71001_, p_71004_);
         this.maxY = Math.max(p_71002_, p_71005_);
         this.maxZ = Math.max(p_71003_, p_71006_);
      }
   }

   public static BoundingBox fromCorners(Vec3i p_162376_, Vec3i p_162377_) {
      return new BoundingBox(
         Math.min(p_162376_.getX(), p_162377_.getX()),
         Math.min(p_162376_.getY(), p_162377_.getY()),
         Math.min(p_162376_.getZ(), p_162377_.getZ()),
         Math.max(p_162376_.getX(), p_162377_.getX()),
         Math.max(p_162376_.getY(), p_162377_.getY()),
         Math.max(p_162376_.getZ(), p_162377_.getZ())
      );
   }

   public static BoundingBox infinite() {
      return new BoundingBox(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
   }

   public static BoundingBox orientBox(
      int p_71032_, int p_71033_, int p_71034_, int p_71035_, int p_71036_, int p_71037_, int p_71038_, int p_71039_, int p_71040_, Direction p_71041_
   ) {
      switch (p_71041_) {
         case SOUTH:
         default:
            return new BoundingBox(
               p_71032_ + p_71035_,
               p_71033_ + p_71036_,
               p_71034_ + p_71037_,
               p_71032_ + p_71038_ - 1 + p_71035_,
               p_71033_ + p_71039_ - 1 + p_71036_,
               p_71034_ + p_71040_ - 1 + p_71037_
            );
         case NORTH:
            return new BoundingBox(
               p_71032_ + p_71035_,
               p_71033_ + p_71036_,
               p_71034_ - p_71040_ + 1 + p_71037_,
               p_71032_ + p_71038_ - 1 + p_71035_,
               p_71033_ + p_71039_ - 1 + p_71036_,
               p_71034_ + p_71037_
            );
         case WEST:
            return new BoundingBox(
               p_71032_ - p_71040_ + 1 + p_71037_,
               p_71033_ + p_71036_,
               p_71034_ + p_71035_,
               p_71032_ + p_71037_,
               p_71033_ + p_71039_ - 1 + p_71036_,
               p_71034_ + p_71038_ - 1 + p_71035_
            );
         case EAST:
            return new BoundingBox(
               p_71032_ + p_71037_,
               p_71033_ + p_71036_,
               p_71034_ + p_71035_,
               p_71032_ + p_71040_ - 1 + p_71037_,
               p_71033_ + p_71039_ - 1 + p_71036_,
               p_71034_ + p_71038_ - 1 + p_71035_
            );
      }
   }

   public Stream<ChunkPos> intersectingChunks() {
      int i = SectionPos.blockToSectionCoord(this.minX());
      int j = SectionPos.blockToSectionCoord(this.minZ());
      int k = SectionPos.blockToSectionCoord(this.maxX());
      int l = SectionPos.blockToSectionCoord(this.maxZ());
      return ChunkPos.rangeClosed(new ChunkPos(i, j), new ChunkPos(k, l));
   }

   public boolean intersects(BoundingBox p_71050_) {
      return this.maxX >= p_71050_.minX
         && this.minX <= p_71050_.maxX
         && this.maxZ >= p_71050_.minZ
         && this.minZ <= p_71050_.maxZ
         && this.maxY >= p_71050_.minY
         && this.minY <= p_71050_.maxY;
   }

   public boolean intersects(int p_71020_, int p_71021_, int p_71022_, int p_71023_) {
      return this.maxX >= p_71020_ && this.minX <= p_71022_ && this.maxZ >= p_71021_ && this.minZ <= p_71023_;
   }

   public static Optional<BoundingBox> encapsulatingPositions(Iterable<BlockPos> p_162379_) {
      Iterator<BlockPos> iterator = p_162379_.iterator();
      if (!iterator.hasNext()) {
         return Optional.empty();
      }

      BoundingBox boundingbox = new BoundingBox(iterator.next());
      iterator.forEachRemaining(boundingbox::encapsulate);
      return Optional.of(boundingbox);
   }

   public static Optional<BoundingBox> encapsulatingBoxes(Iterable<BoundingBox> p_162389_) {
      Iterator<BoundingBox> iterator = p_162389_.iterator();
      if (!iterator.hasNext()) {
         return Optional.empty();
      }

      BoundingBox boundingbox = iterator.next();
      BoundingBox boundingbox1 = new BoundingBox(boundingbox.minX, boundingbox.minY, boundingbox.minZ, boundingbox.maxX, boundingbox.maxY, boundingbox.maxZ);
      iterator.forEachRemaining(boundingbox1::encapsulate);
      return Optional.of(boundingbox1);
   }

   @Deprecated
   public BoundingBox encapsulate(BoundingBox p_162387_) {
      this.minX = Math.min(this.minX, p_162387_.minX);
      this.minY = Math.min(this.minY, p_162387_.minY);
      this.minZ = Math.min(this.minZ, p_162387_.minZ);
      this.maxX = Math.max(this.maxX, p_162387_.maxX);
      this.maxY = Math.max(this.maxY, p_162387_.maxY);
      this.maxZ = Math.max(this.maxZ, p_162387_.maxZ);
      return this;
   }

   public static BoundingBox encapsulating(BoundingBox p_430284_, BoundingBox p_427209_) {
      return new BoundingBox(
         Math.min(p_430284_.minX, p_427209_.minX),
         Math.min(p_430284_.minY, p_427209_.minY),
         Math.min(p_430284_.minZ, p_427209_.minZ),
         Math.max(p_430284_.maxX, p_427209_.maxX),
         Math.max(p_430284_.maxY, p_427209_.maxY),
         Math.max(p_430284_.maxZ, p_427209_.maxZ)
      );
   }

   @Deprecated
   public BoundingBox encapsulate(BlockPos p_162372_) {
      this.minX = Math.min(this.minX, p_162372_.getX());
      this.minY = Math.min(this.minY, p_162372_.getY());
      this.minZ = Math.min(this.minZ, p_162372_.getZ());
      this.maxX = Math.max(this.maxX, p_162372_.getX());
      this.maxY = Math.max(this.maxY, p_162372_.getY());
      this.maxZ = Math.max(this.maxZ, p_162372_.getZ());
      return this;
   }

   @Deprecated
   public BoundingBox move(int p_162368_, int p_162369_, int p_162370_) {
      this.minX += p_162368_;
      this.minY += p_162369_;
      this.minZ += p_162370_;
      this.maxX += p_162368_;
      this.maxY += p_162369_;
      this.maxZ += p_162370_;
      return this;
   }

   @Deprecated
   public BoundingBox move(Vec3i p_162374_) {
      return this.move(p_162374_.getX(), p_162374_.getY(), p_162374_.getZ());
   }

   public BoundingBox moved(int p_71046_, int p_71047_, int p_71048_) {
      return new BoundingBox(this.minX + p_71046_, this.minY + p_71047_, this.minZ + p_71048_, this.maxX + p_71046_, this.maxY + p_71047_, this.maxZ + p_71048_);
   }

   public BoundingBox inflatedBy(int p_191962_) {
      return this.inflatedBy(p_191962_, p_191962_, p_191962_);
   }

   public BoundingBox inflatedBy(int p_332684_, int p_332721_, int p_329326_) {
      return new BoundingBox(
         this.minX() - p_332684_, this.minY() - p_332721_, this.minZ() - p_329326_, this.maxX() + p_332684_, this.maxY() + p_332721_, this.maxZ() + p_329326_
      );
   }

   public boolean isInside(Vec3i p_71052_) {
      return this.isInside(p_71052_.getX(), p_71052_.getY(), p_71052_.getZ());
   }

   public boolean isInside(int p_261671_, int p_261537_, int p_261678_) {
      return p_261671_ >= this.minX
         && p_261671_ <= this.maxX
         && p_261678_ >= this.minZ
         && p_261678_ <= this.maxZ
         && p_261537_ >= this.minY
         && p_261537_ <= this.maxY;
   }

   public Vec3i getLength() {
      return new Vec3i(this.maxX - this.minX, this.maxY - this.minY, this.maxZ - this.minZ);
   }

   public int getXSpan() {
      return this.maxX - this.minX + 1;
   }

   public int getYSpan() {
      return this.maxY - this.minY + 1;
   }

   public int getZSpan() {
      return this.maxZ - this.minZ + 1;
   }

   public BlockPos getCenter() {
      return new BlockPos(this.minX + (this.maxX - this.minX + 1) / 2, this.minY + (this.maxY - this.minY + 1) / 2, this.minZ + (this.maxZ - this.minZ + 1) / 2);
   }

   public void forAllCorners(Consumer<BlockPos> p_162381_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      p_162381_.accept(blockpos$mutableblockpos.set(this.maxX, this.maxY, this.maxZ));
      p_162381_.accept(blockpos$mutableblockpos.set(this.minX, this.maxY, this.maxZ));
      p_162381_.accept(blockpos$mutableblockpos.set(this.maxX, this.minY, this.maxZ));
      p_162381_.accept(blockpos$mutableblockpos.set(this.minX, this.minY, this.maxZ));
      p_162381_.accept(blockpos$mutableblockpos.set(this.maxX, this.maxY, this.minZ));
      p_162381_.accept(blockpos$mutableblockpos.set(this.minX, this.maxY, this.minZ));
      p_162381_.accept(blockpos$mutableblockpos.set(this.maxX, this.minY, this.minZ));
      p_162381_.accept(blockpos$mutableblockpos.set(this.minX, this.minY, this.minZ));
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
         .add("minX", this.minX)
         .add("minY", this.minY)
         .add("minZ", this.minZ)
         .add("maxX", this.maxX)
         .add("maxY", this.maxY)
         .add("maxZ", this.maxZ)
         .toString();
   }

   @Override
   public boolean equals(Object p_162393_) {
      if (this == p_162393_) {
         return true;
      } else {
         return p_162393_ instanceof BoundingBox boundingbox
            ? this.minX == boundingbox.minX
               && this.minY == boundingbox.minY
               && this.minZ == boundingbox.minZ
               && this.maxX == boundingbox.maxX
               && this.maxY == boundingbox.maxY
               && this.maxZ == boundingbox.maxZ
            : false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   public int minX() {
      return this.minX;
   }

   public int minY() {
      return this.minY;
   }

   public int minZ() {
      return this.minZ;
   }

   public int maxX() {
      return this.maxX;
   }

   public int maxY() {
      return this.maxY;
   }

   public int maxZ() {
      return this.maxZ;
   }
}
