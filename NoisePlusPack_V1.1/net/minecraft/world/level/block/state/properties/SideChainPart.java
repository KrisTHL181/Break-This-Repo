package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum SideChainPart implements StringRepresentable {
   UNCONNECTED("unconnected"),
   RIGHT("right"),
   CENTER("center"),
   LEFT("left");

   private final String name;

   SideChainPart(final String p_424443_) {
      this.name = p_424443_;
   }

   @Override
   public String toString() {
      return this.getSerializedName();
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public boolean isConnected() {
      return this != UNCONNECTED;
   }

   public boolean isConnectionTowards(SideChainPart p_427898_) {
      return this == CENTER || this == p_427898_;
   }

   public boolean isChainEnd() {
      return this != CENTER;
   }

   public SideChainPart whenConnectedToTheRight() {
      return switch (this) {
         case UNCONNECTED, LEFT -> LEFT;
         case RIGHT, CENTER -> CENTER;
      };
   }

   public SideChainPart whenConnectedToTheLeft() {
      return switch (this) {
         case UNCONNECTED, RIGHT -> RIGHT;
         case CENTER, LEFT -> CENTER;
      };
   }

   public SideChainPart whenDisconnectedFromTheRight() {
      return switch (this) {
         case UNCONNECTED, LEFT -> UNCONNECTED;
         case RIGHT, CENTER -> RIGHT;
      };
   }

   public SideChainPart whenDisconnectedFromTheLeft() {
      return switch (this) {
         case UNCONNECTED, RIGHT -> UNCONNECTED;
         case CENTER, LEFT -> LEFT;
      };
   }
}
