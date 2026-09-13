package net.minecraft.world.level.pathfinder;

import net.minecraft.network.FriendlyByteBuf;

public class Target extends Node {
   private float bestHeuristic = Float.MAX_VALUE;
   private Node bestNode;
   private boolean reached;

   public Target(Node p_77502_) {
      super(p_77502_.x, p_77502_.y, p_77502_.z);
   }

   public Target(int p_77498_, int p_77499_, int p_77500_) {
      super(p_77498_, p_77499_, p_77500_);
   }

   public void updateBest(float p_77504_, Node p_77505_) {
      if (p_77504_ < this.bestHeuristic) {
         this.bestHeuristic = p_77504_;
         this.bestNode = p_77505_;
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

   public static Target createFromStream(FriendlyByteBuf p_77507_) {
      Target target = new Target(p_77507_.readInt(), p_77507_.readInt(), p_77507_.readInt());
      readContents(p_77507_, target);
      return target;
   }
}
