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
   public static final StreamCodec<FriendlyByteBuf, Path> STREAM_CODEC = StreamCodec.of((output, value) -> value.writeToStream(output), Path::createFromStream);
   private final List<Node> nodes;
   private Path.@Nullable DebugData debugData;
   private int nextNodeIndex;
   private final BlockPos target;
   private final float distToTarget;
   private final boolean reached;

   public Path(final List<Node> nodes, final BlockPos target, final boolean reached) {
      this.nodes = nodes;
      this.target = target;
      this.distToTarget = nodes.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
      this.reached = reached;
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

   public Node getNode(final int i) {
      return this.nodes.get(i);
   }

   public void truncateNodes(final int index) {
      if (this.nodes.size() > index) {
         this.nodes.subList(index, this.nodes.size()).clear();
      }
   }

   public void replaceNode(final int index, final Node replaceWith) {
      this.nodes.set(index, replaceWith);
   }

   public int getNodeCount() {
      return this.nodes.size();
   }

   public int getNextNodeIndex() {
      return this.nextNodeIndex;
   }

   public void setNextNodeIndex(final int nextNodeIndex) {
      this.nextNodeIndex = nextNodeIndex;
   }

   public Vec3 getEntityPosAtNode(final Entity entity, final int index) {
      Node node = this.nodes.get(index);
      double x = node.x + (int)(entity.getBbWidth() + 1.0F) * 0.5;
      double y = node.y;
      double z = node.z + (int)(entity.getBbWidth() + 1.0F) * 0.5;
      return new Vec3(x, y, z);
   }

   public BlockPos getNodePos(final int index) {
      return this.nodes.get(index).asBlockPos();
   }

   public Vec3 getNextEntityPos(final Entity entity) {
      return this.getEntityPosAtNode(entity, this.nextNodeIndex);
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

   public boolean sameAs(final @Nullable Path path) {
      return path != null && this.nodes.equals(path.nodes);
   }

   @Override
   public boolean equals(final Object obj) {
      return !(obj instanceof Path path)
         ? false
         : this.nextNodeIndex == path.nextNodeIndex
            && this.debugData == path.debugData
            && this.reached == path.reached
            && this.target.equals(path.target)
            && this.nodes.equals(path.nodes);
   }

   @Override
   public int hashCode() {
      return this.nextNodeIndex + this.nodes.hashCode() * 31;
   }

   public boolean canReach() {
      return this.reached;
   }

   @VisibleForDebug
   void setDebug(final Node[] openSet, final Node[] closedSet, final Set<Target> targets) {
      this.debugData = new Path.DebugData(openSet, closedSet, targets);
   }

   public Path.@Nullable DebugData debugData() {
      return this.debugData;
   }

   public void writeToStream(final FriendlyByteBuf buffer) {
      if (this.debugData != null && !this.debugData.targetNodes.isEmpty()) {
         buffer.writeBoolean(this.reached);
         buffer.writeInt(this.nextNodeIndex);
         buffer.writeBlockPos(this.target);
         buffer.writeCollection(this.nodes, (out, node) -> node.writeToStream(out));
         this.debugData.write(buffer);
      } else {
         throw new IllegalStateException("Missing debug data");
      }
   }

   public static Path createFromStream(final FriendlyByteBuf buffer) {
      boolean reached = buffer.readBoolean();
      int indexStream = buffer.readInt();
      BlockPos target = buffer.readBlockPos();
      List<Node> nodes = buffer.readList(Node::createFromStream);
      Path.DebugData debugData = Path.DebugData.read(buffer);
      Path path = new Path(nodes, target, reached);
      path.debugData = debugData;
      path.nextNodeIndex = indexStream;
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

   private static Node[] readNodeArray(final FriendlyByteBuf input) {
      Node[] nodes = new Node[input.readVarInt()];

      for (int i = 0; i < nodes.length; i++) {
         nodes[i] = Node.createFromStream(input);
      }

      return nodes;
   }

   private static void writeNodeArray(final FriendlyByteBuf output, final Node[] nodes) {
      output.writeVarInt(nodes.length);

      for (Node node : nodes) {
         node.writeToStream(output);
      }
   }

   public Path copy() {
      Path result = new Path(this.nodes, this.target, this.reached);
      result.debugData = this.debugData;
      result.nextNodeIndex = this.nextNodeIndex;
      return result;
   }

   public record DebugData(Node[] openSet, Node[] closedSet, Set<Target> targetNodes) {
      public void write(final FriendlyByteBuf output) {
         output.writeCollection(this.targetNodes, (out, target) -> target.writeToStream(out));
         Path.writeNodeArray(output, this.openSet);
         Path.writeNodeArray(output, this.closedSet);
      }

      public static Path.DebugData read(final FriendlyByteBuf input) {
         HashSet<Target> targets = input.readCollection(HashSet::new, Target::createFromStream);
         Node[] openSet = Path.readNodeArray(input);
         Node[] closedSet = Path.readNodeArray(input);
         return new Path.DebugData(openSet, closedSet, targets);
      }
   }
}
