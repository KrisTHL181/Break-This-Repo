package net.minecraft.world.level.pathfinder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class Path {
   public static final StreamCodec<FriendlyByteBuf, Path> STREAM_CODEC = StreamCodec.of(
      (p_422249_, p_422250_) -> p_422250_.writeToStream(p_422249_), Path::createFromStream
   );
   private final List<Node> nodes;
   private Path.@Nullable DebugData debugData;
   private int nextNodeIndex;
   private final BlockPos target;
   private final float distToTarget;
   private final boolean reached;

   public Path(List<Node> p_77371_, BlockPos p_77372_, boolean p_77373_) {
      this.nodes = p_77371_;
      this.target = p_77372_;
      this.distToTarget = p_77371_.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
      this.reached = p_77373_;
   }

   public void advance() {
      this.nextNodeIndex++;
   }

   public boolean notStarted() {
      return this.nextNodeIndex <= 0;
   }

   public boolean isDone() {
      return this.nextNodeIndex >= this.nodes.size();
   }

   public @Nullable Node getEndNode() {
      return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1) : null;
   }

   public Node getNode(int p_77376_) {
      return this.nodes.get(p_77376_);
   }

   public void truncateNodes(int p_77389_) {
      if (this.nodes.size() > p_77389_) {
         this.nodes.subList(p_77389_, this.nodes.size()).clear();
      }
   }

   public void replaceNode(int p_77378_, Node p_77379_) {
      this.nodes.set(p_77378_, p_77379_);
   }

   public int getNodeCount() {
      return this.nodes.size();
   }

   public int getNextNodeIndex() {
      return this.nextNodeIndex;
   }

   public void setNextNodeIndex(int p_77394_) {
      this.nextNodeIndex = p_77394_;
   }

   public Vec3 getEntityPosAtNode(Entity p_77383_, int p_77384_) {
      Node node = this.nodes.get(p_77384_);
      double d0 = node.x + (int)(p_77383_.getBbWidth() + 1.0F) * 0.5;
      double d1 = node.y;
      double d2 = node.z + (int)(p_77383_.getBbWidth() + 1.0F) * 0.5;
      return new Vec3(d0, d1, d2);
   }

   public BlockPos getNodePos(int p_77397_) {
      return this.nodes.get(p_77397_).asBlockPos();
   }

   public Vec3 getNextEntityPos(Entity p_77381_) {
      return this.getEntityPosAtNode(p_77381_, this.nextNodeIndex);
   }

   public BlockPos getNextNodePos() {
      return this.nodes.get(this.nextNodeIndex).asBlockPos();
   }

   public Node getNextNode() {
      return this.nodes.get(this.nextNodeIndex);
   }

   public @Nullable Node getPreviousNode() {
      return this.nextNodeIndex > 0 ? this.nodes.get(this.nextNodeIndex - 1) : null;
   }

   public boolean sameAs(@Nullable Path p_77386_) {
      return p_77386_ != null && this.nodes.equals(p_77386_.nodes);
   }

   @Override
   public boolean equals(Object p_425951_) {
      return p_425951_ instanceof Path path
         ? this.nextNodeIndex == path.nextNodeIndex
            && this.debugData == path.debugData
            && this.reached == path.reached
            && this.target.equals(path.target)
            && this.nodes.equals(path.nodes)
         : false;
   }

   @Override
   public int hashCode() {
      return this.nextNodeIndex + this.nodes.hashCode() * 31;
   }

   public boolean canReach() {
      return this.reached;
   }

   @VisibleForDebug
   void setDebug(Node[] p_164710_, Node[] p_164711_, Set<Target> p_164712_) {
      this.debugData = new Path.DebugData(p_164710_, p_164711_, p_164712_);
   }

   public Path.@Nullable DebugData debugData() {
      return this.debugData;
   }

   public void writeToStream(FriendlyByteBuf p_164705_) {
      if (this.debugData != null && !this.debugData.targetNodes.isEmpty()) {
         p_164705_.writeBoolean(this.reached);
         p_164705_.writeInt(this.nextNodeIndex);
         p_164705_.writeBlockPos(this.target);
         p_164705_.writeCollection(this.nodes, (p_296986_, p_296987_) -> p_296987_.writeToStream(p_296986_));
         this.debugData.write(p_164705_);
      } else {
         throw new IllegalStateException("Missing debug data");
      }
   }

   public static Path createFromStream(FriendlyByteBuf p_77391_) {
      boolean flag = p_77391_.readBoolean();
      int i = p_77391_.readInt();
      BlockPos blockpos = p_77391_.readBlockPos();
      List<Node> list = p_77391_.readList(Node::createFromStream);
      Path.DebugData path$debugdata = Path.DebugData.read(p_77391_);
      Path path = new Path(list, blockpos, flag);
      path.debugData = path$debugdata;
      path.nextNodeIndex = i;
      return path;
   }

   @Override
   public String toString() {
      return "Path(length=" + this.nodes.size() + ")";
   }

   public BlockPos getTarget() {
      return this.target;
   }

   public float getDistToTarget() {
      return this.distToTarget;
   }

   static Node[] readNodeArray(FriendlyByteBuf p_300065_) {
      Node[] anode = new Node[p_300065_.readVarInt()];

      for (int i = 0; i < anode.length; i++) {
         anode[i] = Node.createFromStream(p_300065_);
      }

      return anode;
   }

   static void writeNodeArray(FriendlyByteBuf p_300557_, Node[] p_301136_) {
      p_300557_.writeVarInt(p_301136_.length);

      for (Node node : p_301136_) {
         node.writeToStream(p_300557_);
      }
   }

   public Path copy() {
      Path path = new Path(this.nodes, this.target, this.reached);
      path.debugData = this.debugData;
      path.nextNodeIndex = this.nextNodeIndex;
      return path;
   }

   public record DebugData(Node[] openSet, Node[] closedSet, Set<Target> targetNodes) {
      public void write(FriendlyByteBuf p_299578_) {
         p_299578_.writeCollection(this.targetNodes, (p_300614_, p_300157_) -> p_300157_.writeToStream(p_300614_));
         Path.writeNodeArray(p_299578_, this.openSet);
         Path.writeNodeArray(p_299578_, this.closedSet);
      }

      public static Path.DebugData read(FriendlyByteBuf p_299429_) {
         HashSet<Target> hashset = p_299429_.readCollection(HashSet::new, Target::createFromStream);
         Node[] anode = Path.readNodeArray(p_299429_);
         Node[] anode1 = Path.readNodeArray(p_299429_);
         return new Path.DebugData(anode, anode1, hashset);
      }
   }
}
