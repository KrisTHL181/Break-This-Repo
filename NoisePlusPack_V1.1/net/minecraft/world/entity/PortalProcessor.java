package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

public class PortalProcessor {
   private final Portal portal;
   private BlockPos entryPosition;
   private int portalTime;
   private boolean insidePortalThisTick;

   public PortalProcessor(Portal p_343292_, BlockPos p_343694_) {
      this.portal = p_343292_;
      this.entryPosition = p_343694_;
      this.insidePortalThisTick = true;
   }

   public boolean processPortalTeleportation(ServerLevel p_342539_, Entity p_345125_, boolean p_342731_) {
      if (!this.insidePortalThisTick) {
         this.decayTick();
         return false;
      } else {
         this.insidePortalThisTick = false;
         return p_342731_ && this.portalTime++ >= this.portal.getPortalTransitionTime(p_342539_, p_345125_);
      }
   }

   public @Nullable TeleportTransition getPortalDestination(ServerLevel p_344411_, Entity p_342799_) {
      return this.portal.getPortalDestination(p_344411_, p_342799_, this.entryPosition);
   }

   public Portal.Transition getPortalLocalTransition() {
      return this.portal.getLocalTransition();
   }

   private void decayTick() {
      this.portalTime = Math.max(this.portalTime - 4, 0);
   }

   public boolean hasExpired() {
      return this.portalTime <= 0;
   }

   public BlockPos getEntryPosition() {
      return this.entryPosition;
   }

   public void updateEntryPosition(BlockPos p_344295_) {
      this.entryPosition = p_344295_;
   }

   public int getPortalTime() {
      return this.portalTime;
   }

   public boolean isInsidePortalThisTick() {
      return this.insidePortalThisTick;
   }

   public void setAsInsidePortalThisTick(boolean p_342092_) {
      this.insidePortalThisTick = p_342092_;
   }

   public boolean isSamePortal(Portal p_344740_) {
      return this.portal == p_344740_;
   }
}
