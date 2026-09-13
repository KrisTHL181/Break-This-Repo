package net.minecraft.world.level.pathfinder;

import net.minecraft.network.FriendlyByteBuf;

public class Target extends Node {
   private float bestHeuristic = Float.MAX_VALUE;
   private Node bestNode;
   private boolean reached;

   public Target(final Node node) {
      super(node.x, node.y, node.z);
   }

   public Target(final int x, final int y, final int z) {
      super(x, y, z);
   }

   public void updateBest(final float heuristic, final Node node) {
      if (heuristic < this.bestHeuristic) {
         this.bestHeuristic = heuristic;
         this.bestNode = node;
      }
   }

   public Node getBestNode() {
      return this.bestNode;
   }

   public void setReached() {
      this.reached = true;
   }

   public boolean isReached() {
      return this.reached;
   }

   public static Target createFromStream(final FriendlyByteBuf buffer) {
      Target node = new Target(buffer.readInt(), buffer.readInt(), buffer.readInt());
      readContents(buffer, node);
      return node;
   }
}
