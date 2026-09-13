package net.minecraft.world.level.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Node {
   public final int x;
   public final int y;
   public final int z;
   private final int hash;
   public int heapIdx = -1;
   public float g;
   public float h;
   public float f;
   public @Nullable Node cameFrom;
   public boolean closed;
   public float walkedDistance;
   public float costMalus;
   public PathType type = PathType.BLOCKED;

   public Node(int p_77285_, int p_77286_, int p_77287_) {
      this.x = p_77285_;
      this.y = p_77286_;
      this.z = p_77287_;
      this.hash = createHash(p_77285_, p_77286_, p_77287_);
   }

   public Node cloneAndMove(int p_77290_, int p_77291_, int p_77292_) {
      Node node = new Node(p_77290_, p_77291_, p_77292_);
      node.heapIdx = this.heapIdx;
      node.g = this.g;
      node.h = this.h;
      node.f = this.f;
      node.cameFrom = this.cameFrom;
      node.closed = this.closed;
      node.walkedDistance = this.walkedDistance;
      node.costMalus = this.costMalus;
      node.type = this.type;
      return node;
   }

   public static int createHash(int p_77296_, int p_77297_, int p_77298_) {
      return p_77297_ & 0xFF | (p_77296_ & 32767) << 8 | (p_77298_ & 32767) << 24 | (p_77296_ < 0 ? Integer.MIN_VALUE : 0) | (p_77298_ < 0 ? 32768 : 0);
   }

   public float distanceTo(Node p_77294_) {
      float f = p_77294_.x - this.x;
      float f1 = p_77294_.y - this.y;
      float f2 = p_77294_.z - this.z;
      return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
   }

   public float distanceToXZ(Node p_230614_) {
      float f = p_230614_.x - this.x;
      float f1 = p_230614_.z - this.z;
      return Mth.sqrt(f * f + f1 * f1);
   }

   public float distanceTo(BlockPos p_164698_) {
      float f = p_164698_.getX() - this.x;
      float f1 = p_164698_.getY() - this.y;
      float f2 = p_164698_.getZ() - this.z;
      return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
   }

   public float distanceToSqr(Node p_77300_) {
      float f = p_77300_.x - this.x;
      float f1 = p_77300_.y - this.y;
      float f2 = p_77300_.z - this.z;
      return f * f + f1 * f1 + f2 * f2;
   }

   public float distanceToSqr(BlockPos p_164703_) {
      float f = p_164703_.getX() - this.x;
      float f1 = p_164703_.getY() - this.y;
      float f2 = p_164703_.getZ() - this.z;
      return f * f + f1 * f1 + f2 * f2;
   }

   public float distanceManhattan(Node p_77305_) {
      float f = Math.abs(p_77305_.x - this.x);
      float f1 = Math.abs(p_77305_.y - this.y);
      float f2 = Math.abs(p_77305_.z - this.z);
      return f + f1 + f2;
   }

   public float distanceManhattan(BlockPos p_77307_) {
      float f = Math.abs(p_77307_.getX() - this.x);
      float f1 = Math.abs(p_77307_.getY() - this.y);
      float f2 = Math.abs(p_77307_.getZ() - this.z);
      return f + f1 + f2;
   }

   public BlockPos asBlockPos() {
      return new BlockPos(this.x, this.y, this.z);
   }

   public Vec3 asVec3() {
      return new Vec3(this.x, this.y, this.z);
   }

   @Override
   public boolean equals(Object p_77309_) {
      return p_77309_ instanceof Node node ? this.hash == node.hash && this.x == node.x && this.y == node.y && this.z == node.z : false;
   }

   @Override
   public int hashCode() {
      return this.hash;
   }

   public boolean inOpenSet() {
      return this.heapIdx >= 0;
   }

   @Override
   public String toString() {
      return "Node{x=" + this.x + ", y=" + this.y + ", z=" + this.z + "}";
   }

   public void writeToStream(FriendlyByteBuf p_164700_) {
      p_164700_.writeInt(this.x);
      p_164700_.writeInt(this.y);
      p_164700_.writeInt(this.z);
      p_164700_.writeFloat(this.walkedDistance);
      p_164700_.writeFloat(this.costMalus);
      p_164700_.writeBoolean(this.closed);
      p_164700_.writeEnum(this.type);
      p_164700_.writeFloat(this.f);
   }

   public static Node createFromStream(FriendlyByteBuf p_77302_) {
      Node node = new Node(p_77302_.readInt(), p_77302_.readInt(), p_77302_.readInt());
      readContents(p_77302_, node);
      return node;
   }

   protected static void readContents(FriendlyByteBuf p_262984_, Node p_263009_) {
      p_263009_.walkedDistance = p_262984_.readFloat();
      p_263009_.costMalus = p_262984_.readFloat();
      p_263009_.closed = p_262984_.readBoolean();
      p_263009_.type = p_262984_.readEnum(PathType.class);
      p_263009_.f = p_262984_.readFloat();
   }
}
