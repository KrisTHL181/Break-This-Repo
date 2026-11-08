package net.minecraft.world.entity;

public interface PlayerRideableJumping extends PlayerRideable {
   void onPlayerJump(int jumpAmount);

   boolean canJump();

   void handleStartJump(int jumpScale);

   void handleStopJump();

   default int getJumpCooldown() {
      return 0;
   }

   default float getPlayerJumpPendingScale(final int jumpAmount) {
      return jumpAmount >= 90 ? 1.0F : 0.4F + 0.4F * jumpAmount / 90.0F;
   }
}
